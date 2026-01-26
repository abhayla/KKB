# RasoiAI Screen Wireframes

## Document Version: 3.1 | January 2025

This document contains ASCII wireframes for all 13 RasoiAI screens, designed following the established design system and inspired by Ollie.ai's UX patterns, adapted for the Indian market.

**Review Status:** ✅ All screens approved

---

## Design Reference

| Element | Value |
|---------|-------|
| Primary Color | Orange `#FF6838` |
| Secondary Color | Green `#5A822B` |
| Background | Cream `#FDFAF4` |
| Shapes | Rounded (8dp / 16dp / 24dp) |
| Spacing | 8dp grid |
| Language | English only |

---

## Navigation Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           RASOIAI NAVIGATION FLOW                            │
└─────────────────────────────────────────────────────────────────────────────┘

                                 ┌──────────┐
                                 │  APP     │
                                 │  LAUNCH  │
                                 └────┬─────┘
                                      │
                                      ▼
                                 ┌──────────┐
                                 │  SPLASH  │
                                 │ Screen 1 │
                                 └────┬─────┘
                                      │
                         ┌────────────┴────────────┐
                         │                         │
                         ▼                         ▼
                   ┌──────────┐             ┌──────────┐
                   │   AUTH   │             │   HOME   │
                   │ Screen 2 │             │ Screen 4 │
                   │(No Auth) │             │(Has Auth)│
                   └────┬─────┘             └────┬─────┘
                        │                        │
           ┌────────────┴────────────┐          │
           │                         │          │
           ▼                         ▼          │
     ┌──────────┐             ┌──────────┐     │
     │ONBOARDING│             │   HOME   │     │
     │ Screen 3 │             │ Screen 4 │     │
     │(New User)│             │(Existing)│     │
     └────┬─────┘             └────┬─────┘     │
           │                        │          │
           └────────────┬──────────┘          │
                        │                      │
                        ▼                      │
                   ┌─────────────────────────────────────────────┐
                   │                 HOME (Screen 4)              │
                   │            MAIN NAVIGATION HUB               │
                   └─────────────────────┬───────────────────────┘
                                         │
        ┌────────────┬──────────┬────────┴────────┬──────────┬────────────┐
        │            │          │                 │          │            │
        ▼            ▼          ▼                 ▼          ▼            ▼
   ┌─────────┐ ┌─────────┐ ┌─────────┐     ┌─────────┐ ┌─────────┐ ┌─────────┐
   │ RECIPE  │ │ GROCERY │ │  CHAT   │     │FAVORITES│ │ STATS   │ │SETTINGS │
   │ DETAIL  │ │  LIST   │ │         │     │         │ │         │ │         │
   │Screen 5 │ │Screen 7 │ │Screen 9 │     │Screen 8 │ │Screen 11│ │Screen 12│
   └────┬────┘ └─────────┘ └────┬────┘     └────┬────┘ └─────────┘ └─────────┘
        │                       │               │
        ▼                       │               │
   ┌─────────┐                  │               │
   │ COOKING │                  │               │
   │  MODE   │                  │               │
   │Screen 6 │                  │               │
   └────┬────┘                  │               │
        │                       │               │
        └───────────────────────┴───────────────┘
                                │
                                ▼
                          ┌─────────┐
                          │ PANTRY  │
                          │  SCAN   │
                          │Screen 10│
                          └─────────┘


BOTTOM NAVIGATION BAR (Screens 4, 7, 8, 9, 11):
┌─────────────────────────────────────────────────────────────────────────────┐
│     🏠          📋          💬          ❤️          📊                      │
│    Home      Grocery      Chat       Favorites    Stats                     │
│  Screen 4   Screen 7    Screen 9    Screen 8    Screen 11                   │
└─────────────────────────────────────────────────────────────────────────────┘


DETAILED NAVIGATION PATHS:

From HOME (Screen 4):
├── Tap Meal Card ──────────────────► Recipe Detail (Screen 5)
├── Tap [⟲] on Recipe ──────────────► Swap Suggestions (Bottom Sheet)
├── Tap [+ Add] on Meal ────────────► Add Recipe (Bottom Sheet)
├── Tap [🔄 Refresh] ───────────────► Regenerate Options (Bottom Sheet)
├── Tap Festival Banner ────────────► Festival Recipes (Screen 5 filtered)
├── Tap 🔔 (Notifications) ─────────► Notifications Screen
├── Tap 👤 (Profile) ───────────────► Settings (Screen 12)
└── Bottom Nav ─────────────────────► Grocery/Chat/Favorites/Stats

From RECIPE DETAIL (Screen 5):
├── Tap [🍳 Start Cooking Mode] ────► Cooking Mode (Screen 6)
├── Tap [💬 Modify with AI] ────────► Chat (Screen 9) with context
├── Tap ♡ ──────────────────────────► Add to Favorites
├── Tap [+ Add to Grocery] ─────────► Grocery List (Screen 7)
└── Tap ← (Back) ───────────────────► Previous Screen

From COOKING MODE (Screen 6):
├── Complete all steps ─────────────► Rate Meal Dialog
├── Tap ✕ (Close) ──────────────────► Exit Confirmation → Home
└── Rate Meal ──────────────────────► Stats Updated → Home

From CHAT (Screen 9):
├── Tap [View Recipe] ──────────────► Recipe Detail (Screen 5)
├── Tap [Add to Meal Plan] ─────────► Home (Screen 4) updated
├── Tap 📎 (Attachment) ────────────► Pantry Scan (Screen 10)
└── Bottom Nav ─────────────────────► Other screens

From FAVORITES (Screen 8):
├── Tap Recipe Card ────────────────► Recipe Detail (Screen 5)
├── Tap Collection ─────────────────► Collection Detail View
├── Tap [+ New] Collection ─────────► Create Collection Dialog
└── Bottom Nav ─────────────────────► Other screens

From STATS (Screen 11):
├── Tap Calendar Date ──────────────► Day Detail (Bottom Sheet)
├── Tap [View All Achievements] ────► Achievements Screen
├── Tap Achievement ────────────────► Share Achievement Dialog
└── Bottom Nav ─────────────────────► Other screens

From SETTINGS (Screen 12):
├── Tap Family Member ──────────────► Edit Member Dialog
├── Tap Preference Item ────────────► Preference Screen
├── Tap Friends & Leaderboard ──────► Leaderboard Screen
├── Tap Sign Out ───────────────────► Confirmation → Auth (Screen 2)
└── Tap ← (Back) ───────────────────► Home (Screen 4)

From PANTRY SCAN (Screen 10):
├── Capture Photo ──────────────────► Scan Results
├── Tap [Find Recipes] ─────────────► Recipes with Pantry Items
├── Tap [View All] Pantry ──────────► Full Pantry List
└── Tap Recipe ─────────────────────► Recipe Detail (Screen 5)
```

---

## Screen 1: Splash Screen

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│                                     │
│                                     │
│         ┌───────────────┐           │
│         │    🍳         │           │
│         │   RASOIAI     │           │
│         └───────────────┘           │
│                                     │
│      AI Meal Planning for           │
│         Indian Families             │
│                                     │
│                                     │
│                                     │
│          ◐ Loading...               │
│                                     │
│                                     │
│                                     │
│                                     │
└─────────────────────────────────────┘
```

