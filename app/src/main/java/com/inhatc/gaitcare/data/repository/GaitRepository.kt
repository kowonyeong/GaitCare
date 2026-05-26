package com.inhatc.gaitcare.data.repository

import com.inhatc.gaitcare.data.db.AppDatabase
import com.inhatc.gaitcare.data.db.entity.GaitSession
import kotlinx.coroutines.flow.Flow

class GaitRepository(private val db: AppDatabase) {

    fun getByElderly(elderlyId: Long): Flow<List<GaitSession>> =
        db.gaitSessionDao().getByElderly(elderlyId)

    suspend fun insert(session: GaitSession): Long =
        db.gaitSessionDao().insert(session)

    suspend fun delete(session: GaitSession) =
        db.gaitSessionDao().delete(session)

    suspend fun getLatest(elderlyId: Long): GaitSession? =
        db.gaitSessionDao().getLatest(elderlyId)

    suspend fun getRecent(elderlyId: Long, limit: Int = 10): List<GaitSession> =
        db.gaitSessionDao().getRecent(elderlyId, limit)

    suspend fun getAverageScore(elderlyId: Long): Float? =
        db.gaitSessionDao().getAverageScore(elderlyId)

    suspend fun count(elderlyId: Long): Int =
        db.gaitSessionDao().countByElderly(elderlyId)

    suspend fun findById(id: Long): GaitSession? =
        db.gaitSessionDao().findById(id)

    suspend fun update(session: GaitSession) =
        db.gaitSessionDao().update(session)
}
