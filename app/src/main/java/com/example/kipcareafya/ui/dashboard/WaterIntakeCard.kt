package com.example.kipcareafya.ui.dashboard

import android.app.Application
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.data.local.entity.WaterIntakeEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

// ─── ViewModel ───────────────────────────────────────────────

data class WaterUiState(
    val totalMlToday: Int = 0,
    val goalMl: Int = 2500,
    val entries: List<WaterIntakeEntity> = emptyList()
)

class WaterViewModel(app: Application) : AndroidViewModel(app) {

    private val db = HealthDatabase.getInstance(app)
    private val waterDao = db.waterIntakeDao()
    private val onboardingDao = db.onboardingDao()

    private val _uiState = MutableStateFlow(WaterUiState())
    val uiState: StateFlow<WaterUiState> = _uiState.asStateFlow()

    private val userId = 1

    init {
        viewModelScope.launch {
            // Load goal from onboarding
            val onboarding = onboardingDao.get(userId)
            val goal = onboarding?.waterGoalMl ?: 2500
            _uiState.update { it.copy(goalMl = goal) }

            // Load today's intake
            val startOfDay = getStartOfDay()
            waterDao.getTotalMlSince(userId, startOfDay).collect { total ->
                _uiState.update { it.copy(totalMlToday = total) }
            }
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            waterDao.insert(
                WaterIntakeEntity(
                    userId = userId,
                    amountMl = amountMl
                )
            )
        }
    }

    private fun getStartOfDay(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

// ─── Composable ───────────────────────────────────────────────

@Composable
fun WaterIntakeCard(
    vm: WaterViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val progress = if (uiState.goalMl > 0)
        (uiState.totalMlToday.toFloat() / uiState.goalMl).coerceIn(0f, 1f)
    else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "water_progress"
    )

    val waterColor = Color(0xFF0277BD)
    val waterColorLight = Color(0xFF81D4FA)

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = waterColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "Water Intake",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    "${uiState.totalMlToday} / ${uiState.goalMl} ml",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated arc progress ring
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val stroke = 14.dp.toPx()
                    val padding = stroke / 2

                    // Background arc
                    drawArc(
                        color = waterColorLight.copy(alpha = 0.2f),
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = Offset(padding, padding),
                        size = Size(
                            size.width - stroke,
                            size.height - stroke
                        ),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = waterColor,
                        startAngle = 150f,
                        sweepAngle = 240f * animatedProgress,
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
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = waterColor
                    )
                    Text(
                        "of goal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick add buttons
            Text(
                "Quick add",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(150 to "150ml", 200 to "200ml",
                    250 to "250ml", 500 to "500ml").forEach { (ml, label) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { vm.addWater(ml) },
                        color = waterColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 4.dp,
                                vertical = 8.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = waterColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = waterColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}