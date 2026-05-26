package com.inhatc.gaitcare.data.db.dao

import androidx.room.*
import com.inhatc.gaitcare.data.db.entity.Elderly
import kotlinx.coroutines.flow.Flow

@Dao
interface ElderlyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(elderly: Elderly): Long

    @Update
    suspend fun update(elderly: Elderly)

    @Delete
    suspend fun delete(elderly: Elderly)

    @Query("SELECT * FROM elderly WHERE institutionId = :institutionId ORDER BY name ASC")
    fun getAll(institutionId: Long): Flow<List<Elderly>>

    @Query("SELECT * FROM elderly WHERE institutionId = :institutionId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(institutionId: Long, query: String): Flow<List<Elderly>>

    @Query("SELECT * FROM elderly WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Elderly?

    @Query("SELECT COUNT(*) FROM elderly WHERE institutionId = :institutionId")
    suspend fun countByInstitution(institutionId: Long): Int
}
