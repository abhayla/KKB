# Flow 08: Dark Mode

## Metadata
- **Flow Name:** `dark-mode`
- **Goal:** Toggle dark theme, verify visual consistency across 6 key screens
- **Preconditions:** User authenticated with meal plan
- **Estimated Duration:** 4-6 minutes
- **Screens Covered:** Settings, Home, Grocery, Chat, Recipe Detail, Favorites
- **Depends On:** none (needs authenticated user)
- **State Produced:** Theme preference may change (restored to light at end)

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User authenticated with meal plan
- [ ] App on Home screen
- [ ] App currently in Light mode (default)

## Test User Persona

Uses existing Sharma family data. Tests visual theme, not functionality.

## Steps

### Phase A: Enable Dark Mode (Steps 1-3)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| A1 | Navigate: Profile icon → Settings | Settings screen (light theme) | `flow08_settings_light.png` | — |
| A2 | Find dark mode / theme toggle | Toggle or setting for "Dark Mode" or "Theme" | — | — |
| A3 | Enable dark mode | Screen background changes to dark colors | `flow08_settings_dark.png` | — |

**Note:** If dark mode toggle is not directly in Settings, check system settings or the app's appearance section. Some apps follow system theme — in that case:
```bash
# Set system dark mode via ADB
$ADB shell cmd uimode night yes
```

### Phase B: Screen Tour in Dark Mode (Steps 4-11)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| B1 | Navigate to Home (press BACK or bottom nav) | Home in dark theme | `flow08_home_dark.png` | — |
| B2 | Visual check: dark background, light text, proper contrast | No white-on-white or black-on-black text | — | — |
| B3 | Verify meal cards readable | Recipe names visible against dark background | — | — |
| B4 | Tap bottom nav "Grocery" | Grocery in dark theme | `flow08_grocery_dark.png` | — |
| B5 | Visual check: categories and items readable | Proper contrast on all elements | — | — |
| B6 | Tap bottom nav "Chat" | Chat in dark theme | `flow08_chat_dark.png` | — |
| B7 | Visual check: input field, messages visible | No invisible text | — | — |
| B8 | Tap bottom nav "Favs" | Favorites in dark theme | `flow08_favorites_dark.png` | — |
| B9 | Visual check: cards or empty state readable | Proper theming | — | — |
| B10 | Tap bottom nav "Home" → tap meal card → "View Recipe" | Recipe Detail in dark theme | `flow08_recipe_dark.png` | — |
| B11 | Visual check: ingredients, instructions readable | No contrast issues | — | — |

### Phase C: Restore Light Mode (Steps 12-14)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| C1 | Navigate to Settings | Settings screen | — | — |
| C2 | Disable dark mode (toggle back to light) | Screen returns to light theme | — | — |
| C3 | Verify Home in light mode | Light background restored | `flow08_home_light_restored.png` | — |

**If system theme was used:**
```bash
# Restore system light mode
$ADB shell cmd uimode night no
```

## Validation Checkpoints

No `validate_meal_plan.py` checkpoints — validation is visual:
- All screenshots in dark mode show:
  - Dark background (`#1C1B1F` or similar)
  - Light text with adequate contrast
  - No invisible or unreadable text
  - Proper theming of cards, buttons, navigation
  - No hardcoded light-only colors

## Fix Strategy

**Relevant files for this flow:**
- Theme: `core/src/main/java/com/rasoiai/core/ui/theme/` (Color.kt, Theme.kt, Type.kt)
- Dark colors: Look for `darkColorScheme` in Theme.kt
- Settings toggle: `app/presentation/settings/SettingsScreen.kt`
- Individual screens: check for hardcoded colors instead of MaterialTheme references

**Common issues:**
- Hardcoded white background → should use `MaterialTheme.colorScheme.surface`
- Hardcoded black text → should use `MaterialTheme.colorScheme.onSurface`
- Icon tint not adapting → should use `LocalContentColor.current`
- Bottom nav not themed → check `RasoiBottomNavigation` composable

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Settings | A1-A3, C1-C2 | Dark mode toggle |
| Home | B1-B3, C3 | Dark theme + restore |
| Grocery | B4-B5 | Dark theme |
| Chat | B6-B7 | Dark theme |
| Favorites | B8-B9 | Dark theme |
| Recipe Detail | B10-B11 | Dark theme |
