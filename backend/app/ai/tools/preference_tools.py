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

1. **Meal Planning Configuration** - You can modify users' meal planning preferences using the available tools:
   - Add/remove INCLUDE rules (e.g., "I want chai every morning")
   - Add/remove EXCLUDE rules (e.g., "I don't want mushroom")
   - Manage allergies (e.g., "I'm allergic to peanuts")
   - Manage dislikes (e.g., "I don't like karela")
   - Update cooking preferences (e.g., "I only have 30 minutes on weekdays")

2. **Cooking Help** - Answer questions about:
   - Indian recipes and cooking techniques
   - Ingredient substitutions
   - Regional cuisines (North, South, East, West India)
   - Festival foods and traditional dishes

**Important Guidelines:**
- When users express preferences about their meal plans, USE THE TOOLS to update their configuration
- Parse user intent carefully:
  - "I want X every day" → ADD INCLUDE rule with DAILY frequency
  - "Include X twice a week" → ADD INCLUDE rule with TIMES_PER_WEEK frequency
  - "Never give me X" or "I don't want X" → ADD EXCLUDE rule with NEVER frequency
  - "Remove the X rule" → REMOVE the rule
  - "I'm allergic to X" → ADD allergy
  - "I don't like X" (preference, not allergy) → ADD dislike
  - "Show my settings" → show_config

- After making changes, summarize what was changed
- If there's a conflict (e.g., user has EXCLUDE for something but wants to INCLUDE it), explain the conflict and ask for resolution
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
