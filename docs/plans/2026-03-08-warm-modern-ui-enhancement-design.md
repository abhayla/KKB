# Warm-Modern UI Enhancement — Design Document

**Date:** 2026-03-08
**Scope:** Visual polish + design consistency across all 63 screens in `docs/UI Designs/UI-UX-Material3/`
**Approach:** Hybrid — Golden Screens + Design System (Approach C)
**Replication target:** Android app (Kotlin/Jetpack Compose)

---

## Design Philosophy

**"Warm Modern"** — Indian kitchen warmth (saffron, turmeric, clay tones) meets Google Material You cleanliness (generous whitespace, crisp cards, systematic spacing).

**Constraint:** Every CSS token and class must map 1:1 to a Compose equivalent. If it can't be expressed in Compose, don't add it to CSS.

---

## Audit Summary

63 screens audited across 4 polish tiers:

| Tier | Quality | Count | Examples |
|------|---------|-------|---------|
| 1 | Production-ready | 8 | detail-recipe, feature-pantry, feature-leaderboard |
| 2 | High quality | ~20 | onboarding steps 1/3/5, settings-cuisine, modal-conflict |
| 3 | Functional but basic | ~20 | main-home, main-stats, settings-dietary |
| 4 | Bare-bones | ~12 | **main-grocery, main-chat, main-favorites**, settings-notifications |

**Critical gap:** The 3 most-used daily screens (grocery, chat, favorites) are Tier 4 with zero inline styles.

---

## Phase 1: Golden Screens (5 screens)

Establish the visual standard by perfecting these high-impact screens:

### 1.1 auth-splash.html
- **Current:** Static gradient, spinner, no entry animation
- **Upgrade:** Logo `bounceIn` animation, tagline `fadeInUp` with stagger, refined warm hero gradient
- **Compose:** `AnimatedVisibility` + `fadeIn()` + `slideInVertically()`

### 1.2 main-home.html
- **Current:** 2 inline styles, basic meal cards, flat day selector
- **Upgrade:** Warm gradient on day-selector header, elevated meal cards with food emoji accent backgrounds, stagger animation on meal list, warm shadow on cards
- **Compose:** `Card(elevation = 4.dp)`, `LazyColumn` with `animateItemPlacement()`

### 1.3 main-grocery.html
- **Current:** 0 inline styles, bare JS rendering
- **Upgrade:** Category cards with `card-filled` warm background, progress bar per category (checked/total), stagger animation, empty state component
- **Compose:** `Card(colors = filledCardColors(SurfaceWarm))`, `LinearProgressIndicator`

### 1.4 main-chat.html
- **Current:** 0 inline styles, basic bubbles
- **Upgrade:** Warm-tinted user message bubbles (`--surface-warm`), AI bubbles with subtle gradient border, typing indicator dots animation, redesigned quick-chip bar
- **Compose:** `Surface(color = SurfaceWarm, shape = RoundedCornerShape(...))`, `AnimatedContent`

### 1.5 detail-recipe.html
- **Current:** Already Tier 1 (reference screen)
- **Upgrade:** Minor — align gradient to new `--gradient-hero` token, warm shadows, verify all component classes match new system
- **Compose:** Already implemented, token update only

---

## Phase 2: Design System Extraction (shared.css + shared.js)

Extract patterns from golden screens into reusable components.

### 2.1 Color Token Updates

| Token | Current | New | Compose |
|-------|---------|-----|---------|
| `--background` | `#FDFAF4` | `#FFF8F2` | `Background` |
| `--surface-variant` | `#F5EDE5` | `#F7EDE3` | `SurfaceVariant` |
| `--primary` | `#FF6838` | `#E85D2C` | `Primary` |
| `--primary-container` | `#FFDBD0` | `#FFF0E8` | `PrimaryContainer` |
| `--secondary` | `#5A822B` | `#4A7A20` | `Secondary` |
| `--tertiary` | `#8B5A2B` | `#7A4E22` | `Tertiary` |

