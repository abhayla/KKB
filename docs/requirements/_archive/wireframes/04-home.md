# Screen 4: Home (Weekly Meal Plan)

```
┌─────────────────────────────────────┐
│  ☰  RasoiAI                 🔔  👤  │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🎉 Makar Sankranti in 3 days!  ││
│  │    View festive recipes →       ││
│  └─────────────────────────────────┘│
│                                     │
│  This Week's Menu                   │
│  Jan 20 - 26                        │
│                                     │
│  ┌─Mo─┬─Tu─┬─We─┬─Th─┬─Fr─┬─Sa─┬─Su┐│
│  │ 20 │ 21 │ 22 │ 23 │ 24 │ 25 │ 26││
│  │[●] │ ○  │ ○  │ ○  │ ○  │ ○  │ ○ ││
│  └────┴────┴────┴────┴────┴────┴───┘│
│                                     │
│  Monday, Jan 20      [🔓] [🔄 Refresh]│  ← Day unlocked
│  TODAY                              │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🌅 BREAKFAST       [🔓] [+ Add] ││  ← Meal unlocked
│  ├─────────────────────────────────┤│
│  │ ● Poha         20 min  280cal [⟲]││
│  ├─────────────────────────────────┤│
│  │ ● Chai          5 min   80cal [⟲]││
│  ├─────────────────────────────────┤│
│  │              Total: 25 min 360cal││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ ☀️ LUNCH           [🔓] [+ Add] ││  ← Meal unlocked
│  ├─────────────────────────────────┤│
│  │ ● Dal Tadka    25 min  180cal [🔒]││  ← Recipe locked individually
│  ├─────────────────────────────────┤│
│  │ ● Jeera Rice   15 min  220cal [⟲]││
│  ├─────────────────────────────────┤│
│  │ ● Roti (4)     20 min  320cal [⟲]││
│  ├─────────────────────────────────┤│
│  │ ● Salad         5 min   50cal [⟲]││
│  ├─────────────────────────────────┤│
│  │              Total: 45 min 770cal││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🌙 DINNER          [🔒] [+ Add] ││  ← Meal locked
│  ├─────────────────────────────────┤│
│  │ ● Palak Paneer  30 min 320cal [🔒]││
│  ├─────────────────────────────────┤│
│  │ ● Butter Naan   15 min 280cal [🔒]││
│  ├─────────────────────────────────┤│
│  │ ● Raita        10 min   80cal [🔒]││
│  ├─────────────────────────────────┤│
│  │              Total: 40 min 680cal││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🍪 SNACKS          [🔓] [+ Add] ││  ← Meal unlocked
│  ├─────────────────────────────────┤│
│  │ ● Samosa (2)      --   240cal [⟲]││
│  ├─────────────────────────────────┤│
│  │ ● Green Tea     5 min    5cal [⟲]││
│  ├─────────────────────────────────┤│
│  │               Total: 5 min 245cal││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
└─────────────────────────────────────┘
```

---

## Fully Locked Day Example

```
┌─────────────────────────────────────┐
│  ☰  RasoiAI                 🔔  👤  │
│─────────────────────────────────────│
│                                     │
│  This Week's Menu                   │
│  Jan 20 - 26                        │
│                                     │
│  Monday, Jan 20      [🔒] [🔄 Refresh]│  ← Day locked (🔒)
│  TODAY                              │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🌅 BREAKFAST       [🔒] [+ Add] ││  ← Meal locked (inherited)
│  ├─────────────────────────────────┤│
│  │ ● Poha         20 min  280cal [🔒]││  ← Recipe locked
│  ├─────────────────────────────────┤│
│  │ ● Chai          5 min   80cal [🔒]││  ← Recipe locked
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ ☀️ LUNCH           [🔒] [+ Add] ││  ← Meal locked (inherited)
│  ├─────────────────────────────────┤│
│  │ ● Dal Tadka    25 min  180cal [🔒]││
│  ├─────────────────────────────────┤│
│  │ ● Jeera Rice   15 min  220cal [⟲]││  ← User explicitly unlocked this one
│  ├─────────────────────────────────┤│
│  │ ● Roti (4)     20 min  320cal [🔒]││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🌙 DINNER          [🔒] [+ Add] ││  ← Meal locked (inherited)
│  ├─────────────────────────────────┤│
│  │ ● Palak Paneer  30 min 320cal [🔒]││
│  ├─────────────────────────────────┤│
│  │ ● Butter Naan   15 min 280cal [🔒]││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
└─────────────────────────────────────┘
```

