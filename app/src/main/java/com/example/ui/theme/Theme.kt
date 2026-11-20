package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CollegeLightColorScheme = lightColorScheme(
  primary = NavyPrimary,
  onPrimary = Color.White,
  primaryContainer = NavyPrimaryContainer,
  onPrimaryContainer = OnNavyPrimaryContainer,
  secondary = TealSecondary,
  onSecondary = Color.White,
  secondaryContainer = TealSecondaryContainer,
  onSecondaryContainer = OnTealSecondaryContainer,
  tertiary = AmberTertiary,
  onTertiary = Color.White,
  tertiaryContainer = AmberTertiaryContainer,
  onTertiaryContainer = OnAmberTertiaryContainer,
  background = CampusBackground,
  onBackground = TextPrimary,
  surface = CampusSurface,
  onSurface = TextPrimary,
  surfaceVariant = CampusSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = CampusOutline,
  outlineVariant = CampusOutlineVariant,
  error = StatusUrgent,
  onError = Color.White,
  errorContainer = StatusUrgentContainer,
  onErrorContainer = Color(0xFF7F1D1D),
)

private val CollegeDarkColorScheme = darkColorScheme(
  primary = DarkPrimary,
  onPrimary = Color(0xFF0A192F),
  primaryContainer = DarkPrimaryContainer,
  onPrimaryContainer = Color(0xFFDBEAFE),
  secondary = DarkSecondary,
  onSecondary = Color(0xFF042F2E),
  tertiary = DarkTertiary,
  onTertiary = Color(0xFF451A03),
  background = DarkBackground,
  onBackground = Color(0xFFF1F5F9),
  surface = DarkSurface,
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = Color(0xFF334155),
  outlineVariant = Color(0xFF1E293B),
)

val CollegeShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CollegeCoreTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) CollegeDarkColorScheme else CollegeLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = CollegeCoreTypography,
    shapes = CollegeShapes,
    content = content,
  )
}
