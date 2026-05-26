package com.inhatc.gaitcare.data.repository

import com.inhatc.gaitcare.data.db.AppDatabase
import com.inhatc.gaitcare.data.db.entity.Elderly
import kotlinx.coroutines.flow.Flow

class ElderlyRepository(private val db: AppDatabase) {

    fun getAll(institutionId: Long): Flow<List<Elderly>> =
        db.elderlyDao().getAll(institutionId)

    fun search(institutionId: Long, query: String): Flow<List<Elderly>> =
        db.elderlyDao().search(institutionId, query)

    suspend fun insert(elderly: Elderly): Long =
        db.elderlyDao().insert(elderly)

    suspend fun update(elderly: Elderly) =
        db.elderlyDao().update(elderly)

    suspend fun delete(elderly: Elderly) =
        db.elderlyDao().delete(elderly)

    suspend fun findById(id: Long): Elderly? =
        db.elderlyDao().findById(id)

    suspend fun count(institutionId: Long): Int =
        db.elderlyDao().countByInstitution(institutionId)
}
