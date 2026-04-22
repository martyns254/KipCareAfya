package com.example.kipcareafya.ui.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kipcareafya.data.local.dao.UserMetricWithType
import com.example.kipcareafya.data.local.entity.MetricTypeEntity

@Composable
fun MetricsScreen(
    onBack: () -> Unit,
    onNavigateToAdd: (preselectedMetricId: Int?) -> Unit,
    metricsViewModel: MetricsViewModel = viewModel()
) {
    val uiState by metricsViewModel.uiState.collectAsState()
    val grouped = uiState.metricTypes.groupBy { it.category }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Health Metrics", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.metricTypes.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No metrics found. Restart the app to load defaults.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    grouped.forEach { (category, metrics) ->
                        item {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        items(metrics.filter { it.name != "bmi" }) { metricType ->
                            val latest = metricsViewModel.getLatestForType(metricType.name)
                            MetricCard(
                                metricType = metricType,
                                latestEntry = latest,
                                onClick = { onNavigateToAdd(metricType.id) }
                            )
                        }
                        if (metrics.any { it.name == "bmi" }) {
                            item {
                                val bmiEntry = metricsViewModel.getLatestForType("bmi")
                                if (bmiEntry != null) {
                                    BmiCard(bmiEntry)
                                }
                            }
                        }
                    }
                }
            }
        }


        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onNavigateToAdd(null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Record")
        }
    }
}

@Composable
fun MetricCard(
    metricType: MetricTypeEntity,
    latestEntry: UserMetricWithType?,
    onClick: () -> Unit
) {
    val belowNormal = latestEntry != null &&
            metricType.minNormal != null &&
            latestEntry.value < metricType.minNormal
    val aboveNormal = latestEntry != null &&
            metricType.maxNormal != null &&
            latestEntry.value > metricType.maxNormal

    val statusColor = when {
        latestEntry == null -> MaterialTheme.colorScheme.onSurfaceVariant
        belowNormal || aboveNormal -> MaterialTheme.colorScheme.error
        else -> Color(0xFF43A047)
    }

    val statusText = when {
        latestEntry == null -> "Tap to add"
        belowNormal -> "Below normal"
        aboveNormal -> "Above normal"
        else -> "Normal"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metricType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (latestEntry != null) {
                    Text(
                        text = "${latestEntry.value} ${metricType.unit ?: ""}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "Not recorded yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val min = metricType.minNormal
                val max = metricType.maxNormal
                if (min != null || max != null) {
                    val range = buildString {
                        append("Normal: ")
                        if (min != null) append("$min") else append("—")
                        append(" – ")
                        if (max != null) append("$max") else append("—")
                        append(" ${metricType.unit ?: ""}")
                    }
                    Text(
                        text = range,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun BmiCard(bmiEntry: UserMetricWithType) {
    val bmi = bmiEntry.value
    val (statusText, statusColor) = when {
        bmi < 18.5f -> "Underweight" to Color(0xFF1565C0)
        bmi < 25f   -> "Healthy"     to Color(0xFF43A047)
        bmi < 30f   -> "Overweight"  to Color(0xFFF57C00)
        else        -> "Obese"       to Color(0xFFE53935)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "BMI",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = String.format("%.1f", bmi),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Auto-calculated from height & weight",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
            }
        }
    }
}