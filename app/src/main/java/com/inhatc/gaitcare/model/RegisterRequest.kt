package com.inhatc.gaitcare.model

data class RegisterRequest(
    val institutionName: String,
    val businessNumber: String,
    val address: String,
    val institutionPhone: String,
    val managerName: String,
    val userId: String,
    val password: String,
    val managerPhone: String,
    val email: String,
    val agreeMarketing: Boolean
)
