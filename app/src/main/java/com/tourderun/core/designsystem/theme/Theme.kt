package com.tourderun.core.designsystem.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalTdrColors = staticCompositionLocalOf { LightTdrColors }
private val LocalTdrTypography = staticCompositionLocalOf { TdrTypography() }
private val DefaultTypography = TdrTypography()

/**
 * 앱 테마. 화면은 [TdrTheme.colors], [TdrTheme.typography]로 토큰을 읽는다.
 * Material3 컴포넌트(AlertDialog 등)도 같은 색을 쓰도록 colorScheme을 맞춰 둔다.
 *
 * @param darkTheme 러닝 화면(S09)만 true. 시스템 다크 모드는 아직 따르지 않는다.
 */
@Composable
fun TdrTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkTdrColors else LightTdrColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.brandBright,
            onPrimary = colors.inkOnBrand,
            background = colors.bgCanvas,
            onBackground = colors.ink,
            surface = colors.bgSurface,
            onSurface = colors.ink,
            surfaceVariant = colors.bgSubtle,
            onSurfaceVariant = colors.inkMuted,
            outline = colors.borderControl,
            outlineVariant = colors.border,
            error = colors.danger,
            scrim = colors.scrim,
        )
    } else {
        lightColorScheme(
            primary = colors.brand,
            onPrimary = colors.inkOnBrand,
            primaryContainer = colors.brandSoft,
            onPrimaryContainer = colors.brand,
            background = colors.bgCanvas,
            onBackground = colors.ink,
            surface = colors.bgSurface,
            onSurface = colors.ink,
            surfaceVariant = colors.bgSubtle,
            onSurfaceVariant = colors.inkMuted,
            surfaceContainerHigh = colors.bgSurface,
            outline = colors.borderControl,
            outlineVariant = colors.border,
            error = colors.danger,
            scrim = colors.scrim,
        )
    }
    CompositionLocalProvider(
        LocalTdrColors provides colors,
        LocalTdrTypography provides DefaultTypography,
    ) {
        MaterialTheme(colorScheme = scheme) {
            // 글자색은 스타일이 아니라 LocalContentColor로 준다. 그래야 Surface(contentColor) 안에서 바뀐다.
            CompositionLocalProvider(LocalContentColor provides colors.ink) {
                ProvideTextStyle(DefaultTypography.body, content)
            }
        }
    }
}

object TdrTheme {
    val colors: TdrColors
        @Composable @ReadOnlyComposable
        get() = LocalTdrColors.current

    val typography: TdrTypography
        @Composable @ReadOnlyComposable
        get() = LocalTdrTypography.current
}