New tokens:

| Token | Value | Compose | Usage |
|-------|-------|---------|-------|
| `--surface-warm` | `#FFF5ED` | `SurfaceWarm` | Warm card backgrounds |
| `--surface-container` | `#F8F0E8` | `SurfaceContainer` | Section backgrounds |
| `--gradient-hero` | `165deg, #E85D2C → #F4845F → #FFF0E8` | `HeroGradient` | Splash, headers |
| `--gradient-warm` | `135deg, #FFF8F2 → #FFF0E8` | `WarmGradient` | Subtle card bgs |

Dark theme equivalents must be updated in parallel.

### 2.2 Shadow Refinement — Warm Tones

| Level | CSS Value | Compose |
|-------|-----------|---------|
| sm | `0 1px 3px rgba(139,90,43,0.06), 0 1px 2px rgba(139,90,43,0.08)` | `shadow(2.dp)` |
| md | `0 2px 6px rgba(139,90,43,0.06), 0 4px 12px rgba(139,90,43,0.08)` | `shadow(4.dp)` |
| lg | `0 4px 12px rgba(139,90,43,0.06), 0 8px 24px rgba(139,90,43,0.10)` | `shadow(8.dp)` |

### 2.3 Card Components

| CSS Class | Background | Shadow | Border | Compose Equivalent |
|-----------|-----------|--------|--------|-------------------|
| `.card-elevated` | `--surface` | `--shadow-md` | none | `ElevatedCard(elevation = 4.dp)` |
| `.card-filled` | `--surface-warm` | none | none | `Card(colors = cardColors(containerColor = SurfaceWarm))` |
| `.card-outlined` | `--surface` | none | `1px --outline-variant` | `OutlinedCard()` |

All cards: `border-radius: --radius-md (16px)`, padding: `--card-padding (16px)`.

### 2.4 Standardized Components

**`.section-header`**
```css
/* Compose: Text(style = LabelLarge, color = OnSurfaceVariant) */
.section-header {
  font-size: 14px; font-weight: 500;
  text-transform: uppercase; letter-spacing: 0.5px;
  color: var(--on-surface-variant);
  padding: var(--sp-lg) 0 var(--sp-sm);
}
```

**`.empty-state`**
```css
/* Compose: Column(horizontalAlignment = CenterHorizontally) { Icon(...) Text(...) Text(...) Button(...) } */
.empty-state {
  display: flex; flex-direction: column; align-items: center;
  padding: var(--sp-xxl) var(--sp-lg); gap: var(--sp-md); text-align: center;
}
.empty-state .icon { font-size: 48px; }
.empty-state .title { font-size: 16px; font-weight: 500; color: var(--on-surface); }
.empty-state .subtitle { font-size: 14px; color: var(--on-surface-variant); }
```

**`.gradient-header`**
```css
/* Compose: Box(Modifier.background(Brush.linearGradient(HeroGradient))) */
.gradient-header {
  background: var(--gradient-hero);
  padding: var(--sp-lg); color: white;
}
```

**`.stagger-list > *`** — children animate with 50ms stagger delay per item.

### 2.5 Animation Library

| CSS Name | Duration | Easing | Compose Equivalent |
|----------|----------|--------|-------------------|
| `fadeInUp` | 300ms | ease-out | `fadeIn(tween(300)) + slideInVertically(initialOffsetY = { 12 })` |
| `scaleIn` | 200ms | ease-out | `scaleIn(tween(200), initialScale = 0.95f)` |
| `slideUp` | 300ms | ease-out | `slideInVertically(tween(300), initialOffsetY = { it })` |
| `warmPulse` | 600ms | ease-in-out | `animateFloatAsState(tween(600))` scale 1.0→1.02→1.0 |
| `shimmerLoad` | 1200ms | linear, infinite | `Brush.linearGradient` with `infiniteTransition` |
| `staggerChild` | 50ms delay/item | — | `animateItemPlacement()` + `LaunchedEffect` delay |

### 2.6 Dark Theme Updates

