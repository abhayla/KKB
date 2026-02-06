# Screen 2: Onboarding (5 Steps)

## Summary Table

| ID | Element | Behavior | Status | Test Reference |
|----|---------|----------|--------|----------------|
| ONB-001 | Onboarding Flow | 5-step wizard | Implemented | `OnboardingScreenTest.kt` |
| ONB-002 | Progress Indicator | Shows current step | Implemented | `OnboardingScreenTest.kt` |
| ONB-003 | Back Navigation | Return to previous step | Implemented | `OnboardingScreenTest.kt` |
| ONB-004 | Step 1: Household Size | Select family count | Implemented | `OnboardingScreenTest.kt` |
| ONB-005 | Family Member List | Display added members | Implemented | `OnboardingScreenTest.kt` |
| ONB-006 | Add Family Member | Open member dialog | Implemented | `OnboardingScreenTest.kt` |
| ONB-007 | Edit Family Member | Modify member details | Implemented | `OnboardingScreenTest.kt` |
| ONB-008 | Member Name Input | Enter member name | Implemented | `OnboardingScreenTest.kt` |
| ONB-009 | Member Type Selector | Adult/Child/Senior | Implemented | `OnboardingScreenTest.kt` |
| ONB-010 | Member Age Input | Enter age | Implemented | `OnboardingScreenTest.kt` |
| ONB-011 | Member Dietary Needs | Checkboxes for special needs | Implemented | `OnboardingScreenTest.kt` |
| ONB-012 | Step 2: Dietary Preferences | Primary diet selection | Implemented | `OnboardingScreenTest.kt` |
| ONB-013 | Vegetarian Option | Select vegetarian | Implemented | `OnboardingScreenTest.kt` |
| ONB-014 | Eggetarian Option | Select eggetarian | Implemented | `OnboardingScreenTest.kt` |
| ONB-015 | Non-Vegetarian Option | Select non-veg | Implemented | `OnboardingScreenTest.kt` |
| ONB-016 | Special Restrictions | Jain/Sattvic/Halal/Vegan | Implemented | `OnboardingScreenTest.kt` |
| ONB-017 | Step 3: Cuisine Preferences | Select cuisines | Implemented | `OnboardingScreenTest.kt` |
| ONB-018 | North Indian Toggle | Select North cuisine | Implemented | `OnboardingScreenTest.kt` |
| ONB-019 | South Indian Toggle | Select South cuisine | Implemented | `OnboardingScreenTest.kt` |
| ONB-020 | East Indian Toggle | Select East cuisine | Implemented | `OnboardingScreenTest.kt` |
| ONB-021 | West Indian Toggle | Select West cuisine | Implemented | `OnboardingScreenTest.kt` |
| ONB-022 | Spice Level Selector | Mild/Medium/Spicy | Implemented | `OnboardingScreenTest.kt` |
| ONB-023 | Step 4: Dislikes | Select disliked ingredients | Implemented | `OnboardingScreenTest.kt` |
| ONB-024 | Ingredient Search | Search ingredients | Implemented | `OnboardingScreenTest.kt` |
| ONB-025 | Common Dislikes Grid | Quick selection chips | Implemented | `OnboardingScreenTest.kt` |
| ONB-026 | Selected Dislikes List | Show selected items | Implemented | `OnboardingScreenTest.kt` |
| ONB-027 | Step 5: Cooking Time | Time preferences | Implemented | `OnboardingScreenTest.kt` |
| ONB-028 | Weekday Time Selector | Select weekday time | Implemented | `OnboardingScreenTest.kt` |
| ONB-029 | Weekend Time Selector | Select weekend time | Implemented | `OnboardingScreenTest.kt` |
| ONB-030 | Busy Days Selection | Multi-select days | Implemented | `OnboardingScreenTest.kt` |
| ONB-031 | Next Button | Advance to next step | Implemented | `OnboardingScreenTest.kt` |
| ONB-032 | Create Plan Button | Generate first meal plan | Implemented | `OnboardingScreenTest.kt` |
| ONB-033 | Generating Screen | Show plan generation progress | Implemented | `OnboardingScreenTest.kt` |
| ONB-034 | Save Preferences | Persist to backend | Implemented | `OnboardingViewModelTest.kt` |
| ONB-035 | Validation | Require minimum selections | Implemented | `OnboardingViewModelTest.kt` |
| ONB-036 | Entry/Skip Logic | First-time vs returning user navigation | Implemented | `OnboardingNavigationTest.kt` |

