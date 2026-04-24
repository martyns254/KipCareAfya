package com.example.kipcareafya.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.kipcareafya.data.local.UserPreferences

@Composable
fun KipCareAfyaTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val isDarkMode by prefs.isDarkMode.collectAsState(initial = false)

    val colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}