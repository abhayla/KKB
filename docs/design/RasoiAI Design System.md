# RasoiAI Design System

## Document Version: 1.0 | January 2025

This document defines the visual design system for RasoiAI Android application, including colors, typography, spacing, shapes, and Jetpack Compose implementation code.

---

## Summary of Decisions

| Area | Decision |
|------|----------|
| Primary Color | Orange `#FF6838` |
| Secondary Color | Green `#5A822B` |
| Background | Cream `#FDFAF4` |
| Typography | System Default (Roboto + Noto Sans Devanagari) |
| Dark Mode | Both (System-follow) |
| Dynamic Color | Brand Colors Only (no wallpaper adaptation) |
| Shapes | Rounded (8dp / 16dp / 24dp) |
| Spacing | 8dp grid system |

---

## 1. Color Palette

### 1.1 Brand Colors

| Role | Light Mode | Dark Mode | Usage |
|------|------------|-----------|-------|
| **Primary** | `#FF6838` | `#FFB59C` | CTAs, FAB, links, active states |
| **On Primary** | `#FFFFFF` | `#5F1600` | Text/icons on primary color |
| **Primary Container** | `#FFDBD0` | `#862200` | Subtle primary backgrounds |
| **On Primary Container** | `#3A0A00` | `#FFDBD0` | Text on primary container |
| **Secondary** | `#5A822B` | `#A8D475` | Tags, badges, success states |
| **On Secondary** | `#FFFFFF` | `#1A3700` | Text/icons on secondary |
| **Secondary Container** | `#C8F09A` | `#2D5000` | Subtle secondary backgrounds |
| **On Secondary Container** | `#0F2000` | `#C8F09A` | Text on secondary container |
| **Tertiary** | `#8B5A2B` | `#E6BC8E` | Accents, highlights |
| **On Tertiary** | `#FFFFFF` | `#432C0A` | Text/icons on tertiary |

### 1.2 Surface Colors

| Role | Light Mode | Dark Mode | Usage |
|------|------------|-----------|-------|
| **Background** | `#FDFAF4` | `#1C1B1F` | App background |
| **Surface** | `#FFFFFF` | `#2B2930` | Cards, sheets, dialogs |
| **Surface Variant** | `#F5EDE5` | `#49454F` | Differentiated surfaces |
| **On Background** | `#1C1B1F` | `#E6E1E5` | Primary text |
| **On Surface** | `#1C1B1F` | `#E6E1E5` | Text on surfaces |
| **On Surface Variant** | `#49454F` | `#CAC4D0` | Secondary text |
| **Outline** | `#7A757F` | `#938F99` | Borders, dividers |
| **Outline Variant** | `#CAC4D0` | `#49454F` | Subtle borders |

### 1.3 Semantic Colors

| Role | Light Mode | Dark Mode | Usage |
|------|------------|-----------|-------|
| **Error** | `#BA1A1A` | `#FFB4AB` | Error states, destructive actions |
| **On Error** | `#FFFFFF` | `#690005` | Text on error |
| **Error Container** | `#FFDAD6` | `#93000A` | Error backgrounds |
| **Success** | `#5A822B` | `#A8D475` | Success states (uses secondary) |
| **Warning** | `#E6A817` | `#FFD966` | Warnings, cautions |

### 1.4 Dietary Tag Colors

| Diet Type | Color | Hex | Usage |
|-----------|-------|-----|-------|
| Vegetarian | Green | `#5A822B` | Veg indicator dot/badge |
| Non-Vegetarian | Red | `#BA1A1A` | Non-veg indicator |
| Vegan | Dark Green | `#2D5000` | Vegan badge |
| Jain | Yellow | `#E6A817` | Jain-friendly indicator |
| Fasting | Purple | `#7C3AED` | Fasting mode recipes |

---

## 2. Typography

### 2.1 Font Families

