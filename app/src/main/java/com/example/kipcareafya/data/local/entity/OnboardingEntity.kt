package com.example.kipcareafya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "onboarding")
data class OnboardingEntity(
    @PrimaryKey val userId: Int,
    val dateOfBirth: String?,
    val heightCm: Float?,
    val weightKg: Float?,
    val gender: String?,
    val activityLevel: String?,
    val waterGoalMl: Int = 2500,
    val stepGoal: Int = 8000,
    val isCompleted: Boolean = false
)