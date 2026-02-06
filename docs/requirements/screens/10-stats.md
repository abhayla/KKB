# Screen 10: Stats & Achievements

## Summary Table

| ID | Element | Behavior | Status | Test Reference |
|----|---------|----------|--------|----------------|
| STAT-001 | Stats Screen | Display cooking stats | Implemented | `StatsScreenTest.kt` |
| STAT-002 | Back Navigation | Return to previous | Implemented | `StatsScreenTest.kt` |
| STAT-003 | Streak Card | Current cooking streak | Implemented | `StatsScreenTest.kt` |
| STAT-004 | Streak Count | Days count display | Implemented | `StatsScreenTest.kt` |
| STAT-005 | Best Streak | Historical best | Implemented | `StatsScreenTest.kt` |
| STAT-006 | Calendar View | Month cooking activity | Implemented | `StatsScreenTest.kt` |
| STAT-007 | Calendar Navigation | Month prev/next | Implemented | `StatsScreenTest.kt` |
| STAT-008 | Cooked Day Indicator | Show cooked days | Implemented | `StatsScreenTest.kt` |
| STAT-009 | Today Button | Jump to current | Implemented | `StatsScreenTest.kt` |
| STAT-010 | Monthly Stats | Summary metrics | Implemented | `StatsScreenTest.kt` |
| STAT-011 | Meals Cooked Stat | Count display | Implemented | `StatsScreenTest.kt` |
| STAT-012 | New Recipes Stat | Count display | Implemented | `StatsScreenTest.kt` |
| STAT-013 | Average Rating Stat | Rating display | Implemented | `StatsScreenTest.kt` |
| STAT-014 | Achievements Section | Badges display | Implemented | `StatsScreenTest.kt` |
| STAT-015 | Achievement Badge | Individual badge | Implemented | `StatsScreenTest.kt` |
| STAT-016 | View All Achievements | Full list | Implemented | `StatsScreenTest.kt` |
| STAT-017 | Challenge Section | Weekly challenge | Implemented | `StatsScreenTest.kt` |
| STAT-018 | Challenge Card | Challenge details | Implemented | `StatsScreenTest.kt` |
| STAT-019 | Challenge Progress | Completion bar | Implemented | `StatsScreenTest.kt` |
| STAT-020 | Join Challenge | Participate button | Implemented | `StatsScreenTest.kt` |
| STAT-021 | Leaderboard Section | Rankings preview | Implemented | `StatsScreenTest.kt` |
| STAT-022 | Leaderboard Row | Rank display | Implemented | `StatsScreenTest.kt` |
| STAT-023 | View All Leaderboard | Full rankings | Implemented | `StatsScreenTest.kt` |
| STAT-024 | Share Achievement | Social share | Implemented | `StatsScreenTest.kt` |
| STAT-025 | Bottom Navigation | 5 nav items | Implemented | `StatsScreenTest.kt` |

---

## Screen Layout

### Main Stats View
```
┌─────────────────────────────────────┐
│  ←  My Cooking Stats                │
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────┐    │
│  │  🔥 Current Streak         │    │
│  │                             │    │
│  │        12 days              │    │
│  │                             │    │
│  │  Best: 28 days              │    │
│  │  Last cooked: Today         │    │
│  └─────────────────────────────┘    │
│─────────────────────────────────────│
│                                     │
│  ◀  January 2025  ▶      [Today]   │
│  ┌─────────────────────────────┐    │
│  │ Mo Tu We Th Fr Sa Su       │    │
│  │        1  2  3  4  5       │    │
│  │  6  7  8  9 10 11 12       │    │
│  │ 13 ●● 15 ●● 17 18 19       │    │
│  │ 20 ●● ●● ●● 24 25 26       │    │
│  │ 27 28 29 30 31             │    │
│  └─────────────────────────────┘    │
│  ●● = cooked that day              │
│─────────────────────────────────────│
│                                     │
│  ┌────────┐ ┌────────┐ ┌────────┐  │
│  │  🍽    │ │  📗    │ │  ⭐    │  │
│  │  18    │ │   5    │ │  4.2   │  │
│  │ Meals  │ │  New   │ │  Avg   │  │
│  │Cooked  │ │Recipes │ │Rating  │  │
│  └────────┘ └────────┘ └────────┘  │
│─────────────────────────────────────│
│                                     │
│  Achievements                       │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐  │
│  │ 🏆  │ │ 👨‍🍳 │ │ 🌟  │ │ 📚  │  │
│  │First│ │Chef │ │Week │ │ 10   │  │
│  │Cook │ │Mode │ │Str. │ │Recip │  │
│  └─────┘ └─────┘ └─────┘ └─────┘  │
│                   [View All >]      │
│─────────────────────────────────────│
│                                     │
│  Weekly Challenge                   │
│  ┌─────────────────────────────┐    │
│  │ Try 3 South Indian dishes  │    │
│  │ ████████░░░░  2/3           │    │
│  │ [Join Challenge]            │    │
│  └─────────────────────────────┘    │
│─────────────────────────────────────│
│                                     │
│  Leaderboard                        │
│  ┌─────────────────────────────┐    │
│  │ 1. 🥇 Priya S.     450 pts│    │
│  │ 2. 🥈 Amit K.      380 pts│    │
│  │ 3. 🥉 Neha R.      320 pts│    │
│  │              [View All >]   │    │
│  └─────────────────────────────┘    │
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊   │
│  Home  Grocery  Chat  Favs  Stats   │
└─────────────────────────────────────┘
```