---

## Screen Layout

### Step 1: Household Size
```
┌─────────────────────────────────────┐
│  ←                          1 of 5  │
│─────────────────────────────────────│
│  ████░░░░░░░░░░░░░░░░  (20%)       │
│                                     │
│     How many people are you         │
│          cooking for?               │
│                                     │
│  ┌─────────────────────────────┐    │
│  │  4 people                 ▼ │    │
│  └─────────────────────────────┘    │
│                                     │
│  Family members:                    │
│  ┌─────────────────────────────┐    │
│  │ 👤 Amit                     │    │
│  │    Adult, 35 yrs      ✎ 🗑 │    │
│  │─────────────────────────────│    │
│  │ 👤 Priya                    │    │
│  │    Adult, 32 yrs      ✎ 🗑 │    │
│  │─────────────────────────────│    │
│  │ + Add family member         │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │          Next →             │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

### Step 2: Dietary Preferences
```
┌─────────────────────────────────────┐
│  ←                          2 of 5  │
│─────────────────────────────────────│
│  ████████░░░░░░░░░░░░  (40%)       │
│                                     │
│     What's your primary diet?       │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ ◉ Vegetarian                │    │
│  │   No meat, fish, or eggs    │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ ○ Eggetarian                │    │
│  │   Vegetarian + eggs         │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ ○ Non-Vegetarian            │    │
│  │   Includes meat and fish    │    │
│  └─────────────────────────────┘    │
│                                     │
│  Special dietary restrictions:      │
│  ☐ Jain  ☐ Sattvic                 │
│  ☐ Halal ☐ Vegan                   │
│                                     │
│  ┌─────────────────────────────┐    │
│  │          Next →             │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

### Step 3: Cuisine Preferences
```
┌─────────────────────────────────────┐
│  ←                          3 of 5  │
│─────────────────────────────────────│
│  ████████████░░░░░░░░  (60%)       │
│                                     │
│     Which cuisines do you like?     │
│        (Select all that apply)      │
│                                     │
│  ┌──────────┐  ┌──────────┐        │
│  │   🥘     │  │   🍚     │        │
│  │  NORTH   │  │  SOUTH   │        │
│  │ Punjabi, │  │  Tamil,  │        │
│  │ Mughlai  │  │  Kerala  │        │
│  │    ✓     │  │          │        │
│  └──────────┘  └──────────┘        │
│  ┌──────────┐  ┌──────────┐        │
│  │   🍛     │  │   🥗     │        │
│  │   EAST   │  │   WEST   │        │
│  │ Bengali, │  │Gujarati, │        │
│  │  Odia    │  │ Marathi  │        │
│  └──────────┘  └──────────┘        │
│                                     │
│  Spice level:                       │
│  ┌─────────────────────────────┐    │
│  │  Medium                   ▼ │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │          Next →             │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

### Step 4: Disliked Ingredients
```
┌─────────────────────────────────────┐
│  ←                          4 of 5  │
│─────────────────────────────────────│
│  ████████████████░░░░  (80%)       │
│                                     │
│   Any ingredients you dislike?      │
│       (Select all that apply)       │
│                                     │
│  ┌─────────────────────────────┐    │
│  │  Search ingredients...    + │    │
│  └─────────────────────────────┘    │
│                                     │
│  Common dislikes:                   │
│  ┌────────┐ ┌────────┐ ┌────────┐  │
│  │ Karela │ │Lauki   │ │Baingan │  │
│  │(Bitter │ │(Bottle │ │(Egg-   │  │
│  │ gourd) │ │ gourd) │ │ plant) │  │
│  └────────┘ └────────┘ └────────┘  │
│  ┌────────┐ ┌────────┐ ┌────────┐  │
│  │ Tinda  │ │Parwal  │ │ Arbi   │  │
│  │(Apple  │ │(Pointed│ │(Taro   │  │
│  │ gourd) │ │ gourd) │ │ root)  │  │
│  └────────┘ └────────┘ └────────┘  │
│                                     │
│  Selected: Karela, Baingan          │
│                                     │
│  ┌─────────────────────────────┐    │
│  │          Next →             │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