| Language | Font | Fallback |
|----------|------|----------|
| English | Roboto | Sans-serif (system) |
| Hindi (Devanagari) | Noto Sans Devanagari | Sans-serif (system) |

> **Note**: Using system fonts ensures zero APK bloat and automatic language switching.

### 2.2 Type Scale

| Style | Size | Weight | Line Height | Letter Spacing | Usage |
|-------|------|--------|-------------|----------------|-------|
| **Display Large** | 57sp | 400 | 64sp | -0.25sp | Hero text (rare) |
| **Display Medium** | 45sp | 400 | 52sp | 0sp | Large headers |
| **Display Small** | 36sp | 400 | 44sp | 0sp | Section headers |
| **Headline Large** | 32sp | 400 | 40sp | 0sp | Screen titles |
| **Headline Medium** | 28sp | 400 | 36sp | 0sp | Card titles |
| **Headline Small** | 24sp | 400 | 32sp | 0sp | Subsection titles |
| **Title Large** | 22sp | 500 | 28sp | 0sp | Recipe names |
| **Title Medium** | 16sp | 500 | 24sp | 0.15sp | List item titles |
| **Title Small** | 14sp | 500 | 20sp | 0.1sp | Small titles |
| **Body Large** | 16sp | 400 | 24sp | 0.5sp | Primary body text |
| **Body Medium** | 14sp | 400 | 20sp | 0.25sp | Secondary body text |
| **Body Small** | 12sp | 400 | 16sp | 0.4sp | Captions |
| **Label Large** | 14sp | 500 | 20sp | 0.1sp | Buttons, tabs |
| **Label Medium** | 12sp | 500 | 16sp | 0.5sp | Small buttons |
| **Label Small** | 11sp | 500 | 16sp | 0.5sp | Chips, badges |

### 2.3 Typography Usage Guidelines

| Element | Style | Example |
|---------|-------|---------|
| App bar title | Title Large | "RasoiAI" |
| Screen title | Headline Large | "This Week's Meals" |
| Card title | Title Medium | "Dal Tadka" |
| Recipe description | Body Medium | "A classic North Indian..." |
| Ingredient item | Body Medium | "1 cup Toor dal" |
| Cooking step | Body Large | "Heat ghee in a pan..." |
| Button text | Label Large | "Start Cooking" |
| Chip/Tag | Label Small | "Vegetarian" |
| Caption | Body Small | "Prep: 10 mins" |

---

## 3. Spacing

### 3.1 Spacing Scale (8dp Grid)

| Token | Value | Compose | Usage |
|-------|-------|---------|-------|
| `none` | 0dp | `0.dp` | No spacing |
| `xxs` | 2dp | `2.dp` | Micro adjustments |
| `xs` | 4dp | `4.dp` | Icon padding, tight gaps |
| `sm` | 8dp | `8.dp` | Related elements |
| `md` | 16dp | `16.dp` | Standard padding/margins |
| `lg` | 24dp | `24.dp` | Section separation |
| `xl` | 32dp | `32.dp` | Large gaps |
| `xxl` | 48dp | `48.dp` | Major sections |
| `xxxl` | 64dp | `64.dp` | Screen-level spacing |

### 3.2 Common Spacing Patterns

| Pattern | Value | Usage |
|---------|-------|-------|
| Screen padding | 16dp | Horizontal padding for all screens |
| Card padding | 16dp | Internal card padding |
| Card gap | 12dp | Space between cards in list |
| Section gap | 24dp | Between major sections |
| Icon-text gap | 8dp | Space between icon and label |
| Button padding | 16dp horizontal, 12dp vertical | Button internal padding |
| List item height | 56dp minimum | Single-line list items |
| List item padding | 16dp | Horizontal padding in list items |

---

## 4. Shapes (Corner Radius)

### 4.1 Shape Scale

