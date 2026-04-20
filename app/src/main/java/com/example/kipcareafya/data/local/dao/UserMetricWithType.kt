package com.example.kipcareafya.data.local.dao

data class UserMetricWithType(
    val id: Int,
    val userId: Int,
    val metricId: Int,
    val value: Float,
    val notes: String?,
    val recordedAt: Long,
    val name: String,
    val displayName: String,
    val unit: String?
)