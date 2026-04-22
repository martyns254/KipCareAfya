package com.example.kipcareafya.ui.symptoms

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomAnalysisScreen(
    onBack: () -> Unit,
    vm: SymptomViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ── Top bar ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Symptom Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            // ── Selected symptoms chips ───────────────────────
            if (uiState.selectedSymptoms.isNotEmpty()) {
                Text(
                    "Selected (${uiState.selectedSymptoms.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.selectedSymptoms.forEach { symptom ->
                        InputChip(
                            selected = true,
                            onClick = { vm.toggleSymptom(symptom) },
                            label = { Text(symptom) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Symptom groups ────────────────────────────────
            uiState.categories.forEach { category ->
                val symptoms = uiState.symptomsByCategory[category.id] ?: emptyList()
                if (symptoms.isEmpty()) return@forEach

                SymptomCategorySection(
                    categoryName = category.name,
                    symptoms = symptoms,
                    selectedSymptoms = uiState.selectedSymptoms,
                    onToggle = { vm.toggleSymptom(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Severity selector ─────────────────────────────
            Text(
                "How severe are your symptoms?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    "mild"     to Color(0xFF43A047),
                    "moderate" to Color(0xFFF57C00),
                    "severe"   to Color(0xFFE53935)
                ).forEach { (level, color) ->
                    SeverityButton(
                        modifier = Modifier.weight(1f),
                        label = level.replaceFirstChar { it.uppercase() },
                        color = color,
                        selected = uiState.severity == level,
                        onClick = { vm.setSeverity(level) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Analyse button ────────────────────────────────
            Button(
                onClick = { vm.analyseSymptoms() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.selectedSymptoms.isNotEmpty()
                        && !uiState.isAnalysing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isAnalysing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyse Symptoms", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Analysis results ──────────────────────────────
            AnimatedVisibility(
                visible = uiState.analysisResult != null || uiState.showDefaultResult,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    val result = uiState.analysisResult

                    Text(
                        "Analysis Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (result != null) {
                        // Emergency level banner
                        val (bannerColor, bannerText, bannerIcon) = when (result.emergencyLevel) {
                            "high"   -> Triple(
                                Color(0xFFFFEBEE),
                                "Seek immediate medical attention",
                                Icons.Default.Emergency
                            )
                            "medium" -> Triple(
                                Color(0xFFFFF8E1),
                                "Monitor closely — see a doctor if worsening",
                                Icons.Default.Warning
                            )
                            else     -> Triple(
                                Color(0xFFE8F5E9),
                                "Low urgency — home care may be sufficient",
                                Icons.Default.CheckCircle
                            )
                        }
                        val bannerContentColor = when (result.emergencyLevel) {
                            "high"   -> Color(0xFFB71C1C)
                            "medium" -> Color(0xFFE65100)
                            else     -> Color(0xFF2E7D32)
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = bannerColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    bannerIcon,
                                    contentDescription = null,
                                    tint = bannerContentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    bannerText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = bannerContentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Result detail cards
                        ResultCard(
                            icon = Icons.Default.Lightbulb,
                            iconColor = Color(0xFF1565C0),
                            title = "Recommended Actions",
                            body = result.recommendation
                        )

                        if (result.warningSigns != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ResultCard(
                                icon = Icons.Default.Warning,
                                iconColor = Color(0xFFE65100),
                                title = "Warning Signs",
                                body = result.warningSigns
                            )
                        }

                        if (result.basicTreatment != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ResultCard(
                                icon = Icons.Default.HealthAndSafety,
                                iconColor = Color(0xFF2E7D32),
                                title = "Basic Treatment",
                                body = result.basicTreatment
                            )
                        }

                        if (result.seekDoctorIf != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ResultCard(
                                icon = Icons.Default.LocalHospital,
                                iconColor = Color(0xFFB71C1C),
                                title = "Seek Medical Help If",
                                body = result.seekDoctorIf
                            )
                        }

                    } else {
                        // Default fallback — mirrors backend default_response
                        ResultCard(
                            icon = Icons.Default.Info,
                            iconColor = Color(0xFF1565C0),
                            title = "Recommendation",
                            body = "Based on your symptoms, we recommend monitoring your condition and staying hydrated."
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ResultCard(
                            icon = Icons.Default.LocalHospital,
                            iconColor = Color(0xFFB71C1C),
                            title = "Seek Medical Help If",
                            body = "Symptoms persist or worsen after 24 hours."
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Start over button
                    OutlinedButton(
                        onClick = { vm.clearResults() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start New Analysis")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Components ───────────────────────────────────────────────

@Composable
fun SymptomCategorySection(
    categoryName: String,
    symptoms: List<com.example.kipcareafya.data.local.entity.SymptomEntity>,
    selectedSymptoms: Set<String>,
    onToggle: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Category header — tap to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Show count of selected in this category
                    val selectedCount = symptoms.count {
                        it.name in selectedSymptoms
                    }
                    if (selectedCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text("$selectedCount")
                        }
                    }
                    Icon(
                        if (expanded) Icons.Default.ExpandLess
                        else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Symptom checkboxes
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    symptoms.forEach { symptom ->
                        val isSelected = symptom.name in selectedSymptoms
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onToggle(symptom.name) }
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                            .copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .padding(
                                    horizontal = 4.dp,
                                    vertical = 6.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggle(symptom.name) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = symptom.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected)
                                        FontWeight.SemiBold
                                    else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (symptom.description != null) {
                                    Text(
                                        text = symptom.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeverityButton(
    modifier: Modifier = Modifier,
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color else color.copy(alpha = 0.1f))
            .border(
                width = 1.5.dp,
                color = if (selected) color else color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else color
        )
    }
}

@Composable
fun ResultCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    body: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}