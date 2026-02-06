# Screen 11: Notifications

## Summary Table

| ID | Element | Behavior | Status | Test Reference |
|----|---------|----------|--------|----------------|
| NOTIF-001 | Notifications Screen | Display notification list | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-002 | Back Navigation | Return to previous | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-003 | More Options Menu | Notification settings | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-004 | Notification Card | Individual notification | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-005 | Notification Icon | Type indicator | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-006 | Notification Title | Summary text | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-007 | Notification Body | Detail text | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-008 | Notification Time | Timestamp | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-009 | Unread Indicator | Visual unread marker | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-010 | Notification Tap | Navigate to context | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-011 | Mark All Read | Clear unread markers | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-012 | Delete Notification | Remove single | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-013 | Clear All | Remove all notifications | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-014 | Empty State | No notifications message | Implemented | `NotificationsScreenTest.kt` |
| NOTIF-015 | Festival Notification | Festival reminder | Implemented | `NotificationsViewModelTest.kt` |
| NOTIF-016 | Meal Reminder | Daily cooking reminder | Implemented | `NotificationsViewModelTest.kt` |
| NOTIF-017 | Plan Ready | Meal plan generated | Implemented | `NotificationsViewModelTest.kt` |
| NOTIF-018 | Achievement Unlocked | Badge earned | Implemented | `NotificationsViewModelTest.kt` |

---

## Screen Layout

### Default List View
```
┌─────────────────────────────────────┐
│  ←  Notifications         ✓✓ All   │
│─────────────────────────────────────│
│  [All]  [Unread (3)]               │
│─────────────────────────────────────│
│                                     │
│  Today                              │
│  ┌─────────────────────────────┐    │
│  │ 🎉 Festival Reminder    ●  │    │
│  │ Makar Sankranti is         │    │
│  │ tomorrow! Special recipes   │    │
│  │ available.          2h ago  │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ 🍽 Meal Plan Ready      ●  │    │
│  │ Your weekly meal plan has   │    │
│  │ been generated!     5h ago  │    │
│  └─────────────────────────────┘    │
│                                     │
│  Yesterday                          │
│  ┌─────────────────────────────┐    │
│  │ 🏆 Achievement Unlocked    │    │
│  │ You earned "7-Day Streak"  │    │
│  │ badge!              1d ago  │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ 🔔 Cooking Reminder        │    │
│  │ Time to start dinner!      │    │
│  │ Dal Tadka tonight.  1d ago  │    │
│  └─────────────────────────────┘    │
│                                     │
│  ● = unread indicator               │
│  Swipe left → [🗑 Delete]          │
└─────────────────────────────────────┘
```

### Empty State
```
┌─────────────────────────────────────┐
│  ←  Notifications         ✓✓ All   │
│─────────────────────────────────────│
│  [All]  [Unread]                    │
│─────────────────────────────────────│
│                                     │
│                                     │
│                                     │
│           🔔                        │
│                                     │
│     No notifications yet            │
│                                     │
│     You'll see festival             │
│     reminders, meal updates,        │
│     and shopping list               │
│     notifications here.             │
│                                     │
│                                     │
│                                     │
└─────────────────────────────────────┘
```

---

## Detailed Requirements

### NOTIF-001: Notifications Screen Display

| Field | Value |
|-------|-------|
| **Screen** | Notifications |
| **Element** | Full screen |
| **Trigger** | Tap notification bell |
| **Status** | Implemented |
| **Test** | `NotificationsScreenTest.kt:notificationsScreen_displaysCorrectly` |

**Acceptance Criteria:**
- Given: User taps notification bell icon
- When: Screen displays
- Then: Header shows "Notifications"
- And: List of notifications displayed
- And: Most recent at top

---

### NOTIF-004: Notification Card

| Field | Value |
|-------|-------|
| **Screen** | Notifications |
| **Element** | Notification item |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `NotificationsScreenTest.kt:notificationCard_displaysCorrectly` |

**Card Layout:**
```
┌─────────────────────────────────────┐
│ ● 🎉 Makar Sankranti in 3 days!     │
│    Plan your festive meals now.     │
│                           2 hours   │
└─────────────────────────────────────┘
```

**Card Elements:**
| Element | Position | Description |
|---------|----------|-------------|
| Unread dot | Left | Blue dot if unread |
| Icon | Left | Type indicator emoji |
| Title | Top | Summary text |
| Body | Middle | Detail text |
| Time | Right | Relative timestamp |

---

### NOTIF-005: Notification Type Icons

| Field | Value |
|-------|-------|
| **Screen** | Notifications |
| **Element** | Type icon |
| **Trigger** | Notification render |
| **Status** | Implemented |
| **Test** | `NotificationsScreenTest.kt:notificationIcon_matchesType` |

**Notification Types:**
| Type | Icon | Example |
|------|------|---------|
| Festival | 🎉 | "Diwali is coming!" |
| Meal Reminder | 🍽️ | "Time to start dinner" |
| Plan Ready | 📅 | "Your meal plan is ready" |
| Achievement | 🏆 | "You earned a badge!" |
| Tip | 💡 | "Cooking tip of the day" |
| Social | 👥 | "Friend joined leaderboard" |

---

### NOTIF-009: Unread Indicator

| Field | Value |
|-------|-------|
| **Screen** | Notifications |
| **Element** | Blue dot |
| **Trigger** | Notification unread |
| **Status** | Implemented |
| **Test** | `NotificationsScreenTest.kt:unreadIndicator_showsForUnread` |

**Acceptance Criteria:**
- Given: Notification is unread
- When: Card renders
- Then: Blue dot visible on left
- And: Card background slightly highlighted
- And: Dot disappears after tap

---

### NOTIF-010: Notification Tap Action

