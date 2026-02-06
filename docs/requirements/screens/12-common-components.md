# Screen 12: Common Components

## Summary Table

| ID | Element | Behavior | Status | Test Reference |
|----|---------|----------|--------|----------------|
| COM-001 | Bottom Navigation | 5-item nav bar | Implemented | `BottomNavTest.kt` |
| COM-002 | Home Nav Item | Navigate to Home | Implemented | `BottomNavTest.kt` |
| COM-003 | Grocery Nav Item | Navigate to Grocery | Implemented | `BottomNavTest.kt` |
| COM-004 | Chat Nav Item | Navigate to Chat | Implemented | `BottomNavTest.kt` |
| COM-005 | Favorites Nav Item | Navigate to Favorites | Implemented | `BottomNavTest.kt` |
| COM-006 | Stats Nav Item | Navigate to Stats | Implemented | `BottomNavTest.kt` |
| COM-007 | Nav Badge | Unread indicator | Implemented | `BottomNavTest.kt` |
| COM-008 | Offline Banner | No network indicator | Implemented | `OfflineBannerTest.kt` |
| COM-009 | Loading State | Loading indicator | Implemented | `LoadingStateTest.kt` |
| COM-010 | Error State | Error message | Implemented | `ErrorStateTest.kt` |
| COM-011 | Retry Button | Error retry action | Implemented | `ErrorStateTest.kt` |
| COM-012 | Dietary Indicators | Veg/Non-veg icons | Implemented | Various |
| COM-013 | Snackbar | Toast messages | Implemented | Various |
| COM-014 | Confirmation Dialog | Action confirmation | Implemented | Various |
| COM-015 | Bottom Sheet | Modal content | Implemented | Various |
| COM-016 | Pull to Refresh | Refresh gesture | Implemented | Various |
| COM-017 | Empty State | No data message | Implemented | Various |

---

## Screen Layout

### Bottom Navigation Bar
```
┌─────────────────────────────────────┐
│                                     │
│  🏠     📋     💬     ❤️     📊   │
│  Home  Grocery  Chat  Favs  Stats   │
│                                     │
└─────────────────────────────────────┘

Active item: filled icon + primary color
Inactive items: outlined icon + muted color
```

### Error State
```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│                                     │
│             ⚠️                      │
│                                     │
│      Something went wrong           │
│                                     │
│  Unable to load data. Please        │
│  check your connection and try      │
│  again.                             │
│                                     │
│  ┌─────────────────────────────┐    │
│  │         Retry               │    │
│  └─────────────────────────────┘    │
│                                     │
│                                     │
└─────────────────────────────────────┘
```

### Loading State
```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│                                     │
│                                     │
│              ◯                      │
│          (spinning)                 │
│                                     │
│                                     │
│                                     │
└─────────────────────────────────────┘
```

### Offline Banner
```
┌─────────────────────────────────────┐
│ ☁✕ You're offline. Some features   │
│     may be limited.                 │
│─────────────────────────────────────│
│                                     │
│  (rest of screen content)           │
│                                     │
└─────────────────────────────────────┘

Appears at top of any screen when offline.
Background: errorContainer color.
```

---

## Detailed Requirements

### COM-001: Bottom Navigation Bar

| Field | Value |
|-------|-------|
| **Component** | RasoiBottomNavigation |
| **Element** | 5-item nav bar |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `BottomNavTest.kt:bottomNav_displaysAllItems` |

**Layout:**
```
┌─────────────────────────────────────┐
│                                     │
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
│                                     │
└─────────────────────────────────────┘
```

**Navigation Items:**
| Item | Icon | Screen | Route |
|------|------|--------|-------|
| Home | 🏠 | Home | `home` |
| Grocery | 📋 | Grocery List | `grocery` |
| Chat | 💬 | AI Chat | `chat` |
| Favs | ❤️ | Favorites | `favorites` |
| Stats | 📊 | Statistics | `stats` |

**Styling:**
| State | Color |
|-------|-------|
| Active | Orange `#FF6838` (filled) |
| Inactive | Gray (outline) |

**Acceptance Criteria:**
- Given: Any main screen displayed
- When: Bottom nav renders
- Then: 5 items visible
- And: Current screen highlighted
- And: Tap navigates to screen

---

### COM-007: Navigation Badge