**Key observations:**
- Day header shows `[🔒]` (day is locked)
- All meal headers show `[🔒]` (inherited from day)
- Most recipes show `[🔒]` lock icon (inherited lock)
- Jeera Rice shows `[⟲]` swap button (user explicitly unlocked despite parent lock)

---

## Lock Feature Overview

The Home screen supports **3 levels of locking** to protect meals from regeneration:

| Level | UI Element | Action |
|-------|------------|--------|
| **Day** | `[🔒]` button in day header | Locks all meals & recipes for that day |
| **Meal** | `[🔒]` button in meal header | Locks all recipes in that meal section |
| **Recipe** | 🔒 icon on recipe row | Locks individual recipe |

### Lock Inheritance (Soft):
- Locking a Day automatically locks all Meals and Recipes within it
- Locking a Meal automatically locks all Recipes within it
- User can manually unlock individual children even if parent is locked
- Unlocking a parent does NOT automatically unlock children that were explicitly locked

---

## Visual Indicators

| Icon | Meaning |
|------|---------|
| ● (Green) | Vegetarian |
| 🔴 (Red) | Non-vegetarian |
| [🔓] (header button) | Day/meal is **unlocked** - tap to lock |
| [🔒] (header button) | Day/meal is **locked** - tap to unlock |
| [⟲] (recipe row) | Swap button - recipe is **unlocked** |
| [🔒] (recipe row) | Lock icon - recipe is **locked** (replaces swap) |
| 🔓 (swipe action) | Unlock action revealed on swipe for locked recipes |

**Icon Logic:** The icon shows the **current state** (🔒 = locked, 🔓 = unlocked).

**Note:** On recipe rows, `[⟲]` and `[🔒]` are mutually exclusive - only one appears based on lock state.

---

## Day Header States

```
Unlocked state (shows 🔓):
│  Monday, Jan 20      [🔓] [🔄 Refresh]│

Locked state (shows 🔒):
│  Monday, Jan 20      [🔒] [🔄 Refresh]│
```

---

## Meal Header States

```
Unlocked state (shows 🔓):
│ 🌅 BREAKFAST           [🔓] [+ Add] │

Locked state (shows 🔒):
│ 🌅 BREAKFAST           [🔒] [+ Add] │

Long-press shortcut: Long-press on meal header to toggle lock state
```

---

## Recipe Row States (Lock Replaces Swap)

```
Unlocked recipe - shows [⟲] swap button:
│ ● Dal Tadka    25 min  180cal  [⟲]│

Locked recipe - shows [🔒] lock button (swap hidden):
│ ● Dal Tadka    25 min  180cal  [🔒]│
```

**Button Behavior:**
- `[⟲]` and `[🔒]` occupy the **same position** - only one shows at a time
- Dietary indicator (● or 🔴) always remains visible
- Tap either button to open the recipe actions bottom sheet

---

## When Each Button Appears

### `[⟲]` Swap Button appears when recipe is UNLOCKED:

| Scenario | Button | Reason |
|----------|--------|--------|
| Recipe not locked, meal not locked, day not locked | `[⟲]` | Fully unlocked |
| Day locked, but user **explicitly unlocked** this recipe | `[⟲]` | Override unlocked |
| Meal locked, but user **explicitly unlocked** this recipe | `[⟲]` | Override unlocked |

### `[🔒]` Lock Button appears when recipe is LOCKED:

| Scenario | Button | Reason |
|----------|--------|--------|
| User explicitly locked this recipe | `[🔒]` | Directly locked |
| Meal is locked (recipe not explicitly unlocked) | `[🔒]` | Inherited from meal |
| Day is locked (recipe not explicitly unlocked) | `[🔒]` | Inherited from day |

---

## State Transition Diagram

```
                    ┌─────────────┐
                    │  UNLOCKED   │
                    │    [⟲]      │
                    └──────┬──────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
   ┌───────────┐    ┌───────────┐    ┌───────────┐
   │User locks │    │Meal locked│    │Day locked │
   │ recipe    │    │(inherited)│    │(inherited)│
   └─────┬─────┘    └─────┬─────┘    └─────┬─────┘
         │                 │                 │
         └─────────────────┼─────────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │   LOCKED    │
                    │    [🔒]     │
                    └──────┬──────┘
                           │
                           ▼
                    User taps [🔒]
                    → Unlock option
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
       ┌─────────────┐          ┌─────────────┐
       │   UNLOCKED  │          │ Still LOCKED│
       │ (override)  │          │ (cancelled) │
       │    [⟲]      │          │    [🔒]     │
       └─────────────┘          └─────────────┘
```

---

## Quick Reference - User Actions

