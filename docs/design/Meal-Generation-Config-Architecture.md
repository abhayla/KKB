# Meal Generation Config Architecture

This document defines the complete configuration architecture for RasoiAI's meal plan generation system.

**Related:** [Meal-Generation-Algorithm.md](./Meal-Generation-Algorithm.md) - Detailed algorithm implementation

---

## Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        FIRESTORE                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  system_config/meal_generation     (Global - Admin managed)     │
│  ├── meal_structure: {items_per_slot: 2}                       │
│  ├── pairing_rules: {by_cuisine: {...}, by_meal_type: {...}}   │
│  ├── recipe_categories: ["dal", "rice", ...]                   │
│  └── rule_behaviors: {include: "adds_to_pair", ...}            │
│                                                                 │
│  reference_data/                   (Global - Static lists)      │
│  ├── common_ingredients: [{name, aliases, category}, ...]      │
│  ├── common_dishes: [{name, pairs_with, meal_type}, ...]       │
│  └── cuisines: [{id, name, typical_pairings}, ...]             │
│                                                                 │
│  users/{user_id}/preferences       (Per-user - Chat managed)    │
│  ├── recipe_rules: [INCLUDE/EXCLUDE rules]                     │
│  ├── allergies: [{ingredient, severity}]                       │
│  ├── dislikes: [ingredients]                                   │
│  ├── dietary_tags: ["vegetarian", "sattvic"]                   │
│  ├── cooking_time: {weekday: 30, weekend: 60}                  │
│  ├── busy_days: ["MONDAY", "WEDNESDAY"]                        │
│  └── last_change: {data, timestamp}  ← For undo                │
│                                                                 │
│  recipes/{recipe_id}               (Recipe database)            │
│  ├── name, ingredients, instructions, ...                      │
│  └── category: "dal"               ← For pairing               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
    ┌──────────┐       ┌──────────────┐    ┌──────────┐
    │ Android  │       │   Backend    │    │  Admin   │
    │   App    │       │   (Python)   │    │  Script  │
    ├──────────┤       ├──────────────┤    ├──────────┤
    │ • Chat   │       │ • Generate   │    │ • Update │
    │ • Update │       │   meal plan  │    │   system │
    │   user   │       │ • Apply      │    │   config │
    │   prefs  │       │   rules      │    │          │
    └──────────┘       └──────────────┘    └──────────┘
