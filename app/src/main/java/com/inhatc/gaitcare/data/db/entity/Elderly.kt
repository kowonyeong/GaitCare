package com.inhatc.gaitcare.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "elderly",
    foreignKeys = [ForeignKey(
        entity = Institution::class,
        parentColumns = ["id"],
        childColumns = ["institutionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("institutionId")]
)
data class Elderly(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val institutionId: Long,
    val name: String,
    val age: Int,
    val gender: String,
    val roomNumber: String = "",
    val diagnosisNote: String = "",
    val careLevel: Int = 0,
    val contactPhone: String = "",
    val emergencyContact: String = "",
    val photoUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