### Design Notes:
| Element | Specification |
|---------|---------------|
| Background | Cream `#FDFAF4` |
| Logo | Orange `#FF6838` cooking pot icon |
| Duration | 2-3 seconds |
| Behavior | Check auth state → route to Auth or Home |
| Offline | Show offline banner if no network |

---

## Screen 2: Auth Screen

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│                                     │
│         ┌───────────────┐           │
│         │    🍳         │           │
│         │   RASOIAI     │           │
│         └───────────────┘           │
│                                     │
│            Welcome!                 │
│                                     │
│      AI Meal Planning for           │
│         Indian Families             │
│                                     │
│                                     │
│   ┌─────────────────────────────┐   │
│   │  G   Continue with Google   │   │
│   └─────────────────────────────┘   │
│                                     │
│                                     │
│                                     │
│                                     │
│   By continuing, you agree to our   │
│   Terms of Service • Privacy Policy │
│                                     │
└─────────────────────────────────────┘
```

### Design Notes:
| Element | Specification |
|---------|---------------|
| Auth Method | Google OAuth only |
| Backend | Firebase Auth |
| Button Style | Google branded button |
| Flow | Single tap → Google picker → Home/Onboarding |

---

## Screen 3: Onboarding (5 Steps)

### Step 1: Household Size

```
┌─────────────────────────────────────┐
│  ←                          1 of 5  │
│  ━━━━━○○○○○                         │
│─────────────────────────────────────│
│                                     │
│       How many people are           │
│       you cooking for?              │
│                                     │
│                                     │
│   ┌─────────────────────────────┐   │
│   │  4 people                  ▼│   │
│   └─────────────────────────────┘   │
│                                     │
│   Dropdown: 1-8+ people             │
│                                     │
│   Family members:                   │
│   ┌─────────────────────────────┐   │
│   │ 👤 Adult 1             [Edit]│   │
│   │ 👤 Adult 2             [Edit]│   │
│   │ + Add family member          │   │
│   └─────────────────────────────┘   │
│                                     │
│                                     │
│   ┌─────────────────────────────┐   │
│   │           NEXT →            │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### Add Family Member Dialog:

```
┌─────────────────────────────────────┐
│        Add Family Member            │
│─────────────────────────────────────│
│                                     │
│   Name:                             │
│   ┌─────────────────────────────┐   │
│   │ Rahul                       │   │
│   └─────────────────────────────┘   │
│                                     │
│   Type:                             │
│   ┌─────────────────────────────┐   │
│   │  Adult                     ▼│   │
│   └─────────────────────────────┘   │
│   Options: Adult, Child, Senior     │
│                                     │
│   Age:                              │
│   ┌─────────────────────────────┐   │
│   │  35                        ▼│   │
│   └─────────────────────────────┘   │
│                                     │
│   Special dietary needs:            │
│   (Available for ALL member types)  │
│   □ Diabetic                        │
│   □ Low oil                         │
│   □ No spicy                        │
│   □ Soft food                       │
│   □ Low salt                        │
│   □ High protein                    │
│   □ Low carb                        │
│                                     │
│   [CANCEL]           [ADD MEMBER]   │
└─────────────────────────────────────┘
```

### Step 2: Dietary Preferences

```
┌─────────────────────────────────────┐
│  ←                          2 of 5  │
│  ━━━━━━━━━━○○○○                     │
│─────────────────────────────────────│
│                                     │
│       What's your primary diet?     │
│                                     │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ ● Vegetarian                │   │
│   │   No meat, fish, or eggs    │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ ○ Eggetarian                │   │
│   │   Vegetarian + eggs         │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ ○ Non-Vegetarian            │   │
│   │   All foods                 │   │
│   └─────────────────────────────┘   │
│                                     │
│   Special dietary restrictions:     │
│                                     │
│   □ Jain (No root vegetables)       │
│   □ Sattvic (No onion/garlic)       │
│   □ Halal only                      │
│   □ Vegan                           │
│                                     │
│   [← BACK]              [NEXT →]    │
└─────────────────────────────────────┘
```

### Step 3: Cuisine Preferences

```
┌─────────────────────────────────────┐
│  ←                          3 of 5  │
│  ━━━━━━━━━━━━━━━○○                  │
│─────────────────────────────────────│
│                                     │
│       Which cuisines do you like?   │
│       (Select all that apply)       │
│                                     │
│   ┌──────────────┐ ┌──────────────┐ │
│   │     🥘      │ │     🍚      │ │
│   │    NORTH    │ │    SOUTH     │ │
│   │   Punjabi,  │ │   Tamil,     │ │
│   │   Mughlai   │ │   Kerala     │ │
│   │      ✓      │ │      ✓      │ │
│   └──────────────┘ └──────────────┘ │
│                                     │
│   ┌──────────────┐ ┌──────────────┐ │
│   │     🍛      │ │     🥗      │ │
│   │    EAST     │ │    WEST      │ │
│   │   Bengali,  │ │  Gujarati,   │ │
│   │   Odia      │ │ Maharashtrian│ │
│   │             │ │      ✓      │ │
│   └──────────────┘ └──────────────┘ │
│                                     │
│   Spice level:                      │
│   ┌─────────────────────────────┐   │
│   │  Medium                    ▼│   │
│   └─────────────────────────────┘   │
│   Options: Mild, Medium, Spicy,     │
│            Very Spicy               │
│                                     │
│   [← BACK]              [NEXT →]    │
└─────────────────────────────────────┘
```

### Step 4: Disliked Ingredients

```
┌─────────────────────────────────────┐
│  ←                          4 of 5  │
│  ━━━━━━━━━━━━━━━━━━━━○              │
│─────────────────────────────────────│
│                                     │
│       Any ingredients you dislike?  │
│       (Select all that apply)       │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 🔍 Search ingredients...    │   │
│   └─────────────────────────────┘   │
│                                     │
│   Common dislikes:                  │
│                                     │
│   ┌────────┐ ┌────────┐ ┌────────┐  │
│   │ Karela │ │ Lauki  │ │ Turai  │  │
│   │(Bitter │ │(Bottle │ │(Ridge  │  │
│   │ Gourd) │ │ Gourd) │ │ Gourd) │  │
│   │   ✓    │ │        │ │   ✓   │  │
│   └────────┘ └────────┘ └────────┘  │
│                                     │
│   ┌────────┐ ┌────────┐ ┌────────┐  │
│   │Baingan │ │ Bhindi │ │  Arbi  │  │
│   │(Eggplant)│ │ (Okra) │ │(Colocasia)│
│   └────────┘ └────────┘ └────────┘  │
│                                     │
│   ┌────────┐ ┌────────┐ ┌────────┐  │
│   │Coriander│ │ Methi │ │Mushroom│  │
│   │        │ │(Fenugreek)│       │  │
│   └────────┘ └────────┘ └────────┘  │
│                                     │
│   Selected: Karela, Turai           │
│                                     │
│   [← BACK]              [NEXT →]    │
└─────────────────────────────────────┘
```

### Step 5: Cooking Time