```

---

## Key Design Decisions

### 1. Meal Structure

| Decision | Value | Rationale |
|----------|-------|-----------|
| Items per meal slot | 2 (minimum) | Each meal has complementary pair (e.g., Dal + Rice) |
| Expandable | Yes | 3+ items allowed if multiple INCLUDE rules |

### 2. Pairing Rules

| INCLUDE Rules | Item Selection Logic |
|---------------|---------------------|
| 0 rules | Use predefined pairing (Dal + Rice, Sabzi + Roti) |
| 1 rule | INCLUDE item + any random recipe from that meal |
| 2+ rules | All INCLUDE items become the pair (expand if >2) |

### 3. Rule Behaviors

| Rule Type | Behavior |
|-----------|----------|
| **INCLUDE** | Forces item into meal slot, paired with complementary item |
| **EXCLUDE** | Replaces only the excluded item, keeps its pair |
| **INCLUDE DAILY** | Appears in ALL specified meal slots every day |
| **INCLUDE WEEKLY** | Appears with its pair (follows pairing rules) |

### 4. Pairing Variations

| Variation | Supported |
|-----------|-----------|
| By Cuisine | Yes - North/South/East/West have different pairings |
| By Meal Type | Yes - Breakfast/Lunch/Dinner/Snacks have different pairings |

---

## Firestore Collections

### 1. `system_config/meal_generation` (Global)

Admin-managed configuration for meal generation logic.

```json
{
  "meal_structure": {
    "items_per_slot": 2,
    "expandable": true
  },

  "pairing_rules": {
    "by_cuisine": {
      "north": {
        "dal": ["rice", "roti", "paratha"],
        "sabzi": ["roti", "paratha", "rice"],
        "paratha": ["chai", "curd", "pickle"],
        "biryani": ["raita", "salad"]
      },
      "south": {
        "dosa": ["sambar", "chutney"],
        "idli": ["sambar", "chutney"],
        "rice": ["sambar", "rasam", "curry"],
        "uttapam": ["sambar", "chutney"]
      },
      "east": {
        "rice": ["dal", "fish_curry", "sabzi"],
        "luchi": ["aloor_dom", "cholar_dal"]
      },
      "west": {
        "thepla": ["chai", "pickle", "curd"],
        "dhokla": ["chutney", "chai"],
        "dal": ["rice", "roti", "bhakri"]
      }
    },

    "by_meal_type": {
      "breakfast": {
        "default_pairs": ["paratha+chai", "poha+chai", "idli+sambar", "dosa+chutney"],
        "beverage_required": true
      },
      "lunch": {
        "default_pairs": ["dal+rice", "sabzi+roti", "rice+curry"],
        "beverage_required": false
      },
      "dinner": {
        "default_pairs": ["dal+roti", "sabzi+paratha", "khichdi+curd"],
        "beverage_required": false
      },
      "snacks": {
        "default_pairs": ["samosa+chai", "pakora+chai", "biscuit+chai"],
        "beverage_required": true
      }
    }
  },

  "recipe_categories": [
    "dal", "rice", "roti", "paratha", "sabzi", "curry",
    "biryani", "pulao", "khichdi", "dosa", "idli", "uttapam",
    "chutney", "raita", "pickle", "salad",
    "chai", "beverage", "lassi", "buttermilk",
    "snack", "sweet", "dessert",
    "egg_dish", "breakfast_main", "soup"
  ],

  "rule_behaviors": {
    "include": {
      "action": "adds_to_pair",
      "allows_duplicates": true,
      "description": "INCLUDE item is guaranteed, paired with complementary item"
    },
    "exclude": {
      "action": "replaces_item_only",
      "keeps_pair": true,
      "description": "Only excluded item replaced, pair remains"
    }
  },

  "conflict_resolution": {
    "strategy": "ask_user",
    "description": "When INCLUDE conflicts with EXCLUDE, ask user which to keep"
  }
}
```

### 2. `reference_data/` (Global Static Lists)

#### `reference_data/common_ingredients`

```json
{
  "ingredients": [
    {
      "name": "Paneer",
      "aliases": ["cottage cheese", "indian cheese"],
      "category": "dairy"
    },
    {
      "name": "Karela",
      "aliases": ["bitter gourd", "bitter melon"],
      "category": "vegetable"
    },
    {
      "name": "Baingan",
      "aliases": ["brinjal", "eggplant", "aubergine"],
      "category": "vegetable"
    },
    {
      "name": "Peanut",
      "aliases": ["groundnut", "moongfali", "mungfali"],
      "category": "nut"
    },
    {
      "name": "Cashew",
      "aliases": ["kaju", "caju"],
      "category": "nut"
    }
  ]
}
```

#### `reference_data/common_dishes`

```json
{
  "dishes": [
    {
      "name": "Dal Fry",
      "category": "dal",
      "pairs_with": ["rice", "roti"],
      "meal_types": ["lunch", "dinner"],
      "cuisines": ["north", "south", "east", "west"]
    },
    {
      "name": "Masala Dosa",
      "category": "dosa",
      "pairs_with": ["sambar", "chutney"],
      "meal_types": ["breakfast", "dinner"],
      "cuisines": ["south"]
    },
    {
      "name": "Chai",
      "category": "beverage",
      "pairs_with": ["paratha", "biscuit", "samosa", "snack"],
      "meal_types": ["breakfast", "snacks"],
      "cuisines": ["north", "south", "east", "west"]
    }
  ]
}
```

#### `reference_data/cuisines`

```json
{
  "cuisines": [
    {
      "id": "north",
      "name": "North Indian",
      "typical_pairings": {
        "breakfast": ["paratha+chai", "poha+chai", "chole_bhature"],
        "lunch": ["dal+rice+roti", "rajma+rice", "sabzi+roti"],
        "dinner": ["dal+roti", "paneer+naan", "khichdi+curd"],
        "snacks": ["samosa+chai", "pakora+chai"]
      },
      "staple_ingredients": ["wheat", "dairy", "tomato", "onion"]
    },
    {
      "id": "south",
      "name": "South Indian",
      "typical_pairings": {
        "breakfast": ["idli+sambar+chutney", "dosa+sambar", "upma+chutney"],
        "lunch": ["rice+sambar+rasam", "rice+curry+papad"],
        "dinner": ["dosa+chutney", "uttapam+sambar", "rice+curry"],
        "snacks": ["vada+sambar", "bonda+chutney"]
      },
      "staple_ingredients": ["rice", "coconut", "curry_leaves", "tamarind"]
    }
  ]
}
```

### 3. `users/{user_id}/preferences` (Per-User)

User-specific configuration, managed via Chat.

```json
{
  "recipe_rules": [
    {
      "id": "rule_001",
      "type": "INCLUDE",
      "target": "Chai",
      "frequency": "DAILY",
      "meal_slots": ["BREAKFAST", "SNACKS"],
      "reason": "Family tradition",
      "created_at": "2026-01-28T10:00:00Z"
    },
    {
      "id": "rule_002",
      "type": "EXCLUDE",
      "target": "Paneer",
      "frequency": "NEVER",
      "meal_slots": ["ALL"],
      "reason": "Diabetic diet",
      "created_at": "2026-01-28T10:05:00Z"
    },
    {
      "id": "rule_003",
      "type": "INCLUDE",
      "target": "Moringa",
      "frequency": "WEEKLY",
      "times_per_week": 1,
      "meal_slots": ["LUNCH", "DINNER"],
      "reason": "Nutritional boost",
      "created_at": "2026-01-28T10:10:00Z"
    }
  ],

  "allergies": [
    {"ingredient": "Peanuts", "severity": "SEVERE"},
    {"ingredient": "Cashews", "severity": "MILD"}
  ],

  "dislikes": ["Karela", "Baingan", "Mushroom"],

  "dietary_tags": ["vegetarian", "sattvic"],

  "cooking_time": {
    "weekday_minutes": 30,
    "weekend_minutes": 60
  },

  "busy_days": ["MONDAY", "WEDNESDAY", "FRIDAY"],

  "cuisine_preferences": ["north", "south"],

  "spice_level": "medium",

  "last_change": {
    "action": "ADD_RULE",
    "data": {
      "type": "INCLUDE",
      "target": "Chai",
      "frequency": "DAILY"
    },
    "timestamp": "2026-01-28T10:00:00Z",
    "previous_state": null
  }
}
```

### 4. `recipes/{recipe_id}` (Recipe Database)

Add `category` field for pairing rules.

```json
{
  "id": "recipe_12345",
  "name": "Dal Fry",
  "category": "dal",
  "meal_types": ["lunch", "dinner"],
  "cuisine_type": "north",
  "dietary_tags": ["vegetarian", "vegan"],
  "prep_time_minutes": 15,
  "ingredients": [...],
  "instructions": [...],
  "is_active": true
}
```

---

## Chat-Based Config Updates

### Update Flow

```
User: "I want chai every morning"
                │
                ▼
