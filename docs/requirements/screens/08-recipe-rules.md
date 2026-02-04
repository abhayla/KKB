# Screen 8: Recipe Rules

## Summary Table

| ID | Element | Behavior | Status | Test Reference |
|----|---------|----------|--------|----------------|
| RULE-001 | Recipe Rules Screen | Display rule management | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-002 | Back Navigation | Return to Settings/Home | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-003 | Tab Bar | 4 rule categories | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-004 | Recipe Tab | Recipe include/exclude | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-005 | Ingredient Tab | Ingredient include/exclude | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-006 | Meal-Slot Tab | Recipe to meal binding | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-007 | Nutrition Tab | Weekly nutrition goals | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-008 | Rules List | Display existing rules | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-009 | Rule Card | Display rule details | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-010 | Rule Type Badge | Include/Exclude indicator | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-011 | Rule Frequency | Show occurrence pattern | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-012 | Rule Enforcement | Required/Preferred badge | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-013 | Rule Active Toggle | Enable/disable rule | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-014 | Edit Rule Button | Open edit sheet | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-015 | Add Rule Button | Open add sheet | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-016 | Add Rule Sheet | Rule creation form | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-017 | Rule Type Selector | Include/Exclude radio | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-018 | Recipe/Ingredient Search | Find target item | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-019 | Frequency Selector | Set occurrence | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-020 | Day Selector | Specific days option | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-021 | Enforcement Selector | Required/Preferred | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-022 | Meal Slot Selector | For meal-slot rules | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-023 | Save Rule Button | Persist rule | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-024 | Delete Rule | Remove rule | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-025 | Nutrition Goal Card | Display goal progress | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-026 | Goal Progress Bar | Visual completion | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-027 | Add Goal Sheet | Nutrition goal form | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-028 | Food Category Selector | Select food group | Implemented | `RecipeRulesScreenTest.kt` |
| RULE-029 | Weekly Target Input | Set frequency | Implemented | `RecipeRulesScreenTest.kt` |

---

## Detailed Requirements

### RULE-001: Recipe Rules Screen

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules |
| **Element** | Full screen |
| **Trigger** | Settings → Recipe Rules OR Home → ⚙️ |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:recipeRulesScreen_displaysCorrectly` |

**Access Points:**
1. Settings Screen → "Recipe Rules" under Meal Preferences
2. Home Screen → ⚙️ gear icon (quick access)

**Acceptance Criteria:**
- Given: User navigates to Recipe Rules
- When: Screen displays
- Then: Header shows "Recipe Rules"
- And: 4-tab bar below header
- And: Content area for rules
- And: "+ Add" button at bottom

---

### RULE-003: Tab Bar

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules |
| **Element** | 4-tab selector |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:tabBar_switchesBetweenTabs` |

**Tabs:**
| Tab | Icon | Purpose |
|-----|------|---------|
| Recipe | 📖 | Include/exclude specific recipes |
| Ingredient | 🥕 | Include/exclude specific ingredients |
| Meal-Slot | 🍽️ | Lock recipes to specific meal times |
| Nutrition | 🥗 | Weekly nutrition goals by food category |

**Acceptance Criteria:**
- Given: Tab bar displayed
- When: User taps tab
- Then: Tab becomes selected (underlined)
- And: Content area updates
- And: Add button context changes

---

### RULE-004: Recipe Tab

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules |
| **Element** | Recipe rules list |
| **Trigger** | Recipe tab selected |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:recipeTab_displaysRecipeRules` |

**Example Rules:**
| Rule | Type | Frequency |
|------|------|-----------|
| Include "Rajma" | INCLUDE | 1x per week |
| Include "Chai" | INCLUDE | Every day breakfast |
| Exclude "Fish Curry" | EXCLUDE | Never |

**Acceptance Criteria:**
- Given: Recipe tab selected
- When: Content renders
- Then: "MY RECIPE RULES (N)" header
- And: List of recipe rules
- And: "+ ADD RECIPE RULE" button

---

### RULE-005: Ingredient Tab

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules |
| **Element** | Ingredient rules list |
| **Trigger** | Ingredient tab selected |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:ingredientTab_displaysIngredientRules` |

**Example Rules:**
| Rule | Type | Frequency |
|------|------|-----------|
| Include "Spinach" | INCLUDE | 2x per week |
| Exclude "Bitter Gourd" | EXCLUDE | Never |

