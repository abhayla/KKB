# Screen 9: Settings

## Summary Table

| ID | Element | Behavior | Status | Test Reference |
|----|---------|----------|--------|----------------|
| SET-001 | Settings Screen | Display all settings | Implemented | `SettingsScreenTest.kt` |
| SET-002 | Back Navigation | Return to previous | Implemented | `SettingsScreenTest.kt` |
| SET-003 | Profile Section | User info and edit | Implemented | `SettingsScreenTest.kt` |
| SET-004 | Profile Avatar | Display user photo | Implemented | `SettingsScreenTest.kt` |
| SET-005 | User Name | Display name | Implemented | `SettingsScreenTest.kt` |
| SET-006 | User Email | Display email | Implemented | `SettingsScreenTest.kt` |
| SET-007 | Edit Profile Button | Open profile edit | Implemented | `SettingsScreenTest.kt` |
| SET-008 | Family Section | Family member list | Implemented | `SettingsScreenTest.kt` |
| SET-009 | Family Member Row | Display member | Implemented | `SettingsScreenTest.kt` |
| SET-010 | Edit Member Button | Open member edit | Implemented | `SettingsScreenTest.kt` |
| SET-011 | Add Family Member | Add new member | Implemented | `SettingsScreenTest.kt` |
| SET-012 | Meal Preferences Section | Dietary settings | Implemented | `SettingsScreenTest.kt` |
| SET-013 | Dietary Restrictions | Navigate to detail | Implemented | `SettingsScreenTest.kt` |
| SET-014 | Disliked Ingredients | Navigate to detail | Implemented | `SettingsScreenTest.kt` |
| SET-015 | Cuisine Preferences | Navigate to detail | Implemented | `SettingsScreenTest.kt` |
| SET-016 | Cooking Time | Navigate to detail | Implemented | `SettingsScreenTest.kt` |
| SET-017 | Spice Level | Navigate to detail | Implemented | `SettingsScreenTest.kt` |
| SET-018 | App Settings Section | App configuration | Implemented | `SettingsScreenTest.kt` |
| SET-019 | Notifications | Navigate to config | Implemented | `SettingsScreenTest.kt` |
| SET-020 | Dark Mode Toggle | Theme selector | Implemented | `SettingsScreenTest.kt` |
| SET-021 | Units & Measurements | Navigate to config | Implemented | `SettingsScreenTest.kt` |
| SET-022 | Social Section | Social features | Implemented | `SettingsScreenTest.kt` |
| SET-023 | Friends & Leaderboard | Navigate to social | Implemented | `SettingsScreenTest.kt` |
| SET-024 | Connected Accounts | Manage connections | Implemented | `SettingsScreenTest.kt` |
| SET-025 | Share App | Invite friends | Implemented | `SettingsScreenTest.kt` |
| SET-026 | Support Section | Help and legal | Implemented | `SettingsScreenTest.kt` |
| SET-027 | Help & FAQ | Open help | Implemented | `SettingsScreenTest.kt` |
| SET-028 | Contact Us | Open contact form | Implemented | `SettingsScreenTest.kt` |
| SET-029 | Rate App | Open Play Store | Implemented | `SettingsScreenTest.kt` |
| SET-030 | Privacy Policy | Open policy | Implemented | `SettingsScreenTest.kt` |
| SET-031 | Terms of Service | Open terms | Implemented | `SettingsScreenTest.kt` |
| SET-032 | Sign Out Button | Log out user | Implemented | `SettingsScreenTest.kt` |
| SET-033 | App Version | Display version | Implemented | `SettingsScreenTest.kt` |

---

## Detailed Requirements

### SET-001: Settings Screen Display

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | Full screen |
| **Trigger** | Navigate from Home → Profile |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:settingsScreen_displaysCorrectly` |

**Acceptance Criteria:**
- Given: User taps profile icon
- When: Settings screen displays
- Then: Header shows "Settings"
- And: All sections visible with scrolling
- And: Sign Out at bottom

**Sections:**
1. Profile (avatar, name, email)
2. Family
3. Meal Preferences
4. App Settings
5. Social
6. Support

---

### SET-003: Profile Section

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | Profile card |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:profileSection_displaysUserInfo` |

**Layout:**
```
┌─────────────────────────────────────┐
│         ┌───────────┐               │
│         │  [Avatar] │               │
│         └───────────┘               │
│         Priya Sharma                │
│     priya.sharma@gmail.com          │
│           [Edit Profile]            │
└─────────────────────────────────────┘
```

**Acceptance Criteria:**
- Given: Settings screen displayed
- When: Profile section renders
- Then: Avatar from Google account shown
- And: User's name displayed
- And: Email displayed
- And: Edit Profile button visible

---

### SET-007: Edit Profile Button

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | Edit button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:editProfileButton_opensEditScreen` |

**Acceptance Criteria:**
- Given: Profile section displayed
- When: User taps "Edit Profile"
- Then: Profile edit screen opens
- And: Can change name
- And: Can change avatar
- And: Email is read-only (from Google)

---

### SET-008: Family Section

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | Family members list |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:familySection_displaysMemberList` |

**Section Header:** "FAMILY"

**Member Row Layout:**
```
│ 👤 Priya (You)                 [Edit] │
│    Vegetarian                         │
├───────────────────────────────────────┤
│ 👤 Rahul                       [Edit] │
│    Non-vegetarian                     │
├───────────────────────────────────────┤
│ 👧 Ananya (8 yrs)              [Edit] │
│    No spicy                           │
├───────────────────────────────────────┤
│ + Add family member                   │
```

