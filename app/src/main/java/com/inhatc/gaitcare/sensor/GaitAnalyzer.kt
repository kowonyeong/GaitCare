package com.inhatc.gaitcare.sensor

import com.inhatc.gaitcare.model.GaitResult
import kotlin.math.*

/**
 * 보행 데이터 분석기
 *
 * 주머니 착용 조건:
 *  - 핸드폰 뒷면이 허벅지에 닿음
 *  - 핸드폰 아랫면이 바닥 방향
 *  - 자동 축 캘리브레이션으로 실제 보행 방향 검출
 */
class GaitAnalyzer {

    // 각 샘플: [ax, ay, az, gx, gy, gz]  (가속도/자이로 6채널)
    private val rawSamples = mutableListOf<FloatArray>()
    // 타임스탬프는 Long으로 별도 보관 (Float 정밀도 손실 방지)
    private val sampleTimestamps = mutableListOf<Long>()

    // 캘리브레이션 후 보행 방향 축 (0=X, 1=Y, 2=Z)
    private var forwardAxis = 1
    private var lateralAxis = 0
    private var verticalAxis = 2

    companion object {
        private const val GRAVITY = 9.80665f
        private const val LOW_PASS_ALPHA = 0.15f
        private const val STEP_MIN_INTERVAL_MS = 220f   // 최소 보폭 간격
        private const val STEP_MAX_INTERVAL_MS = 3000f  // 최대 보폭 간격 (셔플링 보행 인정)
        private const val STEP_THRESHOLD_MULTIPLIER = 1.03f  // 평균 대비 3% 초과
        private const val STEP_THRESHOLD_ABS = 0.08f         // 최소 절대 임계 (m/s²) 매우 완화
        private const val CALIBRATION_SAMPLES = 50       // 1초 분량 (50Hz)
    }

    fun addSample(ax: Float, ay: Float, az: Float, gx: Float, gy: Float, gz: Float, timestampMs: Long) {
        rawSamples.add(floatArrayOf(ax, ay, az, gx, gy, gz))
        sampleTimestamps.add(timestampMs)
    }

    fun calibrateAxes() {
        if (rawSamples.size < CALIBRATION_SAMPLES) return

        // 캘리브레이션 샘플에서 각 축의 분산 계산
        val calibSamples = rawSamples.take(CALIBRATION_SAMPLES)
        val varX = variance(calibSamples.map { it[0] })
        val varY = variance(calibSamples.map { it[1] })
        val varZ = variance(calibSamples.map { it[2] })

        // 가장 분산이 큰 축이 보행 방향(전후)
        val maxVar = maxOf(varX, varY, varZ)
        forwardAxis = when (maxVar) {
            varX -> 0
            varY -> 1
            else -> 2
        }

        // 수직축은 중력이 가장 강한 축 (평균 절댓값이 가장 큰 축)
        val meanAbsX = calibSamples.map { abs(it[0]) }.average().toFloat()
        val meanAbsY = calibSamples.map { abs(it[1]) }.average().toFloat()
        val meanAbsZ = calibSamples.map { abs(it[2]) }.average().toFloat()

        val maxMeanAbs = maxOf(meanAbsX, meanAbsY, meanAbsZ)
        verticalAxis = when (maxMeanAbs) {
            meanAbsX -> 0
            meanAbsY -> 1
            else -> 2
        }

        // 나머지 하나가 측면(좌우) 축
        val axes = mutableSetOf(0, 1, 2)
        axes.remove(forwardAxis)
        axes.remove(verticalAxis)
        if (axes.isEmpty()) {
            // 보행 축과 수직 축이 같으면 기본값 사용
            verticalAxis = if (forwardAxis != 2) 2 else 1
            axes.add(if (forwardAxis != 0 && verticalAxis != 0) 0 else if (forwardAxis != 1 && verticalAxis != 1) 1 else 2)
        }
        lateralAxis = axes.first()
    }