```
┌─────────────────────────────────────┐
│  ←                          5 of 5  │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━          │
│─────────────────────────────────────│
│                                     │
│       How much time do you have     │
│       for cooking?                  │
│                                     │
│   Weekdays:                         │
│   ┌─────────────────────────────┐   │
│   │  30 minutes                ▼│   │
│   └─────────────────────────────┘   │
│                                     │
│   Weekends:                         │
│   ┌─────────────────────────────┐   │
│   │  45 minutes                ▼│   │
│   └─────────────────────────────┘   │
│                                     │
│   Options: 15, 30, 45, 60, 90 min   │
│                                     │
│   Busy days (quick meals only):     │
│                                     │
│   ┌─────┐┌─────┐┌─────┐┌─────┐      │
│   │ Mon ││ Tue ││ Wed ││ Thu │      │
│   │     ││  ✓  ││     ││  ✓  │      │
│   └─────┘└─────┘└─────┘└─────┘      │
│   ┌─────┐┌─────┐┌─────┐             │
│   │ Fri ││ Sat ││ Sun │             │
│   └─────┘└─────┘└─────┘             │
│                                     │
│   ┌─────────────────────────────┐   │
│   │   ✓ CREATE MY MEAL PLAN     │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### Generating Screen:

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│         ┌───────────────┐           │
│         │   🍳 ◐        │           │
│         └───────────────┘           │
│                                     │
│      Creating your perfect          │
│         meal plan...                │
│                                     │
│   ✓ Analyzing preferences           │
│   ✓ Checking festivals              │
│   ◐ Generating recipes...           │
│   ○ Building grocery list           │
│                                     │
│                                     │
└─────────────────────────────────────┘
```

---

## Screen 4: Home (Weekly Meal Plan)

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
│  Monday, Jan 20      [🔒] [🔄 Refresh]│
│  TODAY                              │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🌅 BREAKFAST       [🔒] [+ Add] ││
│  ├─────────────────────────────────┤│
│  │ ● Poha         20 min  280cal [⟲]││
│  ├─────────────────────────────────┤│
│  │ ● Chai          5 min   80cal [⟲]││
│  ├─────────────────────────────────┤│
│  │              Total: 25 min 360cal││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ ☀️ LUNCH           [🔒] [+ Add] ││
│  ├─────────────────────────────────┤│
│  │ ● Dal Tadka    25 min  180cal [🔒]││
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
│  │ 🌙 DINNER  [🔒 Locked] [+ Add] ││
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
│  │ 🍪 SNACKS          [🔒] [+ Add] ││
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

### Lock Feature Overview

The Home screen supports **3 levels of locking** to protect meals from regeneration:

| Level | UI Element | Action |
|-------|------------|--------|
| **Day** | `[🔒]` button in day header | Locks all meals & recipes for that day |
| **Meal** | `[🔒]` button in meal header | Locks all recipes in that meal section |
| **Recipe** | 🔒 icon on recipe row | Locks individual recipe |

**Lock Inheritance (Soft):**
- Locking a Day automatically locks all Meals and Recipes within it
- Locking a Meal automatically locks all Recipes within it
- User can manually unlock individual children even if parent is locked
- Unlocking a parent does NOT automatically unlock children that were explicitly locked

### Visual Indicators:
| Icon | Meaning |
|------|---------|
| ● (Green) | Vegetarian |
| 🔴 (Red) | Non-vegetarian |
| [🔒] (header button) | Lock button for day/meal (unlocked state) |
| [🔒 Locked] (header) | Lock button for day/meal (locked state) |
| [⟲] (recipe row) | Swap button - recipe is **unlocked** |
| [🔒] (recipe row) | Lock button - recipe is **locked** (replaces swap) |

**Note:** On recipe rows, `[⟲]` and `[🔒]` are mutually exclusive - only one appears based on lock state.

### Day Header with Lock:

```
Unlocked state:
│  Monday, Jan 20      [🔒] [🔄 Refresh]│

Locked state:
│  Monday, Jan 20  [🔒 Locked] [🔄 Refresh]│
```

### Meal Header with Lock:

```
Unlocked state:
│ 🌅 BREAKFAST           [🔒] [+ Add] │

Locked state:
│ 🌅 BREAKFAST   [🔒 Locked] [+ Add] │

Long-press shortcut: Long-press on meal header to toggle lock
```

### Recipe Row States (Option D: Lock Replaces Swap):

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

### When Each Button Appears:

**`[⟲]` Swap Button appears when recipe is UNLOCKED:**

| Scenario | Button | Reason |
|----------|--------|--------|
| Recipe not locked, meal not locked, day not locked | `[⟲]` | Fully unlocked |
| Day locked, but user **explicitly unlocked** this recipe | `[⟲]` | Override unlocked |
| Meal locked, but user **explicitly unlocked** this recipe | `[⟲]` | Override unlocked |

**`[🔒]` Lock Button appears when recipe is LOCKED:**

| Scenario | Button | Reason |
|----------|--------|--------|
| User explicitly locked this recipe | `[🔒]` | Directly locked |
| Meal is locked (recipe not explicitly unlocked) | `[🔒]` | Inherited from meal |
| Day is locked (recipe not explicitly unlocked) | `[🔒]` | Inherited from day |

### State Transition Diagram:

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

### Quick Reference - User Actions:

| User Action | Current State | Result | Button After |
|-------------|---------------|--------|--------------|
| Tap [⟲] → Lock Recipe | Unlocked | Recipe locked | `[🔒]` |
| Tap [🔒] → Unlock Recipe | Locked | Recipe unlocked | `[⟲]` |
| Lock Meal | Recipe unlocked | Recipe becomes locked | `[🔒]` |
| Lock Day | Recipe unlocked | Recipe becomes locked | `[🔒]` |
| Unlock Recipe (while meal/day locked) | Inherited lock | Recipe override unlocked | `[⟲]` |
| Regenerate meals | Locked `[🔒]` | Recipe unchanged | `[🔒]` |
| Regenerate meals | Unlocked `[⟲]` | Recipe may change | `[⟲]` |

### Individual Recipe Actions (Tap [⟲] on unlocked recipe):

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

### Recipe Actions - Locked State:

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

### Lock/Unlock Interactions Summary:

| Action | Method | Result |
|--------|--------|--------|
| Lock Day | Tap [🔒] in day header | All meals & recipes show [🔒] |
| Lock Meal | Tap [🔒] in meal header | All recipes in meal show [🔒] |
| Lock Meal (shortcut) | Long-press meal header | Same as above |
| Lock Recipe | Tap [⟲] → "Lock Recipe" | Recipe button changes to [🔒] |
| Unlock Recipe | Tap [🔒] → "Unlock Recipe" | Recipe button changes to [⟲] |
| Unlock Meal | Tap [🔒 Locked] in meal header | Meal unlocked, explicitly locked recipes keep [🔒] |
| Unlock Day | Tap [🔒 Locked] in day header | Day unlocked, explicitly locked items keep [🔒] |

**Swipe Left on Recipe Row:**
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

### Refresh Options (on [🔄 Refresh] tap):

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

### Swap Individual Recipe Suggestions:

