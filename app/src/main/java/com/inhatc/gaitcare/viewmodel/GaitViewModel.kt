package com.inhatc.gaitcare.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inhatc.gaitcare.data.db.AppDatabase
import com.inhatc.gaitcare.data.db.entity.GaitSession
import com.inhatc.gaitcare.data.repository.GaitRepository
import com.inhatc.gaitcare.model.GaitResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GaitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GaitRepository(AppDatabase.getInstance(application))

    private val _elderlyId = MutableStateFlow(-1L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionList: StateFlow<List<GaitSession>> = _elderlyId
        .flatMapLatest { id ->
            if (id < 0) flowOf(emptyList())
            else repository.getByElderly(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    private val _pendingResult = MutableStateFlow<GaitResult?>(null)
    val pendingResult: StateFlow<GaitResult?> = _pendingResult

    fun setElderlyId(id: Long) {
        _elderlyId.value = id
    }

    fun setPendingResult(result: GaitResult) {
        _pendingResult.value = result
    }

    fun saveSession(elderlyId: Long, result: GaitResult, memo: String = "", measuredBy: String = "") {
        viewModelScope.launch {
            _saveState.value = SaveState.Loading
            try {
                val session = GaitSession(
                    elderlyId = elderlyId,
                    durationSeconds = result.durationSeconds,
                    totalScore = result.totalScore,
                    shakinessScore = result.shakinessScore,
                    rhythmScore = result.rhythmScore,
                    symmetryScore = result.symmetryScore,
                    rotationScore = result.rotationScore,
                    cadenceScore = result.cadenceScore,
                    stepCount = result.stepCount,
                    cadenceSpm = result.cadenceSpm,
                    avgStepIntervalMs = result.avgStepIntervalMs,
                    stepIntervalCvPercent = result.stepIntervalCvPercent,
                    lateralRmsG = result.lateralRmsG,
                    verticalRmsG = result.verticalRmsG,
                    symmetryIndexPercent = result.symmetryIndexPercent,
                    avgRotationDegSec = result.avgRotationDegSec,
                    memo = memo,
                    measuredBy = measuredBy
                )
                val id = repository.insert(session)
                _saveState.value = SaveState.Success(id)
                _pendingResult.value = null
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("저장 중 오류가 발생했습니다")
            }
        }
    }

    fun deleteSession(session: GaitSession) {
        viewModelScope.launch {
            try {
                repository.delete(session)
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("삭제 중 오류가 발생했습니다")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}

sealed class SaveState {
    object Idle : SaveState()
    object Loading : SaveState()
    data class Success(val sessionId: Long) : SaveState()
    data class Error(val message: String) : SaveState()
}