    fun analyze(durationSeconds: Int): GaitResult {
        if (rawSamples.size < 10) {
            return defaultResult(durationSeconds)
        }

        calibrateAxes()

        val effectiveDuration = durationSeconds.coerceAtLeast(1)

        // 캘리브레이션 직후 워밍업 구간 제외
        val analysisStart = minOf(CALIBRATION_SAMPLES, rawSamples.size / 4)
        val samples = if (rawSamples.size > analysisStart) rawSamples.drop(analysisStart) else rawSamples
        val timestamps = if (sampleTimestamps.size > analysisStart) sampleTimestamps.drop(analysisStart) else sampleTimestamps.toList()

        // 각 축 신호 추출 및 저주파 필터(중력 제거)
        val rawForward = samples.map { it[forwardAxis] }
        val rawLateral = samples.map { it[lateralAxis] }
        val rawVertical = samples.map { it[verticalAxis] }
        val rawGx = samples.map { it[3] }
        val rawGy = samples.map { it[4] }
        val rawGz = samples.map { it[5] }

        val gravForward = lowPassFilter(rawForward, LOW_PASS_ALPHA)
        val gravLateral = lowPassFilter(rawLateral, LOW_PASS_ALPHA)
        val gravVertical = lowPassFilter(rawVertical, LOW_PASS_ALPHA)

        val dynForward = subtract(rawForward, gravForward)
        val dynLateral = subtract(rawLateral, gravLateral)
        val dynVertical = subtract(rawVertical, gravVertical)
        val sampleRateHz = if (timestamps.size > 1) {
            1000f * (timestamps.size - 1) / (timestamps.last() - timestamps.first()).coerceAtLeast(1L)
        } else 50f

        // ── 1. 내부 보폭 검출 (리듬/대칭성 계산용, 결과에 노출하지 않음) ──
        val dynMagnitudes = dynForward.indices.map { i ->
            sqrt(dynForward[i].pow(2) + dynLateral[i].pow(2) + dynVertical[i].pow(2))
        }
        val steps = detectSteps(dynMagnitudes, timestamps, sampleRateHz)
        val stepIntervals = steps.zipWithNext { a, b -> (b - a).toFloat() }
            .filter { it in STEP_MIN_INTERVAL_MS..STEP_MAX_INTERVAL_MS }

        // ── 2. 흔들림 (Shakiness) ──
        val lateralRms = rms(dynLateral)
        val verticalRms = rms(dynVertical)
        val shakinessScore = computeShakinessScore(lateralRms, verticalRms)

        // ── 3. 보행 리듬 일정성 (Rhythm) ──
        val (rhythmScore, avgIntervalMs, cvPercent) = if (stepIntervals.size >= 2) {
            val avg = stepIntervals.average().toFloat()
            val cv = coefficientOfVariation(stepIntervals)
            Triple(computeRhythmScore(cv), avg, cv * 100f)
        } else Triple(50, 800f, 20f)

        // ── 4. 보행 대칭성 (Symmetry) ──
        val (symmetryScore, symmetryIndex) = if (steps.size >= 4) {
            val si = computeSymmetryIndex(dynForward, steps, timestamps)
            Pair(computeSymmetryScore(si), si * 100f)
        } else Pair(50, 15f)

        // ── 5. 회전 안정성 (Rotation) ──
        val gyroMagnitudes = samples.indices.map { i ->
            sqrt(rawGx[i].pow(2) + rawGy[i].pow(2) + rawGz[i].pow(2))
        }
        val avgRotation = gyroMagnitudes.average().toFloat()
        val rotationScore = computeRotationScore(avgRotation)

        // ── 6. 보행 속도 (전방 축 동적 가속도 RMS 기반) ──
        val forwardRms = rms(dynForward)
        val forwardRmsG = forwardRms / GRAVITY
        val cadenceScore = computeSpeedScore(forwardRmsG)

        // ── 7. 종합 점수 (가중 평균) ──
        val totalScore = (
            shakinessScore * 0.25f +
            rhythmScore * 0.25f +
            symmetryScore * 0.20f +
            rotationScore * 0.20f +
            cadenceScore * 0.10f
        ).toInt().coerceIn(0, 100)

        return GaitResult(
            totalScore = totalScore,
            shakinessScore = shakinessScore,
            rhythmScore = rhythmScore,
            symmetryScore = symmetryScore,
            rotationScore = rotationScore,
            cadenceScore = cadenceScore,
            stepCount = 0,                              // 사용 안 함 (호환성 유지)
            cadenceSpm = forwardRmsG,                   // 전방축 동적 RMS (g)
            avgStepIntervalMs = avgIntervalMs,
            stepIntervalCvPercent = cvPercent,
            lateralRmsG = lateralRms / GRAVITY,
            verticalRmsG = verticalRms / GRAVITY,
            symmetryIndexPercent = symmetryIndex,
            avgRotationDegSec = Math.toDegrees(avgRotation.toDouble()).toFloat(),
            durationSeconds = durationSeconds
        )
    }

