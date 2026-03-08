# Warm-Modern UI Enhancement — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Elevate all 63 HTML prototype screens to consistent "warm modern" quality — Indian kitchen warmth with Material You cleanliness.

**Architecture:** Phase 1 perfects 5 "golden" screens to establish the visual language. Phase 2 extracts those patterns into shared.css/shared.js as reusable components. Phase 3 rolls the system across remaining 58 screens in 5 batches. Phase 4 creates the Compose mapping reference.

**Tech Stack:** HTML5, CSS3 (custom properties, animations, color-mix), vanilla JS, Material 3 design tokens

**Design Doc:** `docs/plans/2026-03-08-warm-modern-ui-enhancement-design.md`

**Base Directory:** `docs/UI Designs/UI-UX-Material3/`

---

## Task 1: Update Color Tokens in shared.css

**Files:**
- Modify: `shared.css:13-68` (light theme tokens)
- Modify: `shared.css:73-117` (dark theme tokens)
- Modify: `shared.css:120-166` (theme-independent tokens)

**Step 1: Update light theme color tokens**

In `shared.css` lines 13-68, update these values:

```css
:root,
[data-theme="light"] {
  /* Primary - Saffron Orange (warmer, deeper) */
  --primary: #E85D2C;
  --on-primary: #FFFFFF;
  --primary-container: #FFF0E8;
  --on-primary-container: #3A0A00;

  /* Secondary - Curry Leaf Green (richer) */
  --secondary: #4A7A20;
  --on-secondary: #FFFFFF;
  --secondary-container: #C8F09A;
  --on-secondary-container: #0F2000;

  /* Tertiary - Clay Brown (earthier) */
  --tertiary: #7A4E22;
  --on-tertiary: #FFFFFF;
  --tertiary-container: #FFDDB8;
  --on-tertiary-container: #2E1500;

  /* Error — unchanged */
  --error: #BA1A1A;
  --on-error: #FFFFFF;
  --error-container: #FFDAD6;
  --on-error-container: #410002;

  /* Background & Surface — warmer base */
  --background: #FFF8F2;
  --on-background: #1C1B1F;
  --surface: #FFFFFF;
  --on-surface: #1C1B1F;
  --surface-variant: #F7EDE3;
  --on-surface-variant: #49454F;
  --surface-warm: #FFF5ED;
  --surface-container: #F8F0E8;

  /* Outline — unchanged */
  --outline: #7A757F;
  --outline-variant: #CAC4D0;

  /* Inverse */
  --inverse-surface: #313033;
  --inverse-on-surface: #F4EFF4;
  --inverse-primary: #FFB59C;

  /* Scrim — unchanged */
  --scrim: rgba(0,0,0,0.32);
  --scrim-heavy: rgba(0,0,0,0.6);

  /* Shadows — warm brown-tinted */
  --shadow-sm: 0 1px 3px rgba(139,90,43,0.06), 0 1px 2px rgba(139,90,43,0.08);
  --shadow-md: 0 2px 6px rgba(139,90,43,0.06), 0 4px 12px rgba(139,90,43,0.08);
  --shadow-lg: 0 4px 12px rgba(139,90,43,0.06), 0 8px 24px rgba(139,90,43,0.10);
  --shadow-xl: 0 8px 16px rgba(139,90,43,0.08), 0 16px 32px rgba(139,90,43,0.10), 0 32px 64px rgba(139,90,43,0.06);

  /* Gradients */
  --gradient-hero: linear-gradient(165deg, #E85D2C 0%, #F4845F 50%, #FFF0E8 100%);
  --gradient-warm: linear-gradient(135deg, #FFF8F2 0%, #FFF0E8 100%);
  --gradient-card: linear-gradient(180deg, #FFFFFF 0%, #FFF8F2 100%);

  /* Status bar */
  --status-bar-bg: #F0E8E0;
}
```

**Step 2: Update dark theme tokens**

In `shared.css` lines 73-117, update:

```css
[data-theme="dark"] {
  --primary: #FFB59C;
  --on-primary: #5F1600;
  --primary-container: #862200;
  --on-primary-container: #FFDBD0;

  --secondary: #A8D475;
  --on-secondary: #1A3700;
  --secondary-container: #2D5000;
  --on-secondary-container: #C8F09A;

  --tertiary: #E6BC8E;
  --on-tertiary: #432C0A;
  --tertiary-container: #5D4119;
  --on-tertiary-container: #FFDDB8;

  --error: #FFB4AB;
  --on-error: #690005;
  --error-container: #93000A;
  --on-error-container: #FFDAD6;

  --background: #1C1B1F;
  --on-background: #E6E1E5;
  --surface: #2B2930;
  --on-surface: #E6E1E5;
  --surface-variant: #49454F;
  --on-surface-variant: #CAC4D0;
  --surface-warm: #2D2520;
  --surface-container: #352E28;

  --outline: #938F99;
  --outline-variant: #49454F;

  --inverse-surface: #E6E1E5;
  --inverse-on-surface: #313033;
  --inverse-primary: #E85D2C;

  --scrim: rgba(0,0,0,0.5);
  --scrim-heavy: rgba(0,0,0,0.75);

  /* Dark shadows stay neutral */
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.2), 0 1px 4px rgba(0,0,0,0.25);
  --shadow-md: 0 2px 4px rgba(0,0,0,0.2), 0 4px 8px rgba(0,0,0,0.3);
  --shadow-lg: 0 4px 8px rgba(0,0,0,0.2), 0 8px 16px rgba(0,0,0,0.3), 0 16px 32px rgba(0,0,0,0.15);
  --shadow-xl: 0 8px 16px rgba(0,0,0,0.25), 0 16px 32px rgba(0,0,0,0.3), 0 32px 64px rgba(0,0,0,0.15);

  /* Gradients — dark variants */
  --gradient-hero: linear-gradient(165deg, #862200 0%, #5F1600 50%, #2D2520 100%);
  --gradient-warm: linear-gradient(135deg, #1C1B1F 0%, #2D2520 100%);
  --gradient-card: linear-gradient(180deg, #2B2930 0%, #1C1B1F 100%);

  --status-bar-bg: #141316;
}
```