```
┌─────────────────────────────────────┐
│  ←  Swap Dal Tadka                  │
│─────────────────────────────────────│
│                                     │
│   Replace with:                     │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ ● Moong Dal                     ││
│  │   20 min  •  160 cal            ││
│  │   Lighter, quick option         ││
│  │                        [Select] ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ ● Chana Dal                     ││
│  │   30 min  •  200 cal            ││
│  │   High protein                  ││
│  │                        [Select] ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ ● Rajma                         ││
│  │   35 min  •  210 cal            ││
│  │   North Indian favorite         ││
│  │                        [Select] ││
│  └─────────────────────────────────┘│
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 🔍 Search all recipes       │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## Screen 5: Recipe Detail

### Ingredients Tab (Default):

```
┌─────────────────────────────────────┐
│  ←                         ♡    ⋮   │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │                                 ││
│  │      [RECIPE IMAGE HERE]       ││
│  │                                 ││
│  └─────────────────────────────────┘│
│                                     │
│  ● Dal Tadka                        │
│    North Indian • Punjabi           │
│                                     │
│  ┌────────┬────────┬────────┐       │
│  │ ⏱️     │ 👥     │ 🔥     │       │
│  │ 35 min │ 4 serv │ 180cal │       │
│  └────────┴────────┴────────┘       │
│                                     │
│  [● Vegetarian] [Gluten-Free]       │
│  [High Protein] [Easy]              │
│                                     │
│─────────────────────────────────────│
│  NUTRITION PER SERVING              │
│  ┌────────┬────────┬────────┬─────┐ │
│  │Calories│Protein │ Carbs  │ Fat │ │
│  │  180   │  12g   │  22g   │  5g │ │
│  └────────┴────────┴────────┴─────┘ │
│                                     │
│─────────────────────────────────────│
│                                     │
│  ┌────────────────┬────────────────┐│
│  │  INGREDIENTS   │  INSTRUCTIONS  ││
│  │      ━━━━      │                ││
│  └────────────────┴────────────────┘│
│                                     │
│  Servings:                          │
│  ┌─────────────────────────────┐    │
│  │  4 servings                ▼│    │
│  └─────────────────────────────┘    │
│                                     │
│  □ 1 cup Toor dal                   │
│  □ 1 medium Onion, chopped          │
│  □ 2 medium Tomatoes, pureed        │
│  □ 4 cloves Garlic, minced          │
│  □ 1 inch Ginger, grated            │
│  □ 1 tsp Cumin seeds                │
│  □ 1/2 tsp Turmeric powder          │
│  □ 1 tsp Red chili powder           │
│  □ 2 tbsp Ghee                      │
│  □ Fresh Coriander for garnish      │
│  □ Salt to taste                    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │   + Add All to Grocery List │    │
│  └─────────────────────────────┘    │
│                                     │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────┐    │
│  │     🍳 START COOKING MODE   │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │     💬 Modify with AI       │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

### Instructions Tab:

```
┌─────────────────────────────────────┐
│  ←                         ♡    ⋮   │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │      [RECIPE IMAGE HERE]       ││
│  └─────────────────────────────────┘│
│                                     │
│  ● Dal Tadka                        │
│    North Indian • Punjabi           │
│                                     │
│  ┌────────┬────────┬────────┐       │
│  │ ⏱️     │ 👥     │ 🔥     │       │
│  │ 35 min │ 4 serv │ 180cal │       │
│  └────────┴────────┴────────┘       │
│                                     │
│  [● Vegetarian] [Gluten-Free]       │
│                                     │
│─────────────────────────────────────│
│  NUTRITION PER SERVING              │
│  ┌────────┬────────┬────────┬─────┐ │
│  │  180   │  12g   │  22g   │  5g │ │
│  └────────┴────────┴────────┴─────┘ │
│                                     │
│─────────────────────────────────────│
│                                     │
│  ┌────────────────┬────────────────┐│
│  │  INGREDIENTS   │  INSTRUCTIONS  ││
│  │                │      ━━━━      ││
│  └────────────────┴────────────────┘│
│                                     │
│  6 Steps                            │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ Step 1                          ││
│  │ Wash and soak toor dal for 30   ││
│  │ minutes. Pressure cook with     ││
│  │ turmeric and salt for 3         ││
│  │ whistles.                       ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ Step 2                          ││
│  │ Heat ghee in a pan. Add cumin   ││
│  │ seeds and let them splutter.    ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ Step 3                          ││
│  │ Add chopped onions and sauté    ││
│  │ until golden brown (5-7 min).   ││
│  └─────────────────────────────────┘│
│                                     │
│  (Steps 4-6 continue...)            │
│                                     │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │     🍳 START COOKING MODE       ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │     💬 Modify with AI           ││
│  └─────────────────────────────────┘│
│                                     │
└─────────────────────────────────────┘
```

### Button Actions:
| Button | Action |
|--------|--------|
| 🍳 START COOKING MODE | Opens Screen 6 (Cooking Mode) |
| 💬 Modify with AI | Opens Screen 9 (Chat) with recipe context |

### Locked Recipe Indicator

When viewing a recipe that is **locked in the meal plan**, a 🔒 icon appears next to the recipe name:

```
┌─────────────────────────────────────┐
│  ←                         ♡    ⋮   │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │      [RECIPE IMAGE HERE]       ││
│  └─────────────────────────────────┘│
│                                     │
│  ● Dal Tadka  🔒                    │  ← Lock icon next to name
│    North Indian • Punjabi           │
│                                     │
│  ┌────────┬────────┬────────┐       │  ← Rest of screen unchanged
│  │ ⏱️     │ 👥     │ 🔥     │       │
│  │ 35 min │ 4 serv │ 180cal │       │
│  └────────┴────────┴────────┘       │
│                                     │
│  (rest of screen unchanged...)      │
└─────────────────────────────────────┘
```

**When lock indicator appears:**
| Scenario | Lock Shown? |
|----------|-------------|
| Recipe opened from **locked** meal plan item | ✅ Yes |
| Recipe opened from Favorites | ❌ No |
| Recipe opened from Search | ❌ No |
| Recipe opened from Chat suggestions | ❌ No |

**Notes:**
- Lock icon is informational only - all actions remain enabled
- User can still cook, favorite, modify with AI, and add to grocery
- Lock only prevents swap/regeneration on Home screen
- Minimal design: icon only, no banner (keeps screen clean)

---

## Screen 6: Cooking Mode

```
┌─────────────────────────────────────┐
│  ✕  Dal Tadka           Step 1 / 6  │
│  ━━━○○○○○○                          │
│─────────────────────────────────────│
│                                     │
│                                     │
│            STEP 1                   │
│                                     │
│                                     │
│   Wash and soak toor dal for        │
│   30 minutes.                       │
│                                     │
│   Pressure cook with turmeric       │
│   and salt for 3 whistles.          │
│                                     │
│                                     │
│        ┌───────────────┐            │
│        │  [Image/GIF]  │            │
│        │  Soaking dal  │            │
│        └───────────────┘            │
│                                     │
│                                     │
│   ┌─────────────────────────────┐   │
│   │      ⏱️  SET TIMER          │   │
│   │         30:00               │   │
│   └─────────────────────────────┘   │
│                                     │
│                                     │
│  ┌──────────────┐ ┌──────────────┐  │
│  │              │ │              │  │
│  │   ← PREV     │ │   NEXT →     │  │
│  │              │ │              │  │
│  └──────────────┘ └──────────────┘  │
│                                     │
│─────────────────────────────────────│
│        🔒 Screen stays ON           │
└─────────────────────────────────────┘
```

### Timer Running:

```
┌─────────────────────────────────────┐
│   ┌─────────────────────────────┐   │
│   │                             │   │
│   │         ⏱️ 24:35            │   │
│   │                             │   │
│   │    [PAUSE]     [STOP]       │   │
│   │                             │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### Timer Complete:

```
┌─────────────────────────────────────┐
│   ┌─────────────────────────────┐   │
│   │       ⏱️ TIME'S UP!         │   │
│   │     Dal soaking complete    │   │
│   │   🔔 Vibrating + Sound      │   │
│   │      [DISMISS]              │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### Cooking Complete (After Final Step):