┌─────────────────────────────────────────┐
│  LLM with Function Calling              │
│                                         │
│  Functions:                             │
│  - update_recipe_rule()                 │
│  - update_allergy()                     │
│  - update_dislike()                     │
│  - update_preference()                  │
│  - undo_last_change()                   │
│  - show_config()                        │
└─────────────────────────────────────────┘
                │
                ▼
{
  "function": "update_recipe_rule",
  "arguments": {
    "action": "ADD",
    "rule_type": "INCLUDE",
    "target": "Chai",
    "frequency": "DAILY",
    "meal_slots": ["BREAKFAST"]
  }
}
                │
                ▼
┌─────────────────────────────────────────┐
│  Backend Validation                     │
│  - Check for conflicts                  │
│  - Validate target exists               │
│  - Store previous state for undo        │
└─────────────────────────────────────────┘
                │
                ▼
AI: "I'll add this rule:
     ✓ INCLUDE: Chai
     ✓ Frequency: DAILY
     ✓ Meal: BREAKFAST

     Should I save this? (Yes/No)"
                │
                ▼
User: "Yes"
                │
                ▼
Update Firestore → Show updated config summary
```

### LLM Function Definitions

```python
functions = [
    {
        "name": "update_recipe_rule",
        "description": "Add, remove, or modify a recipe rule (INCLUDE/EXCLUDE)",
        "parameters": {
            "type": "object",
            "properties": {
                "action": {
                    "type": "string",
                    "enum": ["ADD", "REMOVE", "MODIFY"]
                },
                "rule_type": {
                    "type": "string",
                    "enum": ["INCLUDE", "EXCLUDE"]
                },
                "target": {
                    "type": "string",
                    "description": "Recipe or ingredient name"
                },
                "frequency": {
                    "type": "string",
                    "enum": ["DAILY", "WEEKLY", "NEVER"]
                },
                "times_per_week": {
                    "type": "integer",
                    "description": "For WEEKLY frequency"
                },
                "meal_slots": {
                    "type": "array",
                    "items": {
                        "type": "string",
                        "enum": ["BREAKFAST", "LUNCH", "DINNER", "SNACKS", "ALL"]
                    }
                },
                "reason": {
                    "type": "string",
                    "description": "Optional reason for the rule"
                }
            },
            "required": ["action", "rule_type", "target"]
        }
    },
    {
        "name": "update_allergy",
        "description": "Add or remove an allergy",
        "parameters": {
            "type": "object",
            "properties": {
                "action": {
                    "type": "string",
                    "enum": ["ADD", "REMOVE"]
                },
                "ingredient": {
                    "type": "string"
                },
                "severity": {
                    "type": "string",
                    "enum": ["MILD", "MODERATE", "SEVERE"]
                }
            },
            "required": ["action", "ingredient"]
        }
    },
    {
        "name": "update_dislike",
        "description": "Add or remove a disliked ingredient",
        "parameters": {
            "type": "object",
            "properties": {
                "action": {
                    "type": "string",
                    "enum": ["ADD", "REMOVE"]
                },
                "ingredient": {
                    "type": "string"
                }
            },
            "required": ["action", "ingredient"]
        }
    },
    {
        "name": "update_preference",
        "description": "Update cooking time, busy days, dietary tags, etc.",
        "parameters": {
            "type": "object",
            "properties": {
                "preference_type": {
                    "type": "string",
                    "enum": ["cooking_time", "busy_days", "dietary_tags", "spice_level", "cuisine"]
                },
                "action": {
                    "type": "string",
                    "enum": ["SET", "ADD", "REMOVE"]
                },
                "value": {
                    "type": "string",
                    "description": "The value to set/add/remove"
                }
            },
            "required": ["preference_type", "action", "value"]
        }
    },
    {
        "name": "undo_last_change",
        "description": "Undo the last configuration change",
        "parameters": {
            "type": "object",
            "properties": {}
        }
    },
    {
        "name": "show_config",
        "description": "Show current configuration (rules, allergies, preferences)",
        "parameters": {
            "type": "object",
            "properties": {
                "section": {
                    "type": "string",
                    "enum": ["all", "rules", "allergies", "dislikes", "preferences"],
                    "description": "Which section to show"
                }
            }
        }
    }
]
```

### Chat Features

| Feature | Behavior |
|---------|----------|
| **Add/Remove/Modify** | Full CRUD for all user preferences |
| **Confirm before save** | Always confirm before making changes |
| **Show summary after change** | Display updated config after each change |
| **View current config** | "Show my rules", "What are my allergies?" |
| **Conflict detection** | Ask user when INCLUDE conflicts with EXCLUDE |
| **Undo last change** | Revert most recent change |
| **Clarifying questions** | Ask when intent is unclear |
| **Suggestions** | Suggest from predefined lists + database search |

### Conflict Resolution

```
User: "Add Paneer Butter Masala to dinner daily"

