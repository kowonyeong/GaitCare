package com.inhatc.gaitcare.ui.elderly

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.inhatc.gaitcare.data.db.AppDatabase
import com.inhatc.gaitcare.data.db.entity.Elderly
import com.inhatc.gaitcare.databinding.ActivityAddElderlyBinding
import com.inhatc.gaitcare.utils.PreferenceManager
import com.inhatc.gaitcare.viewmodel.ElderlyViewModel
import com.inhatc.gaitcare.viewmodel.OperationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddElderlyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddElderlyBinding
    private val viewModel: ElderlyViewModel by viewModels()
    private lateinit var prefs: PreferenceManager

    private var editElderlyId = -1L
    private var existingElderly: Elderly? = null

    companion object {
        const val EXTRA_ELDERLY_ID = "extra_edit_elderly_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddElderlyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        editElderlyId = intent.getLongExtra(EXTRA_ELDERLY_ID, -1L)
        viewModel.setInstitutionId(prefs.getInstitutionId())

        setupUI()
        if (editElderlyId > 0) loadExisting()
        observeViewModel()
    }

    private fun setupUI() {
        val isEdit = editElderlyId > 0
        binding.tvTitle.text = if (isEdit) "어르신 정보 수정" else "어르신 등록"
        binding.btnSave.text = if (isEdit) "수정 완료" else "등록"

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                saveElderly()
            }
        }

        // 성별 선택
        binding.rgGender.setOnCheckedChangeListener { _, _ -> }
    }

    private fun loadExisting() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@AddElderlyActivity)
            existingElderly = withContext(Dispatchers.IO) { db.elderlyDao().findById(editElderlyId) }
            existingElderly?.let { e ->
                binding.etName.setText(e.name)
                binding.etAge.setText(e.age.toString())
                if (e.gender == "남") binding.rbMale.isChecked = true
                else binding.rbFemale.isChecked = true
                binding.etRoom.setText(e.roomNumber)
                binding.etDiagnosis.setText(e.diagnosisNote)
                binding.etContact.setText(e.contactPhone)
                binding.etEmergency.setText(e.emergencyContact)
                binding.sliderCareLevel.value = e.careLevel.toFloat()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var valid = true
        binding.tilName.error = null
        binding.tilAge.error = null

        val name = binding.etName.text.toString().trim()
        val ageStr = binding.etAge.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilName.error = "이름을 입력해주세요"
            valid = false
        }
        if (ageStr.isEmpty()) {
            binding.tilAge.error = "나이를 입력해주세요"
            valid = false
        } else {
            val age = ageStr.toIntOrNull()
            if (age == null || age < 1 || age > 120) {
                binding.tilAge.error = "올바른 나이를 입력해주세요"
                valid = false
            }
        }
        if (!binding.rbMale.isChecked && !binding.rbFemale.isChecked) {
            Snackbar.make(binding.root, "성별을 선택해주세요", Snackbar.LENGTH_SHORT).show()
            valid = false
        }
        return valid
    }

    private fun saveElderly() {
        val name = binding.etName.text.toString().trim()
        val age = binding.etAge.text.toString().trim().toInt()
        val gender = if (binding.rbMale.isChecked) "남" else "여"
        val room = binding.etRoom.text.toString().trim()
        val diagnosis = binding.etDiagnosis.text.toString().trim()
        val contact = binding.etContact.text.toString().trim()
        val emergency = binding.etEmergency.text.toString().trim()
        val careLevel = binding.sliderCareLevel.value.toInt()

        if (editElderlyId > 0 && existingElderly != null) {
            viewModel.updateElderly(existingElderly!!.copy(
                name = name, age = age, gender = gender, roomNumber = room,
                diagnosisNote = diagnosis, contactPhone = contact,
                emergencyContact = emergency, careLevel = careLevel
            ))
        } else {
            viewModel.addElderly(Elderly(
                institutionId = prefs.getInstitutionId(),
                name = name, age = age, gender = gender, roomNumber = room,
                diagnosisNote = diagnosis, contactPhone = contact,
                emergencyContact = emergency, careLevel = careLevel
            ))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.operationState.collect { state ->
                when (state) {
                    is OperationState.Success -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                        viewModel.resetOperationState()
                        finish()
                    }
                    is OperationState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        viewModel.resetOperationState()
                    }
                    else -> {}
                }
            }
        }
    }
}
