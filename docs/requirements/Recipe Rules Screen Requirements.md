# Recipe Rules Screen Requirements

**Document Version:** 1.0
**Date:** January 2025
**Screen:** 13 - Recipe Rules
**Project:** RasoiAI Android App

---

## Table of Contents

1. [Overview](#overview)
2. [User Stories](#user-stories)
3. [Rule Categories](#rule-categories)
4. [Frequency Options](#frequency-options)
5. [Rule Enforcement](#rule-enforcement)
6. [UI Specifications](#ui-specifications)
7. [Navigation](#navigation)
8. [Data Models](#data-models)
9. [Business Logic](#business-logic)
10. [Edge Cases & Error Handling](#edge-cases--error-handling)

---

## Overview

### Purpose

The Recipe Rules screen allows users to define recurring meal planning rules that the AI considers when generating weekly meal plans. Rules help ensure dietary goals, family preferences, and cultural practices are consistently followed.

### Initial Requirements (User-Provided)

1. "User can choose to eat moringa leaves at least once in a week via paratha or something else"
2. "How many vitamins I want to eat in a week"
3. "Cook rajma at least once in a week"
4. "Chai in every breakfast"
5. "Chai in every evening snack"

### Key Decisions Summary

| Aspect | Decision |
|--------|----------|
| Rule Categories | All 4 types: Recipe, Ingredient, Meal-slot, Nutrition |
| Frequency Options | Standard: Daily / X times per week / Specific days |
| Ingredient Rules | Hybrid: System suggests + user customizes |
| Nutrition Goals | Food-category based + Weekly targets |
| Rule Enforcement | User-controlled: Required vs Preferred toggle |
| UI Organization | Tabs for each rule type |
| Navigation Access | Both Settings screen + Home screen shortcut |

---

## User Stories

### Primary User Stories

| ID | As a... | I want to... | So that... |
|----|---------|--------------|------------|
| US-01 | Home cook | Add a rule to include chai in every breakfast | My family's daily routine is respected |
| US-02 | Health-conscious user | Set a goal to eat greens 7 times per week | I ensure adequate vegetable intake |
| US-03 | Parent | Require rajma at least once per week | My kids get regular protein from pulses |
| US-04 | User with dietary goals | Include moringa in meals once per week | I get specific nutritional benefits |
| US-05 | User | Mark some rules as "Required" and others as "Preferred" | Critical rules are never skipped |
| US-06 | User | Enable/disable rules without deleting them | I can temporarily pause rules |
| US-07 | User | See which rules were satisfied in my meal plan | I can track my dietary goals |

### Secondary User Stories

| ID | As a... | I want to... | So that... |
|----|---------|--------------|------------|
| US-08 | User | Edit existing rules | I can adjust as my preferences change |
| US-09 | User | Delete rules I no longer need | My rule list stays clean |
| US-10 | User | See suggested recipes for an ingredient rule | I know what options are available |
| US-11 | User | Add custom recipes to fulfill a rule | I have full control over meal options |

---

## Rule Categories

### 1. Recipe Rules

**Purpose:** Include a specific recipe/dish in the meal plan with defined frequency.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| Recipe | Recipe selection | Yes | The specific recipe to include |
| Frequency | Frequency selector | Yes | How often to include |
| Meal Type | Multi-select | No | Restrict to specific meals (Breakfast/Lunch/Dinner/Snacks) |
| Enforcement | Toggle | Yes | Required or Preferred |
| Enabled | Toggle | Yes | Active or Paused |

**Examples:**
- "Rajma" → 1 time per week → Any meal → Required
- "Chai" → Daily → Breakfast only → Required
- "Kheer" → Every Sunday → Dinner → Preferred

---

### 2. Ingredient Rules

**Purpose:** Include recipes containing a specific ingredient with defined frequency.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| Ingredient | Text/Search | Yes | The ingredient to include |
| Frequency | Frequency selector | Yes | How often to include |
| Meal Type | Multi-select | No | Restrict to specific meals |
| Fulfilling Recipes | Multi-select | Yes | Recipes that can satisfy this rule |
| Enforcement | Toggle | Yes | Required or Preferred |
| Enabled | Toggle | Yes | Active or Paused |

**Hybrid Approach for Fulfilling Recipes:**
1. User enters ingredient (e.g., "Moringa")
2. System searches recipe database for recipes containing that ingredient
3. System displays: "Found X recipes with Moringa"
4. User can select/deselect which recipes can fulfill the rule
5. User can also add custom recipes not in suggestions

**Examples:**
- "Moringa leaves" → 1 time per week → Found recipes: [Moringa Paratha ✓] [Moringa Dal ✓] [Drumstick Sambar]
- "Paneer" → 3 times per week → [Palak Paneer ✓] [Paneer Tikka ✓] [Shahi Paneer ✓]

---

### 3. Meal-Slot Rules

**Purpose:** Always include a specific item in a particular meal slot.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| Recipe/Item | Recipe selection | Yes | What to include |
| Meal Type | Single-select | Yes | Which meal (Breakfast/Lunch/Dinner/Snacks) |
| Days | Multi-select | No | Specific days or "All days" |
| Enforcement | Toggle | Yes | Required or Preferred |
| Enabled | Toggle | Yes | Active or Paused |

**Examples:**
- "Chai" → Every Breakfast → All days → Required
- "Chai" → Evening Snacks → All days → Required
- "Salad" → Lunch → Weekdays only → Preferred
- "Curd/Raita" → Dinner → All days → Preferred

---

### 4. Nutrition Goals (Food-Category Based)

**Purpose:** Set weekly targets for food categories to ensure balanced nutrition.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| Food Category | Dropdown | Yes | Category to track |
| Target | Number | Yes | Servings per week |
| Enforcement | Toggle | Yes | Required or Preferred |
| Enabled | Toggle | Yes | Active or Paused |

**Available Food Categories:**

| Category | Description | Example Items |
|----------|-------------|---------------|
| Green Vegetables | Leafy greens, green veggies | Palak, Methi, Bhindi, Lauki |
| Other Vegetables | Non-green vegetables | Aloo, Gobhi, Baingan, Gajar |
| Fruits | All fruits | Banana, Apple, Mango, Papaya |
| Pulses & Legumes | Dals, beans, lentils | Moong, Toor, Chana, Rajma |
| Dairy | Milk products | Milk, Curd, Paneer, Ghee |
| Whole Grains | Whole grain items | Brown rice, Roti, Oats |
| Nuts & Seeds | Dry fruits, seeds | Almonds, Walnuts, Flax seeds |
| Protein (Non-veg) | Meat, fish, eggs | Chicken, Fish, Eggs |

**Examples:**
- Green Vegetables → 7 servings per week → Required
- Pulses & Legumes → 4 servings per week → Required
- Fruits → 5 servings per week → Preferred
- Dairy → 7 servings per week → Preferred

---

## Frequency Options

### Standard Frequency Types

| Type | Options | Use Case |
|------|---------|----------|
| **Daily** | Every day | "Chai every breakfast" |
| **X times per week** | 1, 2, 3, 4, 5, 6, 7 | "Rajma once per week" |
| **Specific days** | Mon, Tue, Wed, Thu, Fri, Sat, Sun | "Fish every Friday" |

### Frequency Selector UI

```
┌─────────────────────────────────────┐
│  How often?                         │
│                                     │
│  ○ Every day                        │
│  ● X times per week                 │
│      [1] [2] [3] [4] [5] [6] [7]   │
│           ↑ selected                │
│  ○ Specific days                    │
│      [M] [T] [W] [T] [F] [S] [S]   │
└─────────────────────────────────────┘
```

### Meal Type Restriction

For Recipe and Ingredient rules, user can optionally restrict to specific meals:

```
┌─────────────────────────────────────┐
│  For which meals? (Optional)        │
│                                     │
│  [✓ Breakfast] [✓ Lunch]            │
│  [✓ Dinner]    [□ Snacks]           │
│                                     │
│  Leave all unchecked = Any meal     │
└─────────────────────────────────────┘
```

---

## Rule Enforcement

### Enforcement Levels

| Level | Behavior | Visual |
|-------|----------|--------|
| **Required** | Rule MUST be satisfied. Meal plan generation fails if not possible. | Red/Orange indicator |
| **Preferred** | System tries to satisfy. Shows warning if not met, but plan generates. | Blue/Gray indicator |

### Toggle UI

```
┌─────────────────────────────────────┐
│  Enforcement:                       │
│                                     │
│  [Required ●───────○ Preferred]     │
│                                     │
│  Required = Must be included        │
│  Preferred = Try to include         │
└─────────────────────────────────────┘
```

### Conflict Resolution

When generating a meal plan:

1. **Required rules evaluated first**
   - If a Required rule cannot be satisfied → Show error, block generation
   - User must adjust rules or preferences

2. **Preferred rules evaluated second**
   - System attempts to satisfy
   - If not possible → Show warning with reason
   - Plan generates anyway

3. **Conflict between Required rules**
   - Show specific conflict details
   - Example: "Cannot satisfy both 'No carbs on weekdays' and 'Rajma once per week (Required)'"
   - User must resolve by editing rules

---

## UI Specifications

### Screen Layout (Tabs)

```
┌─────────────────────────────────────┐
│  ←  Recipe Rules                ⋮   │
│─────────────────────────────────────│
│                                     │
│ [Recipe][Ingredient][Meal][Nutrition]│
│    ━━━━                             │
│                                     │
│  2 rules                            │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ Rajma                         ⋮ ││
│  │ 1x per week • Any meal          ││
│  │ [Required]              [ON/OFF]││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ Chai                          ⋮ ││
│  │ Daily • Breakfast               ││
│  │ [Required]              [ON/OFF]││
│  └─────────────────────────────────┘│
│                                     │
│                                     │
│                                     │
│                            [+ Add]  │
│                                     │
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
└─────────────────────────────────────┘
```

### Tab Contents

| Tab | Content |
|-----|---------|
| **Recipe** | List of recipe-based rules |
| **Ingredient** | List of ingredient-based rules |
| **Meal** | List of meal-slot rules |
| **Nutrition** | List of food-category goals |

### Rule Card Component

```
┌─────────────────────────────────────┐
│ [Icon] Rule Name                  ⋮ │
│ Frequency • Meal restriction        │
│ [Required/Preferred]       [ON/OFF] │
└─────────────────────────────────────┘

⋮ Menu options:
  - Edit
  - Delete
  - Duplicate
```

### Empty State (per tab)

```
┌─────────────────────────────────────┐
│                                     │
│           [Illustration]            │
│                                     │
│      No recipe rules yet            │
│                                     │
│   Add rules to ensure your          │
│   favorite dishes appear in         │
│   your meal plans                   │
│                                     │
│      [+ Add Recipe Rule]            │
│                                     │
└─────────────────────────────────────┘
```

### Add Rule Flow (Bottom Sheet)

**Step 1: Select Rule Type** (if accessed via FAB on main screen)

```
┌─────────────────────────────────────┐
│  ─────────────────────────────────  │
│                                     │
│      Add New Rule                   │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🍳 Recipe Rule                  ││
│  │    Include a specific dish      ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🥬 Ingredient Rule              ││
│  │    Include an ingredient        ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🍽️ Meal-Slot Rule               ││
│  │    Always include in a meal     ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 📊 Nutrition Goal               ││
│  │    Weekly food category target  ││
│  └─────────────────────────────────┘│
│                                     │
└─────────────────────────────────────┘
```

**Step 2: Configure Rule** (Full-screen or large bottom sheet)

Example for Recipe Rule:

```
┌─────────────────────────────────────┐
│  ←  Add Recipe Rule          [Save] │
│─────────────────────────────────────│
│                                     │
│  Select Recipe:                     │
│  ┌─────────────────────────────────┐│
│  │ 🔍 Search recipes...            ││
│  └─────────────────────────────────┘│
│                                     │
│  Popular choices:                   │
│  [Rajma] [Dal Tadka] [Paneer Tikka] │
│  [Chole] [Aloo Gobi] [Palak Paneer] │
│                                     │
│─────────────────────────────────────│
│                                     │
│  How often?                         │
│  ○ Every day                        │
│  ● X times per week    [1▼]         │
│  ○ Specific days                    │
│                                     │
│─────────────────────────────────────│
│                                     │
│  For which meals? (Optional)        │
│  [□ Breakfast] [□ Lunch]            │
│  [□ Dinner]    [□ Snacks]           │
│                                     │
│─────────────────────────────────────│
│                                     │
│  Enforcement:                       │
│  [Required ●─────○ Preferred]       │
│                                     │
└─────────────────────────────────────┘
```

Example for Ingredient Rule:

```
┌─────────────────────────────────────┐
│  ←  Add Ingredient Rule      [Save] │
│─────────────────────────────────────│
│                                     │
│  Ingredient:                        │
│  ┌─────────────────────────────────┐│
│  │ Moringa                         ││
│  └─────────────────────────────────┘│
│                                     │
│  Found 5 recipes with "Moringa":    │
│  ┌─────────────────────────────────┐│
│  │ [✓] Moringa Paratha             ││
│  │ [✓] Moringa Dal                 ││
│  │ [ ] Drumstick Sambar            ││
│  │ [ ] Moringa Soup                ││
│  │ [ ] Moringa Juice               ││
│  └─────────────────────────────────┘│
│  [+ Add custom recipe]              │
│                                     │
│─────────────────────────────────────│
│                                     │
│  How often?                         │
│  ○ Every day                        │
│  ● X times per week    [1▼]         │
│  ○ Specific days                    │
│                                     │
│─────────────────────────────────────│
│                                     │
│  Enforcement:                       │
│  [Required ○─────● Preferred]       │
│                                     │
└─────────────────────────────────────┘
```

Example for Nutrition Goal:

```
┌─────────────────────────────────────┐
│  ←  Add Nutrition Goal       [Save] │
│─────────────────────────────────────│
│                                     │
│  Food Category:                     │
│  ┌─────────────────────────────────┐│
│  │ Green Vegetables              ▼ ││
│  └─────────────────────────────────┘│
│                                     │
│  Options:                           │
│  • Green Vegetables                 │
│  • Other Vegetables                 │
│  • Fruits                           │
│  • Pulses & Legumes                 │
│  • Dairy                            │
│  • Whole Grains                     │
│  • Nuts & Seeds                     │
│  • Protein (Non-veg)                │
│                                     │
│─────────────────────────────────────│
│                                     │
│  Weekly Target:                     │
│  ┌─────────────────────────────────┐│
│  │    [-]      7 servings     [+]  ││
│  └─────────────────────────────────┘│
│  Recommended: 7-10 servings/week    │
│                                     │
│─────────────────────────────────────│
│                                     │
│  Enforcement:                       │
│  [Required ●─────○ Preferred]       │
│                                     │
└─────────────────────────────────────┘
```

---

## Navigation

### Access Points

| Location | Method | Action |
|----------|--------|--------|
| **Settings Screen** | Tap "Recipe Rules" under Preferences section | Opens Recipe Rules screen |
| **Home Screen** | Tap menu (☰) → "Recipe Rules" | Opens Recipe Rules screen |
| **Home Screen** | Quick access icon in header (optional) | Opens Recipe Rules screen |

### Navigation Flow

```
Home Screen
    │
    ├── ☰ Menu → Recipe Rules → Recipe Rules Screen
    │
    └── Bottom Nav → Settings → Preferences → Recipe Rules → Recipe Rules Screen
```

### Back Navigation

- From Recipe Rules → Returns to previous screen (Settings or Home)
- From Add/Edit Rule → Returns to Recipe Rules list (with unsaved changes warning if applicable)

---

## Data Models

### RecipeRule

```kotlin
data class RecipeRule(
    val id: String,
    val type: RuleType,
    val name: String,                    // Display name
    val recipeId: String?,               // For Recipe rules
    val ingredientName: String?,         // For Ingredient rules
    val fulfillingRecipeIds: List<String>?, // For Ingredient rules
    val foodCategory: FoodCategory?,     // For Nutrition rules
    val frequency: RuleFrequency,
    val mealTypes: List<MealType>?,      // Optional meal restriction
    val targetServings: Int?,            // For Nutrition rules
    val enforcement: RuleEnforcement,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

enum class RuleType {
    RECIPE,
    INGREDIENT,
    MEAL_SLOT,
    NUTRITION
}

sealed class RuleFrequency {
    data object Daily : RuleFrequency()
    data class TimesPerWeek(val times: Int) : RuleFrequency()
    data class SpecificDays(val days: List<DayOfWeek>) : RuleFrequency()
}

enum class RuleEnforcement {
    REQUIRED,
    PREFERRED
}

enum class FoodCategory(val displayName: String) {
    GREEN_VEGETABLES("Green Vegetables"),
    OTHER_VEGETABLES("Other Vegetables"),
    FRUITS("Fruits"),
    PULSES_LEGUMES("Pulses & Legumes"),
    DAIRY("Dairy"),
    WHOLE_GRAINS("Whole Grains"),
    NUTS_SEEDS("Nuts & Seeds"),
    PROTEIN_NONVEG("Protein (Non-veg)")
}
```

### Repository Interface

```kotlin
interface RecipeRulesRepository {
    suspend fun getAllRules(): Flow<List<RecipeRule>>
    suspend fun getRulesByType(type: RuleType): Flow<List<RecipeRule>>
    suspend fun getRuleById(id: String): RecipeRule?
    suspend fun addRule(rule: RecipeRule): Result<Unit>
    suspend fun updateRule(rule: RecipeRule): Result<Unit>
    suspend fun deleteRule(id: String): Result<Unit>
    suspend fun toggleRuleEnabled(id: String, enabled: Boolean): Result<Unit>
    suspend fun getEnabledRules(): Flow<List<RecipeRule>>
    suspend fun searchRecipesByIngredient(ingredient: String): List<Recipe>
}
```

---

## Business Logic

### Meal Plan Generation with Rules

1. **Load enabled rules** grouped by enforcement level
2. **Process Required rules first**
   - For each Required rule, find valid slots
   - If no valid slot found → Return error with rule details
3. **Process Preferred rules second**
   - For each Preferred rule, attempt to find slot
   - If not possible → Add to warnings list
4. **Fill remaining slots** with AI-generated suggestions
5. **Return meal plan** with:
   - Generated meals
   - Rules satisfaction report
   - Warnings for unmet Preferred rules

### Rule Satisfaction Checking

```kotlin
data class RuleSatisfactionReport(
    val satisfiedRules: List<SatisfiedRule>,
    val unsatisfiedRequired: List<UnsatisfiedRule>,  // Blocks generation
    val unsatisfiedPreferred: List<UnsatisfiedRule>  // Warnings only
)

data class SatisfiedRule(
    val rule: RecipeRule,
    val satisfiedBy: List<MealPlanItem>  // Which meals satisfied this rule
)

data class UnsatisfiedRule(
    val rule: RecipeRule,
    val reason: String  // Why it couldn't be satisfied
)
```

### Ingredient Search Logic

```kotlin
suspend fun searchRecipesByIngredient(ingredientName: String): List<Recipe> {
    return recipeRepository.searchRecipes(
        query = "",
        ingredientFilter = ingredientName
    )
}
```

---

## Edge Cases & Error Handling

### Edge Cases

| Scenario | Handling |
|----------|----------|
| No recipes found for ingredient | Show message: "No recipes found with [ingredient]. Try a different spelling or add custom recipes." |
| Conflicting Required rules | Show error with specific conflict details. User must resolve. |
| Too many Required rules for available slots | Show error: "Too many required rules for [X] meals. Please reduce required rules or change frequencies." |
| Rule references deleted recipe | Mark rule as "Invalid" with warning. Prompt user to update. |
| All rules disabled | Generate normal meal plan without rule constraints |

### Validation Rules

| Field | Validation |
|-------|------------|
| Recipe selection | Must select at least one recipe |
| Ingredient name | Must be non-empty, trimmed |
| Fulfilling recipes | At least one recipe must be selected for Ingredient rules |
| Frequency | Must select one option |
| Nutrition target | Must be between 1 and 21 servings per week |

### Error Messages

| Error | Message |
|-------|---------|
| Required rule not satisfiable | "Cannot generate meal plan: [Rule name] requires [X] per week but no available slots match your preferences." |
| Conflicting rules | "These rules conflict: [Rule 1] and [Rule 2]. Please adjust one of them." |
| No recipes for ingredient | "We couldn't find any recipes with [ingredient]. Try checking the spelling or add a custom recipe." |
| Invalid rule | "This rule references a recipe that no longer exists. Please update or delete this rule." |

---

## Future Enhancements (Out of Scope for v1)

1. **Rule templates** - Pre-built rule sets (e.g., "High Protein Week", "Detox Week")
2. **Rule sharing** - Share rules with family members
3. **Smart suggestions** - AI suggests rules based on eating patterns
4. **Seasonal rules** - Rules active only during certain seasons
5. **Budget-based rules** - Include/exclude based on ingredient cost
6. **Rule history** - Track how often rules were satisfied over time

---

*Document Created: January 2025*
*Author: Claude (AI Assistant)*
*Project: RasoiAI Android App*