### Step 5: Cooking Time
```
┌─────────────────────────────────────┐
│  ←                          5 of 5  │
│─────────────────────────────────────│
│  ████████████████████  (100%)      │
│                                     │
│    How much time do you have        │
│          for cooking?               │
│                                     │
│  Weekdays:                          │
│  ┌─────────────────────────────┐    │
│  │  30 minutes               ▼ │    │
│  └─────────────────────────────┘    │
│                                     │
│  Weekends:                          │
│  ┌─────────────────────────────┐    │
│  │  60 minutes               ▼ │    │
│  └─────────────────────────────┘    │
│                                     │
│  Busy days (quick meals only):      │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐  │
│  │ Mon │ │ Tue │ │ Wed │ │ Thu │  │
│  └─────┘ └─────┘ └─────┘ └─────┘  │
│  ┌─────┐ ┌─────┐ ┌─────┐          │
│  │ Fri │ │ Sat │ │ Sun │          │
│  └─────┘ └─────┘ └─────┘          │
│                                     │
│  ┌─────────────────────────────┐    │
│  │     Create My Meal Plan     │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

### Generating Screen
```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│                                     │
│           ┌──────────┐              │
│           │  🍲 ◯   │              │
│           │ Logo +   │              │
│           │ Spinner  │              │
│           └──────────┘              │
│                                     │
│       Creating your perfect         │
│           meal plan...              │
│                                     │
│  ✓ Analyzing preferences            │
│  ✓ Checking festivals               │
│  ◯ Generating recipes               │
│  ○ Building grocery list             │
│                                     │
│                                     │
│                                     │
└─────────────────────────────────────┘

Legend: ✓ = done, ◯ = in progress, ○ = pending
```

---

## Detailed Requirements

### ONB-001: Onboarding Flow Structure

| Field | Value |
|-------|-------|
| **Screen** | Onboarding |
| **Element** | 5-step wizard |
| **Trigger** | First-time auth complete |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:onboarding_displays5Steps` |

**Acceptance Criteria:**
- Given: First-time user completed authentication
- When: Onboarding screen displays
- Then: Step 1 (Household Size) is shown
- And: Progress indicator shows "1 of 5"
- And: User can navigate through all 5 steps

**Steps Overview:**
| Step | Title | Purpose |
|------|-------|---------|
| 1 | Household Size | Family count and members |
| 2 | Dietary Preferences | Veg/Non-veg and restrictions |
| 3 | Cuisine Preferences | Regional cuisine selection |
| 4 | Disliked Ingredients | Ingredients to avoid |
| 5 | Cooking Time | Time constraints |

---

### ONB-002: Progress Indicator

| Field | Value |
|-------|-------|
| **Screen** | Onboarding |
| **Element** | Step progress bar |
| **Trigger** | Each step transition |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:progressIndicator_updatesOnStepChange` |

**Acceptance Criteria:**
- Given: User is on any onboarding step
- When: Step changes
- Then: Progress bar fills proportionally
- And: Step counter shows "X of 5"
- And: Completed steps show filled indicator

---

### ONB-003: Back Navigation

| Field | Value |
|-------|-------|
| **Screen** | Onboarding |
| **Element** | Back button/gesture |
| **Trigger** | User taps back or swipes |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:backButton_returnsToPreviousStep` |

**Acceptance Criteria:**
- Given: User is on step 2-5
- When: User taps back arrow or system back
- Then: Previous step displays
- And: User's selections are preserved
- And: On step 1, back exits to Auth screen

---