---

## Detailed Requirements

### STAT-001: Stats Screen Display

| Field | Value |
|-------|-------|
| **Screen** | Stats |
| **Element** | Full screen |
| **Trigger** | Navigate from bottom nav |
| **Status** | Implemented |
| **Test** | `StatsScreenTest.kt:statsScreen_displaysCorrectly` |

**Acceptance Criteria:**
- Given: User navigates to Stats
- When: Screen displays
- Then: Header shows "My Cooking Stats"
- And: All sections visible with scrolling
- And: Bottom navigation visible

**Sections:**
1. Current Streak
2. Calendar View
3. This Month Stats
4. Achievements
5. Weekly Challenge
6. Leaderboard

---

### STAT-003: Streak Card

| Field | Value |
|-------|-------|
| **Screen** | Stats |
| **Element** | Streak highlight |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `StatsScreenTest.kt:streakCard_displaysCorrectly` |

**Layout:**
```
┌─────────────────────────────────────┐
│        🔥 CURRENT STREAK            │
│             12 days                 │
│   Keep cooking to extend!           │
│   🏆 Best: 23 days                  │
└─────────────────────────────────────┘
```

**Acceptance Criteria:**
- Given: User has cooking history
- When: Streak card renders
- Then: Fire emoji and "CURRENT STREAK" header
- And: Large day count
- And: Motivational text
- And: Best streak shown

---

### STAT-006: Calendar View

| Field | Value |
|-------|-------|
| **Screen** | Stats |
| **Element** | Monthly calendar |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `StatsScreenTest.kt:calendarView_displaysMonth` |

**Layout:**
```
│  January 2025               [Today] │
│  ◀ ──────────────────────────── ▶  │
│                                     │
│  Su   Mo   Tu   We   Th   Fr   Sa   │
│                 1    2    3    4    │
│                 🍳   🍳   🍳   🍳   │
│   5    6    7    8    9   10   11   │
│  🍳   🍳   🍳   🍳   🍳   🍳   🍳   │
│  ...                                │
```

**Icons:**
| Icon | Meaning |
|------|---------|
| 🍳 | Cooked on this day |
| [●] | Today (highlighted) |
| ○ | Future/no activity |

**Acceptance Criteria:**
- Given: Stats screen displayed
- When: Calendar renders
- Then: Current month shown
- And: Cooked days have 🍳 icon
- And: Today is highlighted
- And: Navigation arrows work

---

### STAT-007: Calendar Navigation

| Field | Value |
|-------|-------|
| **Screen** | Stats |
| **Element** | Month navigation |
| **Trigger** | User tap arrows |
| **Status** | Implemented |
| **Test** | `StatsScreenTest.kt:calendarNavigation_changesMonth` |

**Acceptance Criteria:**
- Given: Calendar displayed
- When: User taps ◀ or ▶
- Then: Previous/next month shows
- And: Cooking data updates
- And: "Today" button appears if not current month

---

### STAT-010: Monthly Stats

| Field | Value |
|-------|-------|
| **Screen** | Stats |
| **Element** | Summary metrics |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `StatsScreenTest.kt:monthlyStats_displayMetrics` |

**Layout:**
```
│  THIS MONTH                         │
│  ┌────────┐ ┌────────┐ ┌────────┐   │
│  │   45   │ │   12   │ │  4.2   │   │
│  │ Meals  │ │  New   │ │  Avg   │   │
│  │ Cooked │ │Recipes │ │ Rating │   │
│  └────────┘ └────────┘ └────────┘   │
```

**Metrics:**
| Metric | Description |
|--------|-------------|
| Meals Cooked | Total meals marked complete |
| New Recipes | First-time recipes tried |
| Avg Rating | Average rating given |

---

### STAT-014: Achievements Section

| Field | Value |
|-------|-------|
| **Screen** | Stats |
| **Element** | Badge showcase |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `StatsScreenTest.kt:achievementsSection_displaysBadges` |

