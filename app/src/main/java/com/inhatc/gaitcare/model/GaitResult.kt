package com.inhatc.gaitcare.model

data class GaitResult(
    val totalScore: Int,
    val shakinessScore: Int,
    val rhythmScore: Int,
    val symmetryScore: Int,
    val rotationScore: Int,
    val cadenceScore: Int,
    val stepCount: Int,
    val cadenceSpm: Float,
    val avgStepIntervalMs: Float,
    val stepIntervalCvPercent: Float,
    val lateralRmsG: Float,
    val verticalRmsG: Float,
    val symmetryIndexPercent: Float,
    val avgRotationDegSec: Float,
    val durationSeconds: Int
) {
    fun getScoreLabel(): String = when (totalScore) {
        in 80..100 -> "매우 좋음"
        in 65..79 -> "좋음"
        in 50..64 -> "보통"
        in 35..49 -> "주의 필요"
        else -> "위험"
    }

    fun getScoreColorRes(): Int = when (totalScore) {
        in 80..100 -> android.R.color.holo_green_dark
        in 65..79 -> android.R.color.holo_green_light
        in 50..64 -> android.R.color.holo_orange_light
        in 35..49 -> android.R.color.holo_orange_dark
        else -> android.R.color.holo_red_dark
    }
}