```
┌─────────────────────────────────────┐
│                                     │
│            🎉                       │
│      Cooking Complete!              │
│      Dal Tadka is ready             │
│                                     │
│   ┌─────────────────────────────┐   │
│   │      Rate this dish         │   │
│   │   ☆    ☆    ☆    ☆    ☆    │   │
│   └─────────────────────────────┘   │
│                                     │
│   How did it turn out?              │
│   ┌─────────────────────────────┐   │
│   │ Add feedback (optional)...  │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │         DONE                │   │
│   └─────────────────────────────┘   │
│                                     │
│   [SKIP RATING]                     │
│                                     │
└─────────────────────────────────────┘
```

### Design Notes:
| Feature | Description |
|---------|-------------|
| Screen Lock | FLAG_KEEP_SCREEN_ON enabled |
| Timer | Per-step timer with sound/vibration |
| Navigation | Swipe or tap buttons |
| Rating | Shown after completion, affects AI learning |

---

## Screen 7: Grocery List

```
┌─────────────────────────────────────┐
│  ←  Grocery List                ⋮   │
│─────────────────────────────────────│
│                                     │
│  Week of Jan 20-26                  │
│  32 items                           │
│                                     │
│  ┌─────────────────────────────────┐│
│  │  📱 Share via WhatsApp          ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│                                     │
│  🥬 VEGETABLES (10)             ▼   │
│  ┌─────────────────────────────────┐│
│  │ □ Onion                   1 kg  ││
│  │ □ Tomato                  500g  ││
│  │ □ Potato                  1 kg  ││
│  │ □ Palak (Spinach)      2 bunch  ││
│  │ □ Capsicum                250g  ││
│  │ □ Ginger                  100g  ││
│  │ □ Garlic                  100g  ││
│  │ □ Green Chili            10 pcs ││
│  │ □ Coriander             1 bunch ││
│  │ □ Lemon                   4 pcs ││
│  └─────────────────────────────────┘│
│                                     │
│  🥛 DAIRY (5)                   ▼   │
│  ┌─────────────────────────────────┐│
│  │ □ Paneer                  400g  ││
│  │ □ Curd                    500g  ││
│  │ □ Milk                      1 L ││
│  │ □ Ghee                    200g  ││
│  │ □ Butter                  100g  ││
│  └─────────────────────────────────┘│
│                                     │
│  🌾 PULSES & GRAINS (6)         ▼   │
│  🌶️ SPICES & MASALA (8)         ▼   │
│  🥫 OTHER (3)                   ▼   │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ + Add custom item               ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
└─────────────────────────────────────┘
```

### WhatsApp Share Preview:

```
┌─────────────────────────────────────┐
│  Share to WhatsApp                  │
│─────────────────────────────────────│
│                                     │
│   Preview:                          │
│   ┌─────────────────────────────┐   │
│   │ 🛒 *Grocery List*           │   │
│   │ Week: Jan 20-26             │   │
│   │                             │   │
│   │ *🥬 Vegetables*             │   │
│   │ • Onion - 1 kg              │   │
│   │ • Tomato - 500g             │   │
│   │ ...                         │   │
│   │                             │   │
│   │ _Sent from RasoiAI_ 🍳      │   │
│   └─────────────────────────────┘   │
│                                     │
│   Share:                            │
│   ○ Full list (32 items)            │
│   ○ Unpurchased only (26 items)     │
│                                     │
│   [CANCEL]        [SHARE WHATSAPP]  │
│                                     │
└─────────────────────────────────────┘
```

### Item Swipe Actions:
```
┌───────────────────────────┬────┬────┐
│ □ Onion            1 kg   │ ✏️ │ 🗑️ │
└───────────────────────────┴────┴────┘
✏️ = Edit quantity, 🗑️ = Remove
```

---

## Screen 8: Favorites

```
┌─────────────────────────────────────┐
│  Favorites                     🔍   │
│─────────────────────────────────────│
│                                     │
│  Collections:                       │
│  ┌─────────┐ ┌─────────┐ ┌────────┐ │
│  │ [Image] │ │ [Image] │ │[Image] │ │
│  │   All   │ │Recently │ │Weekend │ │
│  │   (24)  │ │ Viewed  │ │Specials│ │
│  │    ✓    │ │  (12)   │ │  (8)   │ │
│  └─────────┘ └─────────┘ └────────┘ │
│                                     │
│  ┌─────────┐ ┌─────────┐ ┌────────┐ │
│  │ [Image] │ │ [Image] │ │        │ │
│  │  Quick  │ │  Kids   │ │ [+New] │ │
│  │  Meals  │ │Friendly │ │        │ │
│  │  (10)   │ │  (6)    │ │        │ │
│  └─────────┘ └─────────┘ └────────┘ │
│                                     │
│─────────────────────────────────────│
│                                     │
│  Filter:                            │
│  [All ▼] [Cuisine ▼] [Time ▼]       │
│                                     │
│  All (24)                  [Reorder]│
│─────────────────────────────────────│
│                                     │
│  ┌───────────────┐ ┌───────────────┐│
│  │    [Image]    │ │    [Image]    ││
│  │               │ │               ││
│  │ ● Dal Tadka   │ │ ● Palak Paneer││
│  │   North       │ │   North       ││
│  │   35m • 180cal│ │   40m • 320cal││
│  │          ♥  ⋮ │ │          ♥  ⋮ ││
│  └───────────────┘ └───────────────┘│
│                                     │
│  ┌───────────────┐ ┌───────────────┐│
│  │    [Image]    │ │    [Image]    ││
│  │               │ │               ││
│  │ ● Masala Dosa │ │🔴Butter Chicken│
│  │   South       │ │   North       ││
│  │   30m • 280cal│ │   45m • 480cal││
│  │          ♥  ⋮ │ │          ♥  ⋮ ││
│  └───────────────┘ └───────────────┘│
│                                     │
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
└─────────────────────────────────────┘
```

### Key Features:
- **2-column grid layout** for recipes
- **Collections with cover images** (custom or from recipe images)
- **Recently Viewed** - auto-populated default collection
- **Reorder mode** - drag to reorder recipes within collection
- **Filters** - by diet, cuisine, cooking time

### Reorder Mode:

```
┌─────────────────────────────────────┐
│  Favorites                   [Done] │
│─────────────────────────────────────│
│                                     │
│  Drag to reorder recipes            │
│                                     │
│  ┌───────────────┐ ┌───────────────┐│
│  │ ≡  [Image]    │ │ ≡  [Image]    ││
│  │ ● Dal Tadka   │ │ ● Palak Paneer││
│  └───────────────┘ └───────────────┘│
│         ↕                   ↕       │
│  ┌───────────────┐ ┌───────────────┐│
│  │ ≡  [Image]    │ │ ≡  [Image]    ││
│  │ ● Masala Dosa │ │🔴Butter Chicken│
│  └───────────────┘ └───────────────┘│
│                                     │
└─────────────────────────────────────┘
```

---

## Screen 9: Chat (AI Assistant)

