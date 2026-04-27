package com.example.kipcareafya.ui.dashboard

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.data.local.dao.UserMetricWithType
import com.example.kipcareafya.data.local.entity.MetricTypeEntity
import com.example.kipcareafya.ui.dashboard.WaterIntakeCard
import com.example.kipcareafya.ui.dashboard.DailyHealthScoreCard
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─── ViewModel ───────────────────────────────────────────────

data class DashboardUiState(
    val totalRecords: Int = 0,
    val lastCheckDate: String = "--",
    val healthStatus: String = "Good",
    val healthStatusColor: Color = Color(0xFF43A047),
    val recentMetrics: List<UserMetricWithType> = emptyList(),
    val metricTypes: List<MetricTypeEntity> = emptyList(),
    val selectedCategory: String = "All",
    val isLoading: Boolean = true
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val db = HealthDatabase.getInstance(app)
    private val metricDao = db.userMetricDao()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val userId = 1

    init {
        viewModelScope.launch {
            launch { loadMetricTypes() }
            launch { loadRecentEntries() }
        }
    }

    private suspend fun loadMetricTypes() {
        metricDao.getAllMetricTypes().collect { types ->
            _uiState.update { it.copy(metricTypes = types) }
        }
    }

    private suspend fun loadRecentEntries() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        metricDao.getMetricsWithType(userId, thirtyDaysAgo).collect { entries ->
            val lastDate = entries.maxByOrNull { it.recordedAt }?.let {
                java.text.SimpleDateFormat(
                    "MMM dd, yyyy",
                    java.util.Locale.getDefault()
                ).format(java.util.Date(it.recordedAt))
            } ?: "--"

            _uiState.update {
                it.copy(
                    totalRecords = entries.size,
                    lastCheckDate = lastDate,
                    recentMetrics = entries.take(4),
                    isLoading = false
                )
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun getLatestForType(metricName: String): UserMetricWithType? {
        return _uiState.value.recentMetrics
            .filter { it.name == metricName }
            .maxByOrNull { it.recordedAt }
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
    vm: DashboardViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()

    val categories = listOf("All") +
            uiState.metricTypes.map { it.category }.distinct()

    val filteredMetrics = if (uiState.selectedCategory == "All") {
        uiState.metricTypes
    } else {
        uiState.metricTypes.filter { it.category == uiState.selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello 👋",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // ── Big question ──────────────────────────────────────
        Text(
            text = "How is your health\ntoday?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Summary pills row ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryPill(
                modifier = Modifier.weight(1f),
                label = "Status",
                value = uiState.healthStatus,
                valueColor = uiState.healthStatusColor
            )
            SummaryPill(
                modifier = Modifier.weight(1f),
                label = "Last check",
                value = uiState.lastCheckDate
            )
            SummaryPill(
                modifier = Modifier.weight(1f),
                label = "Records",
                value = uiState.totalRecords.toString()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Category filter chips ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick = { vm.selectCategory(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Horizontal metric tiles ───────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            filteredMetrics
                .filter { it.name != "bmi" }
                .forEach { metricType ->
                    val latest = vm.getLatestForType(metricType.name)
                    MetricTile(
                        metricType = metricType,
                        latestEntry = latest,
                        onClick = onNavigateToMetrics
                    )
                }

            // BMI tile
            val bmiType = uiState.metricTypes.find { it.name == "bmi" }
            val bmiEntry = vm.getLatestForType("bmi")
            if (bmiType != null && bmiEntry != null) {
                MetricTile(
                    metricType = bmiType,
                    latestEntry = bmiEntry,
                    onClick = onNavigateToMetrics
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Quick actions ───────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onNavigateToMetrics) {
                Text("View All")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2×2 action grid
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.MonitorHeart,
                    label = "Track Metrics",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onNavigateToMetrics
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Search,
                    label = "Analyse Symptoms",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onNavigateToSymptoms
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.MedicalServices,
                    label = "Medical History",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onNavigateToMedical
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Flag,
                    label = "My Goals",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onNavigateToGoals
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.HealthAndSafety,
                    label = "First Aid",
                    containerColor = Color(0xFFFFEBEE),
                    contentColor = Color(0xFFB71C1C),
                    onClick = onNavigateToFirstAid
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ShowChart,
                    label = "Health Trends",
                    containerColor = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF1B5E20),
                    onClick = onNavigateToTrends
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

// ── Daily health score ────────────────────────────────────
        Text(
            "Today's Score",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            DailyHealthScoreCard()
        }

        Spacer(modifier = Modifier.height(20.dp))

// ── Water intake ──────────────────────────────────────────
        Text(
            "Hydration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            WaterIntakeCard()
            Spacer(modifier = Modifier.height(20.dp))

// ── Daily health score ────────────────────────────────────
            Text(
                "Today's Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                DailyHealthScoreCard()
            }

            Spacer(modifier = Modifier.height(20.dp))

// ── Water intake ──────────────────────────────────────────
            Text(
                "Hydration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                WaterIntakeCard()
            }
        }

        // ── Recent activity ───────────────────────────────────
        if (uiState.recentMetrics.isNotEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToMetrics) {
                    Text("View All")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.recentMetrics.forEach { entry ->
                    RecentEntryRow(entry = entry)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─── Components ───────────────────────────────────────────────

@Composable
fun SummaryPill(
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
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
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
fun MetricTile(
    metricType: MetricTypeEntity,
    latestEntry: UserMetricWithType?,
    onClick: () -> Unit
) {
    val iconData = metricIconAndColor(metricType.name)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconData.second.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconData.first,
                    contentDescription = metricType.displayName,
                    tint = iconData.second,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = metricType.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = if (latestEntry != null)
                    "${latestEntry.value}"
                else "--",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (latestEntry != null) iconData.second
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (metricType.unit != null) {
                Text(
                    text = metricType.unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActionCard(
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RecentEntryRow(entry: UserMetricWithType) {
    val dateStr = remember(entry.recordedAt) {
        java.text.SimpleDateFormat(
            "MMM dd, HH:mm",
            java.util.Locale.getDefault()
        ).format(java.util.Date(entry.recordedAt))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val iconData = metricIconAndColor(entry.name)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconData.second.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconData.first,
                        contentDescription = null,
                        tint = iconData.second,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = 0.6f)
                    )
                }
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

// ─── Helper — map metric name to icon + color ─────────────────

fun metricIconAndColor(metricName: String): Pair<ImageVector, Color> {
    return when (metricName) {
        "weight"                   -> Icons.Default.FitnessCenter to Color(0xFF7B1FA2)
        "height"                   -> Icons.Default.Height        to Color(0xFF1565C0)
        "bmi"                      -> Icons.Default.Monitor       to Color(0xFF00695C)
        "blood_glucose"            -> Icons.Default.Bloodtype     to Color(0xFFB71C1C)
        "total_cholesterol"        -> Icons.Default.Science       to Color(0xFFE65100)
        "hydration"                -> Icons.Default.WaterDrop     to Color(0xFF0277BD)
        "blood_pressure_systolic",
        "blood_pressure_diastolic" -> Icons.Default.Favorite      to Color(0xFFC62828)
        "heart_rate"               -> Icons.Default.MonitorHeart  to Color(0xFFAD1457)
        "oxygen_saturation"        -> Icons.Default.Air           to Color(0xFF00838F)
        else                       -> Icons.Default.HealthAndSafety to Color(0xFF2E7D32)
    }
}