**Acceptance Criteria:**
- Given: Family section displayed
- When: Members render
- Then: Each member shows icon, name, dietary info
- And: Edit button on each row
- And: "+ Add family member" at bottom

---

### SET-012: Meal Preferences Section

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | Preferences menu |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:mealPreferencesSection_displaysAllOptions` |

**Section Header:** "MEAL PREFERENCES"

**Menu Items:**
| Item | Navigation |
|------|------------|
| Dietary Restrictions | Edit Veg/Jain/etc. |
| Disliked Ingredients | Edit dislikes |
| Cuisine Preferences | Edit regions |
| Cooking Time | Edit time limits |
| Spice Level | Edit spice preference |

**Row Layout:**
```
│ Dietary Restrictions              ▶  │
```

**Acceptance Criteria:**
- Given: Meal Preferences section displayed
- When: User taps any item
- Then: Navigates to detail screen
- And: Can modify preferences
- And: Changes saved and synced

---

### SET-018: App Settings Section

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | App config menu |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:appSettingsSection_displaysAllOptions` |

**Section Header:** "APP SETTINGS"

**Menu Items:**
| Item | Type | Description |
|------|------|-------------|
| Notifications | Navigate | Configure push notifications |
| Dark Mode | Toggle | System/Light/Dark |
| Units & Measurements | Navigate | Metric/Indian |

---

### SET-020: Dark Mode Toggle

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | Theme selector |
| **Trigger** | User interaction |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:darkModeToggle_changesTheme` |

**Options:**
| Value | Behavior |
|-------|----------|
| System | Follow device setting |
| Light | Always light theme |
| Dark | Always dark theme |

**Row Layout:**
```
│ Dark Mode                    [System]│
```

**Acceptance Criteria:**
- Given: App Settings displayed
- When: User taps Dark Mode
- Then: Dropdown shows options
- And: Selection immediately applies
- And: Persists across sessions

---

### SET-021: Units & Measurements

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | Units config |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:unitsAndMeasurements_opensConfig` |

**Configuration Options:**

**Volume:**
| Option | Examples |
|--------|----------|
| Metric | ml, L |
| US | cups, fl oz |
| Indian | katori, glass |

**Weight:**
| Option | Examples |
|--------|----------|
| Metric | g, kg |
| US | oz, lbs |

**Small Measurements:**
| Option | Examples |
|--------|----------|
| Metric | tsp, tbsp |
| Indian | chammach |

**Acceptance Criteria:**
- Given: Units screen open
- When: User selects preference
- Then: All recipes display in chosen units
- And: Grocery list uses chosen units

---

### SET-022: Social Section

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | Social menu |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:socialSection_displaysAllOptions` |

**Section Header:** "SOCIAL"

**Menu Items:**
| Item | Navigation |
|------|------------|
| Friends & Leaderboard | View friends and rankings |
| Connected Accounts | Manage linked services |
| Share App with Friends | Invite via share |

---

### SET-023: Friends & Leaderboard

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | Friends screen |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:friendsAndLeaderboard_opensScreen` |

**Screen Content:**
```
┌─────────────────────────────────────┐
│  ←  Friends & Leaderboard           │
├─────────────────────────────────────┤
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
│  └─────────────────────────────────┘│
│                                     │
│  MY FRIENDS (5)                     │
│  ...                                │
└─────────────────────────────────────┘
```

---

### SET-026: Support Section

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | Support menu |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:supportSection_displaysAllOptions` |

**Section Header:** "SUPPORT"

**Menu Items:**
| Item | Action |
|------|--------|
| Help & FAQ | Open help center |
| Contact Us | Open contact form |
| Rate App on Play Store | Open Play Store page |
| Privacy Policy | Open policy page |
| Terms of Service | Open terms page |

---

### SET-032: Sign Out Button

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | Sign out button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:signOutButton_logsOutUser` |

**Acceptance Criteria:**
- Given: User scrolls to bottom
- When: User taps "Sign Out"
- Then: Confirmation dialog appears
- And: On confirm:
  - JWT token cleared
  - Local cache cleared
  - Navigate to Auth screen

---

### SET-033: App Version Display

| Field | Value |
|-------|-------|
| **Screen** | Settings |
| **Element** | Version text |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `SettingsScreenTest.kt:appVersion_displaysCorrectly` |

**Layout:**
```
   App Version 1.0.0
```

**Acceptance Criteria:**
- Given: Settings screen displayed
- When: User scrolls to bottom
- Then: Version number visible below Sign Out
- And: Centered, muted text

---

## Implementation Files

| Component | File Path |
|-----------|-----------|
| Settings Screen | `presentation/settings/SettingsScreen.kt` |
| Settings ViewModel | `presentation/settings/SettingsViewModel.kt` |
| Profile Section | `presentation/settings/components/ProfileSection.kt` |
| Family Section | `presentation/settings/components/FamilySection.kt` |
| Settings Row | `presentation/settings/components/SettingsRow.kt` |

## Test Files

| Test Type | File Path |
|-----------|-----------|
| UI Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/settings/SettingsScreenTest.kt` |
| Unit Tests | `app/src/test/java/com/rasoiai/app/presentation/settings/SettingsViewModelTest.kt` |
| E2E Flow | `app/src/androidTest/java/com/rasoiai/app/e2e/flows/SettingsFlowTest.kt` |

---

*Requirements derived from wireframe: `12-settings.md`*
