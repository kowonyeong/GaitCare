package com.inhatc.gaitcare.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inhatc.gaitcare.data.db.AppDatabase
import com.inhatc.gaitcare.data.repository.AuthRepository
import com.inhatc.gaitcare.model.RegisterRequest
import com.inhatc.gaitcare.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val institutionCode: String) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(AppDatabase.getInstance(application))
    private val prefs = PreferenceManager(application)

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    fun login(institutionCode: String, userId: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val institution = repository.login(institutionCode.trim(), userId.trim(), password)
                if (institution != null) {
                    prefs.saveSession(institution.id, institution.institutionCode, institution.institutionName, institution.managerName)
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("기관 코드, 아이디 또는 비밀번호가 올바르지 않습니다")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("로그인 중 오류가 발생했습니다")
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                val result = repository.register(request)
                if (result.isSuccess) {
                    val institution = result.getOrThrow()
                    _registerState.value = RegisterState.Success(institution.institutionCode)
                } else {
                    _registerState.value = RegisterState.Error(result.exceptionOrNull()?.message ?: "등록에 실패했습니다")
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error("등록 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }
}