**Acceptance Criteria:**
- Given: Ingredient tab selected
- When: Content renders
- Then: "MY INGREDIENT RULES (N)" header
- And: List of ingredient rules
- And: "+ ADD INGREDIENT RULE" button

---

### RULE-006: Meal-Slot Tab

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules |
| **Element** | Meal-slot rules list |
| **Trigger** | Meal-Slot tab selected |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:mealSlotTab_displaysMealSlotRules` |

**Example Rules:**
| Rule | Slot | Days |
|------|------|------|
| "Chai" → Breakfast | Breakfast | Every day |
| "Dosa" → Weekend Breakfast | Breakfast | Sat & Sun |

**Acceptance Criteria:**
- Given: Meal-Slot tab selected
- When: Content renders
- Then: "MY MEAL-SLOT RULES (N)" header
- And: List showing recipe → slot bindings
- And: "+ ADD MEAL-SLOT RULE" button

---

### RULE-007: Nutrition Tab

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules |
| **Element** | Nutrition goals list |
| **Trigger** | Nutrition tab selected |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:nutritionTab_displaysNutritionGoals` |

**Example Goals:**
| Food Category | Target | Current |
|---------------|--------|---------|
| Green leafy vegetables | 7 days | 4/7 |
| Citrus/Vitamin C | 5 times | 2/5 |
| Iron-rich foods | 6 times | 5/6 |

**Acceptance Criteria:**
- Given: Nutrition tab selected
- When: Content renders
- Then: "WEEKLY NUTRITION GOALS" header
- And: Goals with progress bars
- And: "+ ADD NUTRITION GOAL" button

---

### RULE-009: Rule Card

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules |
| **Element** | Individual rule display |
| **Trigger** | Rules list render |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:ruleCard_displaysAllElements` |

**Card Layout:**
```
┌─────────────────────────────────────┐
│ 📖 Include "Rajma" weekly           │
│    At least 1x per week             │
│    [Required] ● Active        [✎]  │
└─────────────────────────────────────┘
```

**Card Elements:**
| Element | Description |
|---------|-------------|
| Icon | 📖 (recipe), 🥕 (ingredient), 🍽️ (meal-slot) |
| Action | Include/Exclude text |
| Target | Recipe or ingredient name |
| Frequency | Occurrence description |
| Enforcement | [Required] or [Preferred] badge |
| Status | ● Active (green) or ○ Inactive (gray) |
| Edit | [✎] button |

---

### RULE-012: Enforcement Badge

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules |
| **Element** | Badge on rule card |
| **Trigger** | Rule display |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:enforcementBadge_displaysCorrectly` |

**Badge Types:**
| Badge | Meaning | Behavior |
|-------|---------|----------|
| [Required] | Must include/exclude | AI strictly enforces |
| [Preferred] | Try to include/exclude | AI attempts but may skip |

**Acceptance Criteria:**
- Given: Rule has enforcement set
- When: Card renders
- Then: Badge shows appropriate label
- And: Badge styled distinctly (color/border)

---

### RULE-015: Add Rule Button

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules |
| **Element** | "+ ADD" button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:addButton_opensAddSheet` |

**Button Labels by Tab:**
| Tab | Button Text |
|-----|-------------|
| Recipe | + ADD RECIPE RULE |
| Ingredient | + ADD INGREDIENT RULE |
| Meal-Slot | + ADD MEAL-SLOT RULE |
| Nutrition | + ADD NUTRITION GOAL |

**Acceptance Criteria:**
- Given: Any tab displayed
- When: User taps add button
- Then: Appropriate bottom sheet opens
- And: Sheet pre-configured for tab type

---

### RULE-016: Add Rule Bottom Sheet

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules |
| **Element** | Rule creation modal |
| **Trigger** | Add button tapped |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:addRuleSheet_displaysForm` |

**Sheet Fields (Recipe/Ingredient):**
| Field | Type | Options |
|-------|------|---------|
| Rule Type | Radio | Include / Exclude |
| Target | Search | Recipe or Ingredient search |
| Frequency | Dropdown + Number | "At least [N] times per [period]" |
| Specific Days | Multi-select | Mon-Sun checkboxes |
| Enforcement | Radio | Required / Preferred |

**Acceptance Criteria:**
- Given: Add sheet open
- When: User fills fields
- Then: Save Rule enabled when valid
- And: Cancel closes without saving

---

