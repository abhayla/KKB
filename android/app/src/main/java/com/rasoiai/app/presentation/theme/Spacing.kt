package com.rasoiai.app.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    val xxxl: Dp = 64.dp,
    // Common patterns
    val screenPadding: Dp = 16.dp,
    val cardPadding: Dp = 16.dp,
    val cardGap: Dp = 12.dp,
    val sectionGap: Dp = 24.dp,
    val iconTextGap: Dp = 8.dp,
    val listItemHeight: Dp = 56.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

val spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
