"""Tests for family health condition enforcement in meal plan post-processing.

Verifies that _enforce_rules() in ai_meal_service.py correctly removes
meal items that are unsafe for family members with health conditions
(DIABETIC, LOW_SALT, NO_SPICY, LOW_OIL) and dietary restrictions
(SATTVIC, JAIN, VEGAN).

Uses the shared FAMILY_CONSTRAINT_MAP from family_constraints.py.
"""

import uuid


from app.services.ai_meal_service import (
    AIMealService,
    DayMeals,
    GeneratedMealPlan,
    MealItem,
    UserPreferences,
)


def _make_item(name: str) -> MealItem:
    """Create a MealItem with the given recipe name."""
    return MealItem(id=str(uuid.uuid4()), recipe_name=name)


def _make_plan(items_by_slot: dict[str, list[str]]) -> GeneratedMealPlan:
    """Create a 1-day plan with items in specified slots.

    Args:
        items_by_slot: e.g. {"breakfast": ["Masala Chai", "Gulab Jamun"]}
    """
    day = DayMeals(
        date="2026-01-27",
        day_name="Monday",
        breakfast=[_make_item(n) for n in items_by_slot.get("breakfast", [])],
        lunch=[_make_item(n) for n in items_by_slot.get("lunch", [])],
        dinner=[_make_item(n) for n in items_by_slot.get("dinner", [])],
        snacks=[_make_item(n) for n in items_by_slot.get("snacks", [])],
    )
    return GeneratedMealPlan(
        week_start_date="2026-01-27",
        week_end_date="2026-02-02",
        days=[day],
    )


def _prefs_with_family(members: list[dict]) -> UserPreferences:
    """Create UserPreferences with given family members."""
    return UserPreferences(family_members=members)


def _member(name: str, health: list[str] = None, dietary: list[str] = None) -> dict:
    """Shorthand to create a family member dict."""
    return {
        "name": name,
        "health_conditions": health or [],
        "dietary_restrictions": dietary or [],
    }


class TestDiabeticEnforcement:
    """Items unsafe for DIABETIC members must be removed."""

    def test_diabetic_items_removed_from_meal_plan(self):
        service = AIMealService.__new__(AIMealService)
        plan = _make_plan(
            {
                "breakfast": ["Masala Chai", "Gulab Jamun"],
                "lunch": ["Dal Fry", "Jalebi"],
                "dinner": ["Paneer Tikka", "Halwa"],
                "snacks": ["Fruit Salad", "Ladoo"],
            }
        )
        prefs = _prefs_with_family([_member("Dadaji", health=["diabetic"])])

        result = service._enforce_rules(plan, prefs)

        day = result.days[0]
        names = {
            item.recipe_name
            for slot in ["breakfast", "lunch", "dinner", "snacks"]
            for item in getattr(day, slot)
        }

        # Safe items kept
        assert "Masala Chai" in names
        assert "Dal Fry" in names
        assert "Paneer Tikka" in names
        assert "Fruit Salad" in names

        # Unsafe items removed
        assert "Gulab Jamun" not in names
        assert "Jalebi" not in names
        assert "Halwa" not in names
        assert "Ladoo" not in names


class TestLowSaltEnforcement:
    """Items with high salt content must be removed for LOW_SALT members."""

    def test_low_salt_items_removed(self):
        service = AIMealService.__new__(AIMealService)
        plan = _make_plan(
            {
                "lunch": ["Jeera Rice", "Mango Pickle", "Papad Fry"],
                "dinner": ["Rajma Curry", "Achaar Rice"],
            }
        )
        prefs = _prefs_with_family([_member("Sunita", health=["low_salt"])])

        result = service._enforce_rules(plan, prefs)

        day = result.days[0]
        lunch_names = [item.recipe_name for item in day.lunch]
        dinner_names = [item.recipe_name for item in day.dinner]

        assert "Jeera Rice" in lunch_names
        assert "Mango Pickle" not in lunch_names
        assert "Papad Fry" not in lunch_names
        assert "Rajma Curry" in dinner_names
        assert "Achaar Rice" not in dinner_names


class TestNoSpicyEnforcement:
    """Spicy items must be removed for NO_SPICY members."""

    def test_no_spicy_items_removed(self):
        service = AIMealService.__new__(AIMealService)
        plan = _make_plan(
            {
                "lunch": ["Plain Dosa", "Green Chili Chutney"],
                "dinner": ["Butter Naan", "Mirchi Bajji"],
            }
        )
        prefs = _prefs_with_family([_member("Aarav", health=["no_spicy"])])

        result = service._enforce_rules(plan, prefs)

        day = result.days[0]
        lunch_names = [item.recipe_name for item in day.lunch]
        dinner_names = [item.recipe_name for item in day.dinner]

        assert "Plain Dosa" in lunch_names
        assert "Green Chili Chutney" not in lunch_names
        assert "Butter Naan" in dinner_names
        assert "Mirchi Bajji" not in dinner_names