| Field | Value |
|-------|-------|
| **Component** | RasoiBottomNavigation |
| **Element** | Badge indicator |
| **Trigger** | Unread content |
| **Status** | Implemented |
| **Test** | `BottomNavTest.kt:navBadge_showsOnUnread` |

**Badge Triggers:**
| Item | Badge Trigger |
|------|---------------|
| Chat | Unread AI messages |

**Acceptance Criteria:**
- Given: Unread content exists
- When: Bottom nav renders
- Then: Small red badge on icon
- And: Badge clears when viewed

---

### COM-008: Offline Banner

| Field | Value |
|-------|-------|
| **Component** | OfflineBanner |
| **Element** | Network status indicator |
| **Trigger** | No network connection |
| **Status** | Implemented |
| **Test** | `OfflineBannerTest.kt:offlineBanner_showsWhenNoNetwork` |

**Layout:**
```
┌─────────────────────────────────────┐
│ ⚠️ You're offline. Some features    │
│    may be limited.                  │
└─────────────────────────────────────┘
```

**Styling:**
| Property | Value |
|----------|-------|
| Background | Warning amber |
| Icon | ⚠️ |
| Position | Top of content area |

**Acceptance Criteria:**
- Given: Device loses network
- When: NetworkMonitor detects offline
- Then: Banner slides in at top
- And: Dismisses when network returns
- And: Shows on all screens

---

### COM-009: Loading State

| Field | Value |
|-------|-------|
| **Component** | LoadingState |
| **Element** | Loading indicator |
| **Trigger** | Data loading |
| **Status** | Implemented |
| **Test** | `LoadingStateTest.kt:loadingState_showsIndicator` |

**Layout:**
```
┌─────────────────────────────────────┐
│         ◐ Loading...               │
└─────────────────────────────────────┘
```

**Variants:**
| Variant | Use Case |
|---------|----------|
| Full screen | Initial load |
| Inline | Content section loading |
| Overlay | Action in progress |

**Acceptance Criteria:**
- Given: Data being fetched
- When: UiState.isLoading = true
- Then: Loading indicator displays
- And: Content hidden or dimmed

---

### COM-010: Error State

| Field | Value |
|-------|-------|
| **Component** | ErrorState |
| **Element** | Error message display |
| **Trigger** | Operation failed |
| **Status** | Implemented |
| **Test** | `ErrorStateTest.kt:errorState_showsMessage` |

**Layout:**
```
┌─────────────────────────────────────┐
│         ⚠️                          │
│    Something went wrong             │
│    [TRY AGAIN]                      │
└─────────────────────────────────────┘
```

**Acceptance Criteria:**
- Given: Error occurred
- When: UiState.errorMessage set
- Then: Error icon and message display
- And: Retry button available
- And: Descriptive error text shown

---

### COM-012: Dietary Indicators

| Field | Value |
|-------|-------|
| **Component** | DietaryIndicator |
| **Element** | Veg/Non-veg icons |
| **Trigger** | Recipe display |
| **Status** | Implemented |
| **Test** | Various recipe tests |

**Indicator Types:**
| Icon | Color | Meaning |
|------|-------|---------|
| ● | Green | Vegetarian |
| 🔴 | Red | Non-vegetarian |
| ● | Dark Green | Vegan |
| ● | Yellow | Jain-friendly |
| ● | Purple | Fasting recipe |

**Acceptance Criteria:**
- Given: Recipe has dietary tags
- When: Recipe card/detail displays
- Then: Appropriate indicator shown
- And: Color matches dietary type

---

### COM-013: Snackbar

| Field | Value |
|-------|-------|
| **Component** | Snackbar |
| **Element** | Toast message |
| **Trigger** | User action feedback |
| **Status** | Implemented |
| **Test** | Various tests |

**Variants:**
| Type | Use Case | Duration |
|------|----------|----------|
| Success | Action completed | 3 seconds |
| Error | Action failed | 5 seconds |
| Info | Information | 3 seconds |
| Action | With undo/action | Until dismissed |

**Example:**
```
┌─────────────────────────────────────┐
│ Recipe added to favorites    [UNDO] │
└─────────────────────────────────────┘
```

**Acceptance Criteria:**
- Given: User performs action
- When: Feedback needed
- Then: Snackbar appears at bottom
- And: Auto-dismisses after duration
- And: Action button if applicable

---

### COM-014: Confirmation Dialog

| Field | Value |
|-------|-------|
| **Component** | ConfirmDialog |
| **Element** | Action confirmation |
| **Trigger** | Destructive action |
| **Status** | Implemented |
| **Test** | Various tests |