    // ── 보폭 검출: 3축 합성 동적 가속도에서 피크 검출 (느린 보행 강건) ──
    private fun detectSteps(signal: List<Float>, timestamps: List<Long>, sampleRateHz: Float): List<Long> {
        if (signal.size < 4) return emptyList()

        val smoothed = movingAverage(signal, (sampleRateHz / 6).toInt().coerceAtLeast(3))
        val meanMag = smoothed.average().toFloat().coerceAtLeast(0.05f)
        // 매우 완화된 임계: 평균보다 3% 또는 0.08 m/s² 큰 지점
        val threshold = minOf(meanMag * STEP_THRESHOLD_MULTIPLIER, meanMag + STEP_THRESHOLD_ABS)
        val minSamples = (STEP_MIN_INTERVAL_MS * sampleRateHz / 1000f).toInt()

        val peaks = mutableListOf<Long>()
        var lastPeakIdx = -minSamples

        // 단순 1샘플 비교 (셔플 보행에서도 작은 피크가 잡히게)
        for (i in 1 until smoothed.size - 1) {
            val v = smoothed[i]
            if (v > threshold &&
                v >= smoothed[i - 1] && v >= smoothed[i + 1]
            ) {
                if (i - lastPeakIdx >= minSamples && timestamps.indices.contains(i)) {
                    peaks.add(timestamps[i])
                    lastPeakIdx = i
                }
            }
        }

        // 피크가 너무 적으면 임계값을 더 낮춰 재시도 (절대 검출 보장)
        if (peaks.size < 4) {
            peaks.clear()
            val fallbackThreshold = meanMag * 1.005f
            lastPeakIdx = -minSamples
            for (i in 1 until smoothed.size - 1) {
                val v = smoothed[i]
                if (v > fallbackThreshold &&
                    v >= smoothed[i - 1] && v >= smoothed[i + 1]
                ) {
                    if (i - lastPeakIdx >= minSamples && timestamps.indices.contains(i)) {
                        peaks.add(timestamps[i])
                        lastPeakIdx = i
                    }
                }
            }
        }
        return peaks
    }

    // ── 대칭성 지수: 홀수/짝수 보폭 크기 비교 ──
    private fun computeSymmetryIndex(signal: List<Float>, stepTimestamps: List<Long>, timestamps: List<Long>): Float {
        if (stepTimestamps.size < 4) return 0.15f

        val stepMags = stepTimestamps.mapIndexed { _, ts ->
            val idx = timestamps.indexOfFirst { it >= ts }.coerceAtLeast(0)
            abs(if (idx < signal.size) signal[idx] else 0f)
        }

        val oddMags = stepMags.filterIndexed { i, _ -> i % 2 == 0 }
        val evenMags = stepMags.filterIndexed { i, _ -> i % 2 == 1 }

        if (oddMags.isEmpty() || evenMags.isEmpty()) return 0.15f

        val avgOdd = oddMags.average().toFloat()
        val avgEven = evenMags.average().toFloat()
        val avgTotal = (avgOdd + avgEven) / 2f

        return if (avgTotal > 0) abs(avgOdd - avgEven) / avgTotal else 0.15f
    }

    // ── 점수 변환 함수 ──
    private fun computeShakinessScore(lateralRms: Float, verticalRms: Float): Int {
        val combined = (lateralRms + verticalRms * 0.5f) / GRAVITY
        return when {
            combined < 0.08f -> 95
            combined < 0.15f -> 88
            combined < 0.22f -> 80
            combined < 0.32f -> 72
            combined < 0.45f -> 63
            combined < 0.62f -> 53
            combined < 0.85f -> 43
            else -> 33
        }
    }

