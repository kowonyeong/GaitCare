package com.gaitcare.data

object FakeRepository {
    val elders = listOf(
        Elder("e1", "김영수", 82, "301"),
        Elder("e2", "박정희", 79, "203"),
        Elder("e3", "이민자", 85, "117")
    )

    fun recordsOf(elderId: String): List<GaitRecord> {
        return listOf(
            GaitRecord("r1", "2026-05-10", 45, "보행 속도 양호"),
            GaitRecord("r2", "2026-05-05", 50, "균형 보조 필요")
        )
    }
}
