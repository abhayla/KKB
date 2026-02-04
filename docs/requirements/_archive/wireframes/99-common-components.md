# Common Components

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

## Offline Banner

```
┌─────────────────────────────────────┐
│ ⚠️ You're offline. Some features    │
│    may be limited.                  │
└─────────────────────────────────────┘
```

---

## Loading State

```
┌─────────────────────────────────────┐
│         ◐ Loading...               │
└─────────────────────────────────────┘
```

---

## Error State

```
┌─────────────────────────────────────┐
│         ⚠️                          │
│    Something went wrong             │
│    [TRY AGAIN]                      │
└─────────────────────────────────────┘
```

---

## Dietary Indicators

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

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 3.1 | Jan 2025 | Added Screen 13: Recipe Rules with 4 tabs |
| 3.0 | Jan 2025 | All 12 screens reviewed and approved |
| 2.3 | Jan 2025 | Screen 4 & 5 lock features updated |
| 2.2 | Jan 2025 | Screen 4: Option D - Lock replaces Swap button |
| 2.1 | Jan 2025 | Screen 4: 3-level locking (day/meal/recipe) with soft inheritance |
| 2.0 | Jan 2025 | Initial wireframes for all screens |

---

*Document Updated: January 2025*
*Version: 3.1 (All 13 screens reviewed and approved)*
*Project: RasoiAI Android App*
