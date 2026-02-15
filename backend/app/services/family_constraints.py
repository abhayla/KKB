"""Family constraint map and forbidden keyword utilities.

Shared module used by:
- ai_meal_service.py (pre-filter INCLUDE rules + post-process meal safety)
- recipe_rules.py (conflict detection on rule creation)

Hindi aliases ensure AI-generated dishes with Hindi names (e.g., "Aloo Paratha",
"Pyaaz Kachori") are correctly caught by constraint checks.
"""

import logging

logger = logging.getLogger(__name__)

FAMILY_CONSTRAINT_MAP: dict[str, set[str]] = {
    "jain": {
        # English
        "onion", "garlic", "potato", "ginger", "carrot", "beetroot", "radish", "turnip",
        # Hindi aliases
        "aloo", "pyaaz", "pyaz", "lahsun", "lehsun", "adrak", "gajar", "mooli",
        "shalgam", "chukandar",
    },
    "sattvic": {
        # English
        "onion", "garlic", "mushroom", "egg", "meat", "chicken", "fish",
        # Hindi aliases
        "pyaaz", "pyaz", "lahsun", "lehsun", "anda", "gosht", "murgh", "machli", "keema",
    },
    "vegan": {
        # English
        "milk", "paneer", "curd", "yogurt", "cream", "butter", "ghee", "cheese", "egg",
        # Hindi aliases
        "dahi", "malai", "makhani", "raita",
    },
    "diabetic": {
        # English
        "sugar", "jaggery", "gulab jamun", "jalebi", "halwa", "ladoo", "barfi", "kheer", "sweet",
        # Hindi aliases
        "mithai", "rasgulla", "rasmalai", "kulfi", "rabri", "sandesh", "peda",
    },
    "low_salt": {"pickle", "papad", "achaar"},
    "no_spicy": {
        # English
        "green chili", "red chili", "chilli", "mirchi",
        # Hindi aliases
        "hari mirch", "lal mirch",
    },
    "soft_food": {"papad", "chips", "bhujia", "crunchy"},
    "low_oil": {"pakora", "pakoda", "bhajiya", "puri", "kachori", "deep fried"},
}


def get_family_forbidden_keywords(
    family_members: list[dict],
) -> dict[str, set[str]]:
    """Build per-member forbidden keyword sets from family constraints.

    Args:
        family_members: List of family member dicts with health_conditions
            and dietary_restrictions fields.

    Returns:
        Dict mapping member name to set of forbidden ingredient keywords.
    """
    result: dict[str, set[str]] = {}
    for member in family_members:
        name = member.get("name", "Member")
        forbidden: set[str] = set()

        for constraint in member.get("health_conditions", []):
            constraint_lower = constraint.lower()
            if constraint_lower in FAMILY_CONSTRAINT_MAP:
                forbidden.update(FAMILY_CONSTRAINT_MAP[constraint_lower])

        for restriction in member.get("dietary_restrictions", []):
            restriction_lower = restriction.lower()
            if restriction_lower in FAMILY_CONSTRAINT_MAP:
                forbidden.update(FAMILY_CONSTRAINT_MAP[restriction_lower])

        if forbidden:
            result[name] = forbidden

    return result
