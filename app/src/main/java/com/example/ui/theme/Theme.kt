package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DuddyPrimaryDark,
    onPrimary = DuddyOnPrimaryDark,
    primaryContainer = DuddyPrimaryContainerDark,
    onPrimaryContainer = DuddyOnPrimaryContainerDark,
    secondary = DuddySecondaryDark,
    onSecondary = DuddyOnSecondaryDark,
    secondaryContainer = DuddySecondaryContainerDark,
    onSecondaryContainer = DuddyOnSecondaryContainerDark,
    tertiary = DuddyTertiaryDark,
    onTertiary = DuddyOnTertiaryDark,
    tertiaryContainer = DuddyTertiaryContainerDark,
    onTertiaryContainer = DuddyOnTertiaryContainerDark,
    background = DuddyBackgroundDark,
    onBackground = DuddyOnBackgroundDark,
    surface = DuddySurfaceDark,
    onSurface = DuddyOnSurfaceDark,
    surfaceVariant = DuddySurfaceVariantDark,
    onSurfaceVariant = DuddyOnSurfaceVariantDark,
    outline = DuddyOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = DuddyPrimaryLight,
    onPrimary = DuddyOnPrimaryLight,
    primaryContainer = DuddyPrimaryContainerLight,
    onPrimaryContainer = DuddyOnPrimaryContainerLight,
    secondary = DuddySecondaryLight,
    onSecondary = DuddyOnSecondaryLight,
    secondaryContainer = DuddySecondaryContainerLight,
    onSecondaryContainer = DuddyOnSecondaryContainerLight,
    tertiary = DuddyTertiaryLight,
    onTertiary = DuddyOnTertiaryLight,
    tertiaryContainer = DuddyTertiaryContainerLight,
    onTertiaryContainer = DuddyOnTertiaryContainerLight,
    background = DuddyBackgroundLight,
    onBackground = DuddyOnBackgroundLight,
    surface = DuddySurfaceLight,
    onSurface = DuddyOnSurfaceLight,
    surfaceVariant = DuddySurfaceVariantLight,
    onSurfaceVariant = DuddyOnSurfaceVariantLight,
    outline = DuddyOutlineLight
)

@Composable
fun DuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to keep Duddy's distinctive expressive palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
