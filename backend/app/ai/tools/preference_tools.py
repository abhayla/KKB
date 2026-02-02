"""Tool definitions for preference updates via chat.

These tools allow Claude to update user meal planning preferences
through natural conversation.
"""

# Tool definitions in Anthropic's format
PREFERENCE_TOOLS = [
    {
        "name": "update_recipe_rule",
        "description": "Add, remove, or modify a recipe rule (INCLUDE or EXCLUDE). "
                      "Use INCLUDE to ensure certain ingredients/recipes appear in meal plans. "
                      "Use EXCLUDE to never include certain ingredients/recipes.",
        "input_schema": {
            "type": "object",
            "properties": {
                "action": {
                    "type": "string",
                    "enum": ["ADD", "REMOVE", "MODIFY"],
                    "description": "Action to perform on the rule"
                },
                "rule_type": {
                    "type": "string",
                    "enum": ["INCLUDE", "EXCLUDE"],
                    "description": "Type of rule - INCLUDE to add items, EXCLUDE to remove items"
                },
                "target": {
                    "type": "string",
                    "description": "The recipe name or ingredient to include/exclude (e.g., 'Chai', 'Paneer', 'Karela')"
                },
                "frequency": {
                    "type": "string",
                    "enum": ["DAILY", "WEEKLY", "TIMES_PER_WEEK", "NEVER"],
                    "description": "How often the rule applies. DAILY=every day, WEEKLY=once per week, NEVER=always exclude"
                },
                "times_per_week": {
                    "type": "integer",
                    "description": "Number of times per week (only for TIMES_PER_WEEK frequency)"
                },
                "meal_slots": {
                    "type": "array",
                    "items": {
                        "type": "string",
                        "enum": ["BREAKFAST", "LUNCH", "DINNER", "SNACKS"]
                    },
                    "description": "Which meal slots the rule applies to"
                },
                "reason": {
                    "type": "string",
                    "description": "Optional reason for the rule (e.g., 'health', 'preference', 'allergy')"
                }
            },
            "required": ["action", "rule_type", "target"]
        }
    },
    {
        "name": "update_allergy",
        "description": "Add or remove an allergy. Allergies will always be excluded from meal plans.",
        "input_schema": {
            "type": "object",
            "properties": {
                "action": {
                    "type": "string",
                    "enum": ["ADD", "REMOVE"],
                    "description": "ADD to add a new allergy, REMOVE to remove an existing one"
                },
                "ingredient": {
                    "type": "string",
                    "description": "The ingredient the user is allergic to"
                },
                "severity": {
                    "type": "string",
                    "enum": ["MILD", "MODERATE", "SEVERE"],
                    "description": "Severity of the allergy"
                }
            },
            "required": ["action", "ingredient"]
        }
    },
    {
        "name": "update_dislike",
        "description": "Add or remove a disliked ingredient. Dislikes will be avoided in meal plans.",
        "input_schema": {
            "type": "object",
            "properties": {
                "action": {
                    "type": "string",
                    "enum": ["ADD", "REMOVE"],
                    "description": "ADD to add a dislike, REMOVE to remove a dislike"
                },
                "ingredient": {
                    "type": "string",
                    "description": "The ingredient the user dislikes"
                }
            },
            "required": ["action", "ingredient"]
        }
    },
    {
        "name": "update_preference",
        "description": "Update cooking preferences like cooking time, busy days, dietary tags, spice level, or cuisine preferences.",
        "input_schema": {
            "type": "object",
            "properties": {
                "preference_type": {
                    "type": "string",
                    "enum": ["cooking_time", "busy_days", "dietary_tags", "spice_level", "cuisine"],
                    "description": "The type of preference to update"
                },
                "action": {
                    "type": "string",
                    "enum": ["SET", "ADD", "REMOVE"],
                    "description": "SET replaces the value, ADD/REMOVE for list-type preferences"
                },
                "value": {
                    "type": "string",
                    "description": "The value to set/add/remove. For cooking_time use 'weekday:30' or 'weekend:60' format. "
                                  "For busy_days use day names like 'MONDAY'. For dietary_tags use 'vegetarian', 'vegan', etc. "
                                  "For cuisine use 'north', 'south', 'east', 'west'."
                }
            },
            "required": ["preference_type", "action", "value"]
        }
    },
    {
        "name": "undo_last_change",
        "description": "Undo the last configuration change made by the user.",
        "input_schema": {
            "type": "object",
            "properties": {}
        }
    },
    {
        "name": "show_config",
        "description": "Show the user's current meal planning configuration including rules, allergies, dislikes, and preferences.",
        "input_schema": {
            "type": "object",
            "properties": {
                "section": {
                    "type": "string",
                    "enum": ["all", "rules", "allergies", "dislikes", "preferences"],
                    "description": "Which section of the configuration to show. Use 'all' to show everything."
                }
            }
        }
    }
]


