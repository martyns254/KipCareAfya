package com.example.kipcareafya.data.local.dao

import androidx.room.*
import com.example.kipcareafya.data.local.entity.MetricTypeEntity
import com.example.kipcareafya.data.local.entity.UserMetricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMetricDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetricType(type: MetricTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetricTypes(types: List<MetricTypeEntity>)

    @Query("SELECT * FROM health_metrics ORDER BY category, displayName")
    fun getAllMetricTypes(): Flow<List<MetricTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMetric(metric: UserMetricEntity)

    @Query("""
        SELECT um.id, um.userId, um.metricId, um.value, um.notes, um.recordedAt,
               hm.name, hm.displayName, hm.unit
        FROM user_metrics um
        JOIN health_metrics hm ON um.metricId = hm.id
        WHERE um.userId = :userId AND um.recordedAt >= :since
        ORDER BY um.recordedAt DESC
    """)
    fun getMetricsWithType(userId: Int, since: Long): Flow<List<UserMetricWithType>>

    @Query("""
        SELECT * FROM user_metrics
        WHERE userId = :userId AND metricId = :metricId AND recordedAt >= :since
        ORDER BY recordedAt ASC
    """)
    fun getTrend(userId: Int, metricId: Int, since: Long): Flow<List<UserMetricEntity>>

    @Query("SELECT COUNT(*) FROM health_metrics")
    suspend fun getMetricTypeCount(): Int

    @Query("""
        SELECT value FROM user_metrics
        WHERE userId = :userId AND metricId = :metricId AND recordedAt >= :since
        ORDER BY recordedAt DESC
        LIMIT 1
    """)
    suspend fun getLatestValue(userId: Int, metricId: Int, since: Long): Float?
}