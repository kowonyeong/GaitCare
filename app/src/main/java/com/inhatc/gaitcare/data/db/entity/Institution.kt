package com.inhatc.gaitcare.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "institutions",
    indices = [Index(value = ["institutionCode"], unique = true), Index(value = ["userId"], unique = true)]
)
data class Institution(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val institutionCode: String,
    val institutionName: String,
    val businessNumber: String,
    val address: String,
    val institutionPhone: String,
    val managerName: String,
    val userId: String,
    val passwordHash: String,
    val managerPhone: String,
    val email: String,
    val agreeMarketing: Boolean = false,
    val isApproved: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
