package com.example.kipcareafya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_metrics")
data class MetricTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val displayName: String,
    val description: String?,
    val unit: String?,
    val category: String,
    val minNormal: Float?,
    val maxNormal: Float?
)