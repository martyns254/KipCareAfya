package com.example.kipcareafya.data.local.dao

import androidx.room.*
import com.example.kipcareafya.data.local.entity.OnboardingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OnboardingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(onboarding: OnboardingEntity)

    @Query("SELECT * FROM onboarding WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: Int): OnboardingEntity?

    @Query("SELECT * FROM onboarding WHERE userId = :userId LIMIT 1")
    fun observe(userId: Int): Flow<OnboardingEntity?>

    @Query("UPDATE onboarding SET isCompleted = 1 WHERE userId = :userId")
    suspend fun markCompleted(userId: Int)
}