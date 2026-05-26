package com.inhatc.gaitcare.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.inhatc.gaitcare.databinding.ActivityLoginBinding
import com.inhatc.gaitcare.ui.main.MainInstitutionActivity
import com.inhatc.gaitcare.utils.PreferenceManager
import com.inhatc.gaitcare.viewmodel.AuthViewModel
import com.inhatc.gaitcare.viewmodel.LoginState
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        checkAutoLogin()
        setupUI()
        observeViewModel()
    }

    private fun checkAutoLogin() {
        if (preferenceManager.isAutoLogin()) {
            val code = preferenceManager.getInstitutionCode()
            val userId = preferenceManager.getUserId()
            val password = preferenceManager.getPassword()
            if (!code.isNullOrEmpty() && !userId.isNullOrEmpty() && !password.isNullOrEmpty()) {
                viewModel.login(code, userId, password)
            }
        }
    }

    private fun setupUI() {
        binding.cbAutoLogin.isChecked = preferenceManager.isAutoLogin()

        binding.btnLogin.setOnClickListener {
            val code = binding.etInstitutionCode.text.toString().trim()
            val userId = binding.etUserId.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (validateInputs(code, userId, password)) {
                if (binding.cbAutoLogin.isChecked) {
                    preferenceManager.saveAutoLoginInfo(code, userId, password)
                } else {
                    preferenceManager.clearAutoLoginInfo()
                }
                viewModel.login(code, userId, password)
            }
        }

        binding.btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInputs(code: String, userId: String, password: String): Boolean {
        var valid = true
        binding.tilInstitutionCode.error = null
        binding.tilUserId.error = null
        binding.tilPassword.error = null

        if (code.isEmpty()) { binding.tilInstitutionCode.error = "기관 코드를 입력해주세요"; valid = false }
        if (userId.isEmpty()) { binding.tilUserId.error = "아이디를 입력해주세요"; valid = false }
        if (password.isEmpty()) { binding.tilPassword.error = "비밀번호를 입력해주세요"; valid = false }
        return valid
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginState.Idle -> setLoadingState(false)
                    is LoginState.Loading -> setLoadingState(true)
                    is LoginState.Success -> {
                        setLoadingState(false)
                        val intent = Intent(this@LoginActivity, MainInstitutionActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    is LoginState.Error -> {
                        setLoadingState(false)
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "로그인 중..." else "로그인"
        binding.btnGoToRegister.isEnabled = !isLoading
    }
}