### ONB-004: Step 1 - Household Size

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 1 |
| **Element** | Household size selector |
| **Trigger** | Step 1 display |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:step1_householdSizeSelection` |

**Acceptance Criteria:**
- Given: User is on Step 1
- When: Screen displays
- Then: Question "How many people are you cooking for?" appears
- And: Dropdown with options 1-8+ people
- And: Family members list section below

---

### ONB-005: Family Member List Display

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 1 |
| **Element** | Family members list |
| **Trigger** | Members added |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:familyList_displaysMembersCorrectly` |

**Acceptance Criteria:**
- Given: User has added family members
- When: Step 1 displays
- Then: Each member shows in list with:
  - Icon based on type (Adult/Child/Senior)
  - Member name
  - Edit button
- And: "+ Add family member" button at bottom

---

### ONB-006: Add Family Member Dialog

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 1 |
| **Element** | Add member dialog |
| **Trigger** | Tap "+ Add family member" |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:addMemberDialog_opensOnTap` |

**Acceptance Criteria:**
- Given: User is on Step 1
- When: User taps "+ Add family member"
- Then: Modal dialog appears with:
  - Name text field
  - Type dropdown (Adult/Child/Senior)
  - Age field
  - Special dietary needs checkboxes
  - Cancel and Add buttons

---

### ONB-011: Member Dietary Needs

| Field | Value |
|-------|-------|
| **Screen** | Onboarding - Add Member Dialog |
| **Element** | Dietary checkboxes |
| **Trigger** | Dialog open |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:memberDialog_showsDietaryOptions` |

**Dietary Options:**
- Diabetic
- Low oil
- No spicy
- Soft food
- Low salt
- High protein
- Low carb

**Acceptance Criteria:**
- Given: Add member dialog is open
- When: User views dietary section
- Then: All dietary options are checkboxes
- And: Multiple can be selected
- And: Available for ALL member types

---

### ONB-012: Step 2 - Dietary Preferences

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 2 |
| **Element** | Primary diet selection |
| **Trigger** | Step 2 display |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:step2_dietaryPreferencesDisplay` |

**Acceptance Criteria:**
- Given: User navigated to Step 2
- When: Screen displays
- Then: Question "What's your primary diet?" appears
- And: Three radio options:
  - Vegetarian (No meat, fish, or eggs)
  - Eggetarian (Vegetarian + eggs)
  - Non-Vegetarian (All foods)
- And: Special restrictions section below

---

### ONB-016: Special Dietary Restrictions

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 2 |
| **Element** | Special restrictions checkboxes |
| **Trigger** | Step 2 display |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:step2_specialRestrictionsSelectable` |

**Restriction Options:**
| Option | Description |
|--------|-------------|
| Jain | No root vegetables |
| Sattvic | No onion/garlic |
| Halal | Halal meat only |
| Vegan | No animal products |

**Acceptance Criteria:**
- Given: User is on Step 2
- When: User views special restrictions
- Then: Each restriction is a checkbox
- And: Multiple can be selected
- And: Descriptions explain each restriction

---

### ONB-017: Step 3 - Cuisine Preferences

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 3 |
| **Element** | Cuisine zone selection |
| **Trigger** | Step 3 display |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:step3_cuisinePreferencesDisplay` |

**Acceptance Criteria:**
- Given: User navigated to Step 3
- When: Screen displays
- Then: Question "Which cuisines do you like?" appears
- And: 4 cuisine zone cards in 2x2 grid
- And: Each card toggleable with checkmark
- And: Spice level selector below

**Cuisine Zones:**
| Zone | Examples |
|------|----------|
| North | Punjabi, Mughlai |
| South | Tamil, Kerala |
| East | Bengali, Odia |
| West | Gujarati, Maharashtrian |

---

### ONB-022: Spice Level Selector

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 3 |
| **Element** | Spice level dropdown |
| **Trigger** | Step 3 display |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:step3_spiceLevelSelection` |

**Options:**
- Mild
- Medium
- Spicy
- Very Spicy

**Acceptance Criteria:**
- Given: User is on Step 3
- When: User taps spice level dropdown
- Then: Options display in dropdown
- And: Selected option shows in field
- And: Default is "Medium"