**Step 3: Verify by opening index.html**

Open `index.html` in browser. Toggle dark mode. All existing screens should render with the warmer palette — no broken styles.

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/shared.css"
git commit -m "style(ui): update color tokens to warm-modern palette

Warmer background (#FFF8F2), deeper saffron primary (#E85D2C),
clay brown tertiary, warm-tinted shadows, new surface-warm/
surface-container/gradient tokens for both light and dark themes."
```

---

## Task 2: Add New Component Classes to shared.css

**Files:**
- Modify: `shared.css` — append new component classes after existing card section (~line 520)

**Step 1: Add card variants, section-header-label, empty-state, gradient-header**

Insert after the existing `.card-outlined` rule (line 520):

```css
/* ============================================
   Card Variants — Warm Modern
   Compose: ElevatedCard / Card / OutlinedCard
   ============================================ */
.card-filled {
  /* Compose: Card(colors = CardDefaults.cardColors(containerColor = SurfaceWarm)) */
  background: var(--surface-warm);
  border-radius: var(--radius-md);
  padding: var(--card-padding);
  transition: all var(--transition-normal);
}

.card-filled:hover {
  background: color-mix(in srgb, var(--surface-warm) 85%, var(--primary-container));
}

/* ============================================
   Section Header — Label Style
   Compose: Text(style = LabelLarge, color = OnSurfaceVariant)
   ============================================ */
.section-header-label {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  color: var(--on-surface-variant);
  padding: var(--sp-lg) 0 var(--sp-sm);
}

/* ============================================
   Empty State
   Compose: Column(horizontalAlignment = CenterHorizontally) { ... }
   ============================================ */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--sp-xxl) var(--sp-lg);
  gap: var(--sp-md);
  text-align: center;
  animation: fadeInUp 0.4s ease-out;
}

.empty-state .empty-icon {
  font-size: 48px;
  line-height: 1;
  opacity: 0.8;
}

.empty-state .empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--on-surface);
}

.empty-state .empty-subtitle {
  font-size: 14px;
  color: var(--on-surface-variant);
  max-width: 260px;
  line-height: 1.5;
}

/* ============================================
   Gradient Header
   Compose: Box(Modifier.background(Brush.linearGradient(HeroGradient)))
   ============================================ */
.gradient-header {
  background: var(--gradient-hero);
  padding: var(--sp-lg) var(--screen-padding);
  color: white;
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
}

.gradient-header .gradient-title {
  font-family: 'Outfit', sans-serif;
  font-size: 20px;
  font-weight: 600;
  color: white;
}

.gradient-header .gradient-subtitle {
  font-size: 14px;
  color: rgba(255,255,255,0.85);
  margin-top: 4px;
}

/* ============================================
   Warm Pulse Animation (for selected states)
   Compose: animateFloatAsState(tween(600)) scale 1.0→1.02→1.0
   ============================================ */
@keyframes warmPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.02); }
}

/* ============================================
   Shimmer Loading Skeleton
   Compose: Brush.linearGradient + infiniteTransition
   ============================================ */
.skeleton {
  background: linear-gradient(90deg, var(--surface-variant) 25%, var(--surface-warm) 50%, var(--surface-variant) 75%);
  background-size: 400% 100%;
  animation: shimmer 1.2s linear infinite;
  border-radius: var(--radius-sm);
}

.skeleton-text {
  height: 14px;
  width: 80%;
  margin-bottom: 8px;
}

.skeleton-text.short {
  width: 50%;
}

.skeleton-circle {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
}

.skeleton-card {
  height: 72px;
  margin-bottom: var(--sp-sm);
}

/* ============================================
   Stagger List — children animate with delay
   Compose: animateItemPlacement + LaunchedEffect delay
   ============================================ */
.stagger-list > * {
  animation: fadeInUp 0.35s ease-out backwards;
}

.stagger-list > *:nth-child(1) { animation-delay: 0s; }
.stagger-list > *:nth-child(2) { animation-delay: 0.04s; }
.stagger-list > *:nth-child(3) { animation-delay: 0.08s; }
.stagger-list > *:nth-child(4) { animation-delay: 0.12s; }
.stagger-list > *:nth-child(5) { animation-delay: 0.16s; }
.stagger-list > *:nth-child(6) { animation-delay: 0.20s; }
.stagger-list > *:nth-child(7) { animation-delay: 0.24s; }
.stagger-list > *:nth-child(8) { animation-delay: 0.28s; }
.stagger-list > *:nth-child(9) { animation-delay: 0.32s; }
.stagger-list > *:nth-child(10) { animation-delay: 0.36s; }
.stagger-list > *:nth-child(n+11) { animation-delay: 0.4s; }

/* ============================================
   Category Progress (for grocery, pantry)
   Compose: LinearProgressIndicator
   ============================================ */
.category-progress {
  display: flex;
  align-items: center;
  gap: var(--sp-sm);
  font-size: 12px;
  color: var(--on-surface-variant);
}

.category-progress .progress-bar {
  flex: 1;
  height: 3px;
}