**Layout:**
```
┌─────────────────────────────────────┐
│         Remove from favorites?      │
│                                     │
│  This recipe will be removed from   │
│  your favorites.                    │
│                                     │
│       [CANCEL]        [REMOVE]      │
└─────────────────────────────────────┘
```

**Acceptance Criteria:**
- Given: Destructive action initiated
- When: Dialog displays
- Then: Clear title and description
- And: Cancel and confirm buttons
- And: Cannot dismiss by tapping outside

---

### COM-015: Bottom Sheet

| Field | Value |
|-------|-------|
| **Component** | ModalBottomSheet |
| **Element** | Modal content container |
| **Trigger** | Action requiring options |
| **Status** | Implemented |
| **Test** | Various tests |

**Common Uses:**
| Use Case | Content |
|----------|---------|
| Recipe Actions | View, Swap, Lock, Remove |
| Add Recipe | Search and suggestions |
| Swap Recipe | Alternative suggestions |
| Share Grocery | WhatsApp options |

**Acceptance Criteria:**
- Given: User triggers sheet action
- When: Sheet opens
- Then: Slides up from bottom
- And: Drag handle at top
- And: Can dismiss by dragging down
- And: Can dismiss by tapping scrim

---

### COM-016: Pull to Refresh

| Field | Value |
|-------|-------|
| **Component** | PullToRefresh |
| **Element** | Refresh gesture |
| **Trigger** | Pull down on content |
| **Status** | Implemented |
| **Test** | Various tests |

**Acceptance Criteria:**
- Given: Scrollable content at top
- When: User pulls down
- Then: Refresh indicator appears
- And: Data refreshes
- And: Indicator dismisses on complete

---

### COM-017: Empty State

| Field | Value |
|-------|-------|
| **Component** | EmptyState |
| **Element** | No data placeholder |
| **Trigger** | Empty list/content |
| **Status** | Implemented |
| **Test** | Various tests |

**Pattern:**
```
┌─────────────────────────────────────┐
│         [Icon]                      │
│                                     │
│     No [items] yet                  │
│                                     │
│   [Description text explaining      │
│    how to add items]                │
│                                     │
│        [CTA BUTTON]                 │
└─────────────────────────────────────┘
```

**Acceptance Criteria:**
- Given: No items to display
- When: Empty state renders
- Then: Relevant icon shown
- And: Clear title
- And: Helpful description
- And: Optional call-to-action

---

## Design System Reference

### Colors

| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| Primary | `#FF6838` | `#FFB59C` | Actions, highlights |
| Secondary | `#5A822B` | `#A8D475` | Success, veg indicator |
| Background | `#FDFAF4` | `#1C1B1F` | Screen background |
| Surface | `#FFFFFF` | `#2C2C2C` | Cards, sheets |
| Error | `#B00020` | `#CF6679` | Errors, non-veg |

### Spacing

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4dp | Tight spacing |
| sm | 8dp | Component padding |
| md | 16dp | Section spacing |
| lg | 24dp | Large gaps |
| xl | 32dp | Screen margins |

### Typography

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| Headline Large | 32sp | Bold | Screen titles |
| Headline Medium | 28sp | Bold | Section headers |
| Body Large | 16sp | Regular | Main content |
| Body Medium | 14sp | Regular | Secondary content |
| Label | 12sp | Medium | Chips, badges |

### Shapes

| Token | Value | Usage |
|-------|-------|-------|
| Small | 8dp | Chips, small buttons |
| Medium | 16dp | Cards, dialogs |
| Large | 24dp | Bottom sheets |

---

## Implementation Files

| Component | File Path |
|-----------|-----------|
| Bottom Navigation | `presentation/home/components/RasoiBottomNavigation.kt` |
| Offline Banner | `presentation/common/components/OfflineBanner.kt` |
| Loading State | `presentation/common/components/LoadingState.kt` |
| Error State | `presentation/common/components/ErrorState.kt` |
| Dietary Indicator | `presentation/common/components/DietaryIndicator.kt` |
| Empty State | `presentation/common/components/EmptyState.kt` |
| Test Tags | `presentation/common/TestTags.kt` |

## Test Files

| Test Type | File Path |
|-----------|-----------|
| Bottom Nav Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/common/BottomNavTest.kt` |
| Component Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/common/` |

---

*Requirements derived from wireframe: `99-common-components.md`*
