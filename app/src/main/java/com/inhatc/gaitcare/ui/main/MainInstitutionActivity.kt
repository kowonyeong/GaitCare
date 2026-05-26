package com.inhatc.gaitcare.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.inhatc.gaitcare.databinding.ActivityMainInstitutionBinding
import com.inhatc.gaitcare.ui.auth.LoginActivity
import com.inhatc.gaitcare.ui.elderly.AddElderlyActivity
import com.inhatc.gaitcare.ui.elderly.ElderlyDetailActivity
import com.inhatc.gaitcare.utils.PreferenceManager
import com.inhatc.gaitcare.viewmodel.ElderlyViewModel
import com.inhatc.gaitcare.viewmodel.OperationState
import kotlinx.coroutines.launch

class MainInstitutionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainInstitutionBinding
    private val viewModel: ElderlyViewModel by viewModels()
    private lateinit var prefs: PreferenceManager
    private lateinit var adapter: ElderlyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainInstitutionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        binding.tvInstitutionName.text = prefs.getInstitutionName()
        binding.tvManagerName.text = "${prefs.getManagerName()} 님"

        viewModel.setInstitutionId(prefs.getInstitutionId())

        binding.btnAddElderly.setOnClickListener {
            startActivity(Intent(this, AddElderlyActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("로그아웃")
                .setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃") { _, _ -> logout() }
                .setNegativeButton("취소", null)
                .show()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = ElderlyAdapter { elderly ->
            val intent = Intent(this, ElderlyDetailActivity::class.java).apply {
                putExtra(ElderlyDetailActivity.EXTRA_ELDERLY_ID, elderly.id)
                putExtra(ElderlyDetailActivity.EXTRA_ELDERLY_NAME, elderly.name)
            }
            startActivity(intent)
        }
        binding.rvElderly.layoutManager = LinearLayoutManager(this)
        binding.rvElderly.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.elderlyList.collect { list ->
                adapter.submitList(list)
                binding.tvEmptyMessage.visibility =
                    if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.tvCountLabel.text = "총 ${list.size}명"
            }
        }

        lifecycleScope.launch {
            viewModel.operationState.collect { state ->
                if (state is OperationState.Success) {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                    viewModel.resetOperationState()
                } else if (state is OperationState.Error) {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.resetOperationState()
                }
            }
        }
    }

    private fun logout() {
        prefs.clearSession()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