.category-progress .progress-label {
  min-width: 32px;
  text-align: right;
  font-weight: 500;
}
```

**Step 2: Update existing `.meal-card .recipe-name` font-weight**

In `shared.css` around line 610-618, change:

```css
.meal-card .recipe-name {
  font-weight: 600;  /* was 500 — bolder for readability */
  font-size: 14px;
  color: var(--on-surface);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
```

**Step 3: Verify index.html still loads cleanly**

Open index.html — no visual changes expected yet (new classes not used by any screen).

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/shared.css"
git commit -m "style(ui): add warm-modern component classes

card-filled, section-header-label, empty-state, gradient-header,
skeleton loading, stagger-list, category-progress, warmPulse animation.
Each class includes Compose mapping comment."
```

---

## Task 3: Golden Screen — auth-splash.html

**Files:**
- Modify: `auth-splash.html`

**Step 1: Rewrite auth-splash.html with warm-modern polish**

Replace the full file content with an enhanced version that:
- Uses `--gradient-hero` token instead of hardcoded gradient
- Adds logo `bounceIn` animation (already exists in shared.css)
- Adds tagline `fadeInUp` with stagger delay
- Adds subtle decorative elements (food emoji scattered, low opacity)
- Uses `btn-filled btn-large` class properly
- Adds a secondary "Already have an account?" text link below the button

Key changes to the `<style>` block:
```css
.splash-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--gradient-hero);
  padding: var(--sp-lg);
  text-align: center;
  gap: var(--sp-lg);
  position: relative;
  overflow: hidden;
}

/* Decorative floating food emojis */
.splash-decor {
  position: absolute;
  font-size: 32px;
  opacity: 0.12;
  animation: floatUp 6s ease-in-out infinite;
}

@keyframes floatUp {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(10deg); }
}

.splash-logo {
  font-family: 'Outfit', sans-serif;
  font-size: 52px;
  font-weight: 700;
  color: white;
  text-shadow: 0 2px 16px rgba(0,0,0,0.15);
  line-height: 1.1;
  animation: bounceIn 0.8s ease-out;
}

.splash-hindi {
  font-size: 22px;
  font-weight: 500;
  color: rgba(255,255,255,0.9);
  margin-top: -4px;
  animation: fadeInUp 0.5s ease-out 0.3s backwards;
}

.splash-tagline {
  font-size: 16px;
  color: rgba(255,255,255,0.85);
  line-height: 1.5;
  max-width: 260px;
  animation: fadeInUp 0.5s ease-out 0.5s backwards;
}

.splash-bottom {
  padding: var(--sp-lg) var(--sp-lg) var(--sp-xxl);
  background: linear-gradient(to top, rgba(255,240,232,0.95) 0%, transparent 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--sp-md);
  animation: fadeInUp 0.5s ease-out 0.7s backwards;
}

.splash-bottom .btn-filled {
  background: white;
  color: var(--primary);
  font-weight: 600;
  box-shadow: var(--shadow-lg);
  width: 100%;
}

.splash-secondary-link {
  font-size: 14px;
  color: rgba(255,255,255,0.7);
  text-decoration: none;
}
```

HTML body adds decorative emojis:
```html
<div class="splash-body">
  <!-- Decorative food emojis -->
  <span class="splash-decor" style="top:10%;left:10%;animation-delay:0s;">🍛</span>
  <span class="splash-decor" style="top:20%;right:15%;animation-delay:1s;">🫖</span>
  <span class="splash-decor" style="bottom:25%;left:20%;animation-delay:2s;">🥘</span>
  <span class="splash-decor" style="bottom:15%;right:10%;animation-delay:3s;">🍚</span>

  <div>
    <div class="splash-logo">RasoiAI</div>
    <div class="splash-hindi">रसोई AI</div>
  </div>

  <div class="splash-spinner-wrap">
    <div class="progress-circular"></div>
  </div>

  <p class="splash-tagline">Your AI-powered Indian kitchen assistant</p>
</div>
```

**Step 2: Open auth-splash.html in browser**

Verify: Logo bounces in, tagline fades up with stagger, food emojis float subtly, warm gradient fills screen, dark mode toggle works.

**Step 3: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/auth-splash.html"
git commit -m "style(ui): enhance splash screen with warm-modern polish

bounceIn logo, staggered fadeInUp text, floating food emoji decor,
gradient-hero token, warm shadow on CTA button."
```

---

## Task 4: Golden Screen — main-home.html

**Files:**
- Modify: `main-home.html`

**Step 1: Enhance home screen with warm-modern treatment**

Key upgrades:
1. **Day selector** — wrap in a warm gradient band with `--surface-warm` background, selected day uses `warmPulse` animation
2. **Meal cards** — use `card-elevated` variant, add warm `border-left` accent on hover (already exists but enhance), food emoji image bg uses `--gradient-warm`
3. **Festival banner** — add warm container background (`--surface-warm`), subtle left border accent with `--tertiary`
4. **Meal section headers** — use `section-header-label` class for "BREAKFAST", "LUNCH" etc.
5. **Add `stagger-list`** class to meals container so cards animate in with stagger

Changes to the `<script>` section `renderMeals()`:
- Add `class="stagger-list"` to container div
- Wrap meal-section-header text in `section-header-label` styling
- Change recipe-image gradient to use `var(--gradient-warm)` background

Changes to day-selector area:
- Wrap in `<div style="background:var(--surface-warm);padding:var(--sp-sm) 0;margin:0 calc(-1 * var(--screen-padding));padding:var(--sp-sm) var(--screen-padding);">`
- Selected day chip gets `animation: warmPulse 0.6s ease-in-out`

Add `<style>` block for home-specific enhancements:
```css
.day-selector-wrap {
  background: var(--surface-warm);
  margin: 0 calc(-1 * var(--screen-padding));
  padding: var(--sp-sm) var(--screen-padding);
  margin-bottom: var(--sp-md);
}

.festival-banner {
  background: var(--surface-warm);
  border-left: 3px solid var(--tertiary);
}

.meal-section-header .meal-type {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  color: var(--on-surface-variant);
}

.meal-card {
  box-shadow: var(--shadow-md);
}

.meal-card .recipe-image {
  background: var(--gradient-warm);
}
```

**Step 2: Open main-home.html in browser**

Verify: Day selector has warm band, meal cards stagger in, festival banner has warm tint and brown left border, section headers are label-style uppercase, dark mode works.

**Step 3: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/main-home.html"
git commit -m "style(ui): enhance home screen with warm-modern design

Warm day-selector band, elevated meal cards with stagger animation,
label-style section headers, warm festival banner with tertiary accent."
```

---

## Task 5: Golden Screen — main-grocery.html

**Files:**
- Modify: `main-grocery.html`

**Step 1: Enhance grocery screen**

Currently 101 lines with zero inline styles. Needs:
1. **Category cards** — wrap each category in `card-filled` with warm background
2. **Category progress** — add `category-progress` bar showing checked/total items
3. **Stagger animation** — wrap categories in `stagger-list`
4. **Summary header** — warm gradient header with total items count, checked count
5. **Empty state** — add `.empty-state` component when no grocery items
6. **Checkbox styling** — custom styled checkboxes with `--primary` accent

Add `<style>` block:
```css
.grocery-summary {
  background: var(--gradient-warm);
  margin: 0 calc(-1 * var(--screen-padding));
  padding: var(--sp-md) var(--screen-padding);
  display: flex;
  justify-content: space-around;
  margin-bottom: var(--sp-md);
}

.grocery-stat {
  text-align: center;
}

.grocery-stat .stat-value {
  font-family: 'Outfit', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--primary);
}

.grocery-stat .stat-label {
  font-size: 12px;
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.grocery-category {
  background: var(--surface-warm);
  border-radius: var(--radius-md);
  margin-bottom: var(--sp-md);
  overflow: hidden;
}

.grocery-category-header {
  display: flex;
  align-items: center;
  gap: var(--sp-sm);
  padding: var(--sp-md);
  cursor: pointer;
  font-weight: 600;
  transition: background var(--transition-fast);
}

.grocery-category-header:hover {
  background: color-mix(in srgb, var(--surface-warm) 85%, var(--primary-container));
}

.grocery-item {
  display: flex;
  align-items: center;
  gap: var(--sp-md);
  padding: var(--sp-sm) var(--sp-md);
  border-top: 1px solid var(--outline-variant);
  transition: background var(--transition-fast);
}

.grocery-item:hover {
  background: var(--surface-variant);
}

.grocery-item.checked .item-name {
  text-decoration: line-through;
  opacity: 0.6;
}
```

Update the JS `renderGrocery()` function to use these new classes and include:
- `<div class="stagger-list">` wrapper around categories
- `<div class="category-progress">` inside each category header
- `<div class="empty-state">` when no grocery data

**Step 2: Open main-grocery.html in browser**

Verify: Warm summary header with stats, categories in warm cards with progress bars, stagger animation, dark mode works.

**Step 3: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/main-grocery.html"
git commit -m "style(ui): enhance grocery screen with warm-modern design

Warm gradient summary header with stats, card-filled categories with
progress bars, stagger animation, styled checkboxes, empty state."
```

---

## Task 6: Golden Screen — main-chat.html

**Files:**
- Modify: `main-chat.html`

**Step 1: Enhance chat screen**

Currently 129 lines with zero inline styles. Needs:
1. **User message bubbles** — warm tint background (`--surface-warm`), right-aligned
2. **AI message bubbles** — white surface with subtle left border accent (`--primary`), subtle `--shadow-sm`
3. **Typing indicator** — 3 dots with pulse animation
4. **Quick-chip bar** — redesign with warm pill styling, stagger animation
5. **Input bar** — warm background, elevated with `--shadow-sm`, send button uses `--primary`
6. **Message timestamps** — consistent `--on-surface-variant` color, `body-small` size
7. **Welcome state** — warm gradient welcome message when chat is empty

Add `<style>` block:
```css
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--sp-md);
  display: flex;
  flex-direction: column;
  gap: var(--sp-sm);
}

