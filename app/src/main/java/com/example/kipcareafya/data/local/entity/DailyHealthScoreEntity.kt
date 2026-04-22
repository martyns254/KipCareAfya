package com.example.kipcareafya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_health_scores")
data class DailyHealthScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val score: Int,
    val date: String,
    val metricScore: Int,
    val waterScore: Int,
    val stepsScore: Int,
    val consistencyScore: Int,
    val calculatedAt: Long = System.currentTimeMillis()
)