package com.example.kipcareafya.data.local.dao

import androidx.room.*
import com.example.kipcareafya.data.local.entity.HealthLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: HealthLogEntity)

    @Delete
    suspend fun delete(log: HealthLogEntity)

    @Query("""
        SELECT * FROM health_logs
        WHERE userId = :userId
        ORDER BY recordedAt DESC
        LIMIT 5
    """)
    fun getRecentLogs(userId: Int): Flow<List<HealthLogEntity>>

    @Query("""
        SELECT * FROM health_logs
        WHERE userId = :userId AND recordedAt >= :since
        ORDER BY recordedAt DESC
    """)
    fun getLogsSince(userId: Int, since: Long): Flow<List<HealthLogEntity>>
}