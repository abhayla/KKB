# RasoiAI Screen Wireframes

## Document Version: 2.0 | January 2025

This document contains ASCII wireframes for all 12 RasoiAI screens, designed following the established design system and inspired by Ollie.ai's UX patterns, adapted for the Indian market.

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
│  Monday, Jan 20          [🔄 Refresh]│
│  TODAY                              │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🌅 BREAKFAST            [+ Add] ││
│  ├─────────────────────────────────┤│
│  │ ● Poha         20 min  280cal [⟲]││
│  ├─────────────────────────────────┤│
│  │ ● Chai          5 min   80cal [⟲]││
│  ├─────────────────────────────────┤│
│  │              Total: 25 min 360cal││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ ☀️ LUNCH                [+ Add] ││
│  ├─────────────────────────────────┤│
│  │ ● Dal Tadka    25 min  180cal [⟲]││
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
│  │ 🌙 DINNER               [+ Add] ││
│  ├─────────────────────────────────┤│
│  │ ● Palak Paneer 30 min  320cal [⟲]││
│  ├─────────────────────────────────┤│
│  │ ● Butter Naan  15 min  280cal [⟲]││
│  ├─────────────────────────────────┤│
│  │ ● Raita        10 min   80cal [⟲]││
│  ├─────────────────────────────────┤│
│  │          Total: 40 min 680cal 🔒││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🍪 SNACKS               [+ Add] ││
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

### Visual Indicators:
| Icon | Meaning |
|------|---------|
| ● (Green) | Vegetarian |
| 🔴 (Red) | Non-vegetarian |
| 🔒 | Locked meal (won't change on regenerate) |
| [⟲] | Swap this individual recipe |

### Individual Recipe Actions (Tap recipe or [⟲]):

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

### Lock/Unlock Recipe:

**To Lock:** Tap recipe → "Lock Recipe"
**Locked Appearance:** Recipe shows 🔒 icon
**To Unlock:** Tap locked recipe → "Unlock Recipe"

**Swipe Left on Recipe Row:**
```
┌─────────────────────────┬────┬────┐
│ ● Dal Tadka   25min 180cal│ 🔒 │ ✕ │
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
│   Note: Locked recipes (🔒) will    │
│   not be changed.                   │
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
| 4. Home | Multiple recipes per meal (4 meal types), individual swap/lock per recipe, refresh for selected date |
| 5. Recipe Detail | Tabs for Ingredients/Instructions |
| 8. Favorites | 2-column grid, reorder, cover images, Recently Viewed collection |
| 9. Chat | Chat history, Clear Chat option, time-based quick actions |
| 10. Pantry | Expiry tracking, grocery integration, auto-remove confirmation |
| 11. Stats | Leaderboards, shareable achievements, weekly/monthly challenges |
| 12. Settings | Full settings with social features |

---

*Document Updated: January 2025*
*Version: 2.0 (All screens approved)*
*Project: RasoiAI Android App*
