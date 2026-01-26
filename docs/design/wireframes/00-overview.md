# RasoiAI Screen Wireframes - Overview

## Document Version: 3.1 | January 2025

This document contains ASCII wireframes for all 13 RasoiAI screens, designed following the established design system and inspired by Ollie.ai's UX patterns, adapted for the Indian market.

**Review Status:** All screens approved

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

## Screen Index

| # | Screen | File |
|---|--------|------|
| 1 | Splash | [01-splash.md](01-splash.md) |
| 2 | Auth | [02-auth.md](02-auth.md) |
| 3 | Onboarding | [03-onboarding.md](03-onboarding.md) |
| 4 | Home (Weekly Meal Plan) | [04-home.md](04-home.md) |
| 5 | Recipe Detail | [05-recipe-detail.md](05-recipe-detail.md) |
| 6 | Cooking Mode | [06-cooking-mode.md](06-cooking-mode.md) |
| 7 | Grocery List | [07-grocery-list.md](07-grocery-list.md) |
| 8 | Favorites | [08-favorites.md](08-favorites.md) |
| 9 | Chat (AI Assistant) | [09-chat.md](09-chat.md) |
| 10 | Pantry Scan | [10-pantry-scan.md](10-pantry-scan.md) |
| 11 | Stats / Gamification | [11-stats.md](11-stats.md) |
| 12 | Settings | [12-settings.md](12-settings.md) |
| 13 | Recipe Rules | [13-recipe-rules.md](13-recipe-rules.md) |

Common components: [99-common-components.md](99-common-components.md)

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
```

---

## Detailed Navigation Paths

### From HOME (Screen 4):
- Tap Meal Card → Recipe Detail (Screen 5)
- Tap [⟲] on Recipe → Swap Suggestions (Bottom Sheet)
- Tap [+ Add] on Meal → Add Recipe (Bottom Sheet)
- Tap [🔄 Refresh] → Regenerate Options (Bottom Sheet)
- Tap Festival Banner → Festival Recipes (Screen 5 filtered)
- Tap 🔔 (Notifications) → Notifications Screen
- Tap 👤 (Profile) → Settings (Screen 12)
- Bottom Nav → Grocery/Chat/Favorites/Stats

### From RECIPE DETAIL (Screen 5):
- Tap [🍳 Start Cooking Mode] → Cooking Mode (Screen 6)
- Tap [💬 Modify with AI] → Chat (Screen 9) with context
- Tap ♡ → Add to Favorites
- Tap [+ Add to Grocery] → Grocery List (Screen 7)
- Tap ← (Back) → Previous Screen

### From COOKING MODE (Screen 6):
- Complete all steps → Rate Meal Dialog
- Tap ✕ (Close) → Exit Confirmation → Home
- Rate Meal → Stats Updated → Home

### From CHAT (Screen 9):
- Tap [View Recipe] → Recipe Detail (Screen 5)
- Tap [Add to Meal Plan] → Home (Screen 4) updated
- Tap 📎 (Attachment) → Pantry Scan (Screen 10)
- Bottom Nav → Other screens

### From FAVORITES (Screen 8):
- Tap Recipe Card → Recipe Detail (Screen 5)
- Tap Collection → Collection Detail View
- Tap [+ New] Collection → Create Collection Dialog
- Bottom Nav → Other screens

### From STATS (Screen 11):
- Tap Calendar Date → Day Detail (Bottom Sheet)
- Tap [View All Achievements] → Achievements Screen
- Tap Achievement → Share Achievement Dialog
- Bottom Nav → Other screens

### From SETTINGS (Screen 12):
- Tap Family Member → Edit Member Dialog
- Tap Preference Item → Preference Screen
- Tap Friends & Leaderboard → Leaderboard Screen
- Tap Sign Out → Confirmation → Auth (Screen 2)
- Tap ← (Back) → Home (Screen 4)

### From PANTRY SCAN (Screen 10):
- Capture Photo → Scan Results
- Tap [Find Recipes] → Recipes with Pantry Items
- Tap [View All] Pantry → Full Pantry List
- Tap Recipe → Recipe Detail (Screen 5)
