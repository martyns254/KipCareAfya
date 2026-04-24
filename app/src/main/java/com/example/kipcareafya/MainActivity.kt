package com.example.kipcareafya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.ui.auth.AuthViewModel
import com.example.kipcareafya.ui.auth.LoginScreen
import com.example.kipcareafya.ui.auth.RegisterScreen
import com.example.kipcareafya.ui.dashboard.DashboardScreen
import com.example.kipcareafya.ui.goals.GoalsScreen
import com.example.kipcareafya.ui.medical.MedicalHistoryScreen
import com.example.kipcareafya.ui.metrics.MetricDetailScreen
import com.example.kipcareafya.ui.metrics.AddMetricScreen
import com.example.kipcareafya.ui.metrics.MetricsScreen
import com.example.kipcareafya.ui.navigation.BottomNavItem
import com.example.kipcareafya.ui.onboarding.OnboardingScreen
import com.example.kipcareafya.ui.symptoms.SymptomAnalysisScreen
import com.example.kipcareafya.ui.theme.KipCareAfyaTheme
import com.example.kipcareafya.ui.trends.TrendsScreen
import com.example.kipcareafya.ui.profile.ProfileScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KipCareAfyaTheme {
                KipCareApp()
            }
        }
    }
}

@Composable
fun KipCareApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // While auth is loading show nothing
    if (authState.isLoading) return

    // Check if onboarding is completed for the logged-in user
    var onboardingComplete by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(authState.userId) {
        if (authState.userId > 0) {
            val db = HealthDatabase.getInstance(context)
            val profile = db.onboardingDao().get(authState.userId)
            onboardingComplete = profile?.isCompleted == true
        } else {
            // Not logged in — reset
            onboardingComplete = null
        }
    }

    // Wait until we know the onboarding status for logged-in users
    if (authState.isLoggedIn && onboardingComplete == null) return

    val startDestination = when {
        !authState.isLoggedIn          -> "login"
        onboardingComplete == false    -> "onboarding"
        else                           -> "dashboard"
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Metrics,
        BottomNavItem.Symptoms,
        BottomNavItem.Medical,
        BottomNavItem.Profile
    )

    // Only show bottom bar on the main tab screens
    val showBottomBar = authState.isLoggedIn &&
            bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                KipCareBottomBar(
                    items = bottomNavItems,
                    currentDestination = currentDestination,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onFabClick = {
                        navController.navigate("add_metric/0")
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {

            // ── Auth ──────────────────────────────────────────
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    },
                    vm = authViewModel
                )
            }

            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate("onboarding") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                    vm = authViewModel
                )
            }

            // ── Onboarding — runs once after registration ─────
            composable("onboarding") {
                OnboardingScreen(
                    userId = authState.userId,
                    onComplete = {
                        navController.navigate("dashboard") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            // ── Main screens ──────────────────────────────────
            composable("dashboard") {
                DashboardScreen(
                    username = authState.username,
                    onNavigateToMetrics      = { navController.navigate("metrics") },
                    onNavigateToMetricDetail = { metricName ->
                        navController.navigate("metric_detail/$metricName")
                    },
                    onNavigateToSymptoms  = { navController.navigate("symptoms") },
                    onNavigateToMedical   = { navController.navigate("medical") },
                    onNavigateToGoals     = { navController.navigate("goals") },
                    onNavigateToFirstAid  = { navController.navigate("firstaid") },
                    onNavigateToTrends    = { navController.navigate("trends") }
                )
            }

            composable("metrics") {
                MetricsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToAdd = { metricId ->
                        navController.navigate("add_metric/${metricId ?: 0}")
                    }
                )
            }

            composable("add_metric/{metricId}") { backStackEntry ->
                val metricId = backStackEntry.arguments
                    ?.getString("metricId")?.toIntOrNull()
                AddMetricScreen(
                    onBack = { navController.popBackStack() },
                    preselectedMetricId = if (metricId == 0) null else metricId
                )
            }

            composable("symptoms") {
                SymptomAnalysisScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("medical") {
                MedicalHistoryScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("goals") {
                GoalsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("trends") {
                TrendsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("profile") {
                ProfileScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }

                )
            }
            composable("metric_detail/{metricName}") { backStackEntry ->
                val metricName = backStackEntry.arguments
                    ?.getString("metricName") ?: return@composable
                MetricDetailScreen(
                    metricName = metricName,
                    onBack = { navController.popBackStack() },
                    onAddRecord = { metricId ->
                        navController.navigate("add_metric/$metricId")
                    }
                )
            }

            // ── Placeholder ───────────────────────────────────
            composable("firstaid") {
                Text(
                    "First Aid — Coming Soon",
                    modifier = Modifier.padding(32.dp)
                )
            }
        }
    }
}

// ─── Bottom bar ───────────────────────────────────────────────

@Composable
fun KipCareBottomBar(
    items: List<BottomNavItem>,
    currentDestination: androidx.navigation.NavDestination?,
    onItemClick: (BottomNavItem) -> Unit,
    onFabClick: () -> Unit
) {
    Box {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            items.forEachIndexed { index, item ->
                // Empty slot in the middle for the FAB gap
                if (index == 2) {
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = {},
                        enabled = false,
                        modifier = Modifier.width(56.dp)
                    )
                }
                val selected = currentDestination?.hierarchy?.any {
                    it.route == item.route
                } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = { onItemClick(item) },
                    icon = {
                        Icon(item.icon, contentDescription = item.label)
                    },
                    label = {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        // Centre FAB floating above the bar
        FloatingActionButton(
            onClick = onFabClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
                .size(56.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(6.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add record",
                tint = Color.White
            )
        }
    }
}

// ─── Profile screen ───────────────────────────────────────────

@Composable
fun ProfileScreen(
    username: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(50.dp)
            )
        }

        Text(
            text = username,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                ProfileMenuItem(
                    icon = Icons.Default.Person,
                    label = "Edit Profile",
                    onClick = { }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProfileMenuItem(
                    icon = Icons.Default.Notifications,
                    label = "Notifications",
                    onClick = { }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProfileMenuItem(
                    icon = Icons.Default.DarkMode,
                    label = "Dark Mode",
                    onClick = { }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProfileMenuItem(
                    icon = Icons.Default.Share,
                    label = "Export Health Data",
                    onClick = { }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}