package com.example.kipcareafya.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kipcareafya.data.local.dao.*
import com.example.kipcareafya.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        MetricTypeEntity::class,
        UserMetricEntity::class,
        HealthLogEntity::class,
        HealthGoalEntity::class,
        MedicalHistoryEntity::class,
        MedicationEntity::class,
        MedicationReminderEntity::class,
        SymptomCategoryEntity::class,
        SymptomEntity::class,
        SymptomLogEntity::class,
        SymptomRecommendationEntity::class,
        WaterIntakeEntity::class,
        DailyHealthScoreEntity::class,
        OnboardingEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun userMetricDao(): UserMetricDao
    abstract fun healthLogDao(): HealthLogDao
    abstract fun goalDao(): GoalDao
    abstract fun medicalHistoryDao(): MedicalHistoryDao
    abstract fun symptomDao(): SymptomDao
    abstract fun waterIntakeDao(): WaterIntakeDao
    abstract fun healthScoreDao(): HealthScoreDao
    abstract fun onboardingDao(): OnboardingDao

    companion object {
        @Volatile private var INSTANCE: HealthDatabase? = null

        fun getInstance(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).let { database ->
                                    seedMetricTypes(database)
                                    seedSymptomCatalogue(database)
                                    seedRecommendations(database)
                                }
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private suspend fun seedMetricTypes(db: HealthDatabase) {
            db.userMetricDao().insertMetricTypes(listOf(
                MetricTypeEntity(name = "blood_glucose",
                    displayName = "Blood Glucose",
                    description = "Blood sugar level", unit = "mg/dL",
                    category = "Biochemical", minNormal = 70f, maxNormal = 100f),
                MetricTypeEntity(name = "total_cholesterol",
                    displayName = "Total Cholesterol",
                    description = "Total blood cholesterol level", unit = "mg/dL",
                    category = "Biochemical", minNormal = null, maxNormal = 200f),
                MetricTypeEntity(name = "hydration",
                    displayName = "Hydration",
                    description = "Daily water intake", unit = "ml",
                    category = "Lifestyle", minNormal = 2000f, maxNormal = 3000f),
                MetricTypeEntity(name = "bmi", displayName = "BMI",
                    description = "Body Mass Index", unit = "kg/m²",
                    category = "Physical", minNormal = 18.5f, maxNormal = 24.9f),
                MetricTypeEntity(name = "height", displayName = "Height",
                    description = "Body height measurement", unit = "cm",
                    category = "Physical", minNormal = null, maxNormal = null),
                MetricTypeEntity(name = "weight", displayName = "Weight",
                    description = "Body weight measurement", unit = "kg",
                    category = "Physical", minNormal = null, maxNormal = null),
                MetricTypeEntity(name = "blood_pressure_systolic",
                    displayName = "Blood Pressure (Systolic)",
                    description = "Upper blood pressure reading", unit = "mmHg",
                    category = "Cardiovascular", minNormal = 90f, maxNormal = 120f),
                MetricTypeEntity(name = "blood_pressure_diastolic",
                    displayName = "Blood Pressure (Diastolic)",
                    description = "Lower blood pressure reading", unit = "mmHg",
                    category = "Cardiovascular", minNormal = 60f, maxNormal = 80f),
                MetricTypeEntity(name = "heart_rate",
                    displayName = "Heart Rate",
                    description = "Resting heart rate", unit = "bpm",
                    category = "Cardiovascular", minNormal = 60f, maxNormal = 100f),
                MetricTypeEntity(name = "oxygen_saturation",
                    displayName = "Oxygen Saturation",
                    description = "Blood oxygen level", unit = "%",
                    category = "Cardiovascular", minNormal = 95f, maxNormal = 100f),
            ))
        }

        private suspend fun seedSymptomCatalogue(db: HealthDatabase) {
            if (db.symptomDao().getCategoryCount() > 0) return
            val generalId = 1
            val respiratoryId = 2
            val digestiveId = 3
            val neurologicalId = 4
            db.symptomDao().insertCategories(listOf(
                SymptomCategoryEntity(id = generalId, name = "General Symptoms"),
                SymptomCategoryEntity(id = respiratoryId, name = "Respiratory Symptoms"),
                SymptomCategoryEntity(id = digestiveId, name = "Digestive Symptoms"),
                SymptomCategoryEntity(id = neurologicalId, name = "Neurological Symptoms"),
            ))
            db.symptomDao().insertSymptoms(listOf(
                SymptomEntity(categoryId = generalId, name = "Fever",
                    description = "Elevated body temperature"),
                SymptomEntity(categoryId = generalId, name = "Fatigue",
                    description = "Feeling tired or exhausted"),
                SymptomEntity(categoryId = generalId, name = "Weakness",
                    description = "General body weakness"),
                SymptomEntity(categoryId = generalId, name = "Chills",
                    description = "Feeling cold with shivering"),
                SymptomEntity(categoryId = respiratoryId, name = "Cough",
                    description = "Persistent coughing"),
                SymptomEntity(categoryId = respiratoryId, name = "Shortness of Breath",
                    description = "Difficulty breathing"),
                SymptomEntity(categoryId = respiratoryId, name = "Sore Throat",
                    description = "Pain or irritation in the throat"),
                SymptomEntity(categoryId = respiratoryId, name = "Runny Nose",
                    description = "Nasal discharge"),
                SymptomEntity(categoryId = digestiveId, name = "Nausea",
                    description = "Feeling of wanting to vomit"),
                SymptomEntity(categoryId = digestiveId, name = "Vomiting",
                    description = "Forceful emptying of stomach"),
                SymptomEntity(categoryId = digestiveId, name = "Diarrhea",
                    description = "Loose or watery stools"),
                SymptomEntity(categoryId = digestiveId, name = "Abdominal Pain",
                    description = "Pain in the stomach area"),
                SymptomEntity(categoryId = neurologicalId, name = "Headache",
                    description = "Pain in the head"),
                SymptomEntity(categoryId = neurologicalId, name = "Dizziness",
                    description = "Feeling lightheaded or unsteady"),
                SymptomEntity(categoryId = neurologicalId, name = "Confusion",
                    description = "Difficulty thinking clearly"),
            ))
        }

        private suspend fun seedRecommendations(db: HealthDatabase) {
            db.symptomDao().insertRecommendations(listOf(
                SymptomRecommendationEntity(
                    symptomCombination = "chills,fatigue,fever",
                    severityLevel = "moderate",
                    recommendation = "Take paracetamol as directed. Rest and drink plenty of fluids.",
                    warningSigns = "Temperature above 39.4°C, severe headache, stiff neck",
                    basicTreatment = "Rest, hydration, paracetamol for fever",
                    seekDoctorIf = "Fever persists more than 3 days",
                    emergencyLevel = "medium"),
                SymptomRecommendationEntity(
                    symptomCombination = "cough,fever,shortness of breath",
                    severityLevel = "moderate",
                    recommendation = "Monitor oxygen levels. Rest and stay hydrated.",
                    warningSigns = "Oxygen saturation below 94%",
                    basicTreatment = "Rest, steam inhalation, warm fluids",
                    seekDoctorIf = "Breathing difficulty worsens",
                    emergencyLevel = "medium"),
                SymptomRecommendationEntity(
                    symptomCombination = "diarrhea,nausea,vomiting",
                    severityLevel = "mild",
                    recommendation = "Stay hydrated with oral rehydration salts.",
                    warningSigns = "Signs of dehydration",
                    basicTreatment = "ORS, BRAT diet, rest",
                    seekDoctorIf = "Symptoms persist beyond 48 hours",
                    emergencyLevel = "low"),
                SymptomRecommendationEntity(
                    symptomCombination = "headache,fever",
                    severityLevel = "mild",
                    recommendation = "Take paracetamol, rest in a quiet dark room.",
                    warningSigns = "Sudden severe headache, stiff neck",
                    basicTreatment = "Paracetamol, rest, hydration",
                    seekDoctorIf = "Headache is worst of life",
                    emergencyLevel = "low"),
            ))
        }
    }
}