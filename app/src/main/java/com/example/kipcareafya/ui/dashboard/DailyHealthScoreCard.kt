package com.example.kipcareafya.ui.dashboard

import android.app.Application
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.data.local.dao.UserMetricWithType
import com.example.kipcareafya.domain.HealthScoreCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.ui.draw.clip


data class HealthScoreUiState(
    val score: Int = 0,
    val breakdown: HealthScoreCalculator.ScoreBreakdown? = null,
    val isLoading: Boolean = true
)

class HealthScoreViewModel(app: Application) : AndroidViewModel(app) {

    private val db = HealthDatabase.getInstance(app)
    private val metricDao = db.userMetricDao()
    private val waterDao = db.waterIntakeDao()
    private val onboardingDao = db.onboardingDao()
    private val healthLogDao = db.healthLogDao()

    private val _uiState = MutableStateFlow(HealthScoreUiState())
    val uiState: StateFlow<HealthScoreUiState> = _uiState.asStateFlow()

    private val userId = 1

    init {
        viewModelScope.launch { computeScore() }
    }

    private suspend fun computeScore() {
        val onboarding = onboardingDao.get(userId)
        val waterGoal = onboarding?.waterGoalMl ?: 2500
        val stepGoal = onboarding?.stepGoal ?: 8000

        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

        // Collect all needed data
        combine(
            metricDao.getMetricsWithType(userId, thirtyDaysAgo),
            waterDao.getTotalMlSince(userId, startOfDay),
            healthLogDao.getLogsSince(userId, sevenDaysAgo)
        ) { metrics, waterMl, logs ->
            Triple(metrics, waterMl, logs)
        }.collect { (metrics, waterMl, logs) ->

            // Get latest per metric
            val latestPerMetric = metrics
                .groupBy { it.name }
                .mapValues { (_, entries) ->
                    entries.maxByOrNull { it.recordedAt }
                }
                .values
                .filterNotNull()

            // Count unique days logged in last 7
            val uniqueDays = logs.map {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date(it.recordedAt))
            }.toSet().size

            val breakdown = HealthScoreCalculator.calculate(
                recentMetrics = latestPerMetric,
                waterIntakeMl = waterMl,
                waterGoalMl = waterGoal,
                stepCount = 0, // will be updated from sensor
                stepGoal = stepGoal,
                logsLast7Days = uniqueDays
            )

            _uiState.update {
                it.copy(
                    score = breakdown.total,
                    breakdown = breakdown,
                    isLoading = false
                )
            }
        }
    }
}

@Composable
fun DailyHealthScoreCard(
    vm: HealthScoreViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val breakdown = uiState.breakdown

    val animatedScore by animateIntAsState(
        targetValue = uiState.score,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "health_score"
    )

    val scoreColor = breakdown?.gradeColor?.let { Color(it) }
        ?: MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Daily Health Score",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (breakdown != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = scoreColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            breakdown.grade,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 4.dp
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Score gauge
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 12.dp.toPx()
                        val padding = stroke / 2
                        val progress = animatedScore / 100f

                        // Background
                        drawArc(
                            color = scoreColor.copy(alpha = 0.15f),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = Offset(padding, padding),
                            size = Size(
                                size.width - stroke,
                                size.height - stroke
                            ),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        // Progress
                        drawArc(
                            color = scoreColor,
                            startAngle = 135f,
                            sweepAngle = 270f * progress,
                            useCenter = false,
                            topLeft = Offset(padding, padding),
                            size = Size(
                                size.width - stroke,
                                size.height - stroke
                            ),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$animatedScore",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                        Text(
                            "/ 100",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Breakdown
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (breakdown != null) {
                        ScoreBreakdownRow(
                            label = "Metrics",
                            score = breakdown.metricScore,
                            maxScore = 40,
                            color = Color(0xFF7B1FA2)
                        )
                        ScoreBreakdownRow(
                            label = "Hydration",
                            score = breakdown.waterScore,
                            maxScore = 20,
                            color = Color(0xFF0277BD)
                        )
                        ScoreBreakdownRow(
                            label = "Activity",
                            score = breakdown.stepsScore,
                            maxScore = 20,
                            color = Color(0xFF2E7D32)
                        )
                        ScoreBreakdownRow(
                            label = "Consistency",
                            score = breakdown.consistencyScore,
                            maxScore = 20,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            if (breakdown != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    breakdown.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = scoreColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ScoreBreakdownRow(
    label: String,
    score: Int,
    maxScore: Int,
    color: Color
) {
    val progress = if (maxScore > 0) score.toFloat() / maxScore else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$score/$maxScore",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(androidx.compose.foundation.shape.CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}