---

### ONB-023: Step 4 - Disliked Ingredients

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 4 |
| **Element** | Dislike selection |
| **Trigger** | Step 4 display |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:step4_dislikedIngredientsDisplay` |

**Acceptance Criteria:**
- Given: User navigated to Step 4
- When: Screen displays
- Then: Question "Any ingredients you dislike?" appears
- And: Search field at top
- And: Common dislikes grid below
- And: Selected items shown in summary

---

### ONB-025: Common Dislikes Grid

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 4 |
| **Element** | Quick selection chips |
| **Trigger** | Step 4 display |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:step4_commonDislikesSelectable` |

**Common Options:**
| English | Hindi |
|---------|-------|
| Karela | Bitter Gourd |
| Lauki | Bottle Gourd |
| Turai | Ridge Gourd |
| Baingan | Eggplant |
| Bhindi | Okra |
| Arbi | Colocasia |
| Coriander | Dhania |
| Methi | Fenugreek |
| Mushroom | Mushroom |

**Acceptance Criteria:**
- Given: User is on Step 4
- When: User taps a chip
- Then: Chip toggles selected state
- And: Selected chips show checkmark
- And: Item added to "Selected" summary

---

### ONB-027: Step 5 - Cooking Time

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 5 |
| **Element** | Time preferences |
| **Trigger** | Step 5 display |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:step5_cookingTimeDisplay` |

**Acceptance Criteria:**
- Given: User navigated to Step 5
- When: Screen displays
- Then: Question "How much time do you have for cooking?" appears
- And: Weekday time dropdown
- And: Weekend time dropdown
- And: Busy days multi-select

---

### ONB-028: Weekday Time Selector

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 5 |
| **Element** | Weekday dropdown |
| **Trigger** | Step 5 display |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:step5_weekdayTimeSelection` |

**Options:**
- 15 minutes
- 30 minutes
- 45 minutes
- 60 minutes
- 90 minutes

**Acceptance Criteria:**
- Given: User is on Step 5
- When: User taps weekday dropdown
- Then: Time options display
- And: Default is 30 minutes

---

### ONB-030: Busy Days Selection

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 5 |
| **Element** | Day toggle buttons |
| **Trigger** | Step 5 display |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:step5_busyDaysSelection` |

**Acceptance Criteria:**
- Given: User is on Step 5
- When: User views busy days section
- Then: 7 day buttons (Mon-Sun) display
- And: Each toggleable independently
- And: Selected days highlighted
- And: Quick meals suggested for busy days

---

### ONB-032: Create Meal Plan Button

| Field | Value |
|-------|-------|
| **Screen** | Onboarding Step 5 |
| **Element** | Create plan button |
| **Trigger** | User taps |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:createPlanButton_triggersMealGeneration` |

**Acceptance Criteria:**
- Given: User completed all steps
- When: User taps "CREATE MY MEAL PLAN"
- Then: Generating screen displays
- And: Preferences saved to backend
- And: Meal plan generation initiated

---

### ONB-033: Generating Screen

| Field | Value |
|-------|-------|
| **Screen** | Onboarding - Generating |
| **Element** | Progress display |
| **Trigger** | Create plan tapped |
| **Status** | Implemented |
| **Test** | `OnboardingScreenTest.kt:generatingScreen_showsProgress` |

**Acceptance Criteria:**
- Given: User tapped Create Plan
- When: Generation in progress
- Then: Animated logo displays
- And: "Creating your perfect meal plan..." text
- And: Progress steps show:
  - Analyzing preferences
  - Checking festivals
  - Generating recipes...
  - Building grocery list
- And: Navigates to Home on completion

---

### ONB-034: Save Preferences to Backend

| Field | Value |
|-------|-------|
| **Screen** | Onboarding |
| **Element** | Backend sync |
| **Trigger** | Create plan tapped |
| **Status** | Implemented |
| **Test** | `OnboardingViewModelTest.kt:savePreferences_sendsToBackend` |

