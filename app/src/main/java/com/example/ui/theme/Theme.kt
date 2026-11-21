package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.core.ThemeMode

@Immutable
data class NuxExtendedColors(
    val isDark: Boolean,
    val glassSurface: Color,
    val glassSurfaceStrong: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val sidebarPanel: Color,
    val paper: Color,
    val brandGradient: Brush,
    val capsuleGradient: Brush,
    val success: Color,
    val warning: Color,
    val codeBackground: Color,
    val codeBorder: Color,
)

val LocalNuxColors = staticCompositionLocalOf {
    NuxExtendedColors(
        isDark = true,
        glassSurface = DarkGlassSurface,
        glassSurfaceStrong = DarkGlassSurfaceStrong,
        glassBorder = DarkGlassBorder,
        glassHighlight = DarkGlassHighlight,
        sidebarPanel = DarkSidebar,
        paper = DarkPaper,
        brandGradient = Brush.linearGradient(listOf(NuxBrandStart, NuxBrandMid, NuxBrandEnd)),
        capsuleGradient = Brush.linearGradient(listOf(NuxBrandStart, NuxCapsuleEnd)),
        success = NuxSuccess,
        warning = NuxWarning,
        codeBackground = Color(0xFF14161D),
        codeBorder = Color(0xFF282C37),
    )
}

private val NuxDarkColorScheme = darkColorScheme(
    primary = NuxBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A2D54),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = DarkSecondary,
    onSecondary = DarkForeground,
    secondaryContainer = DarkAccent,
    onSecondaryContainer = DarkForeground,
    tertiary = NuxBrandEnd,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkCard,
    onSurface = DarkForeground,
    surfaceVariant = DarkPopover,
    onSurfaceVariant = DarkMutedForeground,
    outline = DarkBorder,
    outlineVariant = DarkGlassBorder,
    error = DarkDestructive,
    onError = Color.White,
)

private val NuxLightColorScheme = lightColorScheme(
    primary = NuxBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EEFF),
    onPrimaryContainer = Color(0xFF003B78),
    secondary = LightSecondary,
    onSecondary = LightForeground,
    secondaryContainer = LightAccent,
    onSecondaryContainer = LightForeground,
    tertiary = NuxBrandEnd,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightCard,
    onSurface = LightForeground,
    surfaceVariant = LightMuted,
    onSurfaceVariant = LightMutedForeground,
    outline = LightBorder,
    outlineVariant = LightGlassBorder,
    error = LightDestructive,
    onError = Color.White,
)

private val NuxShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun NuxTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    layoutDirection: LayoutDirection = LayoutDirection.Rtl,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = if (useDark) NuxDarkColorScheme else NuxLightColorScheme
    val extended = NuxExtendedColors(
        isDark = useDark,
        glassSurface = if (useDark) DarkGlassSurface else LightGlassSurface,
        glassSurfaceStrong = if (useDark) DarkGlassSurfaceStrong else LightGlassSurfaceStrong,
        glassBorder = if (useDark) DarkGlassBorder else LightGlassBorder,
        glassHighlight = if (useDark) DarkGlassHighlight else LightGlassHighlight,
        sidebarPanel = if (useDark) DarkSidebar else LightSidebar,
        paper = if (useDark) DarkPaper else LightPaper,
        brandGradient = Brush.linearGradient(listOf(NuxBrandStart, NuxBrandMid, NuxBrandEnd)),
        capsuleGradient = Brush.linearGradient(listOf(NuxBrandStart, NuxCapsuleEnd)),
        success = NuxSuccess,
        warning = NuxWarning,
        codeBackground = if (useDark) Color(0xFF13151C) else Color(0xFF171923),
        codeBorder = if (useDark) Color(0xFF282D3B) else Color(0xFF2D3342),
    )

    CompositionLocalProvider(
        LocalNuxColors provides extended,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NuxTypography,
            shapes = NuxShapes,
            content = content
        )
    }
}