.message {
  max-width: 85%;
  animation: fadeInUp 0.3s ease-out;
}

.message-user {
  align-self: flex-end;
}

.message-user .bubble {
  background: var(--surface-warm);
  border-radius: var(--radius-md) var(--radius-md) var(--radius-xs) var(--radius-md);
  padding: var(--sp-sm) var(--sp-md);
  color: var(--on-surface);
}

.message-ai {
  align-self: flex-start;
}

.message-ai .bubble {
  background: var(--surface);
  border-radius: var(--radius-md) var(--radius-md) var(--radius-md) var(--radius-xs);
  padding: var(--sp-sm) var(--sp-md);
  color: var(--on-surface);
  border-left: 3px solid var(--primary);
  box-shadow: var(--shadow-sm);
}

.message .timestamp {
  font-size: 11px;
  color: var(--on-surface-variant);
  margin-top: 4px;
  padding: 0 var(--sp-xs);
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: var(--sp-sm) var(--sp-md);
  align-self: flex-start;
}

.typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--on-surface-variant);
  animation: pulse 1.2s ease-in-out infinite;
}

.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }

.chat-quick-chips {
  display: flex;
  gap: var(--sp-sm);
  padding: var(--sp-sm) var(--sp-md);
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.chat-quick-chips .chip {
  background: var(--surface-warm);
  border-color: var(--outline-variant);
  flex-shrink: 0;
}

.chat-input-bar {
  display: flex;
  align-items: center;
  gap: var(--sp-sm);
  padding: var(--sp-sm) var(--sp-md);
  background: var(--surface);
  border-top: 1px solid var(--outline-variant);
  box-shadow: 0 -2px 8px rgba(139,90,43,0.04);
}

.chat-input-bar input {
  flex: 1;
  height: 40px;
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-full);
  padding: 0 var(--sp-md);
  font-size: 14px;
  background: var(--surface-warm);
  color: var(--on-surface);
  outline: none;
  transition: border-color var(--transition-fast);
}

.chat-input-bar input:focus {
  border-color: var(--primary);
}

.chat-input-bar .send-btn {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  background: var(--primary);
  color: var(--on-primary);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: all var(--transition-fast);
}

.chat-input-bar .send-btn:hover {
  transform: scale(1.08);
  box-shadow: var(--shadow-md);
}
```

**Step 2: Open main-chat.html in browser**

Verify: User bubbles warm-tinted right, AI bubbles white with saffron left border, typing dots pulse, quick chips have warm pills, input bar is elevated, dark mode works.

**Step 3: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/main-chat.html"
git commit -m "style(ui): enhance chat screen with warm-modern design

Warm user bubbles, AI bubbles with primary border accent, typing
indicator dots, redesigned quick-chips and input bar."
```

---

## Task 7: Golden Screen — detail-recipe.html (minor alignment)

**Files:**
- Modify: `detail-recipe.html`