All new tokens need dark equivalents:

| Token | Light | Dark |
|-------|-------|------|
| `--surface-warm` | `#FFF5ED` | `#2D2520` |
| `--surface-container` | `#F8F0E8` | `#352E28` |
| `--primary` | `#E85D2C` | `#FFB59C` (keep inverse) |
| `--gradient-hero` (dark) | `165deg, #862200 → #5F1600 → #2D2520` | — |
| Shadow color | `rgba(139,90,43,...)` | `rgba(0,0,0,...)` (dark stays neutral) |

---

## Phase 3: Rollout Across 58 Remaining Screens

### Batch 3A: Main Screens (2 screens)
- `main-favorites.html` — card-elevated recipe cards, stagger, empty state
- `main-stats.html` — card-filled stat sections, gradient header, warm calendar

### Batch 3B: Auth + Onboarding (8 screens)
- `auth-phone.html` — warm gradient top section, button consistency
- `auth-otp.html` — warm-tinted OTP boxes, button alignment
- `onboarding-step1` through `step5` — gradient progress bar, card-filled family members, warm chips
- `onboarding-generation.html` — align animations to new library

### Batch 3C: Settings (15 screens)
- Apply `card-filled` to all list rows
- Add `section-header` class to all section labels
- **Redesign `settings-notification-settings.html`** — currently 59 LOC bare list, needs icons, descriptions, card grouping
- Standardize toggle/radio patterns
- Warm shadows on cards in family-members, household screens

### Batch 3D: Modals (21 screens)
- Consistent bottom-sheet styling with `slideUp` animation
- `scaleIn` for center dialogs
- Button hierarchy: primary right, secondary left
- Fix 7 bare-bones modals (delete-confirmation, invite-code, guest-duration, join-confirmation, leave-household, transfer-ownership, dark-mode)

### Batch 3E: Feature Screens (10 screens)
- Gradient headers where appropriate (achievements, leaderboard already have them)
- Empty states for notifications, pantry
- Card consistency with new system
- Stagger animations for lists

---

## Deliverable: COMPOSE-MAPPING.md

A file in the same directory (`docs/UI Designs/UI-UX-Material3/COMPOSE-MAPPING.md`) documenting:

```
## Card Components
| CSS Class       | Compose Implementation                                    |
|-----------------|-----------------------------------------------------------|
| .card-elevated  | ElevatedCard(elevation = CardDefaults.elevatedCardEle...) |
| .card-filled    | Card(colors = CardDefaults.cardColors(containerColor...)) |
| .card-outlined  | OutlinedCard(border = BorderStroke(1.dp, OutlineVariant)) |

## Colors
| CSS Variable        | Compose Value                | File       |
|---------------------|------------------------------|------------|
| --primary           | Color(0xFFE85D2C)            | Color.kt   |
| --surface-warm      | Color(0xFFFFF5ED)            | Color.kt   |
...
```

This file is the bridge between the HTML prototype and Android implementation.

---

## Implementation Order Summary

| Phase | What | Screens | Depends On |
|-------|------|---------|------------|
| 1 | Golden screens | 5 | Nothing |
| 2 | shared.css + shared.js extraction | 2 files | Phase 1 |
| 3A | Main screens | 2 | Phase 2 |
| 3B | Auth + Onboarding | 8 | Phase 2 |
| 3C | Settings | 15 | Phase 2 |
| 3D | Modals | 21 | Phase 2 |
| 3E | Features | 10 | Phase 2 |
| 4 | COMPOSE-MAPPING.md | 1 file | Phase 2 |

---

## Success Criteria

- All 63 screens at Tier 2+ quality (no Tier 3/4 screens remaining)
- Daily-use screens (home, grocery, chat, favorites) at Tier 1
- Dark mode works correctly with all new tokens
- Every CSS class has a documented Compose equivalent
- Warm-tinted shadows throughout, no cold black shadows
- Stagger animations on all list screens
- Empty states on all screens that can have empty content