```
┌─────────────────────────────────────┐
│  ←  RasoiAI Assistant           ⋮   │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🤖 RasoiAI                      ││
│  │                                 ││
│  │ Hi! I'm your AI cooking         ││
│  │ assistant. How can I help you   ││
│  │ today?                          ││
│  │                                 ││
│  │ Quick actions:                  ││
│  │ [Suggest dinner] [Swap a meal]  ││
│  │ [What can I cook?] [Diet tips]  ││
│  └─────────────────────────────────┘│
│                                     │
│  (Chat history from previous        │
│   sessions appears here)            │
│                                     │
│  ┌─────────────────────────────────┐│
│  │                          👤 You ││
│  │                                 ││
│  │ What can I make with paneer,    ││
│  │ tomatoes and spinach?           ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🤖 RasoiAI                      ││
│  │                                 ││
│  │ Great ingredients! Here are     ││
│  │ some recipes you can make:      ││
│  │                                 ││
│  │ 1. Palak Paneer (40 min)        ││
│  │ 2. Paneer Tikka Masala (35 min) ││
│  │ 3. Paneer Bhurji (20 min)       ││
│  │                                 ││
│  │ [View Palak Paneer]             ││
│  │ [View Paneer Tikka Masala]      ││
│  │ [View Paneer Bhurji]            ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ Type a message...        📎  🎤 ││
│  └─────────────────────────────────┘│
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
└─────────────────────────────────────┘
```

### More Menu (⋮):