| User Action | Current State | Result | Button After |
|-------------|---------------|--------|--------------|
| Tap [⟲] → Lock Recipe | Unlocked | Recipe locked | `[🔒]` |
| Tap [🔒] → Unlock Recipe | Locked | Recipe unlocked | `[⟲]` |
| Lock Meal | Recipe unlocked | Recipe becomes locked | `[🔒]` |
| Lock Day | Recipe unlocked | Recipe becomes locked | `[🔒]` |
| Unlock Recipe (while meal/day locked) | Inherited lock | Recipe override unlocked | `[⟲]` |
| Regenerate meals | Locked `[🔒]` | Recipe unchanged | `[🔒]` |
| Regenerate meals | Unlocked `[⟲]` | Recipe may change | `[⟲]` |

---

## Individual Recipe Actions (Tap [⟲] on unlocked recipe)

```
┌─────────────────────────────────────┐
│  ─────────────────────────────────  │
│                                     │
│      Dal Tadka                      │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 👁️ View Recipe              │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 🔄 Swap Recipe              │   │
│   │    Replace with similar     │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 🔒 Lock Recipe              │   │
│   │    Protect from regenerate  │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ ✕  Remove from Meal         │   │
│   └─────────────────────────────┘   │
│                                     │
│            [CANCEL]                 │
└─────────────────────────────────────┘
```

---

## Recipe Actions - Locked State

When a recipe is locked (directly or inherited), the bottom sheet shows:

```
┌─────────────────────────────────────┐
│  ─────────────────────────────────  │
│                                     │
│      Dal Tadka  🔒                  │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 👁️ View Recipe              │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 🔓 Unlock Recipe            │   │
│   │    Allow changes            │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 🔄 Swap Recipe (disabled)   │   │
│   │    Unlock to swap           │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ ✕  Remove (disabled)        │   │
│   │    Unlock to remove         │   │
│   └─────────────────────────────┘   │
│                                     │
│            [CANCEL]                 │
└─────────────────────────────────────┘
```

---

## Lock/Unlock Interactions Summary

| Action | Method | Result |
|--------|--------|--------|
| Lock Day | Tap [🔓] in day header | Day header changes to [🔒], all meals & recipes inherit lock |
| Lock Meal | Tap [🔓] in meal header | Meal header changes to [🔒], all recipes show [🔒] |
| Lock Meal (shortcut) | Long-press meal header | Same as above |
| Lock Recipe | Tap [⟲] → "🔒 Lock Recipe" | Recipe button changes from [⟲] to [🔒] |
| Unlock Day | Tap [🔒] in day header | Day header changes to [🔓], explicitly locked items keep [🔒] |
| Unlock Meal | Tap [🔒] in meal header | Meal header changes to [🔓], explicitly locked recipes keep [🔒] |
| Unlock Recipe | Tap [🔒] → "🔓 Unlock Recipe" | Recipe button changes from [🔒] to [⟲] |

---

## Swipe Left on Recipe Row

```
Unlocked recipe:
┌─────────────────────────┬────┬────┐
│ ● Dal Tadka   25min 180cal│ 🔒 │ ✕ │  ← Swipe reveals Lock & Delete
└─────────────────────────┴────┴────┘

Locked recipe:
┌─────────────────────────┬────┬────┐
│ ● Dal Tadka   25min 180cal│ 🔓 │    │  ← Swipe reveals Unlock only (no delete)
└─────────────────────────┴────┴────┘
```

---

## Refresh Options (on [🔄 Refresh] tap)

```
┌─────────────────────────────────────┐
│  ─────────────────────────────────  │
│                                     │
│      Regenerate Meals               │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 📅 This Day Only            │   │
│   │    Regenerate Monday, Jan 20│   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 📆 Entire Week              │   │
│   │    Regenerate Jan 20-26     │   │
│   └─────────────────────────────┘   │
│                                     │
│   ⚠️ Locked items (🔒) will not    │
│   be changed during regeneration.  │
│   This includes locked days, meals,│
│   and individual recipes.          │
│                                     │
│            [CANCEL]                 │
└─────────────────────────────────────┘
```

---

## Add Recipe to Meal (on [+ Add] tap)

