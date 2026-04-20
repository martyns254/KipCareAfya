package com.example.kipcareafya.ui.dashboard

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.data.local.dao.UserMetricWithType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─── ViewModel ───────────────────────────────────────────────

data class DashboardUiState(
    val username: String = "there",
    val totalRecords: Int = 0,
    val lastCheckDate: String = "--",
    val healthStatus: String = "Good",
    val healthStatusColor: Color = Color(0xFF43A047),
    val recentMetrics: List<UserMetricWithType> = emptyList(),
    val isLoading: Boolean = true
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val db = HealthDatabase.getInstance(app)
    private val metricDao = db.userMetricDao()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val userId = 1

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        viewModelScope.launch {
            metricDao.getMetricsWithType(userId, thirtyDaysAgo).collect { entries ->
                val totalRecords = entries.size
                val lastDate = entries.maxByOrNull { it.recordedAt }?.let {
                    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(it.recordedAt))
                } ?: "--"

                // Determine health status from latest metrics
                val status = computeHealthStatus(entries)

                _uiState.update {
                    it.copy(
                        totalRecords = totalRecords,
                        lastCheckDate = lastDate,
                        healthStatus = status.first,
                        healthStatusColor = status.second,
                        recentMetrics = entries.take(4),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun computeHealthStatus(entries: List<UserMetricWithType>): Pair<String, Color> {
        if (entries.isEmpty()) return "No data yet" to Color(0xFF9E9E9E)
        // Simple rule — if any recent metric looks abnormal flag it
        val types = entries.map { it.name }.toSet()
        val hasAbnormal = entries.any { entry ->
            val type = db.userMetricDao() // just use entry directly
            false // placeholder — we'll use the entries themselves
        }
        return "Good" to Color(0xFF43A047)
    }
}

// ─── Screen ──────────────────────────────────────────────────

@Composable
fun DashboardScreen(
    username: String = "there",
    onNavigateToMetrics: () -> Unit,
    onNavigateToSymptoms: () -> Unit,
    onNavigateToMedical: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToFirstAid: () -> Unit,
    onNavigateToTrends: () -> Unit,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val uiState by dashboardViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Top bar ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello, $username",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Monitor your health journey",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Profile",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Summary cards ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                label = "Health Status",
                value = uiState.healthStatus,
                valueColor = uiState.healthStatusColor
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                label = "Last Check",
                value = uiState.lastCheckDate
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                label = "Records",
                value = uiState.totalRecords.toString()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Quick actions ─────────────────────────────────────
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MonitorHeart,
                label = "Track Metrics",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onNavigateToMetrics
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Search,
                label = "Analyse Symptoms",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onNavigateToSymptoms
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MedicalServices,
                label = "Medical History",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onNavigateToMedical
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Flag,
                label = "My Goals",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onNavigateToGoals
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.HealthAndSafety,
                label = "First Aid",
                containerColor = Color(0xFFFFEBEE),
                contentColor = Color(0xFFB71C1C),
                onClick = onNavigateToFirstAid
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ShowChart,
                label = "Health Trends",
                containerColor = Color(0xFFE8F5E9),
                contentColor = Color(0xFF1B5E20),
                onClick = onNavigateToTrends
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Recent activity ───────────────────────────────────
        if (uiState.recentMetrics.isNotEmpty()) {
            Text(
                "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            uiState.recentMetrics.forEach { entry ->
                RecentEntryRow(entry = entry)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─── Small composables ────────────────────────────────────────

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RecentEntryRow(entry: UserMetricWithType) {
    val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    val dateStr = sdf.format(java.util.Date(entry.recordedAt))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "${entry.value} ${entry.unit ?: ""}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}