# System prompt for chat with config update capabilities
CONFIG_CHAT_SYSTEM_PROMPT = """You are RasoiAI, a friendly and knowledgeable Indian cooking assistant. You help users with:

1. **Meal Plan Queries and Modifications** - You can view and modify users' current meal plans:
   - Query what meals are planned ("What's for dinner today?") → use query_current_meals
   - Swap recipes ("I don't feel like dal, change it to something else") → use swap_meal_recipe
   - Add recipes ("Add paneer to my lunch tomorrow") → use add_recipe_to_meal
   - Remove recipes ("Remove the paratha from breakfast") → use remove_recipe_from_meal

2. **Meal Planning Configuration** - You can modify users' meal planning preferences:
   - Add/remove INCLUDE rules (e.g., "I want chai every morning")
   - Add/remove EXCLUDE rules (e.g., "I don't want mushroom")
   - Manage allergies (e.g., "I'm allergic to peanuts")
   - Manage dislikes (e.g., "I don't like karela")
   - Update cooking preferences (e.g., "I only have 30 minutes on weekdays")

3. **Cooking Help** - Answer questions about:
   - Indian recipes and cooking techniques
   - Ingredient substitutions
   - Regional cuisines (North, South, East, West India)
   - Festival foods and traditional dishes

**Important Guidelines:**
- When users ask about today's/tomorrow's meals, USE THE TOOLS to query their plan
- When users want to change specific meals, use the meal plan tools
- When users express general preferences, use the preference tools
- Parse user intent carefully:
  - "What's for dinner?" → query_current_meals with date='today' and meal_type='DINNER'
  - "Change my lunch" or "Swap the dal" → swap_meal_recipe
  - "Add paneer to dinner" → add_recipe_to_meal
  - "Remove the roti" → remove_recipe_from_meal
  - "I want X every day" → ADD INCLUDE rule with DAILY frequency
  - "I want X twice a week" → ADD INCLUDE rule with TIMES_PER_WEEK frequency (times_per_week=2)
  - "Never give me X" → ADD EXCLUDE rule with NEVER frequency
  - "I'm allergic to X" → ADD allergy
  - "Show my settings" → show_config

- After making changes, summarize what was changed
- If a recipe is locked, explain that it cannot be modified
- If no meal plan exists, suggest generating one
- Be warm and use occasional Hindi/Indian cooking terms naturally
- Keep responses concise but helpful

**Indian Cuisine Context:**
- Understand regional cuisines: North (roti, paratha, dal), South (dosa, idli, sambar), East (fish, rice, mishti), West (dhokla, thepla, undhiyu)
- Know dietary restrictions: Vegetarian, Vegan, Jain (no root vegetables), Sattvic (no onion/garlic)
- Understand common ingredients and their regional names/aliases
"""


def format_config_for_display(config: dict) -> str:
    """Format configuration dictionary for chat display."""
    lines = []

    if "recipe_rules" in config:
        rules = config["recipe_rules"]
        if rules.get("include"):
            lines.append("**INCLUDE Rules:**")
            for r in rules["include"]:
                freq = r.get("frequency", "WEEKLY")
                slots = ", ".join(r.get("meal_slots", []))
                lines.append(f"  - {r['target']} ({freq}, {slots})")

        if rules.get("exclude"):
            lines.append("**EXCLUDE Rules:**")
            for r in rules["exclude"]:
                reason = f" - {r['reason']}" if r.get("reason") else ""
                lines.append(f"  - {r['target']}{reason}")

    if "allergies" in config and config["allergies"]:
        lines.append("**Allergies:**")
        for a in config["allergies"]:
            if isinstance(a, dict):
                lines.append(f"  - {a.get('ingredient')} ({a.get('severity', 'MODERATE')})")
            else:
                lines.append(f"  - {a}")

    if "dislikes" in config and config["dislikes"]:
        lines.append(f"**Dislikes:** {', '.join(config['dislikes'])}")

    if "preferences" in config:
        p = config["preferences"]
        lines.append("**Preferences:**")
        if p.get("dietary_tags"):
            lines.append(f"  - Dietary: {', '.join(p['dietary_tags'])}")
        if p.get("cuisine_preferences"):
            lines.append(f"  - Cuisines: {', '.join(p['cuisine_preferences'])}")
        if p.get("spice_level"):
            lines.append(f"  - Spice Level: {p['spice_level']}")
        if "cooking_time" in p:
            lines.append(f"  - Cooking Time: Weekday {p['cooking_time'].get('weekday', 30)}min, Weekend {p['cooking_time'].get('weekend', 60)}min")
        if p.get("busy_days"):
            lines.append(f"  - Busy Days: {', '.join(p['busy_days'])}")

    return "\n".join(lines) if lines else "No configuration set."


