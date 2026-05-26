package com.inhatc.gaitcare.ui.auth

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.inhatc.gaitcare.databinding.ActivityRegisterBinding
import com.inhatc.gaitcare.model.RegisterRequest
import com.inhatc.gaitcare.viewmodel.AuthViewModel
import com.inhatc.gaitcare.viewmodel.RegisterState
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.cbAgreeAll.setOnCheckedChangeListener { _, isChecked ->
            binding.cbTermsOfService.isChecked = isChecked
            binding.cbPrivacyPolicy.isChecked = isChecked
            binding.cbHealthDataConsent.isChecked = isChecked
            binding.cbMarketing.isChecked = isChecked
        }

        listOf(binding.cbTermsOfService, binding.cbPrivacyPolicy, binding.cbHealthDataConsent, binding.cbMarketing)
            .forEach { cb ->
                cb.setOnCheckedChangeListener { _, _ ->
                    val allChecked = listOf(binding.cbTermsOfService, binding.cbPrivacyPolicy, binding.cbHealthDataConsent, binding.cbMarketing).all { it.isChecked }
                    binding.cbAgreeAll.setOnCheckedChangeListener(null)
                    binding.cbAgreeAll.isChecked = allChecked
                    resetAgreeAllListener()
                }
            }

        binding.btnRegister.setOnClickListener {
            if (validateAllInputs()) showConfirmDialog()
        }
    }

    private fun resetAgreeAllListener() {
        binding.cbAgreeAll.setOnCheckedChangeListener { _, isChecked ->
            binding.cbTermsOfService.isChecked = isChecked
            binding.cbPrivacyPolicy.isChecked = isChecked
            binding.cbHealthDataConsent.isChecked = isChecked
            binding.cbMarketing.isChecked = isChecked
        }
    }

    private fun validateAllInputs(): Boolean {
        var isValid = true
        val institutionName = binding.etInstitutionName.text.toString().trim()
        val businessNumber = binding.etBusinessNumber.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val institutionPhone = binding.etInstitutionPhone.text.toString().trim()
        val managerName = binding.etManagerName.text.toString().trim()
        val userId = binding.etRegUserId.text.toString().trim()
        val password = binding.etRegPassword.text.toString()
        val passwordConfirm = binding.etRegPasswordConfirm.text.toString()
        val managerPhone = binding.etManagerPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        listOf(
            Pair(binding.tilInstitutionName, institutionName.isEmpty() to "기관명을 입력해주세요"),
            Pair(binding.tilAddress, address.isEmpty() to "기관 주소를 입력해주세요"),
            Pair(binding.tilInstitutionPhone, institutionPhone.isEmpty() to "기관 전화번호를 입력해주세요"),
            Pair(binding.tilManagerName, managerName.isEmpty() to "담당자 이름을 입력해주세요"),
            Pair(binding.tilManagerPhone, managerPhone.isEmpty() to "담당자 연락처를 입력해주세요")
        ).forEach { (til, validation) ->
            til.error = null
            if (validation.first) { til.error = validation.second; isValid = false }
        }

        binding.tilBusinessNumber.error = null
        if (businessNumber.isEmpty() || !isValidBusinessNumber(businessNumber)) {
            binding.tilBusinessNumber.error = "올바른 사업자등록번호를 입력해주세요 (10자리)"
            isValid = false
        }

        binding.tilRegUserId.error = null
        if (!isValidUserId(userId)) { binding.tilRegUserId.error = "영문, 숫자 조합 6~20자"; isValid = false }

        binding.tilRegPassword.error = null
        if (!isValidPassword(password)) { binding.tilRegPassword.error = "8자 이상, 영문·숫자·특수문자 포함"; isValid = false }

        binding.tilRegPasswordConfirm.error = null
        if (password != passwordConfirm) { binding.tilRegPasswordConfirm.error = "비밀번호가 일치하지 않습니다"; isValid = false }

        binding.tilEmail.error = null
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "올바른 이메일 형식이 아닙니다"; isValid = false
        }

        if (!binding.cbTermsOfService.isChecked || !binding.cbPrivacyPolicy.isChecked || !binding.cbHealthDataConsent.isChecked) {
            Snackbar.make(binding.root, "필수 약관에 모두 동의해주세요", Snackbar.LENGTH_SHORT).show()
            isValid = false
        }
        return isValid
    }

    private fun isValidBusinessNumber(number: String) = number.replace("-", "").let { it.length == 10 && it.all { c -> c.isDigit() } }
    private fun isValidUserId(userId: String) = Regex("^[a-zA-Z0-9]{6,20}$").matches(userId)
    private fun isValidPassword(password: String) = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]{8,}$").matches(password)

    private fun showConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("기관 등록")
            .setMessage("입력한 정보로 기관을 등록하시겠습니까?")
            .setPositiveButton("등록") { _, _ -> submitRegistration() }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun submitRegistration() {
        val request = RegisterRequest(
            institutionName = binding.etInstitutionName.text.toString().trim(),
            businessNumber = binding.etBusinessNumber.text.toString().trim(),
            address = binding.etAddress.text.toString().trim(),
            institutionPhone = binding.etInstitutionPhone.text.toString().trim(),
            managerName = binding.etManagerName.text.toString().trim(),
            userId = binding.etRegUserId.text.toString().trim(),
            password = binding.etRegPassword.text.toString(),
            managerPhone = binding.etManagerPhone.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            agreeMarketing = binding.cbMarketing.isChecked
        )
        viewModel.register(request)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.registerState.collect { state ->
                when (state) {
                    is RegisterState.Idle -> setLoadingState(false)
                    is RegisterState.Loading -> setLoadingState(true)
                    is RegisterState.Success -> {
                        setLoadingState(false)
                        MaterialAlertDialogBuilder(this@RegisterActivity)
                            .setTitle("등록 완료")
                            .setMessage("기관이 등록되었습니다!\n기관 코드: ${state.institutionCode}\n\n이 코드로 로그인하세요.")
                            .setPositiveButton("확인") { _, _ -> finish() }
                            .setCancelable(false)
                            .show()
                    }
                    is RegisterState.Error -> {
                        setLoadingState(false)
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnRegister.isEnabled = !isLoading
        binding.btnRegister.text = if (isLoading) "처리 중..." else "기관 등록 신청"
    }
}
