package com.example.kipcareafya.data.local.dao

import androidx.room.*
import com.example.kipcareafya.data.local.entity.WaterIntakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterIntakeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WaterIntakeEntity)

    @Query("""
        SELECT * FROM water_intake
        WHERE userId = :userId AND recordedAt >= :since
        ORDER BY recordedAt DESC
    """)
    fun getEntriesSince(userId: Int, since: Long): Flow<List<WaterIntakeEntity>>

    @Query("""
        SELECT COALESCE(SUM(amountMl), 0) FROM water_intake
        WHERE userId = :userId AND recordedAt >= :since
    """)
    fun getTotalMlSince(userId: Int, since: Long): Flow<Int>

    @Query("DELETE FROM water_intake WHERE userId = :userId AND recordedAt < :before")
    suspend fun deleteOldEntries(userId: Int, before: Long)
}