# Meal plan related tools
MEAL_PLAN_TOOLS = [
    {
        "name": "query_current_meals",
        "description": "Query the user's current meal plan for a specific date or meal type. "
                      "Use this to tell users what meals are planned for today, tomorrow, or a specific day.",
        "input_schema": {
            "type": "object",
            "properties": {
                "date": {
                    "type": "string",
                    "description": "The date to query in YYYY-MM-DD format. Use 'today' or 'tomorrow' for relative dates."
                },
                "meal_type": {
                    "type": "string",
                    "enum": ["BREAKFAST", "LUNCH", "DINNER", "SNACKS", "ALL"],
                    "description": "Which meal to query. Use 'ALL' to get all meals for the day."
                }
            },
            "required": ["date"]
        }
    },
    {
        "name": "swap_meal_recipe",
        "description": "Swap a recipe in the user's meal plan with a different recipe suggestion. "
                      "Use this when the user wants to change a specific meal to something else.",
        "input_schema": {
            "type": "object",
            "properties": {
                "date": {
                    "type": "string",
                    "description": "The date of the meal to swap in YYYY-MM-DD format. Use 'today' or 'tomorrow' for relative dates."
                },
                "meal_type": {
                    "type": "string",
                    "enum": ["BREAKFAST", "LUNCH", "DINNER", "SNACKS"],
                    "description": "Which meal slot to swap"
                },
                "current_recipe_name": {
                    "type": "string",
                    "description": "The name of the current recipe to replace (optional, if not specified will swap the first item)"
                },
                "requested_recipe_name": {
                    "type": "string",
                    "description": "The name of the recipe the user wants instead (optional, if not specified will suggest alternatives)"
                }
            },
            "required": ["date", "meal_type"]
        }
    },
    {
        "name": "add_recipe_to_meal",
        "description": "Add a recipe to a meal slot in the user's meal plan. "
                      "Use this when the user wants to add a specific dish to their meal plan.",
        "input_schema": {
            "type": "object",
            "properties": {
                "date": {
                    "type": "string",
                    "description": "The date to add the meal in YYYY-MM-DD format. Use 'today' or 'tomorrow' for relative dates."
                },
                "meal_type": {
                    "type": "string",
                    "enum": ["BREAKFAST", "LUNCH", "DINNER", "SNACKS"],
                    "description": "Which meal slot to add to"
                },
                "recipe_name": {
                    "type": "string",
                    "description": "The name of the recipe to add"
                }
            },
            "required": ["date", "meal_type", "recipe_name"]
        }
    },
    {
        "name": "remove_recipe_from_meal",
        "description": "Remove a recipe from a meal slot in the user's meal plan. "
                      "Use this when the user wants to remove a specific dish from their plan.",
        "input_schema": {
            "type": "object",
            "properties": {
                "date": {
                    "type": "string",
                    "description": "The date of the meal in YYYY-MM-DD format. Use 'today' or 'tomorrow' for relative dates."
                },
                "meal_type": {
                    "type": "string",
                    "enum": ["BREAKFAST", "LUNCH", "DINNER", "SNACKS"],
                    "description": "Which meal slot to remove from"
                },
                "recipe_name": {
                    "type": "string",
                    "description": "The name of the recipe to remove"
                }
            },
            "required": ["date", "meal_type", "recipe_name"]
        }
    }
]


# Combined tools for chat assistant
ALL_CHAT_TOOLS = PREFERENCE_TOOLS + MEAL_PLAN_TOOLS