**Step 1: Align detail-recipe to new tokens**

This screen is already Tier 1. Only update:
1. Hardcoded gradient colors → use `var(--gradient-hero)` token
2. Hardcoded shadow values → use `var(--shadow-*)` tokens
3. Verify `--primary` references update naturally from token change
4. Add `stagger-list` to ingredients list

Find and replace in the `<style>` block:
- Any `#FF6838` → should already flow from `var(--primary)`
- Any `linear-gradient(...)` in the hero section → `var(--gradient-hero)`
- Any `rgba(0,0,0,...)` in box-shadows → verify they use `var(--shadow-*)` tokens

**Step 2: Open detail-recipe.html in browser**

Verify: Hero gradient uses new warmer saffron, shadows feel warmer, no visual regressions, dark mode works.

**Step 3: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/detail-recipe.html"
git commit -m "style(ui): align recipe detail to warm-modern tokens

Replace hardcoded gradients/shadows with CSS variable tokens,
add stagger-list to ingredients."
```

---

## Task 8: Batch 3A — Main Screens (favorites, stats)

**Files:**
- Modify: `main-favorites.html`
- Modify: `main-stats.html`

**Step 1: Enhance main-favorites.html**

Add `<style>` block with:
- Recipe grid cards use `card-elevated` styling with warm shadow
- Filter chips use warm tint when inactive
- Empty state uses `.empty-state` component with heart emoji
- Stagger animation on recipe grid
- Favorite heart icon uses `warmPulse` on toggle

**Step 2: Enhance main-stats.html**

Add `<style>` block with:
- Streak card uses `gradient-header` with warm tones
- Calendar grid cells use `--surface-warm` for days with meals cooked
- Achievement cards use `card-filled` warm variant
- Cuisine progress bars use `--primary` and `--secondary` fills
- Stats numbers use `font-family: 'Outfit'` for display emphasis
- Stagger animation on stat sections

**Step 3: Verify both in browser, dark mode**

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/main-favorites.html" "docs/UI Designs/UI-UX-Material3/main-stats.html"
git commit -m "style(ui): enhance favorites and stats with warm-modern design

Favorites: elevated recipe cards, warm filter chips, heart pulse, empty state.
Stats: gradient streak header, warm calendar cells, Outfit stat numbers."
```

---

## Task 9: Batch 3B — Auth + Onboarding (5 screens)

**Files:**
- Modify: `auth-phone.html`
- Modify: `auth-otp.html`
- Modify: `onboarding-step1-household.html`
- Modify: `onboarding-step2-dietary.html`
- Modify: `onboarding-step3-cuisine.html`
- Modify: `onboarding-step4-ingredients.html`
- Modify: `onboarding-step5-cooking-time.html`
- Modify: `onboarding-generation.html`

**Step 1: Enhance auth-phone.html**

- Add warm gradient top section (smaller than splash — just phone illustration area)
- Warm-tinted input field focus state
- Button uses updated `--primary` token
- "Continue with phone" button uses `btn-filled btn-large`
- Add fadeInUp animation to form elements

**Step 2: Enhance auth-otp.html**

- OTP input boxes use `--surface-warm` background
- Focus state border uses `--primary`
- Resend timer uses `--on-surface-variant`
- Success checkmark uses `--secondary` (green)
- Stagger animation on OTP boxes

**Step 3: Enhance onboarding steps 1-5**

Apply consistent pattern across all 5 steps:
- Progress bar at top uses `--primary` fill on `--surface-warm` track
- Step indicator uses `--primary` for active, `--outline-variant` for inactive
- Selection cards (cuisine, spice) use warm hover states
- Chip selections use `--secondary-container` for selected state
- Family member cards (step 1) use `card-filled` with avatars
- Day toggle chips (step 5) use `--surface-warm` base, `--primary` when active
- All steps get consistent `stagger-list` on their option groups

**Step 4: Enhance onboarding-generation.html**

- Already has good animations — align gradient to `--gradient-hero`
- Step icons use warm colors (existing bounceIn keeps)
- Success state background uses `--surface-warm`

**Step 5: Verify all 8 screens, dark mode toggle on each**

**Step 6: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/auth-phone.html" \
  "docs/UI Designs/UI-UX-Material3/auth-otp.html" \
  "docs/UI Designs/UI-UX-Material3/onboarding-step1-household.html" \
  "docs/UI Designs/UI-UX-Material3/onboarding-step2-dietary.html" \
  "docs/UI Designs/UI-UX-Material3/onboarding-step3-cuisine.html" \
  "docs/UI Designs/UI-UX-Material3/onboarding-step4-ingredients.html" \
  "docs/UI Designs/UI-UX-Material3/onboarding-step5-cooking-time.html" \
  "docs/UI Designs/UI-UX-Material3/onboarding-generation.html"
git commit -m "style(ui): enhance auth and onboarding with warm-modern design

