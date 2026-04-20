package com.example.kipcareafya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_goals")
data class HealthGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val title: String,
    val metricType: String?,
    val targetValue: Float?,
    val currentValue: Float?,
    val targetDate: Long?,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)