AI: "I found a conflict:

     ❌ Existing: EXCLUDE Paneer (never)
     ➕ New: INCLUDE Paneer Butter Masala (daily)

     Which should I keep?
     1. Keep EXCLUDE, cancel new rule
     2. Remove EXCLUDE, add INCLUDE"

User: "2"

AI: "Done! I've:
     ✓ Removed: EXCLUDE Paneer
     ✓ Added: INCLUDE Paneer Butter Masala (daily, dinner)

     Updated Rules:
     • INCLUDE: Chai - DAILY (Breakfast, Snacks)
     • INCLUDE: Paneer Butter Masala - DAILY (Dinner)

     Allergies: Peanuts (Severe), Cashews (Mild)
     Dislikes: Karela, Baingan, Mushroom"
```

---

## Config Sync Strategy

### Source of Truth

```
┌─────────────────┐      ┌─────────────┐      ┌─────────────┐
│  Config File    │ ───► │ Sync Script │ ───► │  Firestore  │
│  (YAML/JSON)    │      │             │      │  (Runtime)  │
│  in Git repo    │      │             │      │             │
└─────────────────┘      └─────────────┘      └─────────────┘
   Version Control         One Command         App Reads From
   Code Review             Updates DB          Single Source
```

### Files in Repository

```
backend/
├── config/
│   ├── meal_generation.yaml      ← System config source
│   └── reference_data/
│       ├── ingredients.yaml
│       ├── dishes.yaml
│       └── cuisines.yaml
└── scripts/
    └── sync_config.py            ← Sync to Firestore