Auth: warm gradient header, warm input fields, stagger animations.
Onboarding: consistent progress bars, warm selection cards, card-filled
family members, stagger on all option groups."
```

---

## Task 10: Batch 3C — Settings (15 screens)

**Files:**
- Modify: `settings-main.html`
- Modify: `settings-dietary-restrictions.html`
- Modify: `settings-disliked-ingredients.html`
- Modify: `settings-cuisine-preferences.html`
- Modify: `settings-spice-level.html`
- Modify: `settings-cooking-time.html`
- Modify: `settings-family-members.html`
- Modify: `settings-notification-settings.html`
- Modify: `settings-units.html`
- Modify: `settings-edit-profile.html`
- Modify: `settings-connected-accounts.html`
- Modify: `settings-household.html`
- Modify: `settings-household-members.html`
- Modify: `settings-household-member-detail.html`
- Modify: `settings-join-household.html`

**Step 1: Apply consistent warm-modern pattern to all settings screens**

Shared pattern for ALL settings screens:
- Use `section-header-label` class for section dividers
- List items use `card-filled` grouping (group related items in warm container)
- All toggles/switches use `--primary` accent color
- All screens get `stagger-list` on main content list
- Back button and title use consistent `top-app-bar` pattern

**Step 2: Full redesign of settings-notification-settings.html**

Currently 59 LOC bare list. Redesign to:
- Add emoji icon per notification type (🔔 Meal Reminders, 🏆 Achievements, 📊 Weekly Summary, 📢 Promotions)
- Add descriptive subtitle for each toggle
- Group toggles in `card-filled` container
- Add `section-header-label` ("Notification Preferences")
- Match visual richness of settings-spice-level.html

**Step 3: Polish settings-units.html**

- Add section-header-labels ("Measurement System", "Temperature", "Volume")
- Radio options in `card-filled` containers with icons
- Add descriptive subtitles (e.g., "Used in recipes and grocery lists")

**Step 4: Polish settings-edit-profile.html**

- Avatar section with warm gradient background circle
- Form fields use `--surface-warm` background
- Save button uses updated `btn-filled btn-large`

**Step 5: Fix settings-household-members.html orphaned animation**

- Add proper `stagger-list` class instead of orphaned `animation-delay` inline style
- Remove the broken inline `animation-delay` from JavaScript

**Step 6: Verify all 15 screens, toggle dark mode on each**

**Step 7: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/settings-"*
git commit -m "style(ui): enhance all 15 settings screens with warm-modern design

Consistent section-header-label, card-filled grouping, stagger animations.
Full redesign of notification-settings (icons, descriptions, card groups).
Polish units, edit-profile, fix household-members orphaned animation."
```

---

## Task 11: Batch 3D — Modals (21 screens)

**Files:**
- Modify: All 21 `modal-*.html` files

**Step 1: Apply consistent bottom-sheet warm styling**

For all bottom-sheet modals:
- Sheet background uses `var(--surface)` (keep crisp white for contrast)
- Handle bar uses `var(--outline-variant)` (already correct)
- Title uses `font-family: 'Outfit'` if not already
- Add consistent `slideUp` animation (already in shared.css)
- Button row: primary button right, text/outlined left

For all center dialogs:
- Dialog uses `var(--surface)` with `var(--shadow-xl)`
- Add `scaleIn` animation (already in shared.css)
- Consistent padding and border-radius

**Step 2: Enhance 7 bare-bones modals**

These need the most work:

**modal-delete-confirmation.html:**
- Warning icon in `--error-container` circle
- Descriptive text with item name highlighted
- "Delete" button uses `--error` background
- "Cancel" button uses `btn-text`

**modal-invite-code-share.html:**
- Invite code in large monospace font with `card-filled` background
- Copy button with success feedback (icon changes to ✓)
- QR code placeholder with warm border
- WhatsApp share button with green accent

**modal-guest-duration.html:**
- Guest name and emoji header
- Date inputs in `card-filled` containers
- Clear visual of start→end date range

**modal-join-confirmation.html:**
- Household name in `card-filled` card with emoji
- Checklist of what joining means (styled list with check icons)
- "Join" button prominent, "Cancel" secondary

**modal-leave-household.html:**
- Warning banner with `--error-container` background
- Impact list with red dot bullets
- "Leave" button uses `--error` background
- "Stay" button uses `btn-filled` primary

**modal-transfer-ownership.html:**
- Member cards as radio selections with avatars
- Selected member highlighted with `--primary-container` background
- "Transfer" button with confirmation intent

**modal-dark-mode.html:**
- Three options as visual cards (☀️ Light / 🌙 Dark / 🔄 System)
- Selected option uses `--primary-container` background with check
- Preview strip showing how colors look in each mode

**Step 3: Polish remaining 14 modals**

Apply warm-modern touches:
- `--surface-warm` for input field backgrounds
- Warm shadows on action buttons
- Consistent typography hierarchy
- Form fields aligned with new tokens

**Step 4: Verify all 21 modals, dark mode**

**Step 5: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/modal-"*
git commit -m "style(ui): enhance all 21 modals with warm-modern design

Consistent bottom-sheet/dialog styling. Full redesign of 7 bare-bones
modals (delete, invite-code, guest-duration, join, leave, transfer,
dark-mode). Warm tokens on remaining 14 modals."
```

---

## Task 12: Batch 3E — Feature Screens (10 screens)

**Files:**
- Modify: `feature-pantry.html`
- Modify: `feature-recipe-rules.html`
- Modify: `feature-notifications.html`
- Modify: `feature-achievements.html`
- Modify: `feature-leaderboard.html`
- Modify: `feature-accessibility-mode.html`
- Modify: `feature-dietary-profiles.html`
- Modify: `feature-family-communication.html`
- Modify: `feature-data-lifecycle.html`
- Modify: `feature-portion-customization.html`

**Step 1: Align Tier 1 feature screens to new tokens**

feature-pantry, feature-recipe-rules, feature-achievements, feature-leaderboard, feature-notifications are already high quality. For these:
- Replace hardcoded colors with CSS variable tokens
- Update any `rgba(0,0,0,...)` shadows to warm variants or `var(--shadow-*)` tokens
- Add `stagger-list` to list sections if not present
- Ensure dark mode tokens flow correctly

**Step 2: Enhance 5 Tier 2 feature screens**

**feature-accessibility-mode.html:**
- Mode option cards use `card-filled` with warm tint
- Selected mode uses `--primary-container` highlight
- Add preview area with warm background

**feature-dietary-profiles.html:**
- Profile cards use `card-filled`
- Active profile highlighted with `--primary` border
- Add stagger animation

**feature-family-communication.html:**
- Timeline dots use `--primary` for recent, `--outline-variant` for older
- Activity cards use `card-filled`
- Tab bar aligns to shared.css tab-bar component

**feature-data-lifecycle.html:**
- Info cards use `card-filled` with `--secondary-container` for safe items
- Warning cards use `--error-container` for deletion zones
- Button hierarchy: delete = error colored, export = primary

**feature-portion-customization.html:**
- Member cards use `card-filled`
- Portion size segment control uses `--primary` for active segment
- Add stagger animation on member list

**Step 3: Verify all 10 feature screens, dark mode**

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/feature-"*
git commit -m "style(ui): enhance all 10 feature screens with warm-modern design

Align Tier 1 screens to new tokens. Enhance Tier 2 screens with
card-filled containers, warm highlights, stagger animations."
```

