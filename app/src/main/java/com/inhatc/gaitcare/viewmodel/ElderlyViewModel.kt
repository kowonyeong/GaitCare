package com.inhatc.gaitcare.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inhatc.gaitcare.data.db.AppDatabase
import com.inhatc.gaitcare.data.db.entity.Elderly
import com.inhatc.gaitcare.data.repository.ElderlyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ElderlyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ElderlyRepository(AppDatabase.getInstance(application))

    private val _institutionId = MutableStateFlow(-1L)
    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val elderlyList: StateFlow<List<Elderly>> = combine(_institutionId, _searchQuery) { id, query ->
        Pair(id, query)
    }.flatMapLatest { (id, query) ->
        if (id < 0) flowOf(emptyList())
        else if (query.isBlank()) repository.getAll(id)
        else repository.search(id, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState

    fun setInstitutionId(id: Long) {
        _institutionId.value = id
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addElderly(elderly: Elderly) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                repository.insert(elderly)
                _operationState.value = OperationState.Success("어르신이 등록되었습니다")
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("등록 중 오류가 발생했습니다")
            }
        }
    }

    fun updateElderly(elderly: Elderly) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                repository.update(elderly)
                _operationState.value = OperationState.Success("정보가 수정되었습니다")
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("수정 중 오류가 발생했습니다")
            }
        }
    }

    fun deleteElderly(elderly: Elderly) {
        viewModelScope.launch {
            try {
                repository.delete(elderly)
                _operationState.value = OperationState.Success("삭제되었습니다")
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("삭제 중 오류가 발생했습니다")
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}

sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}
