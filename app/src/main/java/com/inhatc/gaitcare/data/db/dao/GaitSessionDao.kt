package com.inhatc.gaitcare.data.db.dao

import androidx.room.*
import com.inhatc.gaitcare.data.db.entity.GaitSession
import kotlinx.coroutines.flow.Flow

@Dao
interface GaitSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: GaitSession): Long

    @Delete
    suspend fun delete(session: GaitSession)

    @Query("SELECT * FROM gait_sessions WHERE elderlyId = :elderlyId ORDER BY measuredAt DESC")
    fun getByElderly(elderlyId: Long): Flow<List<GaitSession>>

    @Query("SELECT * FROM gait_sessions WHERE elderlyId = :elderlyId ORDER BY measuredAt DESC LIMIT 1")
    suspend fun getLatest(elderlyId: Long): GaitSession?

    @Query("SELECT * FROM gait_sessions WHERE elderlyId = :elderlyId ORDER BY measuredAt DESC LIMIT :limit")
    suspend fun getRecent(elderlyId: Long, limit: Int): List<GaitSession>

    @Query("SELECT AVG(totalScore) FROM gait_sessions WHERE elderlyId = :elderlyId")
    suspend fun getAverageScore(elderlyId: Long): Float?

    @Query("SELECT COUNT(*) FROM gait_sessions WHERE elderlyId = :elderlyId")
    suspend fun countByElderly(elderlyId: Long): Int

    @Query("SELECT * FROM gait_sessions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): GaitSession?

    @Update
    suspend fun update(session: GaitSession)
}
