package com.example.kipcareafya.data.local.dao

import androidx.room.*
import com.example.kipcareafya.data.local.entity.SymptomCategoryEntity
import com.example.kipcareafya.data.local.entity.SymptomEntity
import com.example.kipcareafya.data.local.entity.SymptomLogEntity
import com.example.kipcareafya.data.local.entity.SymptomRecommendationEntity
import kotlinx.coroutines.flow.Flow

data class SymptomCount(
    val symptomName: String,
    val count: Int
)

@Dao
interface SymptomDao {

    // --- Catalogue ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<SymptomCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSymptoms(symptoms: List<SymptomEntity>)

    @Query("SELECT * FROM symptom_categories ORDER BY name")
    fun getCategories(): Flow<List<SymptomCategoryEntity>>

    @Query("SELECT * FROM symptoms WHERE categoryId = :categoryId")
    fun getSymptomsForCategory(categoryId: Int): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptoms ORDER BY name")
    fun getAllSymptoms(): Flow<List<SymptomEntity>>

    // --- Logs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SymptomLogEntity)

    @Query("""
        SELECT * FROM symptoms_log
        WHERE userId = :userId AND recordedAt >= :since
        ORDER BY recordedAt DESC
        LIMIT 30
    """)
    fun getLogs(userId: Int, since: Long): Flow<List<SymptomLogEntity>>

    @Query("""
        SELECT symptomName, COUNT(*) as count
        FROM symptoms_log
        WHERE userId = :userId
        GROUP BY symptomName
        ORDER BY count DESC
        LIMIT 5
    """)
    fun getTopSymptoms(userId: Int): Flow<List<SymptomCount>>

    // --- Recommendations ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecommendations(recs: List<SymptomRecommendationEntity>)

    @Query("""
        SELECT * FROM symptom_recommendations
        WHERE symptomCombination = :combo AND severityLevel = :severity
        LIMIT 1
    """)
    suspend fun findExactRecommendation(
        combo: String,
        severity: String
    ): SymptomRecommendationEntity?

    @Query("""
        SELECT * FROM symptom_recommendations
        WHERE symptomCombination LIKE '%' || :symptom || '%'
        AND severityLevel = :severity
        LIMIT 1
    """)
    suspend fun findPartialRecommendation(
        symptom: String,
        severity: String
    ): SymptomRecommendationEntity?

    @Query("SELECT COUNT(*) FROM symptom_categories")
    suspend fun getCategoryCount(): Int
}