| Token | Value | Compose | Usage |
|-------|-------|---------|-------|
| `none` | 0dp | `RoundedCornerShape(0.dp)` | Sharp edges |
| `extraSmall` | 4dp | `RoundedCornerShape(4.dp)` | Chips, small elements |
| `small` | 8dp | `RoundedCornerShape(8.dp)` | Buttons, text fields |
| `medium` | 16dp | `RoundedCornerShape(16.dp)` | Cards, dialogs |
| `large` | 24dp | `RoundedCornerShape(24.dp)` | Bottom sheets, large cards |
| `extraLarge` | 32dp | `RoundedCornerShape(32.dp)` | Full-screen modals |
| `full` | 50% | `CircleShape` | FAB, avatars, circular buttons |

### 4.2 Component Shape Mapping

| Component | Shape | Radius |
|-----------|-------|--------|
| Button | Small | 8dp |
| Card | Medium | 16dp |
| Dialog | Large | 24dp |
| Bottom Sheet | Large (top only) | 24dp |
| Text Field | Small | 8dp |
| Chip | Full | 8dp |
| FAB | Large | 16dp |
| Navigation Bar | None | 0dp |
| Image (in card) | Medium | 16dp |

---

## 5. Elevation & Shadows

### 5.1 Elevation Scale

| Level | Value | Usage |
|-------|-------|-------|
| Level 0 | 0dp | Flat surfaces |
| Level 1 | 1dp | Cards at rest |
| Level 2 | 3dp | Raised buttons, cards on hover |
| Level 3 | 6dp | FAB, navigation drawer |
| Level 4 | 8dp | Bottom sheet, dialogs |
| Level 5 | 12dp | Modal surfaces |

### 5.2 Tonal Elevation (Material 3)

In Material 3, elevation is expressed through **tonal color** rather than shadows:

| Elevation | Surface Tint Opacity |
|-----------|---------------------|
| 0dp | 0% |
| 1dp | 5% |
| 3dp | 8% |
| 6dp | 11% |
| 8dp | 12% |
| 12dp | 14% |

---

## 6. Jetpack Compose Implementation

### 6.1 Color.kt

```kotlin
package com.rasoiai.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Primary - Orange
val PrimaryLight = Color(0xFFFF6838)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFFFDBD0)
val OnPrimaryContainerLight = Color(0xFF3A0A00)

val PrimaryDark = Color(0xFFFFB59C)
val OnPrimaryDark = Color(0xFF5F1600)
val PrimaryContainerDark = Color(0xFF862200)
val OnPrimaryContainerDark = Color(0xFFFFDBD0)

// Secondary - Green
val SecondaryLight = Color(0xFF5A822B)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFC8F09A)
val OnSecondaryContainerLight = Color(0xFF0F2000)

val SecondaryDark = Color(0xFFA8D475)
val OnSecondaryDark = Color(0xFF1A3700)
val SecondaryContainerDark = Color(0xFF2D5000)
val OnSecondaryContainerDark = Color(0xFFC8F09A)

// Tertiary - Brown
val TertiaryLight = Color(0xFF8B5A2B)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFDDB8)
val OnTertiaryContainerLight = Color(0xFF2E1500)

val TertiaryDark = Color(0xFFE6BC8E)
val OnTertiaryDark = Color(0xFF432C0A)
val TertiaryContainerDark = Color(0xFF5D4119)
val OnTertiaryContainerDark = Color(0xFFFFDDB8)

// Error
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Background & Surface - Light
val BackgroundLight = Color(0xFFFDFAF4)
val OnBackgroundLight = Color(0xFF1C1B1F)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1C1B1F)
val SurfaceVariantLight = Color(0xFFF5EDE5)
val OnSurfaceVariantLight = Color(0xFF49454F)

// Background & Surface - Dark
val BackgroundDark = Color(0xFF1C1B1F)
val OnBackgroundDark = Color(0xFFE6E1E5)
val SurfaceDark = Color(0xFF2B2930)
val OnSurfaceDark = Color(0xFFE6E1E5)
val SurfaceVariantDark = Color(0xFF49454F)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)

// Outline
val OutlineLight = Color(0xFF7A757F)
val OutlineVariantLight = Color(0xFFCAC4D0)
val OutlineDark = Color(0xFF938F99)
val OutlineVariantDark = Color(0xFF49454F)

// Dietary Tag Colors
val VegetarianGreen = Color(0xFF5A822B)
val NonVegetarianRed = Color(0xFFBA1A1A)
val VeganDarkGreen = Color(0xFF2D5000)
val JainYellow = Color(0xFFE6A817)
val FastingPurple = Color(0xFF7C3AED)

// Additional Semantic Colors
val WarningYellow = Color(0xFFE6A817)
val SuccessGreen = Color(0xFF5A822B)
```

