package com.inhatc.gaitcare.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.inhatc.gaitcare.model.GaitResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class MeasurementState {
    IDLE,
    CALIBRATING,
    MEASURING,
    FINISHED
}

class GaitSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val analyzer = GaitAnalyzer()

    private val _state = MutableStateFlow(MeasurementState.IDLE)
    val state: StateFlow<MeasurementState> = _state

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds

    private val _calibrationProgress = MutableStateFlow(0)
    val calibrationProgress: StateFlow<Int> = _calibrationProgress

    private var startTimeMs = 0L
    private var calibrationStartMs = 0L
    private var sampleCount = 0

    // 최근 가속도 값 (실시간 표시용)
    private val _currentAccelMagnitude = MutableStateFlow(0f)
    val currentAccelMagnitude: StateFlow<Float> = _currentAccelMagnitude

    // 캘리브레이션 완료 후 진짜 측정 시작 시간
    private var measureStartTimeMs = 0L

    private var lastGyro = floatArrayOf(0f, 0f, 0f)

    companion object {
        private const val SAMPLE_RATE_US = 20_000  // 50Hz
        private const val CALIBRATION_DURATION_MS = 10_000L  // 10초 캘리브레이션 (준비 + 축 검출)
    }

    fun startMeasurement() {
        if (_state.value != MeasurementState.IDLE) return

        analyzer.reset()
        sampleCount = 0
        calibrationStartMs = System.currentTimeMillis()
        startTimeMs = calibrationStartMs
        _state.value = MeasurementState.CALIBRATING
        _calibrationProgress.value = 0
        _elapsedSeconds.value = 0

        sensorManager.registerListener(this, accelerometer, SAMPLE_RATE_US)
        sensorManager.registerListener(this, gyroscope, SAMPLE_RATE_US)
    }

    fun stopMeasurement(): GaitResult {
        val durationMs = System.currentTimeMillis() - measureStartTimeMs
        val durationSeconds = (durationMs / 1000L).toInt().coerceAtLeast(1)
        sensorManager.unregisterListener(this)
        _state.value = MeasurementState.FINISHED
        return analyzer.analyze(durationSeconds)
    }

    fun reset() {
        sensorManager.unregisterListener(this)
        analyzer.reset()
        sampleCount = 0
        _state.value = MeasurementState.IDLE
        _elapsedSeconds.value = 0
        _calibrationProgress.value = 0
        _currentAccelMagnitude.value = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        val nowMs = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val magnitude = kotlin.math.sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
                _currentAccelMagnitude.value = magnitude

                when (_state.value) {
                    MeasurementState.CALIBRATING -> {
                        val elapsed = nowMs - calibrationStartMs
                        val progress = (elapsed * 100 / CALIBRATION_DURATION_MS).toInt().coerceIn(0, 100)
                        _calibrationProgress.value = progress

                        // 캘리브레이션 샘플 수집
                        analyzer.addSample(ax, ay, az, lastGyro[0], lastGyro[1], lastGyro[2], nowMs)
                        sampleCount++

                        if (elapsed >= CALIBRATION_DURATION_MS) {
                            // 캘리브레이션 완료 → 측정 시작
                            analyzer.calibrateAxes()
                            analyzer.reset()  // 분석용 샘플 초기화 (캘리브레이션 데이터는 버림)
                            measureStartTimeMs = nowMs
                            _state.value = MeasurementState.MEASURING
                            _calibrationProgress.value = 100
                        }
                    }
                    MeasurementState.MEASURING -> {
                        analyzer.addSample(ax, ay, az, lastGyro[0], lastGyro[1], lastGyro[2], nowMs)
                        val measuredSecs = ((nowMs - measureStartTimeMs) / 1000L).toInt()
                        _elapsedSeconds.value = measuredSecs
                    }
                    else -> {}
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyro[0] = event.values[0]
                lastGyro[1] = event.values[1]
                lastGyro[2] = event.values[2]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun isAccelerometerAvailable(): Boolean = accelerometer != null
    fun isGyroscopeAvailable(): Boolean = gyroscope != null
}