**Layout:**
```
│  ACHIEVEMENTS              [View All]│
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │
│  │ 🏅  │ │ 🥇  │ │ 👨‍🍳 │ │ 🌟  │   │
│  │First│ │ 7-Day│ │Master│ │ 50  │   │
│  │Meal │ │Streak│ │ Chef │ │Meals│   │
│  └─────┘ └─────┘ └─────┘ └─────┘   │
```

**Acceptance Criteria:**
- Given: User has achievements
- When: Section renders
- Then: 4 recent badges shown
- And: Earned badges are colored
- And: Locked badges are grayed
- And: "View All" opens full list

---

### STAT-015: Achievement Badge

| Field | Value |
|-------|-------|
| **Screen** | Stats |
| **Element** | Individual badge |
| **Trigger** | Badge earned |
| **Status** | Implemented |
| **Test** | `StatsScreenTest.kt:achievementBadge_displaysCorrectly` |

**Example Achievements:**
| Badge | Name | Criteria |
|-------|------|----------|
| 🏅 | First Meal | Complete first cooking |
| 🥇 | 7-Day Streak | 7 consecutive days |
| 👨‍🍳 | Master Chef | 100 meals cooked |
| 🌟 | Half Century | 50 meals cooked |
| 🌈 | Explorer | Try all 4 cuisine regions |
| 🥬 | Green Champion | 30 vegetarian meals |

**Acceptance Criteria:**
- Given: Badge displayed
- When: Tap on badge
- Then: Badge detail modal opens
- And: Shows name, description, date earned
- And: Share option available

---

### STAT-017: Challenge Section

| Field | Value |
|-------|-------|
| **Screen** | Stats |
| **Element** | Weekly challenge |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `StatsScreenTest.kt:challengeSection_displaysCurrentChallenge` |

**Layout:**
```
│  THIS WEEK'S CHALLENGE      [Join]  │
│  ┌─────────────────────────────────┐│
│  │ 🏆 South Indian Week            ││
│  │    Cook 5 South Indian dishes   ││
│  │    Progress: 2/5                ││
│  │    ━━━━━━━━░░░░░░░░░░░░        ││
│  │    Reward: Explorer Badge       ││
│  └─────────────────────────────────┘│
```

**Acceptance Criteria:**
- Given: Active challenge exists
- When: Challenge section renders
- Then: Challenge name and description shown
- And: Progress bar shows completion
- And: Reward displayed
- And: Join button if not participating

---

### STAT-021: Leaderboard Section

| Field | Value |
|-------|-------|
| **Screen** | Stats |
| **Element** | Rankings preview |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `StatsScreenTest.kt:leaderboardSection_displaysRankings` |

**Layout:**
```
│  LEADERBOARD               [View All]│
│  ┌─────────────────────────────────┐│
│  │ 🥇 Anjali M.      18 meals      ││
│  │ 🥈 You (Priya)    15 meals      ││
│  │ 🥉 Meera S.       14 meals      ││
│  └─────────────────────────────────┘│
```

**Acceptance Criteria:**
- Given: User has friends
- When: Leaderboard section renders
- Then: Top 3 shown with medals
- And: Current user highlighted
- And: Meal count shown
- And: "View All" opens full list

---

### STAT-024: Share Achievement

| Field | Value |
|-------|-------|
| **Screen** | Stats |
| **Element** | Share modal |
| **Trigger** | Achievement unlocked |
| **Status** | Implemented |
| **Test** | `StatsScreenTest.kt:shareAchievement_opensShareOptions` |

**Share Modal:**
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

**Share Platforms:**
- Facebook
- Instagram
- Twitter
- WhatsApp

**Acceptance Criteria:**
- Given: Achievement unlocked
- When: Share tapped
- Then: Share options displayed
- And: Generates shareable image
- And: Opens selected platform

---

## Implementation Files

| Component | File Path |
|-----------|-----------|
| Stats Screen | `presentation/stats/StatsScreen.kt` |
| Stats ViewModel | `presentation/stats/StatsViewModel.kt` |
| Streak Card | `presentation/stats/components/StreakCard.kt` |
| Calendar View | `presentation/stats/components/CookingCalendar.kt` |
| Achievement Badge | `presentation/stats/components/AchievementBadge.kt` |
| Challenge Card | `presentation/stats/components/ChallengeCard.kt` |
| Leaderboard Row | `presentation/stats/components/LeaderboardRow.kt` |

## Test Files

| Test Type | File Path |
|-----------|-----------|
| UI Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/stats/StatsScreenTest.kt` |
| Unit Tests | `app/src/test/java/com/rasoiai/app/presentation/stats/StatsViewModelTest.kt` |
| E2E Flow | `app/src/androidTest/java/com/rasoiai/app/e2e/flows/StatsFlowTest.kt` |

---

*Requirements derived from wireframe: `11-stats.md`*
