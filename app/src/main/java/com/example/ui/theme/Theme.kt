package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val OrosiDarkColorScheme = darkColorScheme(
    primary = PersianLapisLight,
    onPrimary = PersianLapis,
    primaryContainer = PersianLapis,
    onPrimaryContainer = PersianLapisFixed,
    secondary = NishapurTurquoiseSoft,
    onSecondary = NishapurTurquoiseDeep,
    secondaryContainer = NishapurTurquoiseContainer,
    onSecondaryContainer = NishapurTurquoiseFixed,
    tertiary = SaffronGoldLight,
    onTertiary = SaffronGoldDeep,
    tertiaryContainer = SaffronGoldDeep,
    onTertiaryContainer = SaffronGoldFixed,
    background = OrosiSurface,
    onBackground = OrosiOnSurface,
    surface = OrosiSurface,
    onSurface = OrosiOnSurface,
    surfaceVariant = OrosiSurfaceHighest,
    onSurfaceVariant = OrosiOnSurfaceVariant,
    surfaceContainerLowest = OrosiSurfaceLowest,
    surfaceContainerLow = OrosiSurfaceLow,
    surfaceContainer = OrosiSurfaceContainer,
    surfaceContainerHigh = OrosiSurfaceHigh,
    surfaceContainerHighest = OrosiSurfaceHighest,
    outline = OrosiOutline,
    outlineVariant = OrosiOutlineVariant,
    error = RubyRoseSoft,
    onError = RubyRoseDeep,
    errorContainer = RubyRose,
    onErrorContainer = AlabasterSilk
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = OrosiDarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
