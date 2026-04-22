package com.example.kipcareafya.ui.onboarding

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.data.local.entity.OnboardingEntity
import com.example.kipcareafya.data.local.entity.UserMetricEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── ViewModel ───────────────────────────────────────────────

data class OnboardingState(
    val currentStep: Int = 0,
    val dateOfBirth: String = "",
    val gender: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val activityLevel: String = "moderate",
    val waterGoalMl: Int = 2500,
    val stepGoal: Int = 8000,
    val isCompleted: Boolean = false
)

class OnboardingViewModel(app: Application) : AndroidViewModel(app) {

    private val db = HealthDatabase.getInstance(app)

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun updateState(block: OnboardingState.() -> OnboardingState) {
        _state.update { it.block() }
    }

    fun nextStep() {
        _state.update { it.copy(currentStep = it.currentStep + 1) }
    }

    fun prevStep() {
        _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun complete(userId: Int) {
        viewModelScope.launch {
            val s = _state.value

            // Save onboarding profile
            db.onboardingDao().save(
                OnboardingEntity(
                    userId = userId,
                    dateOfBirth = s.dateOfBirth.ifBlank { null },
                    heightCm = s.heightCm.toFloatOrNull(),
                    weightKg = s.weightKg.toFloatOrNull(),
                    gender = s.gender.ifBlank { null },
                    activityLevel = s.activityLevel,
                    waterGoalMl = s.waterGoalMl,
                    stepGoal = s.stepGoal,
                    isCompleted = true
                )
            )

            // Pre-populate height and weight as initial metric readings
            val allTypes = db.userMetricDao().getAllMetricTypes().first()

            val heightFloat = s.heightCm.toFloatOrNull()
            if (heightFloat != null) {
                val heightType = allTypes.find { it.name == "height" }
                if (heightType != null) {
                    db.userMetricDao().insertUserMetric(
                        UserMetricEntity(
                            userId = userId,
                            metricId = heightType.id,
                            value = heightFloat,
                            notes = "Set during onboarding"
                        )
                    )
                }
            }

            val weightFloat = s.weightKg.toFloatOrNull()
            if (weightFloat != null) {
                val weightType = allTypes.find { it.name == "weight" }
                if (weightType != null) {
                    db.userMetricDao().insertUserMetric(
                        UserMetricEntity(
                            userId = userId,
                            metricId = weightType.id,
                            value = weightFloat,
                            notes = "Set during onboarding"
                        )
                    )
                }
            }

            _state.update { it.copy(isCompleted = true) }
        }
    }
}

// ─── Screen ──────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    userId: Int,
    onComplete: () -> Unit,
    vm: OnboardingViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) onComplete()
    }

    val totalSteps = 3 // steps 0,1,2 — step 3 is the "all set" screen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Progress bar — only show on steps 0–2
        if (state.currentStep < totalSteps) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(totalSteps) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index <= state.currentStep)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Step ${state.currentStep + 1} of $totalSteps",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Animated step content
        AnimatedContent(
            targetState = state.currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(
                        slideOutHorizontally { it } + fadeOut()
                    )
                }
            },
            modifier = Modifier.weight(1f),
            label = "onboarding_step"
        ) { step ->
            when (step) {
                0    -> WelcomeStep()
                1    -> AboutYouStep(
                    state = state,
                    onUpdate = { transform -> vm.updateState(transform) }
                )
                2    -> GoalsStep(
                    state = state,
                    onUpdate = { transform -> vm.updateState(transform) }
                )
                else -> AllSetStep()
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.currentStep in 1..2) {
                OutlinedButton(
                    onClick = { vm.prevStep() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back")
                }
            }

            Button(
                onClick = {
                    when (state.currentStep) {
                        0, 1, 2 -> vm.nextStep()
                        else    -> vm.complete(userId)
                    }
                },
                modifier = Modifier.weight(
                    if (state.currentStep in 1..2) 2f else 1f
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = when (state.currentStep) {
                        0    -> "Get Started"
                        1, 2 -> "Next"
                        else -> "Start My Journey"
                    },
                    fontWeight = FontWeight.SemiBold
                )
                if (state.currentStep != 3) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

// ─── Step 0 — Welcome ─────────────────────────────────────────

@Composable
fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.HealthAndSafety,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to\nKipCare Afya",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 40.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your personal health companion.\nTrack metrics, monitor symptoms,\nand achieve your wellness goals.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        listOf(
            Icons.Default.MonitorHeart to "Track vital health metrics",
            Icons.Default.Search       to "Analyse symptoms instantly",
            Icons.Default.Flag         to "Set and achieve health goals",
            Icons.Default.WaterDrop    to "Monitor daily water intake"
        ).forEach { (icon, text) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

// ─── Step 1 — About You ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutYouStep(
    state: OnboardingState,
    onUpdate: (OnboardingState.() -> OnboardingState) -> Unit
) {
    var genderExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tell us about yourself",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "This helps us personalise your health insights",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.dateOfBirth,
            onValueChange = { new -> onUpdate { copy(dateOfBirth = new) } },
            label = { Text("Date of birth") },
            placeholder = { Text("YYYY-MM-DD") },
            leadingIcon = {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded }
        ) {
            OutlinedTextField(
                value = state.gender.ifBlank { "Select gender" },
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(14.dp)
            )
            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false }
            ) {
                listOf("Male", "Female", "Prefer not to say").forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g) },
                        onClick = {
                            onUpdate { copy(gender = g) }
                            genderExpanded = false
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.heightCm,
                onValueChange = { new -> onUpdate { copy(heightCm = new) } },
                label = { Text("Height (cm)") },
                placeholder = { Text("e.g. 175") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = state.weightKg,
                onValueChange = { new -> onUpdate { copy(weightKg = new) } },
                label = { Text("Weight (kg)") },
                placeholder = { Text("e.g. 70") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            )
        }

        Text(
            text = "Activity level",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        listOf(
            "sedentary"   to "Sedentary — little or no exercise",
            "light"       to "Light — 1–3 days/week",
            "moderate"    to "Moderate — 3–5 days/week",
            "active"      to "Active — 6–7 days/week",
            "very_active" to "Very Active — athlete level"
        ).forEach { (level, label) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = state.activityLevel == level,
                    onClick = { onUpdate { copy(activityLevel = level) } }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─── Step 2 — Goals ───────────────────────────────────────────

@Composable
fun GoalsStep(
    state: OnboardingState,
    onUpdate: (OnboardingState.() -> OnboardingState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Set your daily goals",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "You can always change these later in settings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        GoalSliderCard(
            icon = Icons.Default.WaterDrop,
            iconColor = Color(0xFF0277BD),
            title = "Daily water goal",
            value = "${state.waterGoalMl} ml",
            sliderValue = state.waterGoalMl.toFloat(),
            valueRange = 1000f..5000f,
            steps = 39,
            onValueChange = { new -> onUpdate { copy(waterGoalMl = new.toInt()) } }
        )

        GoalSliderCard(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            iconColor = Color(0xFF2E7D32),
            title = "Daily step goal",
            value = "${state.stepGoal} steps",
            sliderValue = state.stepGoal.toFloat(),
            valueRange = 1000f..20000f,
            steps = 19,
            onValueChange = { new -> onUpdate { copy(stepGoal = new.toInt()) } }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun GoalSliderCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    sliderValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Slider(
                value = sliderValue,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = iconColor,
                    activeTrackColor = iconColor
                )
            )
        }
    }
}

// ─── Step 3 — All Set ─────────────────────────────────────────

@Composable
fun AllSetStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFF43A047).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF43A047),
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "You're all set!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your profile is ready.\nLet's start your health journey!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}