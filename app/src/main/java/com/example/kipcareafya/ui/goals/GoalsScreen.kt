package com.example.kipcareafya.ui.goals

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kipcareafya.data.local.entity.HealthGoalEntity
import com.example.kipcareafya.ui.medical.EmptyState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    vm: GoalsViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.savedMessage) {
        if (uiState.savedMessage != null) {
            snackbarHostState.showSnackbar(uiState.savedMessage!!)
            vm.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Header
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
                    "My Health Goals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Stats row
            if (uiState.activeGoals.isNotEmpty() ||
                uiState.completedGoals.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GoalStatCard(
                        modifier = Modifier.weight(1f),
                        value = uiState.activeGoals.size.toString(),
                        label = "Active",
                        color = MaterialTheme.colorScheme.primary
                    )
                    GoalStatCard(
                        modifier = Modifier.weight(1f),
                        value = uiState.completedGoals.size.toString(),
                        label = "Completed",
                        color = Color(0xFF43A047)
                    )
                    GoalStatCard(
                        modifier = Modifier.weight(1f),
                        value = run {
                            val total = uiState.activeGoals.size +
                                    uiState.completedGoals.size
                            if (total == 0) "0%"
                            else "${(uiState.completedGoals.size * 100 / total)}%"
                        },
                        label = "Success rate",
                        color = Color(0xFF7B1FA2)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Tabs
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { vm.selectTab(0) },
                    text = {
                        Text("Active (${uiState.activeGoals.size})")
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { vm.selectTab(1) },
                    text = {
                        Text("Completed (${uiState.completedGoals.size})")
                    }
                )
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.selectedTab == 0 -> {
                    if (uiState.activeGoals.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Flag,
                            message = "No active goals",
                            hint = "Tap + to set your first health goal"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.activeGoals) { goal ->
                                GoalCard(
                                    goal = goal,
                                    onMarkComplete = { vm.markComplete(goal) },
                                    onDelete = { vm.deleteGoal(goal) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    if (uiState.completedGoals.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.CheckCircle,
                            message = "No completed goals yet",
                            hint = "Complete your active goals to see them here"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.completedGoals) { goal ->
                                GoalCard(
                                    goal = goal,
                                    onMarkComplete = {},
                                    onDelete = { vm.deleteGoal(goal) },
                                    isCompleted = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false }
        ) {
            AddGoalForm(
                onSave = { title, metricType, targetValue, targetDate ->
                    vm.addGoal(title, metricType, targetValue, targetDate)
                    showAddSheet = false
                },
                onDismiss = { showAddSheet = false }
            )
        }
    }
}

@Composable
fun GoalCard(
    goal: HealthGoalEntity,
    onMarkComplete: () -> Unit,
    onDelete: () -> Unit,
    isCompleted: Boolean = false
) {
    val dateStr = goal.targetDate?.let {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            .format(Date(it))
    } ?: "No deadline"

    // Calculate progress if we have current and target values
    val progress = if (goal.targetValue != null && goal.currentValue != null &&
        goal.targetValue > 0f) {
        (goal.currentValue / goal.targetValue).coerceIn(0f, 1f)
    } else null

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                Color(0xFFE8F5E9)
            else MaterialTheme.colorScheme.surface
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCompleted) Color(0xFF43A047).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isCompleted) Icons.Default.CheckCircle
                            else Icons.Default.Flag,
                            contentDescription = null,
                            tint = if (isCompleted) Color(0xFF43A047)
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!goal.metricType.isNullOrBlank()) {
                            Text(
                                text = goal.metricType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row {
                    if (!isCompleted) {
                        IconButton(
                            onClick = onMarkComplete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Mark complete",
                                tint = Color(0xFF43A047),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Target value row
            if (goal.targetValue != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Target: ${goal.targetValue}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (goal.currentValue != null) {
                        Text(
                            text = "Current: ${goal.currentValue}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Progress bar
                if (progress != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isCompleted) Color(0xFF43A047)
                        else MaterialTheme.colorScheme.primary,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Deadline row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GoalStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalForm(
    onSave: (String, String, Float?, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var metricType by remember { mutableStateOf("") }
    var targetValue by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }

    val metricOptions = listOf(
        "Weight", "Blood Glucose", "Blood Pressure",
        "Heart Rate", "BMI", "Hydration",
        "Steps", "Sleep Hours", "Custom"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "New Health Goal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Goal title") },
            placeholder = { Text("e.g. Lose 5kg by July") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(Icons.Default.Flag, contentDescription = null)
            }
        )

        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = !typeExpanded }
        ) {
            OutlinedTextField(
                value = metricType.ifBlank { "Select type (optional)" },
                onValueChange = {},
                readOnly = true,
                label = { Text("Related metric") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                metricOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            metricType = option
                            typeExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = targetValue,
            onValueChange = { targetValue = it },
            label = { Text("Target value (optional)") },
            placeholder = { Text("e.g. 70") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = targetDate,
            onValueChange = { targetDate = it },
            label = { Text("Target date (optional)") },
            placeholder = { Text("e.g. 2025-12-31") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel") }

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val parsedDate = try {
                            if (targetDate.isNotBlank())
                                SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).parse(targetDate)?.time
                            else null
                        } catch (e: Exception) { null }

                        onSave(
                            title,
                            metricType,
                            targetValue.toFloatOrNull(),
                            parsedDate
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save Goal") }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}