```
┌─────────────────────────────────────┐
│   ┌─────────────────────────────┐   │
│   │ 🗑️ Clear Chat History       │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ ⚙️ Chat Settings            │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### Key Features:
- **Chat history** persisted from previous sessions
- **Clear Chat** option in menu
- **Time-based quick actions:**
  - Morning (6-11 AM): "Quick breakfast", "Healthy start"
  - Afternoon (11 AM-4 PM): "Lunch ideas", "Light meal"
  - Evening (4-9 PM): "Dinner suggestions", "Family meal"
  - Night (9 PM+): "Light snack", "Quick bite"
- **Voice input** (🎤) for hands-free
- **Attachment** (📎) for pantry photos

---

## Screen 10: Pantry Scan

```
┌─────────────────────────────────────┐
│  ←  Pantry Scan                     │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │                                 ││
│  │      [CAMERA VIEWFINDER]       ││
│  │                                 ││
│  │    Point at your fridge,        ││
│  │    pantry, or vegetables        ││
│  │                                 ││
│  └─────────────────────────────────┘│
│                                     │
│           [ 📸 CAPTURE ]            │
│                                     │
│  ┌─────────────────────────────────┐│
│  │      🖼️ Choose from Gallery     ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│                                     │
│  My Pantry (18 items)    [View All] │
│                                     │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │
│  │ 🥔  │ │ 🧅  │ │ 🍅  │ │ 🥛  │   │
│  │Potato│ │Onion│ │Tomato│ │Milk │   │
│  │ 3d ⚠️│ │ 5d  │ │ 2d  │ │ 1d ⚠️│   │
│  └─────┘ └─────┘ └─────┘ └─────┘   │
│                                     │
│  ⚠️ = Expiring soon                 │
│                                     │
│  ┌─────────────────────────────────┐│
│  │  🍳 Find Recipes (24 matches)   ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
└─────────────────────────────────────┘
```

### Expiry Tracking:

| Category | Default Shelf Life |
|----------|-------------------|
| Leafy Vegetables | 3 days |
| Other Vegetables | 7 days |
| Fruits | 5 days |
| Dairy (Milk, Curd) | 5 days |
| Dairy (Paneer, Cheese) | 7 days |
| Eggs | 14 days |
| Bread | 5 days |
| Grains & Pulses | No expiry |
| Spices | No expiry |

### Auto-Remove Confirmation:

```
┌─────────────────────────────────────┐
│                                     │
│      Remove Expired Items?          │
│                                     │
│   These items may have expired:     │
│                                     │
│   • Milk (added 6 days ago)         │
│   • Coriander (added 4 days ago)    │
│                                     │
│   [KEEP ALL]    [REMOVE SELECTED]   │
│                                     │
└─────────────────────────────────────┘
```

### Grocery Integration:
- When item marked "Purchased" in Grocery List → Auto-added to Pantry
- Expiry countdown starts from purchase date

---

## Screen 11: Stats / Gamification

```
┌─────────────────────────────────────┐
│  ←  My Cooking Stats                │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │        🔥 CURRENT STREAK        ││
│  │             12 days             ││
│  │   Keep cooking to extend!       ││
│  │   🏆 Best: 23 days              ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│                                     │
│  January 2025               [Today] │
│  ◀ ──────────────────────────── ▶  │
│                                     │
│  Su   Mo   Tu   We   Th   Fr   Sa   │
│                 1    2    3    4    │
│                 🍳   🍳   🍳   🍳   │
│   5    6    7    8    9   10   11   │
│  🍳   🍳   🍳   🍳   🍳   🍳   🍳   │
│  12   13   14   15   16   17   18   │
│  🍳   🍳   🍳   🍳   🍳   🍳   🍳   │
│  19   20   21   22   23   24   25   │
│  🍳  [●]   ○    ○    ○    ○    ○   │
│       TODAY                         │
│                                     │
│─────────────────────────────────────│
│                                     │
│  THIS MONTH                         │
│  ┌────────┐ ┌────────┐ ┌────────┐   │
│  │   45   │ │   12   │ │  4.2   │   │
│  │ Meals  │ │  New   │ │  Avg   │   │
│  │ Cooked │ │Recipes │ │ Rating │   │
│  └────────┘ └────────┘ └────────┘   │
│                                     │
│─────────────────────────────────────│
│                                     │
│  ACHIEVEMENTS              [View All]│
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │
│  │ 🏅  │ │ 🥇  │ │ 👨‍🍳 │ │ 🌟  │   │
│  │First│ │ 7-Day│ │Master│ │ 50  │   │
│  │Meal │ │Streak│ │ Chef │ │Meals│   │
│  └─────┘ └─────┘ └─────┘ └─────┘   │
│                                     │
│─────────────────────────────────────│
│                                     │
│  THIS WEEK'S CHALLENGE      [Join]  │
│  ┌─────────────────────────────────┐│
│  │ 🏆 South Indian Week            ││
│  │    Cook 5 South Indian dishes   ││
│  │    Progress: 2/5                ││
│  │    ━━━━━━━━░░░░░░░░░░░░        ││
│  │    Reward: Explorer Badge       ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│                                     │
│  LEADERBOARD               [View All]│
│  ┌─────────────────────────────────┐│
│  │ 🥇 Anjali M.      18 meals      ││
│  │ 🥈 You (Priya)    15 meals      ││
│  │ 🥉 Meera S.       14 meals      ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
└─────────────────────────────────────┘
```

### Key Features:
- **Leaderboards** - Compare with friends weekly
- **Shareable achievements** - Share to Facebook/Instagram/Twitter
- **Weekly/Monthly challenges** - Themed cooking challenges with rewards

### Share Achievement:

```
┌─────────────────────────────────────┐
│         🎉 ACHIEVEMENT              │
│            UNLOCKED!                │
│                                     │
│         ┌───────────┐               │
│         │    🌟     │               │
│         └───────────┘               │
│         Half Century                │
│      You've cooked 50 meals!        │
│                                     │
│   ┌─────────────────────────────┐   │
│   │         AWESOME!            │   │
│   └─────────────────────────────┘   │
│                                     │
│   Share to:                         │
│   [📘 Facebook] [📸 Instagram]      │
│   [🐦 Twitter] [📱 WhatsApp]        │
│                                     │
└─────────────────────────────────────┘
```

---

## Screen 12: Settings

```
┌─────────────────────────────────────┐
│  ←  Settings                        │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │         ┌───────────┐           ││
│  │         │  [Avatar] │           ││
│  │         └───────────┘           ││
│  │         Priya Sharma            ││
│  │     priya.sharma@gmail.com      ││
│  │           [Edit Profile]        ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  FAMILY                             │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │ 👤 Priya (You)           [Edit] ││
│  │    Vegetarian                   ││
│  ├─────────────────────────────────┤│
│  │ 👤 Rahul                 [Edit] ││
│  │    Non-vegetarian               ││
│  ├─────────────────────────────────┤│
│  │ 👧 Ananya (8 yrs)        [Edit] ││
│  │    No spicy                     ││
│  ├─────────────────────────────────┤│
│  │ 👴 Dadi (72 yrs)         [Edit] ││
│  │    Sattvic, Diabetic            ││
│  ├─────────────────────────────────┤│
│  │ + Add family member             ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  MEAL PREFERENCES                   │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │ Dietary Restrictions        ▶  ││
│  ├─────────────────────────────────┤│
│  │ Disliked Ingredients        ▶  ││
│  ├─────────────────────────────────┤│
│  │ Cuisine Preferences         ▶  ││
│  ├─────────────────────────────────┤│
│  │ Cooking Time                ▶  ││
│  ├─────────────────────────────────┤│
│  │ Spice Level                 ▶  ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  APP SETTINGS                       │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │ Notifications               ▶  ││
│  ├─────────────────────────────────┤│
│  │ Dark Mode              [System]││
│  ├─────────────────────────────────┤│
│  │ Units & Measurements        ▶  ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  SOCIAL                             │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │ Friends & Leaderboard       ▶  ││
│  ├─────────────────────────────────┤│
│  │ Connected Accounts          ▶  ││
│  ├─────────────────────────────────┤│
│  │ Share App with Friends      ▶  ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  SUPPORT                            │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │ Help & FAQ                  ▶  ││
│  ├─────────────────────────────────┤│
│  │ Contact Us                  ▶  ││
│  ├─────────────────────────────────┤│
│  │ Rate App on Play Store      ▶  ││
│  ├─────────────────────────────────┤│
│  │ Privacy Policy              ▶  ││
│  ├─────────────────────────────────┤│
│  │ Terms of Service            ▶  ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │          Sign Out               ││
│  └─────────────────────────────────┘│
│                                     │
│  App Version 1.0.0                  │
│                                     │
└─────────────────────────────────────┘
```

### Units & Measurements:

```
┌─────────────────────────────────────┐
│  Volume:                            │
│  ○ Metric (ml, L)                   │
│  ○ US (cups, fl oz)                 │
│  ● Indian (katori, glass)           │
│                                     │
│  Weight:                            │
│  ● Metric (g, kg)                   │
│  ○ US (oz, lbs)                     │
│                                     │
│  Small measurements:                │
│  ○ Metric (tsp, tbsp)               │
│  ● Indian (chammach)                │
└─────────────────────────────────────┘
```

### Friends & Leaderboard:

```
┌─────────────────────────────────────┐
│  ←  Friends & Leaderboard           │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ + Invite Friends                ││
│  └─────────────────────────────────┘│
│                                     │
│  THIS WEEK'S LEADERBOARD            │
│  ┌─────────────────────────────────┐│
│  │ 🥇 1. Anjali M.          18 🍳 ││
│  │ 🥈 2. You (Priya)        15 🍳 ││
│  │ 🥉 3. Meera S.           14 🍳 ││
│  │    4. Kavita R.          12 🍳 ││
│  │    5. Neha P.            10 🍳 ││
│  └─────────────────────────────────┘│
│                                     │
│  MY FRIENDS (5)                     │
│  ┌─────────────────────────────────┐│
│  │ [Avatar] Anjali M.              ││
│  │          18 meals • 12-day 🔥   ││
│  ├─────────────────────────────────┤│
│  │ [Avatar] Meera S.               ││
│  │          14 meals • 8-day 🔥    ││
│  └─────────────────────────────────┘│
│                                     │
└─────────────────────────────────────┘
```

---

## Screen 13: Recipe Rules

Access: Settings → Recipe Rules OR Home → ⚙️ (gear icon)

```
┌─────────────────────────────────────┐
│  ←  Recipe Rules                    │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 📖 Recipe  │ 🥕 Ingredient │... ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  MY RECIPE RULES (3)                │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │ 📖 Include "Rajma" weekly       ││
│  │    At least 1x per week         ││
│  │    [Required] ● Active    [✎]  ││
│  ├─────────────────────────────────┤│
│  │ 📖 Include "Moringa Curry"      ││
│  │    3x per week                  ││
│  │    [Preferred] ● Active   [✎]  ││
│  ├─────────────────────────────────┤│
│  │ 📖 Include "Chai"               ││
│  │    Every day with breakfast     ││
│  │    [Required] ● Active    [✎]  ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │      + ADD RECIPE RULE          ││
│  └─────────────────────────────────┘│
│                                     │
└─────────────────────────────────────┘
```

### Tab Bar:

```
┌─────────────────────────────────────────────────────────────┐
│  📖 Recipe  │ 🥕 Ingredient │ 🍽️ Meal-Slot │ 🥗 Nutrition  │
│   ━━━━━━━   │               │              │               │
└─────────────────────────────────────────────────────────────┘
```

**Tab Descriptions:**
- **Recipe Tab**: Include/exclude specific recipes
- **Ingredient Tab**: Include/exclude specific ingredients
- **Meal-Slot Tab**: Lock specific recipes to specific meal times
- **Nutrition Tab**: Weekly nutrition goals based on food categories

### Ingredient Rules Tab:

```
┌─────────────────────────────────────┐
│  ←  Recipe Rules                    │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 📖 Recipe │ 🥕 Ingredient │... ││
│  │           │   ━━━━━━━━━━━  │    ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  MY INGREDIENT RULES (2)            │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │ 🥕 Include "Spinach"            ││
│  │    At least 2x per week         ││
│  │    [Required] ● Active    [✎]  ││
│  ├─────────────────────────────────┤│
│  │ 🚫 Exclude "Bitter Gourd"       ││
│  │    Never include                ││
│  │    [Required] ● Active    [✎]  ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │     + ADD INGREDIENT RULE       ││
│  └─────────────────────────────────┘│
│                                     │
└─────────────────────────────────────┘
```

### Meal-Slot Rules Tab:

```
┌─────────────────────────────────────┐
│  ←  Recipe Rules                    │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 📖 Recipe │...│ 🍽️ Meal-Slot │...││
│  │           │   │  ━━━━━━━━━━━  │  ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  MY MEAL-SLOT RULES (2)             │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │ 🍽️ "Chai" → Breakfast           ││
│  │    Every day                    ││
│  │    [Required] ● Active    [✎]  ││
│  ├─────────────────────────────────┤│
│  │ 🍽️ "Dosa" → Weekend Breakfast   ││
│  │    Saturday & Sunday            ││
│  │    [Preferred] ● Active   [✎]  ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │     + ADD MEAL-SLOT RULE        ││
│  └─────────────────────────────────┘│
│                                     │
└─────────────────────────────────────┘
```

### Nutrition Goals Tab:

```
┌─────────────────────────────────────┐
│  ←  Recipe Rules                    │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 📖 Recipe │...│ 🥗 Nutrition    ││
│  │           │   │  ━━━━━━━━━━━    ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  WEEKLY NUTRITION GOALS             │
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │ 🥗 Green leafy vegetables       ││
│  │    ━━━━━━━━━━━━░░░░░░ 4/7 days  ││
│  │    Current: 4 days this week    ││
│  ├─────────────────────────────────┤│
│  │ 🍋 Citrus/Vitamin C foods       ││
│  │    ━━━━━━░░░░░░░░░░░░ 2/5 times ││
│  │    Current: 2 times this week   ││
│  ├─────────────────────────────────┤│
│  │ 🥜 Iron-rich foods              ││
│  │    ━━━━━━━━━━━━━━━━░░ 5/6 times ││
│  │    Current: 5 times this week   ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │     + ADD NUTRITION GOAL        ││
│  └─────────────────────────────────┘│
│                                     │
└─────────────────────────────────────┘
```

### Add Rule Bottom Sheet:

```
┌─────────────────────────────────────┐
│                                     │
│  ─────  (drag handle)               │
│                                     │
│  ADD RECIPE RULE                    │
│                                     │
│  Rule Type:                         │
│  ┌─────────────────────────────────┐│
│  │ ● Include this recipe           ││
│  │ ○ Exclude this recipe           ││
│  └─────────────────────────────────┘│
│                                     │
│  Recipe:                            │
│  ┌─────────────────────────────────┐│
│  │ Search recipes...          🔍   ││
│  └─────────────────────────────────┘│
│  Suggestions: Rajma, Dal Makhani,   │
│  Chole, Paneer Butter Masala        │
│                                     │
│  Frequency:                         │
│  ┌─────────────────────────────────┐│
│  │ At least [1] times per [week ▼] ││
│  └─────────────────────────────────┘│
│                                     │
│  OR specific days:                  │
│  [ ] Mon [✓] Tue [ ] Wed [ ] Thu   │
│  [ ] Fri [✓] Sat [ ] Sun            │
│                                     │
│  Enforcement:                       │
│  ┌─────────────────────────────────┐│
│  │ ● Required (must include)       ││
│  │ ○ Preferred (try to include)    ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │          SAVE RULE              ││
│  └─────────────────────────────────┘│
│                                     │
└─────────────────────────────────────┘
```

### Nutrition Goal Bottom Sheet:

```
┌─────────────────────────────────────┐
│                                     │
│  ─────  (drag handle)               │
│                                     │
│  ADD NUTRITION GOAL                 │
│                                     │
│  Food Category:                     │
│  ┌─────────────────────────────────┐│
│  │ Select category...          ▼   ││
│  └─────────────────────────────────┘│
│                                     │
│  Available Categories:              │
│  • Green leafy vegetables           │
│  • Citrus/Vitamin C rich foods      │
│  • Iron-rich foods                  │
│  • High protein foods               │
│  • Calcium-rich foods               │
│  • Fiber-rich foods                 │
│  • Omega-3 rich foods               │
│  • Antioxidant-rich foods           │
│                                     │
│  Weekly Target:                     │
│  ┌─────────────────────────────────┐│
│  │ At least [3] times per week     ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │          SAVE GOAL              ││
│  └─────────────────────────────────┘│
│                                     │
└─────────────────────────────────────┘
```

### Key Features:
- **4 Tab Categories** - Recipe, Ingredient, Meal-Slot, Nutrition
- **Required vs Preferred** - User controls enforcement level
- **Hybrid Ingredient Matching** - System suggests similar ingredients, user can customize
- **Food-Category Nutrition** - Goals based on food groups (not micronutrients)
- **Weekly Tracking** - Progress bars show goal completion
- **Search with Suggestions** - Recipe/ingredient search with contextual suggestions

### Access Points:
1. **Settings Screen** - Recipe Rules menu item under Meal Preferences
2. **Home Screen** - ⚙️ gear icon in header (quick access)

---

## Bottom Navigation Bar

```
┌─────────────────────────────────────┐
│                                     │
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
│                                     │
└─────────────────────────────────────┘
```

| Item | Screen | Icon |
|------|--------|------|
| Home | Screen 4 | 🏠 |
| Grocery | Screen 7 | 📋 |
| Chat | Screen 9 | 💬 |
| Favs | Screen 8 | ❤️ |
| Stats | Screen 11 | 📊 |

- Active: Orange `#FF6838` filled icon
- Inactive: Gray outline icon
- Badge on Chat if unread messages

