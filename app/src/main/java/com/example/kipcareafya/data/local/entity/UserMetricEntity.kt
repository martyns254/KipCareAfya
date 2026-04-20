package com.example.kipcareafya.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_metrics",
    foreignKeys = [ForeignKey(
        entity = MetricTypeEntity::class,
        parentColumns = ["id"],
        childColumns = ["metricId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("metricId")]
)
data class UserMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val metricId: Int,
    val value: Float,
    val notes: String?,
    val recordedAt: Long = System.currentTimeMillis()
)