package com.inhatc.gaitcare.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gait_sessions",
    foreignKeys = [ForeignKey(
        entity = Elderly::class,
        parentColumns = ["id"],
        childColumns = ["elderlyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("elderlyId")]
)
data class GaitSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val elderlyId: Long,
    val measuredAt: Long = System.currentTimeMillis(),
    val durationSeconds: Int,

    // 종합 점수 (0~100)
    val totalScore: Int,

    // 세부 지표 점수 (0~100)
    val shakinessScore: Int,
    val rhythmScore: Int,
    val symmetryScore: Int,
    val rotationScore: Int,
    val cadenceScore: Int,

    // 실제 측정값
    val stepCount: Int,
    val cadenceSpm: Float,
    val avgStepIntervalMs: Float,
    val stepIntervalCvPercent: Float,
    val lateralRmsG: Float,
    val verticalRmsG: Float,
    val symmetryIndexPercent: Float,
    val avgRotationDegSec: Float,

    // 메모
    val memo: String = "",

    // 측정자 (로그인한 기관 담당자)
    val measuredBy: String = ""
)