class TestLowOilEnforcement:
    """Deep-fried/high-oil items must be removed for LOW_OIL members."""

    def test_low_oil_items_removed(self):
        service = AIMealService.__new__(AIMealService)
        plan = _make_plan(
            {
                "breakfast": ["Idli Sambar", "Pakora"],
                "snacks": ["Kachori", "Roasted Makhana"],
            }
        )
        prefs = _prefs_with_family([_member("Ramesh", health=["low_oil"])])

        result = service._enforce_rules(plan, prefs)

        day = result.days[0]
        breakfast_names = [item.recipe_name for item in day.breakfast]
        snack_names = [item.recipe_name for item in day.snacks]

        assert "Idli Sambar" in breakfast_names
        assert "Pakora" not in breakfast_names
        assert "Roasted Makhana" in snack_names
        assert "Kachori" not in snack_names


class TestSattvicEnforcement:
    """SATTVIC dietary restriction must remove onion, garlic, mushroom, egg."""

    def test_sattvic_items_removed(self):
        service = AIMealService.__new__(AIMealService)
        plan = _make_plan(
            {
                "breakfast": ["Poha", "Masala Egg Omelette"],
                "lunch": ["Mushroom Curry", "Steamed Rice"],
                "dinner": ["Garlic Naan", "Dal Tadka"],
            }
        )
        prefs = _prefs_with_family([_member("Dadiji", dietary=["sattvic"])])

        result = service._enforce_rules(plan, prefs)

        day = result.days[0]
        all_names = {
            item.recipe_name
            for slot in ["breakfast", "lunch", "dinner", "snacks"]
            for item in getattr(day, slot)
        }

        assert "Poha" in all_names
        assert "Steamed Rice" in all_names
        assert "Dal Tadka" in all_names
        assert "Masala Egg Omelette" not in all_names
        assert "Mushroom Curry" not in all_names
        assert "Garlic Naan" not in all_names


class TestJainEnforcement:
    """JAIN dietary restriction must remove root vegetables, onion, garlic."""

    def test_jain_items_removed(self):
        service = AIMealService.__new__(AIMealService)
        plan = _make_plan(
            {
                "breakfast": ["Sabudana Khichdi", "Aloo Paratha"],
                "lunch": ["Gajar Halwa", "Paneer Tikka"],
            }
        )
        prefs = _prefs_with_family([_member("Dadiji", dietary=["jain"])])

        result = service._enforce_rules(plan, prefs)

        day = result.days[0]
        breakfast_names = [item.recipe_name for item in day.breakfast]
        lunch_names = [item.recipe_name for item in day.lunch]

        assert "Sabudana Khichdi" in breakfast_names
        # Aloo = potato, forbidden for Jain
        assert "Aloo Paratha" not in breakfast_names
        # Gajar = carrot, forbidden for Jain
        assert "Gajar Halwa" not in lunch_names
        assert "Paneer Tikka" in lunch_names


class TestVeganEnforcement:
    """VEGAN dietary restriction must remove dairy and egg items."""

    def test_vegan_items_removed(self):
        service = AIMealService.__new__(AIMealService)
        plan = _make_plan(
            {
                "breakfast": ["Masala Chai", "Paneer Paratha"],
                "lunch": ["Curd Rice", "Mixed Veg Curry"],
            }
        )
        prefs = _prefs_with_family([_member("Priya", dietary=["vegan"])])

        result = service._enforce_rules(plan, prefs)

        day = result.days[0]
        all_names = {
            item.recipe_name
            for slot in ["breakfast", "lunch"]
            for item in getattr(day, slot)
        }

        assert "Masala Chai" in all_names  # chai != milk explicitly
        assert "Mixed Veg Curry" in all_names
        assert "Paneer Paratha" not in all_names
        assert "Curd Rice" not in all_names


class TestHindiAliases:
    """Hindi ingredient names must be caught by constraint matching."""

    def test_hindi_aliases_caught(self):
        """Aloo Paratha caught by JAIN (aloo = potato)."""
        service = AIMealService.__new__(AIMealService)
        plan = _make_plan(
            {
                "breakfast": ["Aloo Paratha", "Pyaaz Kachori", "Plain Roti"],
            }
        )
        prefs = _prefs_with_family([_member("Dadiji", dietary=["jain"])])

        result = service._enforce_rules(plan, prefs)

        day = result.days[0]
        breakfast_names = [item.recipe_name for item in day.breakfast]

        # Both Hindi-named items should be caught
        assert "Aloo Paratha" not in breakfast_names  # aloo = potato
        assert "Pyaaz Kachori" not in breakfast_names  # pyaaz = onion
        assert "Plain Roti" in breakfast_names


