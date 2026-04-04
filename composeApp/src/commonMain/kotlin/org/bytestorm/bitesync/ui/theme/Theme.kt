package org.bytestorm.bitesync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BrightRed = Color(0xFFFF6B6B)
val Orange = Color(0xFFFF8E53)
val Yellow = Color(0xFFFFD93D)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate50 = Color(0xFFF8FAFC)
val DeepRed = Color(0xFF1a0000)
val DarkRed = Color(0xFF8B0000)
val DarkBlue = Color(0xFF1a1a2e)
val DarkerBlue = Color(0xFF16213e)

data class BiteSyncGradients(
    val main: Brush,
    val suddenDeath: Brush
)

val LocalBiteSyncGradients = staticCompositionLocalOf<BiteSyncGradients> {
    error("No BiteSyncGradients provided")
}

object BiteSyncTheme {
    val gradients: BiteSyncGradients
        @Composable
        @ReadOnlyComposable
        get() = LocalBiteSyncGradients.current
}

@Composable
fun BiteSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val gradients = if (darkTheme) {
        BiteSyncGradients(
            main = Brush.verticalGradient(listOf(Slate900, Slate800)),
            suddenDeath = Brush.verticalGradient(listOf(DeepRed, DarkRed, DeepRed))
        )
    } else {
        BiteSyncGradients(
            main = Brush.verticalGradient(listOf(Color.White, Slate50)),
            suddenDeath = Brush.verticalGradient(listOf(BrightRed, DarkRed))
        )
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = BrightRed,
            secondary = Orange,
            background = Slate900,
            surface = Slate800,
            surfaceVariant = Slate900,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Slate50,
            onSurface = Slate50
        )
    } else {
        lightColorScheme(
            primary = BrightRed,
            secondary = Orange,
            background = Slate50,
            surface = Color.White,
            surfaceVariant = Color(0xFFF1F5F9),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Slate900,
            onSurface = Slate900
        )
    }

    CompositionLocalProvider(
        LocalBiteSyncGradients provides gradients
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
