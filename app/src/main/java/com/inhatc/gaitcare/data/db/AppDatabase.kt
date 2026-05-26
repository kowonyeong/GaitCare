package com.inhatc.gaitcare.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inhatc.gaitcare.data.db.dao.ElderlyDao
import com.inhatc.gaitcare.data.db.dao.GaitSessionDao
import com.inhatc.gaitcare.data.db.dao.InstitutionDao
import com.inhatc.gaitcare.data.db.entity.Elderly
import com.inhatc.gaitcare.data.db.entity.GaitSession
import com.inhatc.gaitcare.data.db.entity.Institution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

@Database(
    entities = [Institution::class, Elderly::class, GaitSession::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun institutionDao(): InstitutionDao
    abstract fun elderlyDao(): ElderlyDao
    abstract fun gaitSessionDao(): GaitSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gaitcare_db"
                )
                    .addCallback(SeedCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun hashPassword(password: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(password.toByteArray())
            return digest.fold("") { str, b -> str + "%02x".format(b) }
        }
    }

    private class SeedCallback(private val context: Context) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context)
                seedData(database)
            }
        }

        private suspend fun seedData(db: AppDatabase) {
            // 샘플 기관 등록
            val institution = Institution(
                institutionCode = "DEMO001",
                institutionName = "행복요양원",
                businessNumber = "1234567890",
                address = "서울시 강남구 테헤란로 123",
                institutionPhone = "02-1234-5678",
                managerName = "김관리",
                userId = "admin",
                passwordHash = hashPassword("admin123!"),
                managerPhone = "010-1234-5678",
                email = "admin@happycare.com",
                isApproved = true
            )
            val institutionId = db.institutionDao().insert(institution)

            // 샘플 어르신 등록
            val elderlyList = listOf(
                Elderly(institutionId = institutionId, name = "이순자", age = 78, gender = "여", roomNumber = "101호", careLevel = 2, diagnosisNote = "무릎 관절염", contactPhone = "010-2345-6789"),
                Elderly(institutionId = institutionId, name = "박철수", age = 82, gender = "남", roomNumber = "102호", careLevel = 3, diagnosisNote = "파킨슨병 초기", contactPhone = "010-3456-7890"),
                Elderly(institutionId = institutionId, name = "최영희", age = 75, gender = "여", roomNumber = "103호", careLevel = 1, diagnosisNote = "고혈압", contactPhone = "010-4567-8901"),
                Elderly(institutionId = institutionId, name = "정대원", age = 80, gender = "남", roomNumber = "201호", careLevel = 2, diagnosisNote = "당뇨, 보행 불안정", contactPhone = "010-5678-9012"),
                Elderly(institutionId = institutionId, name = "강미숙", age = 72, gender = "여", roomNumber = "202호", careLevel = 1, diagnosisNote = "골다공증", contactPhone = "010-6789-0123")
            )

            val elderlyIds = mutableListOf<Long>()
            elderlyList.forEach { elderly ->
                elderlyIds.add(db.elderlyDao().insert(elderly))
            }

            // 샘플 보행 데이터 (최근 5회 측정)
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L

            val sampleSessions = listOf(
                // 이순자 - 회복 추세
                GaitSession(elderlyId = elderlyIds[0], measuredAt = now - 8 * dayMs, durationSeconds = 60, totalScore = 62, shakinessScore = 58, rhythmScore = 65, symmetryScore = 60, rotationScore = 63, cadenceScore = 64, stepCount = 0, cadenceSpm = 0.13f, avgStepIntervalMs = 810f, stepIntervalCvPercent = 12.5f, lateralRmsG = 0.18f, verticalRmsG = 0.22f, symmetryIndexPercent = 8.5f, avgRotationDegSec = 42f, memo = "보행 시 약간의 불균형 관찰"),
                GaitSession(elderlyId = elderlyIds[0], measuredAt = now - 6 * dayMs, durationSeconds = 60, totalScore = 67, shakinessScore = 63, rhythmScore = 70, symmetryScore = 65, rotationScore = 68, cadenceScore = 69, stepCount = 0, cadenceSpm = 0.16f, avgStepIntervalMs = 769f, stepIntervalCvPercent = 10.2f, lateralRmsG = 0.15f, verticalRmsG = 0.20f, symmetryIndexPercent = 7.1f, avgRotationDegSec = 38f, memo = "전주 대비 호전"),
                GaitSession(elderlyId = elderlyIds[0], measuredAt = now - 4 * dayMs, durationSeconds = 60, totalScore = 71, shakinessScore = 68, rhythmScore = 74, symmetryScore = 69, rotationScore = 72, cadenceScore = 72, stepCount = 0, cadenceSpm = 0.19f, avgStepIntervalMs = 732f, stepIntervalCvPercent = 8.8f, lateralRmsG = 0.13f, verticalRmsG = 0.18f, symmetryIndexPercent = 5.9f, avgRotationDegSec = 35f, memo = "꾸준히 향상 중"),
                GaitSession(elderlyId = elderlyIds[0], measuredAt = now - 2 * dayMs, durationSeconds = 60, totalScore = 75, shakinessScore = 72, rhythmScore = 78, symmetryScore = 73, rotationScore = 76, cadenceScore = 76, stepCount = 0, cadenceSpm = 0.21f, avgStepIntervalMs = 698f, stepIntervalCvPercent = 7.5f, lateralRmsG = 0.11f, verticalRmsG = 0.16f, symmetryIndexPercent = 4.8f, avgRotationDegSec = 32f, memo = ""),
                GaitSession(elderlyId = elderlyIds[0], measuredAt = now - dayMs, durationSeconds = 60, totalScore = 78, shakinessScore = 75, rhythmScore = 81, symmetryScore = 76, rotationScore = 79, cadenceScore = 79, stepCount = 0, cadenceSpm = 0.23f, avgStepIntervalMs = 674f, stepIntervalCvPercent = 6.8f, lateralRmsG = 0.10f, verticalRmsG = 0.15f, symmetryIndexPercent = 4.2f, avgRotationDegSec = 30f, memo = ""),

                // 박철수 - 파킨슨으로 낮은 점수
                GaitSession(elderlyId = elderlyIds[1], measuredAt = now - 5 * dayMs, durationSeconds = 45, totalScore = 41, shakinessScore = 35, rhythmScore = 40, symmetryScore = 42, rotationScore = 44, cadenceScore = 44, stepCount = 0, cadenceSpm = 0.09f, avgStepIntervalMs = 937f, stepIntervalCvPercent = 22.1f, lateralRmsG = 0.32f, verticalRmsG = 0.38f, symmetryIndexPercent = 18.5f, avgRotationDegSec = 68f, memo = "떨림 증상 관찰, 보행 보조 필요"),
                GaitSession(elderlyId = elderlyIds[1], measuredAt = now - 2 * dayMs, durationSeconds = 45, totalScore = 38, shakinessScore = 32, rhythmScore = 37, symmetryScore = 39, rotationScore = 41, cadenceScore = 41, stepCount = 0, cadenceSpm = 0.07f, avgStepIntervalMs = 1017f, stepIntervalCvPercent = 24.8f, lateralRmsG = 0.35f, verticalRmsG = 0.42f, symmetryIndexPercent = 20.1f, avgRotationDegSec = 72f, memo = "상태 약간 악화, 의료진 보고 필요"),

                // 최영희 - 양호한 상태
                GaitSession(elderlyId = elderlyIds[2], measuredAt = now - 3 * dayMs, durationSeconds = 60, totalScore = 85, shakinessScore = 83, rhythmScore = 87, symmetryScore = 84, rotationScore = 86, cadenceScore = 85, stepCount = 0, cadenceSpm = 0.28f, avgStepIntervalMs = 625f, stepIntervalCvPercent = 5.2f, lateralRmsG = 0.08f, verticalRmsG = 0.12f, symmetryIndexPercent = 3.1f, avgRotationDegSec = 25f, memo = ""),
                GaitSession(elderlyId = elderlyIds[2], measuredAt = now - dayMs, durationSeconds = 60, totalScore = 87, shakinessScore = 85, rhythmScore = 89, symmetryScore = 86, rotationScore = 88, cadenceScore = 87, stepCount = 0, cadenceSpm = 0.30f, avgStepIntervalMs = 612f, stepIntervalCvPercent = 4.8f, lateralRmsG = 0.07f, verticalRmsG = 0.11f, symmetryIndexPercent = 2.8f, avgRotationDegSec = 23f, memo = "")
            )

            sampleSessions.forEach { session ->
                db.gaitSessionDao().insert(session)
            }
        }
    }
}