| Field | Value |
|-------|-------|
| **Screen** | Notifications |
| **Element** | Card tap |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `NotificationsScreenTest.kt:notificationTap_navigatesToContext` |

**Navigation by Type:**
| Type | Destination |
|------|-------------|
| Festival | Festival recipes |
| Meal Reminder | Home (today's plan) |
| Plan Ready | Home screen |
| Achievement | Stats/Achievements |
| Social | Leaderboard |

**Acceptance Criteria:**
- Given: User taps notification
- When: Notification has context
- Then: Navigate to relevant screen
- And: Mark notification as read

---

### NOTIF-011: Mark All as Read

| Field | Value |
|-------|-------|
| **Screen** | Notifications |
| **Element** | Menu option |
| **Trigger** | More menu tap |
| **Status** | Implemented |
| **Test** | `NotificationsScreenTest.kt:markAllRead_clearsUnreadIndicators` |

**Acceptance Criteria:**
- Given: User has unread notifications
- When: User taps ⋮ → "Mark all as read"
- Then: All unread dots disappear
- And: Badge count on bell icon clears

---

### NOTIF-013: Clear All Notifications

| Field | Value |
|-------|-------|
| **Screen** | Notifications |
| **Element** | Menu option |
| **Trigger** | More menu tap |
| **Status** | Implemented |
| **Test** | `NotificationsScreenTest.kt:clearAll_removesAllNotifications` |

**Acceptance Criteria:**
- Given: User has notifications
- When: User taps ⋮ → "Clear all"
- Then: Confirmation dialog appears
- And: On confirm, all notifications deleted
- And: Empty state displays

---

### NOTIF-014: Empty State

| Field | Value |
|-------|-------|
| **Screen** | Notifications |
| **Element** | Empty message |
| **Trigger** | No notifications |
| **Status** | Implemented |
| **Test** | `NotificationsScreenTest.kt:emptyState_showsMessage` |

**Empty State Content:**
```
🔔
No notifications

You're all caught up!
New notifications will appear here.
```

**Acceptance Criteria:**
- Given: No notifications exist
- When: Screen displays
- Then: Empty state with icon and message
- And: Friendly confirmation text

---

### NOTIF-015: Festival Notification

| Field | Value |
|-------|-------|
| **Screen** | Notifications |
| **Element** | Festival reminder |
| **Trigger** | Festival within 7 days |
| **Status** | Implemented |
| **Test** | `NotificationsViewModelTest.kt:festivalNotification_createdWhenUpcoming` |

**Notification Content:**
| Field | Example |
|-------|---------|
| Icon | 🎉 |
| Title | "Makar Sankranti in 3 days!" |
| Body | "Plan your festive meals now. View til-gur recipes →" |

**Acceptance Criteria:**
- Given: Festival is 7 days away
- When: Notification triggers
- Then: Created with festival name
- And: Days countdown in title
- And: Tap opens festival recipes

---

### NOTIF-016: Meal Reminder Notification

| Field | Value |
|-------|-------|
| **Screen** | Notifications |
| **Element** | Cooking reminder |
| **Trigger** | Scheduled time (default 4 PM) |
| **Status** | Implemented |
| **Test** | `NotificationsViewModelTest.kt:mealReminder_triggersAtScheduledTime` |

**Notification Content:**
| Field | Example |
|-------|---------|
| Icon | 🍽️ |
| Title | "Time to start dinner!" |
| Body | "Tonight: Palak Paneer with Butter Naan" |

**Acceptance Criteria:**
- Given: Reminder enabled in settings
- When: Scheduled time reached
- Then: Push notification sent
- And: Shows tonight's dinner
- And: Tap opens Home screen

---

### NOTIF-017: Plan Ready Notification

| Field | Value |
|-------|-------|
| **Screen** | Notifications |
| **Element** | Plan generated alert |
| **Trigger** | Meal plan generation complete |
| **Status** | Implemented |
| **Test** | `NotificationsViewModelTest.kt:planReady_notificationSent` |

**Notification Content:**
| Field | Example |
|-------|---------|
| Icon | 📅 |
| Title | "Your meal plan is ready!" |
| Body | "Week of Jan 20-26. 21 meals planned." |

**Acceptance Criteria:**
- Given: Meal plan generation requested
- When: Generation completes
- Then: Notification sent
- And: Shows week range and meal count
- And: Tap opens Home screen

---

### NOTIF-018: Achievement Unlocked Notification

| Field | Value |
|-------|-------|
| **Screen** | Notifications |
| **Element** | Badge earned alert |
| **Trigger** | Achievement criteria met |
| **Status** | Implemented |
| **Test** | `NotificationsViewModelTest.kt:achievementUnlocked_notificationSent` |

**Notification Content:**
| Field | Example |
|-------|---------|
| Icon | 🏆 |
| Title | "Achievement Unlocked!" |
| Body | "You earned the '7-Day Streak' badge!" |

**Acceptance Criteria:**
- Given: User meets achievement criteria
- When: Achievement unlocked
- Then: Notification sent immediately
- And: Badge name in body
- And: Tap opens Stats/Achievements

---

## Implementation Files

| Component | File Path |
|-----------|-----------|
| Notifications Screen | `presentation/notifications/NotificationsScreen.kt` |
| Notifications ViewModel | `presentation/notifications/NotificationsViewModel.kt` |
| Notification Card | `presentation/notifications/components/NotificationCard.kt` |
| Notification Service | `data/notification/NotificationService.kt` |

## Test Files

| Test Type | File Path |
|-----------|-----------|
| UI Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/notifications/NotificationsScreenTest.kt` |
| Unit Tests | `app/src/test/java/com/rasoiai/app/presentation/notifications/NotificationsViewModelTest.kt` |

---

*Note: This screen was identified as a gap in wireframes but implemented based on technical design.*
