package com.inhatc.gaitcare.ui.gait

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.inhatc.gaitcare.databinding.ActivityGaitMeasurementBinding
import com.inhatc.gaitcare.sensor.GaitSensorManager
import com.inhatc.gaitcare.sensor.MeasurementState
import kotlinx.coroutines.launch
import kotlin.math.min

class GaitMeasurementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGaitMeasurementBinding
    private lateinit var sensorManager: GaitSensorManager

    private var elderlyId = -1L
    private var elderlyName = ""

    private val uiHandler = Handler(Looper.getMainLooper())
    private var blinkAnimation: AlphaAnimation? = null

    companion object {
        const val EXTRA_ELDERLY_ID = "extra_elderly_id"
        const val EXTRA_ELDERLY_NAME = "extra_elderly_name"
        const val MIN_MEASURE_SECONDS = 20
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGaitMeasurementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        elderlyId = intent.getLongExtra(EXTRA_ELDERLY_ID, -1L)
        elderlyName = intent.getStringExtra(EXTRA_ELDERLY_NAME) ?: ""

        if (elderlyId < 0) { finish(); return }

        sensorManager = GaitSensorManager(this)

        if (!sensorManager.isAccelerometerAvailable()) {
            Snackbar.make(binding.root, "이 기기는 가속도 센서를 지원하지 않습니다", Snackbar.LENGTH_LONG).show()
            finish()
            return
        }

        setupUI()
        observeSensor()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { confirmCancel() }
        })
    }

    private fun setupUI() {
        binding.tvElderlyName.text = elderlyName
        binding.btnBack.setOnClickListener { confirmCancel() }

        binding.btnStart.setOnClickListener {
            sensorManager.startMeasurement()
        }

        binding.btnStop.setOnClickListener {
            val elapsed = sensorManager.elapsedSeconds.value
            if (elapsed < MIN_MEASURE_SECONDS) {
                Snackbar.make(binding.root, "최소 ${MIN_MEASURE_SECONDS}초 이상 측정해주세요 (현재 ${elapsed}초)", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            finishMeasurement()
        }

        showIdleState()
    }

    private fun observeSensor() {
        lifecycleScope.launch {
            sensorManager.state.collect { state ->
                when (state) {
                    MeasurementState.IDLE -> showIdleState()
                    MeasurementState.CALIBRATING -> showCalibratingState()
                    MeasurementState.MEASURING -> showMeasuringState()
                    MeasurementState.FINISHED -> {}
                }
            }
        }

        lifecycleScope.launch {
            sensorManager.elapsedSeconds.collect { seconds ->
                if (sensorManager.state.value == MeasurementState.MEASURING) {
                    binding.tvTimer.text = formatTime(seconds)
                    binding.btnStop.isEnabled = seconds >= MIN_MEASURE_SECONDS
                    if (seconds < MIN_MEASURE_SECONDS) {
                        binding.btnStop.alpha = 0.5f
                        binding.tvMinTimeHint.text = "최소 ${MIN_MEASURE_SECONDS - seconds}초 더 측정"
                    } else {
                        binding.btnStop.alpha = 1.0f
                        binding.tvMinTimeHint.text = "측정 완료 가능"
                    }
                }
            }
        }

        lifecycleScope.launch {
            sensorManager.calibrationProgress.collect { progress ->
                if (sensorManager.state.value == MeasurementState.CALIBRATING) {
                    binding.progressCalibration.progress = progress
                    binding.tvCalibrationProgress.text = "$progress%"
                }
            }
        }

        lifecycleScope.launch {
            sensorManager.currentAccelMagnitude.collect { mag ->
                if (sensorManager.state.value == MeasurementState.MEASURING) {
                    val normalizedLevel = min(1f, (mag / 15f))
                    val alpha = 0.15f + 0.85f * normalizedLevel
                    binding.viewWave.alpha = alpha
                    val scale = 0.8f + 0.4f * normalizedLevel
                    binding.viewWave.scaleX = scale
                    binding.viewWave.scaleY = scale
                }
            }
        }
    }

    private fun showIdleState() {
        binding.layoutIdle.visibility = View.VISIBLE
        binding.layoutCalibrating.visibility = View.GONE
        binding.layoutMeasuring.visibility = View.GONE
        stopBlinking()
    }

    private fun showCalibratingState() {
        binding.layoutIdle.visibility = View.GONE
        binding.layoutCalibrating.visibility = View.VISIBLE
        binding.layoutMeasuring.visibility = View.GONE
        binding.progressCalibration.progress = 0
        startBlinking(binding.tvCalibrationStatus)
    }

    private fun showMeasuringState() {
        stopBlinking()
        binding.layoutIdle.visibility = View.GONE
        binding.layoutCalibrating.visibility = View.GONE
        binding.layoutMeasuring.visibility = View.VISIBLE
        binding.tvTimer.text = "00:00"
        binding.btnStop.isEnabled = false
        binding.btnStop.alpha = 0.5f
        startBlinking(binding.tvRecordingIndicator)
    }

    private fun startBlinking(view: View) {
        blinkAnimation = AlphaAnimation(1f, 0.2f).apply {
            duration = 600
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        view.startAnimation(blinkAnimation)
    }

    private fun stopBlinking() {
        blinkAnimation?.cancel()
        binding.tvRecordingIndicator.clearAnimation()
        binding.tvCalibrationStatus.clearAnimation()
    }

    private fun finishMeasurement() {
        val result = sensorManager.stopMeasurement()
        val intent = Intent(this, GaitResultActivity::class.java).apply {
            putExtra(GaitResultActivity.EXTRA_ELDERLY_ID, elderlyId)
            putExtra(GaitResultActivity.EXTRA_ELDERLY_NAME, elderlyName)
            putExtra(GaitResultActivity.EXTRA_TOTAL_SCORE, result.totalScore)
            putExtra(GaitResultActivity.EXTRA_SHAKINESS_SCORE, result.shakinessScore)
            putExtra(GaitResultActivity.EXTRA_RHYTHM_SCORE, result.rhythmScore)
            putExtra(GaitResultActivity.EXTRA_SYMMETRY_SCORE, result.symmetryScore)
            putExtra(GaitResultActivity.EXTRA_ROTATION_SCORE, result.rotationScore)
            putExtra(GaitResultActivity.EXTRA_CADENCE_SCORE, result.cadenceScore)
            putExtra(GaitResultActivity.EXTRA_STEP_COUNT, result.stepCount)
            putExtra(GaitResultActivity.EXTRA_CADENCE_SPM, result.cadenceSpm)
            putExtra(GaitResultActivity.EXTRA_AVG_INTERVAL, result.avgStepIntervalMs)
            putExtra(GaitResultActivity.EXTRA_CV_PERCENT, result.stepIntervalCvPercent)
            putExtra(GaitResultActivity.EXTRA_LATERAL_RMS, result.lateralRmsG)
            putExtra(GaitResultActivity.EXTRA_VERTICAL_RMS, result.verticalRmsG)
            putExtra(GaitResultActivity.EXTRA_SYMMETRY_INDEX, result.symmetryIndexPercent)
            putExtra(GaitResultActivity.EXTRA_ROTATION_DEG, result.avgRotationDegSec)
            putExtra(GaitResultActivity.EXTRA_DURATION, result.durationSeconds)
            putExtra(GaitResultActivity.EXTRA_VIEW_ONLY, false)
        }
        startActivity(intent)
        finish()
    }

    private fun confirmCancel() {
        if (sensorManager.state.value == MeasurementState.MEASURING ||
            sensorManager.state.value == MeasurementState.CALIBRATING
        ) {
            MaterialAlertDialogBuilder(this)
                .setTitle("측정 취소")
                .setMessage("측정을 취소하시겠습니까? 측정 데이터가 삭제됩니다.")
                .setPositiveButton("취소") { _, _ ->
                    sensorManager.reset()
                    finish()
                }
                .setNegativeButton("계속 측정", null)
                .show()
        } else {
            finish()
        }
    }

    private fun formatTime(totalSeconds: Int): String {
        val min = totalSeconds / 60
        val sec = totalSeconds % 60
        return "%02d:%02d".format(min, sec)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.reset()
    }

}