---

## Common Components

### Offline Banner:
```
┌─────────────────────────────────────┐
│ ⚠️ You're offline. Some features    │
│    may be limited.                  │
└─────────────────────────────────────┘
```

### Loading State:
```
┌─────────────────────────────────────┐
│         ◐ Loading...               │
└─────────────────────────────────────┘
```

### Error State:
```
┌─────────────────────────────────────┐
│         ⚠️                          │
│    Something went wrong             │
│    [TRY AGAIN]                      │
└─────────────────────────────────────┘
```

### Dietary Indicators:
| Icon | Meaning |
|------|---------|
| ● (Green) | Vegetarian |
| 🔴 (Red) | Non-vegetarian |
| ● (Dark Green) | Vegan |
| ● (Yellow) | Jain-friendly |
| ● (Purple) | Fasting recipe |

---

## Summary of Approved Changes

| Screen | Key Changes |
|--------|-------------|
| All | English only (removed Hindi) |
| 2. Auth | Google OAuth only (removed Phone OTP) |
| 3. Onboarding | Dropdowns for selections, dietary needs for all member types |
| 4. Home | Multiple recipes per meal (4 meal types), **3-level locking** (day/meal/recipe with soft inheritance), long-press meal header shortcut, refresh for selected date |
| 5. Recipe Detail | Tabs for Ingredients/Instructions, **lock indicator** when viewing locked recipe |
| 8. Favorites | 2-column grid, reorder, cover images, Recently Viewed collection |
| 9. Chat | Chat history, Clear Chat option, time-based quick actions |
| 10. Pantry | Expiry tracking, grocery integration, auto-remove confirmation |
| 11. Stats | Leaderboards, shareable achievements, weekly/monthly challenges |
| 12. Settings | Full settings with social features |
| 13. Recipe Rules | **NEW** - 4 tabs (Recipe/Ingredient/Meal-Slot/Nutrition), Required vs Preferred enforcement, food-category nutrition goals, dual access (Settings + Home) |

---

*Document Updated: January 2025*
*Version: 3.1 (All 13 screens reviewed and approved)*
*Project: RasoiAI Android App*

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 3.1 | Jan 2025 | Added Screen 13: Recipe Rules with 4 tabs |
| 3.0 | Jan 2025 | All 12 screens reviewed and approved |
| 2.3 | Jan 2025 | Screen 4 & 5 lock features updated |
| 2.2 | Jan 2025 | Screen 4: Option D - Lock replaces Swap button |
| 2.1 | Jan 2025 | Screen 4: 3-level locking (day/meal/recipe) with soft inheritance |
| 2.0 | Jan 2025 | Initial wireframes for all screens |
