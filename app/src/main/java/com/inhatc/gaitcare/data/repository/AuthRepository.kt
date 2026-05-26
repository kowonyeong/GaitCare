package com.inhatc.gaitcare.data.repository

import com.inhatc.gaitcare.data.db.AppDatabase
import com.inhatc.gaitcare.data.db.entity.Institution
import com.inhatc.gaitcare.model.RegisterRequest

class AuthRepository(private val db: AppDatabase) {

    suspend fun login(institutionCode: String, userId: String, password: String): Institution? {
        val passwordHash = AppDatabase.hashPassword(password)
        return db.institutionDao().login(institutionCode, userId, passwordHash)
    }

    suspend fun register(request: RegisterRequest): Result<Institution> {
        return try {
            val codeCount = db.institutionDao().countByCode(generateCode(request.institutionName))
            val userIdCount = db.institutionDao().countByUserId(request.userId)

            if (userIdCount > 0) {
                return Result.failure(Exception("이미 사용 중인 아이디입니다"))
            }

            val code = generateUniqueCode(request.institutionName)
            val institution = Institution(
                institutionCode = code,
                institutionName = request.institutionName,
                businessNumber = request.businessNumber,
                address = request.address,
                institutionPhone = request.institutionPhone,
                managerName = request.managerName,
                userId = request.userId,
                passwordHash = AppDatabase.hashPassword(request.password),
                managerPhone = request.managerPhone,
                email = request.email,
                agreeMarketing = request.agreeMarketing,
                isApproved = true
            )
            val id = db.institutionDao().insert(institution)
            Result.success(institution.copy(id = id))
        } catch (e: Exception) {
            Result.failure(Exception("등록 중 오류가 발생했습니다: ${e.message}"))
        }
    }

    private suspend fun generateUniqueCode(name: String): String {
        var code = generateCode(name)
        var attempt = 0
        while (db.institutionDao().countByCode(code) > 0) {
            attempt++
            code = generateCode(name) + attempt
        }
        return code
    }

    private fun generateCode(name: String): String {
        val prefix = name.take(3).uppercase().filter { it.isLetterOrDigit() }.padEnd(3, 'X')
        val num = (1000..9999).random()
        return "$prefix$num"
    }
}