---

## Task 13: Update shared.js — Dark Theme Defaults

**Files:**
- Modify: `shared.js`

**Step 1: Update SHARMA_DEFAULTS dark theme handling**

Ensure `applyTheme()` and `toggleDarkMode()` work with the updated color tokens. No functional changes needed — tokens update via CSS variables automatically.

**Step 2: Add helper function for warm gradient inline styles**

Add to shared.js:
```javascript
// --- Warm Modern Helpers ---
function getWarmGradient() {
  return 'var(--gradient-warm)';
}

function getHeroGradient() {
  return 'var(--gradient-hero)';
}
```

**Step 3: Verify dark mode toggle on index.html**

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/shared.js"
git commit -m "style(ui): add warm-modern helper functions to shared.js"
```

---

## Task 14: Create COMPOSE-MAPPING.md

**Files:**
- Create: `docs/UI Designs/UI-UX-Material3/COMPOSE-MAPPING.md`

**Step 1: Write the Compose mapping reference**

```markdown
# CSS → Jetpack Compose Mapping Reference

This file maps every CSS class and token in the UI prototype to its
Jetpack Compose equivalent for Android implementation.

## Color Tokens

| CSS Variable | Value (Light) | Value (Dark) | Compose Property |
|---|---|---|---|
| `--primary` | `#E85D2C` | `#FFB59C` | `MaterialTheme.colorScheme.primary` |
| `--on-primary` | `#FFFFFF` | `#5F1600` | `MaterialTheme.colorScheme.onPrimary` |
| `--primary-container` | `#FFF0E8` | `#862200` | `MaterialTheme.colorScheme.primaryContainer` |
| `--secondary` | `#4A7A20` | `#A8D475` | `MaterialTheme.colorScheme.secondary` |
| `--tertiary` | `#7A4E22` | `#E6BC8E` | `MaterialTheme.colorScheme.tertiary` |
| `--background` | `#FFF8F2` | `#1C1B1F` | `MaterialTheme.colorScheme.background` |
| `--surface` | `#FFFFFF` | `#2B2930` | `MaterialTheme.colorScheme.surface` |
| `--surface-variant` | `#F7EDE3` | `#49454F` | `MaterialTheme.colorScheme.surfaceVariant` |
| `--surface-warm` | `#FFF5ED` | `#2D2520` | Custom: `SurfaceWarm` in Color.kt |
| `--surface-container` | `#F8F0E8` | `#352E28` | Custom: `SurfaceContainer` in Color.kt |
| `--error` | `#BA1A1A` | `#FFB4AB` | `MaterialTheme.colorScheme.error` |
| `--outline` | `#7A757F` | `#938F99` | `MaterialTheme.colorScheme.outline` |
| `--outline-variant` | `#CAC4D0` | `#49454F` | `MaterialTheme.colorScheme.outlineVariant` |

## Gradient Tokens

| CSS Variable | Compose Implementation |
|---|---|
| `--gradient-hero` | `Brush.linearGradient(listOf(Primary, Color(0xFFF4845F), PrimaryContainer), start=Offset(0f,0f), end=Offset(width,height))` |
| `--gradient-warm` | `Brush.linearGradient(listOf(Background, PrimaryContainer))` |
| `--gradient-card` | `Brush.verticalGradient(listOf(Surface, Background))` |

## Spacing Tokens

| CSS Variable | Value | Compose |
|---|---|---|
| `--sp-xs` | `4px` | `4.dp` |
| `--sp-sm` | `8px` | `8.dp` |
| `--sp-md` | `16px` | `16.dp` |
| `--sp-lg` | `24px` | `24.dp` |
| `--sp-xl` | `32px` | `32.dp` |
| `--sp-xxl` | `48px` | `48.dp` |
| `--screen-padding` | `16px` | `16.dp` (horizontal padding) |
| `--card-padding` | `16px` | `16.dp` |

## Shape Tokens

| CSS Variable | Value | Compose |
|---|---|---|
| `--radius-xs` | `4px` | `RoundedCornerShape(4.dp)` |
| `--radius-sm` | `8px` | `RoundedCornerShape(8.dp)` |
| `--radius-md` | `16px` | `RoundedCornerShape(16.dp)` |
| `--radius-lg` | `24px` | `RoundedCornerShape(24.dp)` |
| `--radius-xl` | `32px` | `RoundedCornerShape(32.dp)` |
| `--radius-full` | `9999px` | `CircleShape` |

## Card Components

| CSS Class | Compose Implementation |
|---|---|
| `.card-elevated` | `ElevatedCard(elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp), shape = RoundedCornerShape(16.dp))` |
| `.card-filled` | `Card(colors = CardDefaults.cardColors(containerColor = SurfaceWarm), shape = RoundedCornerShape(16.dp))` |
| `.card-outlined` | `OutlinedCard(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp))` |

## Button Components

| CSS Class | Compose Implementation |
|---|---|
| `.btn-filled` | `Button(onClick, shape = CircleShape) { Text(...) }` |
| `.btn-filled.btn-large` | `Button(onClick, modifier = Modifier.height(48.dp).fillMaxWidth(), shape = CircleShape)` |
| `.btn-outlined` | `OutlinedButton(onClick, shape = CircleShape)` |
| `.btn-text` | `TextButton(onClick)` |
| `.btn-tonal` | `FilledTonalButton(onClick, shape = CircleShape)` |
| `.icon-btn` | `IconButton(onClick) { Icon(...) }` |
| `.fab` | `FloatingActionButton(onClick, containerColor = PrimaryContainer)` |

## Typography

