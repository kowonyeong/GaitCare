package com.inhatc.gaitcare.ui.gait

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.inhatc.gaitcare.R
import com.inhatc.gaitcare.data.db.AppDatabase
import com.inhatc.gaitcare.data.db.entity.GaitSession
import com.inhatc.gaitcare.databinding.ActivityGaitResultBinding
import com.inhatc.gaitcare.model.GaitResult
import com.inhatc.gaitcare.utils.PreferenceManager
import com.inhatc.gaitcare.viewmodel.GaitViewModel
import com.inhatc.gaitcare.viewmodel.SaveState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GaitResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGaitResultBinding
    private val viewModel: GaitViewModel by viewModels()
    private lateinit var prefs: PreferenceManager

    private var elderlyId = -1L
    private var elderlyName = ""
    private var sessionId = -1L
    private var isViewOnly = false
    private var result: GaitResult? = null

    companion object {
        const val EXTRA_ELDERLY_ID = "extra_elderly_id"
        const val EXTRA_ELDERLY_NAME = "extra_elderly_name"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_VIEW_ONLY = "extra_view_only"
        const val EXTRA_TOTAL_SCORE = "extra_total_score"
        const val EXTRA_SHAKINESS_SCORE = "extra_shakiness_score"
        const val EXTRA_RHYTHM_SCORE = "extra_rhythm_score"
        const val EXTRA_SYMMETRY_SCORE = "extra_symmetry_score"
        const val EXTRA_ROTATION_SCORE = "extra_rotation_score"
        const val EXTRA_CADENCE_SCORE = "extra_cadence_score"
        const val EXTRA_STEP_COUNT = "extra_step_count"
        const val EXTRA_CADENCE_SPM = "extra_cadence_spm"
        const val EXTRA_AVG_INTERVAL = "extra_avg_interval"
        const val EXTRA_CV_PERCENT = "extra_cv_percent"
        const val EXTRA_LATERAL_RMS = "extra_lateral_rms"
        const val EXTRA_VERTICAL_RMS = "extra_vertical_rms"
        const val EXTRA_SYMMETRY_INDEX = "extra_symmetry_index"
        const val EXTRA_ROTATION_DEG = "extra_rotation_deg"
        const val EXTRA_DURATION = "extra_duration"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGaitResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        elderlyId = intent.getLongExtra(EXTRA_ELDERLY_ID, -1L)
        elderlyName = intent.getStringExtra(EXTRA_ELDERLY_NAME) ?: ""
        sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        isViewOnly = intent.getBooleanExtra(EXTRA_VIEW_ONLY, false)

        binding.btnBack.setOnClickListener { finish() }

        if (isViewOnly && sessionId > 0) {
            loadFromDb()
        } else {
            loadFromIntent()
        }

        observeViewModel()
    }

    private fun loadFromIntent() {
        val r = GaitResult(
            totalScore = intent.getIntExtra(EXTRA_TOTAL_SCORE, 0),
            shakinessScore = intent.getIntExtra(EXTRA_SHAKINESS_SCORE, 0),
            rhythmScore = intent.getIntExtra(EXTRA_RHYTHM_SCORE, 0),
            symmetryScore = intent.getIntExtra(EXTRA_SYMMETRY_SCORE, 0),
            rotationScore = intent.getIntExtra(EXTRA_ROTATION_SCORE, 0),
            cadenceScore = intent.getIntExtra(EXTRA_CADENCE_SCORE, 0),
            stepCount = intent.getIntExtra(EXTRA_STEP_COUNT, 0),
            cadenceSpm = intent.getFloatExtra(EXTRA_CADENCE_SPM, 0f),
            avgStepIntervalMs = intent.getFloatExtra(EXTRA_AVG_INTERVAL, 0f),
            stepIntervalCvPercent = intent.getFloatExtra(EXTRA_CV_PERCENT, 0f),
            lateralRmsG = intent.getFloatExtra(EXTRA_LATERAL_RMS, 0f),
            verticalRmsG = intent.getFloatExtra(EXTRA_VERTICAL_RMS, 0f),
            symmetryIndexPercent = intent.getFloatExtra(EXTRA_SYMMETRY_INDEX, 0f),
            avgRotationDegSec = intent.getFloatExtra(EXTRA_ROTATION_DEG, 0f),
            durationSeconds = intent.getIntExtra(EXTRA_DURATION, 0)
        )
        result = r
        displayResult(r)
        setupSaveButtons(r)
    }

    private fun loadFromDb() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@GaitResultActivity)
            val session = withContext(Dispatchers.IO) { db.gaitSessionDao().findById(sessionId) }
            session?.let {
                val r = GaitResult(
                    totalScore = it.totalScore, shakinessScore = it.shakinessScore,
                    rhythmScore = it.rhythmScore, symmetryScore = it.symmetryScore,
                    rotationScore = it.rotationScore, cadenceScore = it.cadenceScore,
                    stepCount = it.stepCount, cadenceSpm = it.cadenceSpm,
                    avgStepIntervalMs = it.avgStepIntervalMs, stepIntervalCvPercent = it.stepIntervalCvPercent,
                    lateralRmsG = it.lateralRmsG, verticalRmsG = it.verticalRmsG,
                    symmetryIndexPercent = it.symmetryIndexPercent, avgRotationDegSec = it.avgRotationDegSec,
                    durationSeconds = it.durationSeconds
                )
                displayResult(r)
                displayDate(it.measuredAt)
                displayMemo(it.memo)
            }
        }
        binding.layoutSaveButtons.visibility = View.GONE
    }

    private fun displayResult(r: GaitResult) {
        binding.tvElderlyName.text = elderlyName
        binding.tvTotalScore.text = r.totalScore.toString()
        binding.tvScoreLabel.text = r.getScoreLabel()
        binding.tvDuration.text = "${r.durationSeconds}초 측정"

        val scoreColor = getScoreColor(r.totalScore)
        binding.tvTotalScore.setTextColor(ContextCompat.getColor(this, scoreColor))
        binding.tvScoreLabel.setTextColor(ContextCompat.getColor(this, scoreColor))

        // 세부 지표
        setMetricBar(binding.progressShakiness, binding.tvShakinessScore, r.shakinessScore)
        setMetricBar(binding.progressRhythm, binding.tvRhythmScore, r.rhythmScore)
        setMetricBar(binding.progressSymmetry, binding.tvSymmetryScore, r.symmetryScore)
        setMetricBar(binding.progressRotation, binding.tvRotationScore, r.rotationScore)
        setMetricBar(binding.progressCadence, binding.tvCadenceScore, r.cadenceScore)

        // 상세 수치
        binding.tvStepCount.text = "—"
        binding.tvCadenceValue.text = "%.3f g".format(r.cadenceSpm)
        binding.tvLateralRms.text = "%.3f g".format(r.lateralRmsG)
        binding.tvVerticalRms.text = "%.3f g".format(r.verticalRmsG)
        binding.tvSymmetryIndex.text = "%.1f%%".format(r.symmetryIndexPercent)
        binding.tvRotationDeg.text = "%.1f °/s".format(r.avgRotationDegSec)
        binding.tvCvPercent.text = "%.1f%%".format(r.stepIntervalCvPercent)

        // 소견
        binding.tvAdvice.text = buildAdvice(r)
    }

    private fun setMetricBar(progressView: View, scoreText: android.widget.TextView, score: Int) {
        val parent = progressView.parent as? android.view.ViewGroup ?: return
        val barFill = progressView
        val params = barFill.layoutParams
        val maxWidth = (parent.layoutParams?.width ?: 0)
        if (maxWidth > 0) {
            params.width = (maxWidth * score / 100f).toInt()
            barFill.layoutParams = params
        }
        scoreText.text = score.toString()
        val color = ContextCompat.getColor(this, getScoreColor(score))
        scoreText.setTextColor(color)
    }

    private fun setupSaveButtons(r: GaitResult) {
        binding.layoutSaveButtons.visibility = View.VISIBLE
        binding.btnSave.setOnClickListener {
            val memo = binding.etMemo.text.toString().trim()
            viewModel.saveSession(elderlyId, r, memo, prefs.getManagerName())
        }
        binding.btnRemeasure.setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.saveState.collect { state ->
                when (state) {
                    is SaveState.Success -> {
                        Snackbar.make(binding.root, "보행 기록이 저장되었습니다", Snackbar.LENGTH_SHORT).show()
                        viewModel.resetSaveState()
                        finish()
                    }
                    is SaveState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        viewModel.resetSaveState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun displayDate(timestamp: Long) {
        val fmt = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA)
        binding.tvMeasureDate.text = fmt.format(Date(timestamp))
        binding.tvMeasureDate.visibility = View.VISIBLE
    }

    private fun displayMemo(memo: String) {
        if (memo.isNotBlank()) {
            binding.etMemo.setText(memo)
            binding.etMemo.isEnabled = false
        }
    }

    private fun buildAdvice(r: GaitResult): String {
        val issues = mutableListOf<String>()
        if (r.shakinessScore < 60) issues.add("• 걸음 시 몸의 흔들림이 큽니다. 보행 보조기 사용을 권장합니다.")
        if (r.rhythmScore < 60) issues.add("• 보행 리듬이 불규칙합니다. 일정한 속도로 걷는 연습이 필요합니다.")
        if (r.symmetryScore < 60) issues.add("• 왼발과 오른발의 보폭 차이가 큽니다. 균형 운동을 권장합니다.")
        if (r.rotationScore < 60) issues.add("• 방향 전환 시 불안정합니다. 회전 동작에 주의가 필요합니다.")
        if (r.cadenceScore < 50) issues.add("• 보행 속도가 정상 범위를 벗어났습니다. 의료진 상담을 권장합니다.")

        return if (issues.isEmpty()) {
            when {
                r.totalScore >= 80 -> "보행 상태가 매우 양호합니다. 현재의 활동 수준을 유지하세요."
                r.totalScore >= 65 -> "보행 상태가 전반적으로 양호합니다. 꾸준한 운동을 권장합니다."
                else -> "보행 상태가 보통 수준입니다. 정기적인 보행 평가를 권장합니다."
            }
        } else {
            issues.joinToString("\n")
        }
    }

    private fun getScoreColor(score: Int) = when (score) {
        in 80..100 -> R.color.score_excellent
        in 65..79 -> R.color.score_good
        in 50..64 -> R.color.score_normal
        in 35..49 -> R.color.score_poor
        else -> R.color.score_bad
    }
}
