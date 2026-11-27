package com.kibitzer.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkAccent,
    tertiary = AccentGold,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = CardFace,
    onSecondary = CardFace,
    onTertiary = CardBlack,
    onBackground = CardFace,
    onSurface = CardFace,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TableDark,
    secondary = CardBlue,
    tertiary = AccentGold,
    background = CardFace,
    surface = CardFace,
    onPrimary = CardFace,
    onSecondary = CardFace,
    onTertiary = CardBlack,
    onBackground = CardBlack,
    onSurface = CardBlack,
  )

@Composable
fun KibitzerTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
    typography = KibitzerTypography,
    content = content,
  )
}