class TestWordBoundary:
    """Word boundary matching must prevent false positives."""

    def test_word_boundary_no_false_positive(self):
        """'Unsweetened Tea' must NOT be caught by DIABETIC 'sweet' keyword."""
        service = AIMealService.__new__(AIMealService)
        plan = _make_plan(
            {
                "breakfast": ["Unsweetened Tea", "Sweet Lassi"],
            }
        )
        prefs = _prefs_with_family([_member("Dadaji", health=["diabetic"])])

        result = service._enforce_rules(plan, prefs)

        day = result.days[0]
        breakfast_names = [item.recipe_name for item in day.breakfast]

        # "Unsweetened" should NOT match word-boundary \bsweet\b
        assert "Unsweetened Tea" in breakfast_names
        # "Sweet" as standalone word SHOULD be caught
        assert "Sweet Lassi" not in breakfast_names


class TestMultipleConditions:
    """Multiple family members with different conditions must all be enforced."""

    def test_multiple_conditions_combined(self):
        service = AIMealService.__new__(AIMealService)
        plan = _make_plan(
            {
                "breakfast": [
                    "Masala Chai",
                    "Gulab Jamun",
                ],  # DIABETIC removes Gulab Jamun
                "lunch": ["Papad Fry", "Dal Rice"],  # LOW_SALT removes Papad
                "dinner": ["Mirchi Bajji", "Roti Sabzi"],  # NO_SPICY removes Mirchi
                "snacks": ["Pakora", "Fruit Bowl"],  # LOW_OIL removes Pakora
            }
        )
        prefs = _prefs_with_family(
            [
                _member("Ramesh", health=["diabetic", "low_oil"]),
                _member("Sunita", health=["low_salt"]),
                _member("Aarav", health=["no_spicy"]),
            ]
        )

        result = service._enforce_rules(plan, prefs)

        day = result.days[0]
        all_names = {
            item.recipe_name
            for slot in ["breakfast", "lunch", "dinner", "snacks"]
            for item in getattr(day, slot)
        }

        # All safe items kept
        assert "Masala Chai" in all_names
        assert "Dal Rice" in all_names
        assert "Roti Sabzi" in all_names
        assert "Fruit Bowl" in all_names

        # All unsafe items removed (each by different member's condition)
        assert "Gulab Jamun" not in all_names  # DIABETIC
        assert "Papad Fry" not in all_names  # LOW_SALT
        assert "Mirchi Bajji" not in all_names  # NO_SPICY
        assert "Pakora" not in all_names  # LOW_OIL


class TestSafeItemsPreserved:
    """Items not matching any constraint must be kept."""

    def test_safe_items_preserved(self):
        service = AIMealService.__new__(AIMealService)
        safe_items = [
            "Masala Chai",
            "Plain Dosa",
            "Idli Sambar",
            "Jeera Rice",
            "Dal Fry",
            "Roti",
            "Mixed Veg Curry",
            "Fruit Salad",
        ]
        plan = _make_plan(
            {
                "breakfast": safe_items[:2],
                "lunch": safe_items[2:4],
                "dinner": safe_items[4:6],
                "snacks": safe_items[6:],
            }
        )
        prefs = _prefs_with_family(
            [
                _member("Ramesh", health=["diabetic"]),
                _member("Sunita", health=["low_salt"]),
            ]
        )

        result = service._enforce_rules(plan, prefs)

        day = result.days[0]
        all_names = {
            item.recipe_name
            for slot in ["breakfast", "lunch", "dinner", "snacks"]
            for item in getattr(day, slot)
        }

        for name in safe_items:
            assert name in all_names, f"{name} was incorrectly removed"


class TestEmptyFamily:
    """No family members means no constraint filtering."""

    def test_empty_family_no_filtering(self):
        service = AIMealService.__new__(AIMealService)
        all_items = ["Gulab Jamun", "Pickle Rice", "Mirchi Pakora", "Aloo Paratha"]
        plan = _make_plan({"breakfast": all_items})
        prefs = _prefs_with_family([])  # No family members

        result = service._enforce_rules(plan, prefs)

        day = result.days[0]
        breakfast_names = [item.recipe_name for item in day.breakfast]

        # All items kept — no family constraints to apply
        for name in all_items:
            assert name in breakfast_names, f"{name} was incorrectly removed"
