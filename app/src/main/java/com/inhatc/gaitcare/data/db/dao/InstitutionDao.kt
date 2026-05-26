package com.inhatc.gaitcare.data.db.dao

import androidx.room.*
import com.inhatc.gaitcare.data.db.entity.Institution
import kotlinx.coroutines.flow.Flow

@Dao
interface InstitutionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(institution: Institution): Long

    @Query("SELECT * FROM institutions WHERE institutionCode = :code AND userId = :userId AND passwordHash = :passwordHash LIMIT 1")
    suspend fun login(code: String, userId: String, passwordHash: String): Institution?

    @Query("SELECT * FROM institutions WHERE institutionCode = :code LIMIT 1")
    suspend fun findByCode(code: String): Institution?

    @Query("SELECT * FROM institutions WHERE userId = :userId LIMIT 1")
    suspend fun findByUserId(userId: String): Institution?

    @Query("SELECT * FROM institutions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Institution?

    @Update
    suspend fun update(institution: Institution)

    @Query("SELECT COUNT(*) FROM institutions WHERE institutionCode = :code")
    suspend fun countByCode(code: String): Int

    @Query("SELECT COUNT(*) FROM institutions WHERE userId = :userId")
    suspend fun countByUserId(userId: String): Int
}
