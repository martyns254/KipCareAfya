package com.example.kipcareafya.ui.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMetricScreen(
    onBack: () -> Unit,
    preselectedMetricId: Int? = null,
    metricsViewModel: MetricsViewModel = viewModel()
) {
    val uiState by metricsViewModel.uiState.collectAsState()
    val selectableMetrics = uiState.metricTypes.filter { it.name != "bmi" }

    var selectedMetric by remember(selectableMetrics) {
        mutableStateOf(
            // If coming from a card tap, preselect that metric
            selectableMetrics.find { it.id == preselectedMetricId }
                ?: selectableMetrics.firstOrNull()
        )
    }
    var value by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var valueError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.savedMessage) {
        if (uiState.savedMessage != null) {
            metricsViewModel.clearMessage()
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Add Health Record",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Last recorded value banner
        selectedMetric?.let { metric ->
            val last = metricsViewModel.getLatestForType(metric.name)
            if (last != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Last recorded: ${last.value} ${metric.unit ?: ""}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Metric type dropdown
        Text(
            "Select Metric",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedMetric?.displayName ?: "Loading...",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                selectableMetrics.forEach { metric ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    metric.displayName,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (metric.unit != null) {
                                    Text(
                                        text = "Unit: ${metric.unit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            selectedMetric = metric
                            value = ""
                            valueError = null
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Value input
        val unitLabel = selectedMetric?.unit?.let { " ($it)" } ?: ""
        Text(
            "Value$unitLabel",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                valueError = null
            },
            placeholder = {
                val min = selectedMetric?.minNormal
                val max = selectedMetric?.maxNormal
                if (min != null && max != null) {
                    Text(
                        "Normal: $min – $max",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = valueError != null,
            supportingText = {
                if (valueError != null) {
                    Text(valueError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Notes input
        Text(
            "Notes (optional)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = {
                Text(
                    "e.g. after breakfast, morning reading...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // BMI auto-calc notice
        val currentName = selectedMetric?.name
        if (currentName == "weight" || currentName == "height") {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "BMI will be automatically calculated once both your height and weight are recorded.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Save button
        Button(
            onClick = {
                val metric = selectedMetric
                if (metric == null) {
                    valueError = "Please select a metric"
                    return@Button
                }
                val floatVal = value.toFloatOrNull()
                if (floatVal == null) {
                    valueError = "Please enter a valid number"
                    return@Button
                }
                if (floatVal <= 0f) {
                    valueError = "Value must be greater than 0"
                    return@Button
                }
                metricsViewModel.saveMetric(metric.id, floatVal, notes)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading
        ) {
            Text("Save Record")
        }
    }
}