    private fun computeRhythmScore(cv: Float): Int {
        val cvPct = cv * 100f
        return when {
            cvPct < 4f -> 95
            cvPct < 8f -> 88
            cvPct < 14f -> 80
            cvPct < 22f -> 72
            cvPct < 32f -> 63
            cvPct < 45f -> 53
            cvPct < 60f -> 43
            else -> 33
        }
    }

    private fun computeSymmetryScore(si: Float): Int {
        val siPct = si * 100f
        return when {
            siPct < 3f -> 95
            siPct < 6f -> 88
            siPct < 12f -> 80
            siPct < 20f -> 72
            siPct < 30f -> 63
            siPct < 45f -> 53
            siPct < 60f -> 43
            else -> 33
        }
    }

    private fun computeRotationScore(avgRotRad: Float): Int {
        val deg = Math.toDegrees(avgRotRad.toDouble()).toFloat()
        return when {
            deg < 15f -> 95
            deg < 32f -> 88
            deg < 52f -> 80
            deg < 78f -> 72
            deg < 108f -> 63
            deg < 145f -> 53
            deg < 185f -> 43
            else -> 33
        }
    }

    // 전방축 동적 가속도 RMS(g)로 보행 속도/추진력 추정 — 노인 보행 분포 기준
    private fun computeSpeedScore(forwardRmsG: Float): Int {
        return when {
            forwardRmsG < 0.04f -> 30     // 거의 정지 / 셔플
            forwardRmsG < 0.08f -> 55     // 매우 느린 셔플
            forwardRmsG < 0.13f -> 73     // 느린 노인 보행
            forwardRmsG < 0.25f -> 90     // 정상 노인 보행
            forwardRmsG < 0.45f -> 88     // 활발한 보행
            forwardRmsG < 0.75f -> 73     // 매우 빠른 보행
            else -> 55                    // 비정상 (조깅/충격)
        }
    }

    // ── 신호 처리 유틸 ──
    private fun lowPassFilter(signal: List<Float>, alpha: Float): List<Float> {
        val result = mutableListOf<Float>()
        var prev = signal.firstOrNull() ?: 0f
        for (v in signal) {
            prev = alpha * v + (1 - alpha) * prev
            result.add(prev)
        }
        return result
    }

    private fun subtract(a: List<Float>, b: List<Float>): List<Float> =
        a.zip(b).map { (x, y) -> x - y }

    private fun rms(signal: List<Float>): Float {
        if (signal.isEmpty()) return 0f
        return sqrt(signal.map { it * it }.average().toFloat())
    }

    private fun variance(signal: List<Float>): Float {
        if (signal.size < 2) return 0f
        val mean = signal.average().toFloat()
        return signal.map { (it - mean).pow(2) }.average().toFloat()
    }

    private fun coefficientOfVariation(signal: List<Float>): Float {
        if (signal.size < 2) return 0.20f
        val mean = signal.average().toFloat()
        if (mean == 0f) return 0.20f
        val std = sqrt(variance(signal))
        return std / mean
    }

    private fun movingAverage(signal: List<Float>, window: Int): List<Float> {
        val w = window.coerceAtLeast(1)
        return signal.indices.map { i ->
            val from = maxOf(0, i - w / 2)
            val to = minOf(signal.size, i + w / 2 + 1)
            signal.subList(from, to).average().toFloat()
        }
    }

    private fun defaultResult(durationSeconds: Int) = GaitResult(
        totalScore = 0, shakinessScore = 0, rhythmScore = 0,
        symmetryScore = 0, rotationScore = 0, cadenceScore = 0,
        stepCount = 0, cadenceSpm = 0f, avgStepIntervalMs = 0f,
        stepIntervalCvPercent = 0f, lateralRmsG = 0f, verticalRmsG = 0f,
        symmetryIndexPercent = 0f, avgRotationDegSec = 0f,
        durationSeconds = durationSeconds
    )

    fun reset() {
        rawSamples.clear()
        sampleTimestamps.clear()
    }
}
