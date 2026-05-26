package com.inhatc.gaitcare.ui.elderly

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.inhatc.gaitcare.data.db.entity.GaitSession
import com.inhatc.gaitcare.databinding.ActivityElderlyDetailBinding
import com.inhatc.gaitcare.ui.gait.GaitMeasurementActivity
import com.inhatc.gaitcare.ui.gait.GaitResultActivity
import com.inhatc.gaitcare.viewmodel.GaitViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ElderlyDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityElderlyDetailBinding
    private val viewModel: GaitViewModel by viewModels()
    private lateinit var adapter: GaitHistoryAdapter

    private var elderlyId = -1L
    private var elderlyName = ""

    companion object {
        const val EXTRA_ELDERLY_ID = "extra_elderly_id"
        const val EXTRA_ELDERLY_NAME = "extra_elderly_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityElderlyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        elderlyId = intent.getLongExtra(EXTRA_ELDERLY_ID, -1L)
        elderlyName = intent.getStringExtra(EXTRA_ELDERLY_NAME) ?: ""

        if (elderlyId < 0) { finish(); return }

        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        binding.tvElderlyName.text = elderlyName
        binding.btnBack.setOnClickListener { finish() }

        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, AddElderlyActivity::class.java).apply {
                putExtra(AddElderlyActivity.EXTRA_ELDERLY_ID, elderlyId)
            }
            startActivity(intent)
        }

        binding.btnStartMeasurement.setOnClickListener {
            val intent = Intent(this, GaitMeasurementActivity::class.java).apply {
                putExtra(GaitMeasurementActivity.EXTRA_ELDERLY_ID, elderlyId)
                putExtra(GaitMeasurementActivity.EXTRA_ELDERLY_NAME, elderlyName)
            }
            startActivity(intent)
        }

        viewModel.setElderlyId(elderlyId)
    }

    private fun setupRecyclerView() {
        adapter = GaitHistoryAdapter(
            onItemClick = { session ->
                val intent = Intent(this, GaitResultActivity::class.java).apply {
                    putExtra(GaitResultActivity.EXTRA_SESSION_ID, session.id)
                    putExtra(GaitResultActivity.EXTRA_ELDERLY_NAME, elderlyName)
                    putExtra(GaitResultActivity.EXTRA_VIEW_ONLY, true)
                }
                startActivity(intent)
            },
            onDeleteClick = { session -> confirmDelete(session) }
        )
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.sessionList.collect { sessions ->
                adapter.submitList(sessions)

                if (sessions.isEmpty()) {
                    binding.tvNoHistory.visibility = android.view.View.VISIBLE
                    binding.rvHistory.visibility = android.view.View.GONE
                    binding.cardLatestScore.visibility = android.view.View.GONE
                } else {
                    binding.tvNoHistory.visibility = android.view.View.GONE
                    binding.rvHistory.visibility = android.view.View.VISIBLE
                    binding.cardLatestScore.visibility = android.view.View.VISIBLE

                    val latest = sessions.first()
                    binding.tvLatestScore.text = latest.totalScore.toString()
                    binding.tvLatestDate.text = formatDate(latest.measuredAt)
                    binding.tvLatestLabel.text = getScoreLabel(latest.totalScore)
                    binding.tvTotalCount.text = "${sessions.size}회 측정"

                    val avg = sessions.map { it.totalScore }.average()
                    binding.tvAvgScore.text = "평균 %.1f점".format(avg)
                }
            }
        }
    }

    private fun confirmDelete(session: GaitSession) {
        AlertDialog.Builder(this)
            .setTitle("기록 삭제")
            .setMessage("이 보행 기록을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ -> viewModel.deleteSession(session) }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun formatDate(timestampMs: Long): String {
        return SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(timestampMs))
    }

    private fun getScoreLabel(score: Int): String = when (score) {
        in 80..100 -> "매우 좋음"
        in 65..79 -> "좋음"
        in 50..64 -> "보통"
        in 35..49 -> "주의 필요"
        else -> "위험"
    }
}