| CSS Class | Compose Style |
|---|---|
| `.display-large` | `MaterialTheme.typography.displayLarge` (Outfit 57/700) |
| `.headline-large` | `MaterialTheme.typography.headlineLarge` (Outfit 32/600) |
| `.title-large` | `MaterialTheme.typography.titleLarge` (Outfit 22/500) |
| `.title-medium` | `MaterialTheme.typography.titleMedium` (DM Sans 16/500) |
| `.body-large` | `MaterialTheme.typography.bodyLarge` (DM Sans 16/400) |
| `.body-medium` | `MaterialTheme.typography.bodyMedium` (DM Sans 14/400) |
| `.label-large` | `MaterialTheme.typography.labelLarge` (DM Sans 14/500) |
| `.section-header-label` | `Text(style = LabelSmall.copy(fontWeight = W600, letterSpacing = 0.8.sp), color = OnSurfaceVariant, text = text.uppercase())` |

## Animation Mapping

| CSS Animation | Compose Equivalent |
|---|---|
| `fadeInUp` (300ms) | `fadeIn(tween(300)) + slideInVertically(initialOffsetY = { 12 })` |
| `scaleIn` (200ms) | `scaleIn(tween(200), initialScale = 0.95f)` |
| `slideUp` (300ms) | `slideInVertically(tween(300), initialOffsetY = { it })` |
| `bounceIn` (500ms) | `scaleIn(spring(dampingRatio = 0.5f, stiffness = 300f))` |
| `warmPulse` (600ms) | `val scale by animateFloatAsState(if(selected) 1.02f else 1f, tween(600))` |
| `shimmer` (1200ms) | `val transition = rememberInfiniteTransition(); val offset by transition.animateFloat(...)` |
| `stagger-list` (50ms/item) | `LazyColumn { itemsIndexed { i, item -> AnimatedVisibility(visibleState, enter = fadeIn(tween(350, delayMillis = i * 40))) } }` |

## Reusable Components

| CSS Pattern | Compose Composable |
|---|---|
| `.empty-state` | `EmptyState(icon: String, title: String, subtitle: String, action: @Composable (() -> Unit)? = null)` |
| `.gradient-header` | `GradientHeader(title: String, subtitle: String? = null)` using `Box(Modifier.background(HeroGradient))` |
| `.skeleton` / `.skeleton-text` | `ShimmerPlaceholder(modifier)` using animated gradient brush |
| `.category-progress` | `CategoryProgress(checked: Int, total: Int)` using `LinearProgressIndicator` |
| `.section-header-label` | `SectionHeaderLabel(text: String)` |

## Shadow Mapping

| CSS | Compose | Notes |
|---|---|---|
| `--shadow-sm` | `Modifier.shadow(2.dp, shape)` | Light theme uses warm `rgba(139,90,43,...)` — Compose shadow color is system-controlled, so warm tint comes from surface color, not shadow |
| `--shadow-md` | `Modifier.shadow(4.dp, shape)` | |
| `--shadow-lg` | `Modifier.shadow(8.dp, shape)` | |
| `--shadow-xl` | `Modifier.shadow(16.dp, shape)` | Used for phone frame, FAB, dialogs |

**Note:** Android shadow colors aren't directly controllable in Compose (they use system ambient/key light). The warm shadow effect in CSS is purely cosmetic — on Android, warm tones come from the surface colors and elevation tinting system.
```

**Step 2: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/COMPOSE-MAPPING.md"
git commit -m "docs(ui): create CSS-to-Compose mapping reference

Complete mapping of all color tokens, gradients, spacing, shapes,
card/button/typography components, animations, and reusable patterns
from HTML prototype to Jetpack Compose equivalents."
```

---

## Task 15: Update index.html with warm-modern branding

**Files:**
- Modify: `index.html`

**Step 1: Update index.html header**

- Update screen count: "63 Screens" (verify actual count)
- Add subtitle: "Warm Modern Design System"
- Update gradient to use `--gradient-hero` token
- Add "View Compose Mapping" button that links to `COMPOSE-MAPPING.md`

**Step 2: Verify index renders correctly**

**Step 3: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/index.html"
git commit -m "style(ui): update index page with warm-modern branding"
```

---

## Task 16: Final Visual QA Pass

**Files:**
- All 63 screens + shared.css + shared.js

**Step 1: Open every screen category in browser and check**

Checklist per screen:
- [ ] Warm background tone visible (`#FFF8F2`)
- [ ] Cards use consistent shadow (warm-tinted in light, neutral in dark)
- [ ] Section headers use `section-header-label` or consistent `section-header`
- [ ] Lists have stagger animation
- [ ] Dark mode toggle works — no broken colors, all tokens applied
- [ ] No hardcoded `#FF6838` remaining (should be `--primary` or `#E85D2C`)
- [ ] No hardcoded `rgba(0,0,0,...)` in light theme shadows
- [ ] Buttons follow consistent hierarchy (filled/outlined/text)

**Step 2: Fix any remaining inconsistencies found during QA**

**Step 3: Final commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/"
git commit -m "style(ui): final QA pass — warm-modern consistency across all 63 screens"
```

---

## Summary

| Task | What | Files | Phase |
|------|------|-------|-------|
| 1 | Color token updates | shared.css | 2 (System) |
| 2 | New component classes | shared.css | 2 (System) |
| 3 | Golden: auth-splash | 1 file | 1 (Golden) |
| 4 | Golden: main-home | 1 file | 1 (Golden) |
| 5 | Golden: main-grocery | 1 file | 1 (Golden) |
| 6 | Golden: main-chat | 1 file | 1 (Golden) |
| 7 | Golden: detail-recipe | 1 file | 1 (Golden) |
| 8 | Batch 3A: favorites, stats | 2 files | 3 (Rollout) |
| 9 | Batch 3B: auth + onboarding | 8 files | 3 (Rollout) |
| 10 | Batch 3C: settings | 15 files | 3 (Rollout) |
| 11 | Batch 3D: modals | 21 files | 3 (Rollout) |
| 12 | Batch 3E: features | 10 files | 3 (Rollout) |
| 13 | shared.js helpers | 1 file | 2 (System) |
| 14 | COMPOSE-MAPPING.md | 1 file | 4 (Reference) |
| 15 | index.html branding | 1 file | Polish |
| 16 | Final QA pass | All files | Polish |