**Acceptance Criteria:**
- Given: User completed onboarding
- When: Create plan tapped
- Then: All preferences sent to `PUT /api/v1/users/preferences`
- And: Family members sent to `POST /api/v1/users/family`
- And: Local cache updated

---

### ONB-035: Form Validation

| Field | Value |
|-------|-------|
| **Screen** | Onboarding |
| **Element** | Validation rules |
| **Trigger** | Next/Create tapped |
| **Status** | Implemented |
| **Test** | `OnboardingViewModelTest.kt:validation_enforced` |

**Validation Rules:**
| Step | Requirement |
|------|-------------|
| 1 | Household size > 0 |
| 2 | Primary diet selected |
| 3 | At least 1 cuisine selected |
| 4 | Optional (can skip) |
| 5 | Weekday and weekend times selected |

**Acceptance Criteria:**
- Given: User on any step
- When: Required fields not completed
- Then: Next button disabled OR shows error
- And: User cannot proceed until valid

---

## Implementation Files

| Component | File Path |
|-----------|-----------|
| Onboarding Screen | `presentation/onboarding/OnboardingScreen.kt` |
| Onboarding ViewModel | `presentation/onboarding/OnboardingViewModel.kt` |
| Onboarding Steps | `presentation/onboarding/OnboardingSteps.kt` |
| Family Member Dialog | `presentation/onboarding/components/FamilyMemberDialog.kt` |

## Test Files

| Test Type | File Path |
|-----------|-----------|
| UI Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/onboarding/OnboardingScreenTest.kt` |
| Unit Tests | `app/src/test/java/com/rasoiai/app/presentation/onboarding/OnboardingViewModelTest.kt` |
| E2E Flow | `app/src/androidTest/java/com/rasoiai/app/e2e/flows/OnboardingFlowTest.kt` |

---

### ONB-036: Onboarding Entry/Skip Logic

| Field | Value |
|-------|-------|
| **Screen** | Onboarding / Navigation |
| **Element** | Entry decision logic |
| **Trigger** | User authenticated |
| **Status** | Implemented |
| **Test** | `OnboardingNavigationTest.kt` |
| **GitHub Issue** | [#41](https://github.com/abhayla/KKB/issues/41) |

**Acceptance Criteria:**

**Scenario A: First-time user sees onboarding**
- Given: User completed Google Sign-In
- And: User has NOT previously completed onboarding (`isOnboarded = false`)
- When: Auth flow completes
- Then: Navigate to Onboarding Step 1
- And: User must complete all 5 steps

**Scenario B: Returning user skips onboarding**
- Given: User completed Google Sign-In
- And: User HAS previously completed onboarding (`isOnboarded = true`)
- When: Auth flow completes
- Then: Navigate directly to Home screen
- And: Onboarding is NOT shown

**Scenario C: App restart for onboarded user**
- Given: User previously completed onboarding
- And: App was closed/restarted
- When: App launches (Splash screen)
- Then: Navigate directly to Home screen (after splash delay)
- And: Onboarding is NOT shown

**Scenario D: App restart for non-onboarded user**
- Given: User authenticated but did not complete onboarding
- And: App was closed/restarted
- When: App launches (Splash screen)
- Then: Navigate to Onboarding screen (after splash delay)
- And: User must complete onboarding

**State Persistence:**
- `isOnboarded` flag stored in Android DataStore (`UserPreferencesDataStore.kt`)
- Flag set to `true` when user taps "Create My Meal Plan" on Step 5
- Flag persists across app restarts

**Implementation Files:**
| Component | File | Logic |
|-----------|------|-------|
| State Storage | `UserPreferencesDataStore.kt:128` | `isOnboarded: Flow<Boolean>` |
| Splash Decision | `SplashViewModel.kt:82-87` | `if (!isOnboarded && !hasMealPlan) → Onboarding` |
| Post-Auth Decision | `AuthViewModel.kt:133-138` | `if (isOnboarded) → Home else → Onboarding` |
| Set Complete | `OnboardingViewModel.kt:333` | `saveOnboardingComplete()` sets flag |

---

*Requirements derived from wireframe: `03-onboarding.md`*
