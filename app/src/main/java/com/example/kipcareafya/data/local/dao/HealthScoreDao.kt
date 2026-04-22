package com.example.kipcareafya.data.local.dao

import androidx.room.*
import com.example.kipcareafya.data.local.entity.DailyHealthScoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthScoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: DailyHealthScoreEntity)

    @Query("""
        SELECT * FROM daily_health_scores
        WHERE userId = :userId
        ORDER BY calculatedAt DESC
        LIMIT 1
    """)
    fun getLatestScore(userId: Int): Flow<DailyHealthScoreEntity?>

    @Query("""
        SELECT * FROM daily_health_scores
        WHERE userId = :userId
        ORDER BY calculatedAt DESC
        LIMIT 7
    """)
    fun getWeeklyScores(userId: Int): Flow<List<DailyHealthScoreEntity>>
}