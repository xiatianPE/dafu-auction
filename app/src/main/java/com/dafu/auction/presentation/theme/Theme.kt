package com.dafu.auction.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dafu.auction.domain.model.ThemeType

// 先声明颜色常量，避免顶层属性初始化顺序问题
private val OnColorWhite = Color(0xFFFFFFFF)
private val OnColorWhite70 = Color(0xB3FFFFFF)
private val OnColorBlack87 = Color(0xDE000000)
private val OnColorBlack60 = Color(0x99000000)
private val DarkError = Color(0xFFFFB4AB)

private val LightColors = lightColorScheme(
    primary = Purple40,
    onPrimary = OnColorWhite,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = PurpleDark,
    secondary = Gold40,
    onSecondary = OnColorWhite,
    secondaryContainer = Gold80,
    onSecondaryContainer = OnColorBlack87,
    background = LightBackground,
    onBackground = OnColorBlack87,
    surface = LightSurface,
    onSurface = OnColorBlack87,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnColorBlack60,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = Purple80,
    onPrimary = OnColorBlack87,
    primaryContainer = PurpleDark,
    onPrimaryContainer = OnColorWhite,
    secondary = Gold80,
    onSecondary = OnColorBlack87,
    secondaryContainer = Gold40,
    onSecondaryContainer = OnColorWhite,
    background = DarkBackground,
    onBackground = OnColorWhite,
    surface = DarkSurface,
    onSurface = OnColorWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnColorWhite70,
    error = DarkError
)

val DafuTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
)

@Composable
fun DafuAuctionTheme(
    theme: ThemeType = ThemeType.AUTO,
    content: @Composable () -> Unit
) {
    val dark = when (theme) {
        ThemeType.LIGHT -> false
        ThemeType.DARK -> true
        ThemeType.AUTO -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = DafuTypography,
        content = content
    )
}
