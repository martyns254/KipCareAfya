package com.example.kipcareafya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import com.example.kipcareafya.ui.theme.KipCareAfyaTheme
import com.example.kipcareafya.ui.dashboard.DashboardScreen
import com.example.kipcareafya.ui.metrics.MetricsScreen
import com.example.kipcareafya.ui.metrics.AddMetricScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KipCareAfyaTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "dashboard"
                ) {

                    // ── Dashboard ─────────────────────────────
                    composable("dashboard") {
                        DashboardScreen(
                            username = "TKS",
                            onNavigateToMetrics  = { navController.navigate("metrics") },
                            onNavigateToSymptoms = { navController.navigate("symptoms") },
                            onNavigateToMedical  = { navController.navigate("medical") },
                            onNavigateToGoals    = { navController.navigate("goals") },
                            onNavigateToFirstAid = { navController.navigate("firstaid") },
                            onNavigateToTrends   = { navController.navigate("trends") }
                        )
                    }

                    // ── Metrics ───────────────────────────────
                    composable("metrics") {
                        MetricsScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToAdd = { metricId ->
                                val id = metricId ?: 0
                                navController.navigate("add_metric/$id")
                            }
                        )
                    }

                    composable("add_metric/{metricId}") { backStackEntry ->
                        val metricId = backStackEntry.arguments
                            ?.getString("metricId")
                            ?.toIntOrNull()
                        AddMetricScreen(
                            onBack = { navController.popBackStack() },
                            preselectedMetricId = if (metricId == 0) null else metricId
                        )
                    }

                    // ── Placeholders (we'll build these next) ─
                    composable("symptoms") {
                        Text("Symptoms Screen — Coming Soon")
                    }
                    composable("medical") {
                        Text("Medical History — Coming Soon")
                    }
                    composable("goals") {
                        Text("Goals — Coming Soon")
                    }
                    composable("firstaid") {
                        Text("First Aid Guide — Coming Soon")
                    }
                    composable("trends") {
                        Text("Health Trends — Coming Soon")
                    }
                }
            }
        }
    }
}