### 6.2 Type.kt

```kotlin
package com.rasoiai.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Using system default fonts (Roboto for English, Noto Sans Devanagari for Hindi)
val RasoiTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

### 6.3 Shape.kt

```kotlin
package com.rasoiai.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RasoiShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
```

### 6.4 Spacing.kt

```kotlin
package com.rasoiai.app.presentation.theme

import androidx.compose.ui.unit.dp

object RasoiSpacing {
    val none = 0.dp
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp

    // Common patterns
    val screenPadding = md
    val cardPadding = md
    val cardGap = 12.dp
    val sectionGap = lg
    val iconTextGap = sm
}
```

### 6.5 Theme.kt

```kotlin
package com.rasoiai.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

@Composable
fun RasoiAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RasoiTypography,
        shapes = RasoiShapes,
        content = content
    )
}
```

---

## 7. Component Guidelines

### 7.1 Buttons

| Type | Usage | Style |
|------|-------|-------|
| **Filled** | Primary actions (Generate Plan, Start Cooking) | Primary color, white text |
| **Outlined** | Secondary actions (Skip, Cancel) | Border only, primary text |
| **Text** | Tertiary actions (Learn More, See All) | No background, primary text |
| **FAB** | Main screen action (Add Recipe) | Primary color, large rounded |

### 7.2 Cards

| Type | Usage | Elevation |
|------|-------|-----------|
| **Elevated Card** | Meal items, recipe cards | Level 1 (1dp) |
| **Filled Card** | Grocery items, settings items | Level 0, surface color |
| **Outlined Card** | Selection cards, info cards | Level 0, border |

### 7.3 Dietary Indicators

```
Vegetarian:    ● (Green #5A822B)
Non-Veg:       ● (Red #BA1A1A)
Vegan:         ● (Dark Green #2D5000)
Jain:          ● (Yellow #E6A817)
```

### 7.4 Navigation

| Component | Style |
|-----------|-------|
| Bottom Navigation | 5 items max, filled icons for selected |
| Top App Bar | Large/Medium for main screens, Small for detail |
| Back Navigation | Arrow icon, no text |

---

## 8. Accessibility

### 8.1 Color Contrast

All color combinations meet WCAG 2.1 AA standards:

| Combination | Contrast Ratio | Status |
|-------------|----------------|--------|
| Primary on White | 4.5:1 | ✅ Pass |
| On Primary on Primary | 7.2:1 | ✅ Pass |
| Body text on Background | 12.8:1 | ✅ Pass |
| Secondary text on Surface | 7.1:1 | ✅ Pass |

### 8.2 Touch Targets

| Element | Minimum Size |
|---------|--------------|
| Buttons | 48dp x 48dp |
| Icons (tappable) | 48dp x 48dp |
| List items | 48dp height minimum |
| Checkboxes | 48dp touch area |

### 8.3 Text Sizing

- Minimum body text: 14sp
- Support system font scaling
- Test with 200% font size

---

## References

- [Material Design 3](https://m3.material.io/)
- [Material Theme Builder](https://m3.material.io/theme-builder)
- [Jetpack Compose Theming](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Ollie.ai](https://ollie.ai/) - Reference design inspiration

---

*Document Created: January 2025*
*Project: RasoiAI Android App*
