package com.gaitcare.data

data class Elder(
    val id: String,
    val name: String,
    val age: Int,
    val roomNumber: String
)

data class GaitRecord(
    val id: String,
    val date: String,
    val durationSec: Int,
    val summary: String
)