```
┌─────────────────────────────────────┐
│  ─────────────────────────────────  │
│                                     │
│      Add to Breakfast               │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 🔍 Search recipes...        │   │
│   └─────────────────────────────┘   │
│                                     │
│   Suggestions for Breakfast:        │
│                                     │
│  ┌───────────────┐ ┌───────────────┐│
│  │    [Image]    │ │    [Image]    ││
│  │               │ │               ││
│  │ ● Upma        │ │ ● Poha        ││
│  │   South       │ │   West        ││
│  │   15m • 180cal│ │   20m • 200cal││
│  └───────────────┘ └───────────────┘│
│                                     │
│  ┌───────────────┐ ┌───────────────┐│
│  │    [Image]    │ │    [Image]    ││
│  │               │ │               ││
│  │ ● Paratha     │ │ ● Idli        ││
│  │   North       │ │   South       ││
│  │   20m • 280cal│ │   25m • 120cal││
│  └───────────────┘ └───────────────┘│
│                                     │
│  ┌───────────────┐ ┌───────────────┐│
│  │    [Image]    │ │    [Image]    ││
│  │               │ │               ││
│  │ ● Dalia       │ │ ● Smoothie    ││
│  │   North       │ │   Fusion      ││
│  │   15m • 150cal│ │   10m • 200cal││
│  └───────────────┘ └───────────────┘│
│                                     │
│   Recently Added:                   │
│  ┌───────────────┐ ┌───────────────┐│
│  │    [Image]    │ │    [Image]    ││
│  │               │ │               ││
│  │ ● Aloo Paratha│ │ ● Masala Dosa ││
│  │   North       │ │   South       ││
│  │   25m • 320cal│ │   30m • 280cal││
│  └───────────────┘ └───────────────┘│
│                                     │
│            [CANCEL]                 │
└─────────────────────────────────────┘
```

### Add Recipe Behavior:

| Action | Result |
|--------|--------|
| Tap recipe card | Recipe added to meal, bottom sheet closes |
| Search & select | Recipe added to meal, bottom sheet closes |
| Added recipe state | New recipe appears with [⟲] (unlocked) |
| Multiple adds | User can tap [+ Add] again to add more |

### Component Reuse:

**Reuse `RecipeGridItem` from Favorites** (`presentation/favorites/components/RecipeGridItem.kt`):

| Feature | Favorites Screen | Add Recipe Sheet |
|---------|-----------------|------------------|
| 2-column grid | ✅ | ✅ |
| Recipe image | ✅ | ✅ |
| Veg/Non-veg indicator | ✅ | ✅ |
| Recipe name | ✅ | ✅ |
| Cuisine type | ✅ | ✅ |
| Time & calories | ✅ | ✅ |
| Favorite icon (♥) | ✅ | ❌ (hide) |
| More menu (⋮) | ✅ | ❌ (hide) |
| Reorder mode | ✅ | ❌ (not needed) |

**Implementation approach:**
```kotlin
// Option 1: Add parameters to existing RecipeGridItem
RecipeGridItem(
    recipe = recipe,
    onClick = { onRecipeSelected(recipe) },
    showFavoriteIcon = false,    // New param
    showMoreMenu = false,        // New param
    // ... other params
)

// Option 2: Create simplified RecipeSelectableCard
// in presentation/common/components/RecipeSelectableCard.kt
// that wraps RecipeGridItem with selection-only behavior
```

**Notes:**
- Use same card dimensions and styling as Favorites
- Suggestions are contextual based on meal type (Breakfast/Lunch/Dinner/Snacks)
- Recently added recipes from user history shown at bottom
- Search allows finding any recipe in the system
- Added recipes are always unlocked initially

---

## Swap Individual Recipe Suggestions

```
┌─────────────────────────────────────┐
│  ←  Swap Dal Tadka                  │
│─────────────────────────────────────│
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 🔍 Search recipes...        │   │
│   └─────────────────────────────┘   │
│                                     │
│   Similar recipes:                  │
│                                     │
│  ┌───────────────┐ ┌───────────────┐│
│  │    [Image]    │ │    [Image]    ││
│  │               │ │               ││
│  │ ● Moong Dal   │ │ ● Chana Dal   ││
│  │   North       │ │   North       ││
│  │   20m • 160cal│ │   30m • 200cal││
│  └───────────────┘ └───────────────┘│
│                                     │
│  ┌───────────────┐ ┌───────────────┐│
│  │    [Image]    │ │    [Image]    ││
│  │               │ │               ││
│  │ ● Rajma       │ │ ● Masoor Dal  ││
│  │   North       │ │   North       ││
│  │   35m • 210cal│ │   25m • 180cal││
│  └───────────────┘ └───────────────┘│
│                                     │
│            [CANCEL]                 │
└─────────────────────────────────────┘
```

### Swap Behavior:

| Action | Result |
|--------|--------|
| Tap recipe card | Original recipe replaced, bottom sheet closes |
| Search & select | Original recipe replaced, bottom sheet closes |
| Swapped recipe state | New recipe appears with [⟲] (unlocked) |

**Component Reuse:** Same `RecipeGridItem` component as Add Recipe sheet (without ♥ and ⋮).