### RULE-018: Recipe/Ingredient Search

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules - Add Sheet |
| **Element** | Search input |
| **Trigger** | User types |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:search_findsItems` |

**Acceptance Criteria:**
- Given: Search field focused
- When: User types
- Then: Results filter in real-time
- And: Suggestions shown below
- And: Common items pre-suggested

**Suggestions (Recipe):**
- Rajma, Dal Makhani, Chole, Paneer Butter Masala

**Suggestions (Ingredient):**
- Spinach, Karela, Paneer, Chicken

---

### RULE-019: Frequency Selector

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules - Add Sheet |
| **Element** | Frequency inputs |
| **Trigger** | Form display |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:frequencySelector_setsOccurrence` |

**Frequency Format:**
```
At least [N] times per [period]
```

**Options:**
| Field | Values |
|-------|--------|
| N | 1, 2, 3, 4, 5, 6, 7 |
| Period | day, week, month |

**Acceptance Criteria:**
- Given: Frequency section displayed
- When: User selects values
- Then: Preview updates
- And: "1x per week" = once weekly

---

### RULE-022: Meal Slot Selector (Meal-Slot Tab)

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules - Meal-Slot Sheet |
| **Element** | Slot dropdown |
| **Trigger** | Meal-slot rule creation |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:mealSlotSelector_bindsToSlot` |

**Slot Options:**
| Slot | Description |
|------|-------------|
| Breakfast | Morning meal |
| Lunch | Midday meal |
| Snacks | Afternoon snack |
| Dinner | Evening meal |
| Weekend Breakfast | Sat/Sun only |
| Weekend Dinner | Sat/Sun only |

**Acceptance Criteria:**
- Given: Meal-slot sheet open
- When: User selects slot
- Then: Recipe bound to that slot
- And: Day options adjust for weekend slots

---

### RULE-025: Nutrition Goal Card

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules - Nutrition Tab |
| **Element** | Goal display card |
| **Trigger** | Nutrition tab selected |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:nutritionGoalCard_displaysProgress` |

**Card Layout:**
```
┌─────────────────────────────────────┐
│ 🥗 Green leafy vegetables           │
│    ━━━━━━━━━━━━░░░░░░ 4/7 days      │
│    Current: 4 days this week        │
└─────────────────────────────────────┘
```

**Acceptance Criteria:**
- Given: Nutrition goal exists
- When: Card renders
- Then: Food category icon and name
- And: Progress bar showing completion
- And: "X/Y" numeric progress
- And: Current week status text

---

### RULE-028: Food Category Selector

| Field | Value |
|-------|-------|
| **Screen** | Recipe Rules - Nutrition Sheet |
| **Element** | Category dropdown |
| **Trigger** | Add goal sheet open |
| **Status** | Implemented |
| **Test** | `RecipeRulesScreenTest.kt:foodCategorySelector_displaysOptions` |

**Food Categories:**
| Category | Example Foods |
|----------|---------------|
| Green leafy vegetables | Spinach, Methi, Sarson |
| Citrus/Vitamin C rich | Orange, Lemon, Amla |
| Iron-rich foods | Spinach, Beetroot, Jaggery |
| High protein foods | Paneer, Dal, Chicken |
| Calcium-rich foods | Milk, Curd, Cheese |
| Fiber-rich foods | Oats, Rajma, Vegetables |
| Omega-3 rich foods | Fish, Walnuts, Flaxseed |
| Antioxidant-rich foods | Berries, Green Tea |

**Acceptance Criteria:**
- Given: Nutrition goal sheet open
- When: User taps category dropdown
- Then: All categories displayed
- And: Selection fills the field
- And: Category icon shown

---

## Implementation Files

| Component | File Path |
|-----------|-----------|
| Recipe Rules Screen | `presentation/reciperules/RecipeRulesScreen.kt` |
| Recipe Rules ViewModel | `presentation/reciperules/RecipeRulesViewModel.kt` |
| Rule Card | `presentation/reciperules/components/RuleCard.kt` |
| Add Rule Sheet | `presentation/reciperules/components/AddRuleSheet.kt` |
| Nutrition Goal Card | `presentation/reciperules/components/NutritionGoalCard.kt` |

## Test Files

| Test Type | File Path |
|-----------|-----------|
| UI Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/reciperules/RecipeRulesScreenTest.kt` |
| Unit Tests | `app/src/test/java/com/rasoiai/app/presentation/reciperules/RecipeRulesViewModelTest.kt` |
| E2E Flow | `app/src/androidTest/java/com/rasoiai/app/e2e/flows/RecipeRulesFlowTest.kt` |

---

*Requirements derived from wireframe: `13-recipe-rules.md`*
