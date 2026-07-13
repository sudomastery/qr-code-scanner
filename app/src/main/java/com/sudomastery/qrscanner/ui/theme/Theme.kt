package com.sudomastery.qrscanner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Selectable accent themes. Each swaps the primary and secondary tonal
 * families while the dark neutral surfaces stay the same, keeping the
 * Material You look across all of them. The swatch is what the theme
 * picker in Settings shows.
 */
private class Accent(
    val swatch: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color
)

private val Accents = mapOf(
    "blue" to Accent(
        swatch = Color(0xFFA8C7FA),
        primary = Color(0xFFA8C7FA),
        onPrimary = Color(0xFF062E6F),
        primaryContainer = Color(0xFF0842A0),
        onPrimaryContainer = Color(0xFFD3E3FD),
        secondary = Color(0xFFBFC6DC),
        onSecondary = Color(0xFF293041),
        secondaryContainer = Color(0xFF3F4759),
        onSecondaryContainer = Color(0xFFDBE2F9)
    ),
    "orange" to Accent(
        swatch = Color(0xFFFFB77C),
        primary = Color(0xFFFFB77C),
        onPrimary = Color(0xFF4A2800),
        primaryContainer = Color(0xFF6A3C00),
        onPrimaryContainer = Color(0xFFFFDCBE),
        secondary = Color(0xFFE2C0A4),
        onSecondary = Color(0xFF422C16),
        secondaryContainer = Color(0xFF5B422A),
        onSecondaryContainer = Color(0xFFFFDCBE)
    ),
    "purple" to Accent(
        swatch = Color(0xFFD0BCFF),
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8)
    ),
    "green" to Accent(
        swatch = Color(0xFF8BD8A0),
        primary = Color(0xFF8BD8A0),
        onPrimary = Color(0xFF003919),
        primaryContainer = Color(0xFF00522A),
        onPrimaryContainer = Color(0xFFA6F4BB),
        secondary = Color(0xFFB7CCB8),
        onSecondary = Color(0xFF233426),
        secondaryContainer = Color(0xFF394B3C),
        onSecondaryContainer = Color(0xFFD3E8D3)
    ),
    "yellow" to Accent(
        swatch = Color(0xFFDBC66E),
        primary = Color(0xFFDBC66E),
        onPrimary = Color(0xFF3A3000),
        primaryContainer = Color(0xFF534600),
        onPrimaryContainer = Color(0xFFF8E287),
        secondary = Color(0xFFCFC6A4),
        onSecondary = Color(0xFF363016),
        secondaryContainer = Color(0xFF4E472A),
        onSecondaryContainer = Color(0xFFEBE3BF)
    ),
    "red" to Accent(
        swatch = Color(0xFFFFB4AB),
        primary = Color(0xFFFFB4AB),
        onPrimary = Color(0xFF690005),
        primaryContainer = Color(0xFF93000A),
        onPrimaryContainer = Color(0xFFFFDAD6),
        secondary = Color(0xFFE7BDB8),
        onSecondary = Color(0xFF442926),
        secondaryContainer = Color(0xFF5D3F3C),
        onSecondaryContainer = Color(0xFFFFDAD6)
    )
)

const val DEFAULT_THEME = "blue"

/** Ordered options for the theme picker: name to swatch color. */
val ThemeOptions: List<Pair<String, Color>> = listOf(
    "blue", "orange", "purple", "green", "yellow", "red"
).map { it to Accents.getValue(it).swatch }

private fun schemeFor(themeColor: String): ColorScheme {
    val accent = Accents[themeColor] ?: Accents.getValue(DEFAULT_THEME)
    return darkColorScheme(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        secondary = accent.secondary,
        onSecondary = accent.onSecondary,
        secondaryContainer = accent.secondaryContainer,
        onSecondaryContainer = accent.onSecondaryContainer,
        tertiary = Color(0xFFDDBCE0),
        onTertiary = Color(0xFF3F2844),
        tertiaryContainer = Color(0xFF573E5C),
        onTertiaryContainer = Color(0xFFFAD8FD),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
        background = Color(0xFF121318),
        onBackground = Color(0xFFE3E2E6),
        surface = Color(0xFF121318),
        onSurface = Color(0xFFE3E2E6),
        surfaceVariant = Color(0xFF44474F),
        onSurfaceVariant = Color(0xFFC4C6D0),
        surfaceContainer = Color(0xFF1E1F25),
        surfaceContainerHigh = Color(0xFF282A2F),
        surfaceContainerHighest = Color(0xFF33353A),
        surfaceContainerLow = Color(0xFF1A1B20),
        surfaceContainerLowest = Color(0xFF0D0E13),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF44474F)
    )
}

// Big friendly rounded corners everywhere
private val QrShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun QrScannerTheme(themeColor: String = DEFAULT_THEME, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = schemeFor(themeColor),
        shapes = QrShapes,
        typography = Typography(),
        content = content
    )
}
