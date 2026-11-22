package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SummitLightColorScheme = lightColorScheme(
    primary = PinePrimaryLight,
    onPrimary = PineOnPrimaryLight,
    primaryContainer = PinePrimaryContainerLight,
    onPrimaryContainer = PineOnPrimaryContainerLight,
    secondary = TerracottaSecondaryLight,
    onSecondary = TerracottaOnSecondaryLight,
    secondaryContainer = TerracottaSecondaryContainerLight,
    onSecondaryContainer = TerracottaOnSecondaryContainerLight,
    tertiary = OchreTertiaryLight,
    onTertiary = OchreOnTertiaryLight,
    tertiaryContainer = OchreTertiaryContainerLight,
    onTertiaryContainer = OchreOnTertiaryContainerLight,
    background = BirchBackgroundLight,
    onBackground = BirchOnBackgroundLight,
    surface = BirchSurfaceLight,
    onSurface = BirchOnSurfaceLight,
    surfaceVariant = BirchSurfaceVariantLight,
    onSurfaceVariant = BirchOnSurfaceVariantLight,
    surfaceContainerLowest = BirchSurfaceContainerLowestLight,
    surfaceContainerLow = BirchSurfaceContainerLowLight,
    surfaceContainer = BirchSurfaceContainerLight,
    surfaceContainerHigh = BirchSurfaceContainerHighLight,
    outline = BirchOutlineLight,
    outlineVariant = BirchOutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val SummitDarkColorScheme = darkColorScheme(
    primary = PinePrimaryDark,
    onPrimary = PineOnPrimaryDark,
    primaryContainer = PinePrimaryContainerDark,
    onPrimaryContainer = PineOnPrimaryContainerDark,
    secondary = TerracottaSecondaryDark,
    onSecondary = TerracottaOnSecondaryDark,
    secondaryContainer = TerracottaSecondaryContainerDark,
    onSecondaryContainer = TerracottaOnSecondaryContainerDark,
    tertiary = OchreTertiaryDark,
    onTertiary = OchreOnTertiaryDark,
    tertiaryContainer = OchreTertiaryContainerDark,
    onTertiaryContainer = OchreOnTertiaryContainerDark,
    background = RidgeBackgroundDark,
    onBackground = RidgeOnBackgroundDark,
    surface = RidgeSurfaceDark,
    onSurface = RidgeOnSurfaceDark,
    surfaceVariant = RidgeSurfaceVariantDark,
    onSurfaceVariant = RidgeOnSurfaceVariantDark,
    surfaceContainerLowest = RidgeSurfaceContainerLowestDark,
    surfaceContainerLow = RidgeSurfaceContainerLowDark,
    surfaceContainer = RidgeSurfaceContainerDark,
    surfaceContainerHigh = RidgeSurfaceContainerHighDark,
    outline = RidgeOutlineDark,
    outlineVariant = RidgeOutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

val SummitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SummitPaceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SummitDarkColorScheme else SummitLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SummitShapes,
        content = content
    )
}