```

### Sync Commands

```bash
# Sync all config to Firestore
python scripts/sync_config.py

# Sync specific environment
python scripts/sync_config.py --env=staging
python scripts/sync_config.py --env=production

# Dry run (preview changes)
python scripts/sync_config.py --dry-run
```

---

## Meal Generation Algorithm

> **Note:** For detailed implementation including fallback strategies, allergen expansion, and daily ingredient tracking, see [Meal-Generation-Algorithm.md](./Meal-Generation-Algorithm.md).

### Flow (Summary)

```
1. Load user preferences from Firestore
   └── recipe_rules, allergies, dislikes, dietary_tags, cooking_time, busy_days

2. Load system config from Firestore
   └── meal_structure, pairing_rules, recipe_categories

3. For each day (7 days):
   │
   ├── Determine day type (weekday/weekend/busy)
   │   └── Set max_cooking_time accordingly
   │
   ├── For each meal slot (breakfast, lunch, dinner, snacks):
   │   │
   │   ├── Count INCLUDE rules for this slot
   │   │   │
   │   │   ├── 0 rules: Use predefined pairing
   │   │   │   └── Pick category pair (e.g., dal + rice)
   │   │   │   └── Find recipes matching categories
   │   │   │
   │   │   ├── 1 rule: INCLUDE item + random recipe
   │   │   │   └── Find recipe matching INCLUDE target
   │   │   │   └── Pick any other recipe for slot
   │   │   │
   │   │   └── 2+ rules: All INCLUDE items
   │   │       └── Find recipes for each INCLUDE target
   │   │
   │   ├── Apply EXCLUDE filters
   │   │   └── Remove recipes with excluded ingredients
   │   │   └── Remove recipes with allergens
   │   │   └── Remove recipes with dislikes
   │   │
   │   ├── Apply cooking time filter
   │   │   └── Only recipes <= max_cooking_time
   │   │
   │   └── Select final recipes (avoid duplicates in week)
   │
   └── Build meal plan day

4. Save meal plan to Firestore
```

### Pairing Selection Logic

```python
def select_meal_items(slot, include_rules, user_prefs, system_config):
    """Select items for a meal slot."""

    # Count INCLUDE rules for this slot
    slot_rules = [r for r in include_rules if slot in r['meal_slots']]

    if len(slot_rules) == 0:
        # Use predefined pairing
        cuisine = user_prefs['cuisine_preferences'][0]
        pairing = system_config['pairing_rules']['by_cuisine'][cuisine]
        category_pair = random.choice(pairing['default_pairs'])
        return find_recipes_by_categories(category_pair)

    elif len(slot_rules) == 1:
        # INCLUDE item + random recipe
        include_recipe = find_recipe_matching(slot_rules[0]['target'])
        other_recipe = find_random_recipe_for_slot(slot, exclude=[include_recipe])
        return [include_recipe, other_recipe]

    else:
        # All INCLUDE items
        recipes = [find_recipe_matching(r['target']) for r in slot_rules]
        return recipes
```

---

## Implementation Checklist

### Phase 1: Database Setup
- [ ] Create `system_config/meal_generation` document
- [ ] Create `reference_data/` collections
- [ ] Add `category` field to all recipes
- [ ] Create sync script for config files

### Phase 2: Backend Updates
- [ ] Update meal generation to use system config
- [ ] Implement pairing logic
- [ ] Add 2-item per slot support
- [ ] Handle INCLUDE/EXCLUDE rule behaviors

### Phase 3: Chat Integration
- [ ] Define LLM function schemas
- [ ] Implement chat command handlers
- [ ] Add confirmation flow
- [ ] Implement undo functionality
- [ ] Add conflict detection

### Phase 4: Testing
- [ ] Test pairing rules by cuisine
- [ ] Test INCLUDE/EXCLUDE behaviors
- [ ] Test conflict resolution
- [ ] Test undo functionality
- [ ] Test chat natural language understanding

---

*Last updated: January 28, 2026*
