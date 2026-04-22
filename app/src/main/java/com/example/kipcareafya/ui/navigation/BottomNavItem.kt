package com.example.kipcareafya.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home    : BottomNavItem("dashboard", "Home",    Icons.Default.Home)
    object Metrics : BottomNavItem("metrics",   "Metrics", Icons.Default.MonitorHeart)
    object Symptoms: BottomNavItem("symptoms",  "Symptoms",Icons.Default.Search)
    object Medical : BottomNavItem("medical",   "Medical", Icons.Default.MedicalServices)
    object Profile : BottomNavItem("profile",   "Profile", Icons.Default.Person)
}