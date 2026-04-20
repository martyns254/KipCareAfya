package com.example.kipcareafya.data.local.dao

import androidx.room.*
import com.example.kipcareafya.data.local.entity.HealthGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: HealthGoalEntity)

    @Update
    suspend fun update(goal: HealthGoalEntity)

    @Delete
    suspend fun delete(goal: HealthGoalEntity)

    @Query("SELECT * FROM health_goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGoals(userId: Int): Flow<List<HealthGoalEntity>>

    @Query("""
        SELECT * FROM health_goals
        WHERE userId = :userId AND isCompleted = 0
        ORDER BY targetDate ASC
    """)
    fun getActiveGoals(userId: Int): Flow<List<HealthGoalEntity>>
}