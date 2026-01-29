"""Meal generation service with pairing logic.

This service generates personalized meal plans with complementary recipe pairs.
Each meal slot contains 2 items (e.g., Dal + Rice, Sabzi + Roti) by default.

Key features:
- Config-driven pairing rules (from Firestore system_config)
- INCLUDE rules: Force items into meals with complementary pairs
- EXCLUDE rules: Replace only excluded items, keep pairs
- Respect cooking time limits (weekday vs weekend, busy days)
- Avoid duplicate recipes across the week
"""

import logging
import random
import uuid
from dataclasses import dataclass, field
from datetime import date, timedelta
from typing import Any, Optional

from app.repositories.recipe_repository import RecipeRepository
from app.repositories.user_repository import UserRepository
from app.services.config_service import ConfigService, MealGenerationConfig

logger = logging.getLogger(__name__)


@dataclass
class MealItem:
    """A single item in a meal slot."""
    id: str
    recipe_id: str
    recipe_name: str
    recipe_image_url: Optional[str] = None
    prep_time_minutes: int = 30
    calories: int = 0
    is_locked: bool = False
    dietary_tags: list[str] = field(default_factory=list)
    category: str = ""


@dataclass
class DayMeals:
    """All meals for a single day."""
    date: str
    day_name: str
    breakfast: list[MealItem] = field(default_factory=list)
    lunch: list[MealItem] = field(default_factory=list)
    dinner: list[MealItem] = field(default_factory=list)
    snacks: list[MealItem] = field(default_factory=list)
    festival: Optional[dict] = None


@dataclass
class GeneratedMealPlan:
    """A complete generated meal plan."""
    week_start_date: str
    week_end_date: str
    days: list[DayMeals] = field(default_factory=list)
    rules_applied: dict = field(default_factory=dict)


@dataclass
class UserPreferences:
    """User preferences for meal generation."""
    dietary_tags: list[str] = field(default_factory=lambda: ["vegetarian"])
    cuisine_type: Optional[str] = None
    allergies: list[dict] = field(default_factory=list)
    dislikes: list[str] = field(default_factory=list)
    weekday_cooking_time: int = 30
    weekend_cooking_time: int = 60
    busy_days: list[str] = field(default_factory=list)
    include_rules: list[dict] = field(default_factory=list)
    exclude_rules: list[dict] = field(default_factory=list)
    nutrition_goals: list[dict] = field(default_factory=list)


class MealGenerationService:
    """Service for generating meal plans with complementary pairing.

    Each meal slot contains 2 complementary items by default (configurable).
    Pairing rules come from Firestore config synced from YAML files.
    """

    def __init__(self):
        self.config_service = ConfigService()
        self.recipe_repo = RecipeRepository()
        self.user_repo = UserRepository()

    async def generate_meal_plan(
        self,
        user_id: str,
        week_start_date: date,
    ) -> GeneratedMealPlan:
        """Generate a 7-day meal plan with paired recipes.

        Args:
            user_id: User ID to generate plan for
            week_start_date: Start date of the week (usually Monday)

        Returns:
            GeneratedMealPlan with 7 days of paired meals
        """
        logger.info(f"Generating meal plan for user {user_id}")

        # Load config and user preferences
        config = await self.config_service.get_config()
        prefs = await self._load_user_preferences(user_id)

        logger.info(
            f"Preferences: cuisine={prefs.cuisine_type}, "
            f"dietary={prefs.dietary_tags}, "
            f"include_rules={len(prefs.include_rules)}, "
            f"exclude_rules={len(prefs.exclude_rules)}"
        )

        # Track used recipes to avoid duplicates
        used_recipe_ids: set[str] = set()

        # Pre-compute INCLUDE rule assignments for the week
        include_tracker = self._build_include_tracker(prefs.include_rules)

        # Generate 7 days
        days: list[DayMeals] = []
        current_date = week_start_date

        for day_index in range(7):
            day_name = current_date.strftime("%A")
            is_weekend = day_name in ["Saturday", "Sunday"]
            is_busy_day = day_name.upper() in prefs.busy_days

            # Determine max cooking time for this day
            max_time = prefs.weekday_cooking_time
            if is_busy_day:
                max_time = prefs.weekday_cooking_time
            elif is_weekend:
                max_time = prefs.weekend_cooking_time

            logger.debug(f"Generating {day_name} (weekend={is_weekend}, busy={is_busy_day}, max_time={max_time})")

            day_meals = await self._generate_day_meals(
                config=config,
                prefs=prefs,
                day_index=day_index,
                day_name=day_name,
                max_cooking_time=max_time,
                include_tracker=include_tracker,
                used_recipe_ids=used_recipe_ids,
            )

            days.append(DayMeals(
                date=current_date.isoformat(),
                day_name=day_name,
                breakfast=day_meals.get("breakfast", []),
                lunch=day_meals.get("lunch", []),
                dinner=day_meals.get("dinner", []),
                snacks=day_meals.get("snacks", []),
            ))

            current_date += timedelta(days=1)

        # Log INCLUDE rule fulfillment
        for rule_id, tracker in include_tracker.items():
            logger.info(
                f"INCLUDE rule '{tracker['target']}': "
                f"assigned {tracker['times_assigned']}/{tracker['times_needed']} times"
            )

        week_end = week_start_date + timedelta(days=6)

        return GeneratedMealPlan(
            week_start_date=week_start_date.isoformat(),
            week_end_date=week_end.isoformat(),
            days=days,
            rules_applied={
                "include_rules": len(prefs.include_rules),
                "exclude_rules": len(prefs.exclude_rules),
                "nutrition_goals": len(prefs.nutrition_goals),
                "allergies_excluded": len(prefs.allergies),
                "dislikes_excluded": len(prefs.dislikes),
            }
        )

    async def _load_user_preferences(self, user_id: str) -> UserPreferences:
        """Load user preferences from Firestore."""
        prefs_data = await self.user_repo.get_preferences(user_id)

        if not prefs_data:
            return UserPreferences()

        # Parse recipe rules by type
        recipe_rules = prefs_data.get("recipe_rules", [])
        include_rules = [r for r in recipe_rules if r.get("type") == "INCLUDE"]
        exclude_rules = [r for r in recipe_rules if r.get("type") == "EXCLUDE"]
        nutrition_goals = [r for r in recipe_rules if r.get("type") == "NUTRITION_GOAL"]

        # Get cuisine preference
        cuisines = prefs_data.get("cuisine_preferences", [])
        cuisine_type = cuisines[0].lower() if cuisines else None

        return UserPreferences(
            dietary_tags=prefs_data.get("dietary_tags", ["vegetarian"]),
            cuisine_type=cuisine_type,
            allergies=prefs_data.get("allergies", []),
            dislikes=prefs_data.get("disliked_ingredients", []),
            weekday_cooking_time=prefs_data.get("weekday_cooking_time_minutes", 30),
            weekend_cooking_time=prefs_data.get("weekend_cooking_time_minutes", 60),
            busy_days=[d.upper() for d in prefs_data.get("busy_days", [])],
            include_rules=include_rules,
            exclude_rules=exclude_rules,
            nutrition_goals=nutrition_goals,
        )

    def _build_include_tracker(self, include_rules: list[dict]) -> dict[str, dict]:
        """Build tracker for INCLUDE rule assignments across the week."""
        tracker = {}
        for idx, rule in enumerate(include_rules):
            frequency = rule.get("frequency", "WEEKLY")
            times_needed = 7 if frequency == "DAILY" else rule.get("times_per_week", 1)

            tracker[str(idx)] = {
                "rule": rule,
                "target": rule.get("target", ""),
                "meal_slots": [s.lower() for s in rule.get("meal_slot", ["breakfast", "lunch", "dinner", "snacks"])],
                "times_needed": times_needed,
                "times_assigned": 0,
                "frequency": frequency,
            }
        return tracker

    async def _generate_day_meals(
        self,
        config: MealGenerationConfig,
        prefs: UserPreferences,
        day_index: int,
        day_name: str,
        max_cooking_time: int,
        include_tracker: dict[str, dict],
        used_recipe_ids: set[str],
    ) -> dict[str, list[MealItem]]:
        """Generate all meals for a single day with pairing."""
        meals = {
            "breakfast": [],
            "lunch": [],
            "dinner": [],
            "snacks": [],
        }

        # Build exclusion set from EXCLUDE rules, allergies, and dislikes
        exclude_ingredients = self._build_exclude_list(prefs)

        # Track main ingredients used today to avoid repetition (e.g., Rajma in lunch AND dinner)
        used_ingredients_today: set[str] = set()

        for slot in ["breakfast", "lunch", "dinner", "snacks"]:
            # Determine cooking time for this slot
            # For dinner, use slightly more time since it's the main meal
            slot_max_time = max_cooking_time
            if slot == "dinner" and max_cooking_time < 45:
                slot_max_time = 45  # Minimum 45 min for dinner to ensure good options

            # Check for INCLUDE rules that apply to this slot
            include_items = await self._process_include_rules(
                slot=slot,
                day_index=day_index,
                include_tracker=include_tracker,
                prefs=prefs,
                max_cooking_time=slot_max_time,
                exclude_ingredients=exclude_ingredients,
                used_recipe_ids=used_recipe_ids,
            )

            if include_items:
                # INCLUDE rules satisfied - add them plus any needed pairs
                items_per_slot = config.meal_structure.items_per_slot

                if len(include_items) >= items_per_slot:
                    # Multiple INCLUDE rules fill the slot
                    meals[slot] = include_items[:items_per_slot]
                else:
                    # Need to add complementary items
                    for item in include_items:
                        meals[slot].append(item)
                        used_recipe_ids.add(item.recipe_id)
                        # Track main ingredient
                        self._track_main_ingredient(item.recipe_name, used_ingredients_today)

                    # Add complementary pair(s)
                    needed = items_per_slot - len(meals[slot])
                    if needed > 0 and include_items:
                        # Get pairing for the first include item
                        primary = include_items[0]
                        pair_item = await self._get_complementary_item(
                            primary_category=primary.category,
                            config=config,
                            prefs=prefs,
                            meal_type=slot,
                            max_cooking_time=slot_max_time,
                            exclude_ingredients=exclude_ingredients,
                            used_recipe_ids=used_recipe_ids,
                            used_ingredients_today=used_ingredients_today,
                        )
                        if pair_item:
                            meals[slot].append(pair_item)
                            used_recipe_ids.add(pair_item.recipe_id)
                            self._track_main_ingredient(pair_item.recipe_name, used_ingredients_today)
            else:
                # No INCLUDE rules - use default pairing
                paired_items = await self._generate_paired_meal(
                    slot=slot,
                    config=config,
                    prefs=prefs,
                    max_cooking_time=slot_max_time,
                    exclude_ingredients=exclude_ingredients,
                    used_recipe_ids=used_recipe_ids,
                    used_ingredients_today=used_ingredients_today,
                )
                meals[slot] = paired_items
                for item in paired_items:
                    used_recipe_ids.add(item.recipe_id)
                    self._track_main_ingredient(item.recipe_name, used_ingredients_today)

        return meals

    def _track_main_ingredient(self, recipe_name: str, used_ingredients: set[str]) -> None:
        """Extract and track main ingredient from recipe name to avoid repetition."""
        # Common main ingredients to track
        main_ingredients = [
            "rajma", "chole", "dal", "paneer", "aloo", "gobi", "palak",
            "bhindi", "baingan", "matar", "chana", "moong", "toor",
            "idli", "dosa", "paratha", "roti", "rice", "biryani", "pulao",
            "sambar", "rasam", "curry", "sabzi", "khichdi", "poha", "upma",
        ]
        name_lower = recipe_name.lower()
        for ing in main_ingredients:
            if ing in name_lower:
                used_ingredients.add(ing)

    def _recipe_uses_ingredient_today(self, recipe: dict, used_ingredients: set[str]) -> bool:
        """Check if recipe uses an ingredient already used today."""
        name_lower = recipe.get("name", "").lower()
        for ing in used_ingredients:
            if ing in name_lower:
                return True
        return False

    def _build_exclude_list(self, prefs: UserPreferences) -> set[str]:
        """Build set of ingredients to exclude from exclude rules, allergies, dislikes."""
        exclude = set()

        # From EXCLUDE rules with NEVER frequency
        for rule in prefs.exclude_rules:
            if rule.get("frequency") == "NEVER":
                target = rule.get("target", "").lower()
                if target:
                    exclude.add(target)
                    # Also add singular/plural variants
                    if target.endswith("s"):
                        exclude.add(target[:-1])  # peanuts -> peanut
                    else:
                        exclude.add(target + "s")  # peanut -> peanuts

        # From allergies - CRITICAL: allergies must be strictly excluded
        for allergy in prefs.allergies:
            if isinstance(allergy, dict):
                ingredient = allergy.get("ingredient", "").lower()
            else:
                ingredient = str(allergy).lower()
            if ingredient:
                exclude.add(ingredient)
                # Also add singular/plural variants for allergies
                if ingredient.endswith("s"):
                    exclude.add(ingredient[:-1])  # peanuts -> peanut
                else:
                    exclude.add(ingredient + "s")  # peanut -> peanuts
                # Common allergen variants
                allergen_variants = {
                    "peanut": ["peanuts", "groundnut", "groundnuts", "moongphali"],
                    "peanuts": ["peanut", "groundnut", "groundnuts", "moongphali"],
                    "dairy": ["milk", "cheese", "paneer", "curd", "yogurt", "cream", "butter", "ghee"],
                    "gluten": ["wheat", "maida", "atta", "bread", "roti", "naan"],
                    "shellfish": ["shrimp", "prawn", "crab", "lobster"],
                    "tree nuts": ["almond", "cashew", "walnut", "pistachio", "kaju", "badam"],
                }
                if ingredient in allergen_variants:
                    for variant in allergen_variants[ingredient]:
                        exclude.add(variant)

        # From dislikes
        for dislike in prefs.dislikes:
            exclude.add(dislike.lower())

        logger.debug(f"Built exclude list with {len(exclude)} items: {exclude}")
        return exclude

    async def _process_include_rules(
        self,
        slot: str,
        day_index: int,
        include_tracker: dict[str, dict],
        prefs: UserPreferences,
        max_cooking_time: int,
        exclude_ingredients: set[str],
        used_recipe_ids: set[str],
    ) -> list[MealItem]:
        """Process INCLUDE rules for a slot and return items to include."""
        items = []

        for rule_id, tracker in include_tracker.items():
            # Skip if this slot doesn't match
            if slot not in tracker["meal_slots"]:
                continue

            # Skip if quota already met
            if tracker["times_assigned"] >= tracker["times_needed"]:
                continue

            # Determine if we should assign on this day
            should_assign = False
            frequency = tracker["frequency"]

            if frequency == "DAILY":
                should_assign = True
            elif frequency == "WEEKLY" or frequency == "TIMES_PER_WEEK":
                # Spread evenly across the week
                times_needed = tracker["times_needed"]
                times_assigned = tracker["times_assigned"]
                days_remaining = 7 - day_index
                times_remaining = times_needed - times_assigned

                if times_remaining > 0:
                    # Must assign if running out of days
                    if days_remaining <= times_remaining:
                        should_assign = True
                    # Otherwise assign on evenly spaced days
                    elif times_needed > 0 and (day_index % max(1, 7 // times_needed)) == 0:
                        should_assign = True

            if not should_assign:
                continue

            # Find recipe matching the target
            target = tracker["target"]
            config = await self.config_service.get_config()
            aliases = self.config_service.get_ingredient_aliases(target)

            recipes = await self.recipe_repo.search_by_ingredient(
                ingredient=target,
                ingredient_aliases=aliases,
                cuisine_type=prefs.cuisine_type,
                dietary_tags=prefs.dietary_tags,
                meal_type=slot,
                max_time_minutes=max_cooking_time,
                exclude_ids=used_recipe_ids,
                limit=10,
            )

            # Filter out excluded ingredients
            recipes = self._filter_by_excludes(recipes, exclude_ingredients)

            if recipes:
                recipe = random.choice(recipes)
                item = self._recipe_to_meal_item(recipe)
                items.append(item)
                tracker["times_assigned"] += 1
                logger.debug(f"INCLUDE rule '{target}' assigned to {slot}")

        return items

    async def _generate_paired_meal(
        self,
        slot: str,
        config: MealGenerationConfig,
        prefs: UserPreferences,
        max_cooking_time: int,
        exclude_ingredients: set[str],
        used_recipe_ids: set[str],
        used_ingredients_today: set[str] = None,
    ) -> list[MealItem]:
        """Generate a meal with complementary paired items."""
        if used_ingredients_today is None:
            used_ingredients_today = set()

        items = []

        # Get default pairs for this meal type
        pairs = config.meal_type_pairs.get(slot, [])

        if not pairs:
            # Fallback to generic pair based on slot
            if slot == "breakfast":
                pairs = ["paratha:chai", "poha:chai", "idli:sambar", "dosa:chutney"]
            elif slot == "dinner":
                pairs = ["dal:roti", "sabzi:paratha", "curry:rice", "dal:rice"]
            elif slot == "snacks":
                pairs = ["snack:chai", "snack:chutney"]
            else:  # lunch
                pairs = ["dal:rice", "sabzi:roti", "curry:rice"]

        # Try each pair until we find one that works
        random.shuffle(pairs)

        for pair_str in pairs:
            if ":" not in pair_str:
                continue

            primary_cat, accompaniment_cat = pair_str.split(":")

            primary, accompaniment = await self.recipe_repo.get_recipe_pair(
                primary_category=primary_cat,
                accompaniment_category=accompaniment_cat,
                cuisine_type=prefs.cuisine_type,
                dietary_tags=prefs.dietary_tags,
                meal_type=slot,
                max_time_minutes=max_cooking_time,
                exclude_ids=used_recipe_ids,
            )

            if primary:
                # Filter by excludes and daily ingredient check
                if (not self._recipe_matches_excludes(primary, exclude_ingredients) and
                    not self._recipe_uses_ingredient_today(primary, used_ingredients_today)):
                    items.append(self._recipe_to_meal_item(primary))

            if accompaniment:
                if (not self._recipe_matches_excludes(accompaniment, exclude_ingredients) and
                    not self._recipe_uses_ingredient_today(accompaniment, used_ingredients_today)):
                    items.append(self._recipe_to_meal_item(accompaniment))

            if len(items) >= config.meal_structure.items_per_slot:
                break

        # If we still don't have enough items, fall back to any recipes
        if len(items) < config.meal_structure.items_per_slot:
            needed = config.meal_structure.items_per_slot - len(items)
            fallback_recipes = await self.recipe_repo.search(
                cuisine_type=prefs.cuisine_type,
                dietary_tags=prefs.dietary_tags,
                meal_type=slot,
                max_time_minutes=max_cooking_time,
                limit=needed * 5,
            )

            fallback_recipes = self._filter_by_excludes(fallback_recipes, exclude_ingredients)
            fallback_recipes = [r for r in fallback_recipes if r.get("id") not in used_recipe_ids]
            # Prefer recipes that don't repeat today's ingredients
            fallback_recipes = [r for r in fallback_recipes
                               if not self._recipe_uses_ingredient_today(r, used_ingredients_today)]

            for recipe in fallback_recipes[:needed]:
                items.append(self._recipe_to_meal_item(recipe))

        # CRITICAL: If still empty, try again with relaxed time constraint (no time limit)
        if len(items) < config.meal_structure.items_per_slot:
            logger.warning(f"Slot {slot} has {len(items)} items, retrying with relaxed time constraint")
            needed = config.meal_structure.items_per_slot - len(items)

            # Search without time limit
            fallback_recipes = await self.recipe_repo.search(
                cuisine_type=prefs.cuisine_type,
                dietary_tags=prefs.dietary_tags,
                meal_type=slot,
                max_time_minutes=None,  # No time limit
                limit=needed * 5,
            )

            fallback_recipes = self._filter_by_excludes(fallback_recipes, exclude_ingredients)
            fallback_recipes = [r for r in fallback_recipes if r.get("id") not in used_recipe_ids]
            # Skip ingredient check for fallback to ensure we fill the slot
            existing_ids = {item.recipe_id for item in items}
            fallback_recipes = [r for r in fallback_recipes if r.get("id") not in existing_ids]

            for recipe in fallback_recipes[:needed]:
                items.append(self._recipe_to_meal_item(recipe))

        return items[:config.meal_structure.items_per_slot]

    async def _get_complementary_item(
        self,
        primary_category: str,
        config: MealGenerationConfig,
        prefs: UserPreferences,
        meal_type: str,
        max_cooking_time: int,
        exclude_ingredients: set[str],
        used_recipe_ids: set[str],
        used_ingredients_today: set[str] = None,
    ) -> Optional[MealItem]:
        """Get a complementary item to pair with a primary recipe."""
        if used_ingredients_today is None:
            used_ingredients_today = set()

        cuisine = prefs.cuisine_type or "north"

        # Get pairing categories for this primary
        pairing_categories = self.config_service.get_pairing_categories(cuisine, primary_category)

        if not pairing_categories:
            # Default pairings if not found in config
            default_pairs = {
                "dal": ["rice", "roti"],
                "sabzi": ["roti", "paratha"],
                "curry": ["rice", "naan"],
                "dosa": ["sambar", "chutney"],
                "idli": ["sambar", "chutney"],
                "paratha": ["chai", "curd"],
                "poha": ["chai"],
                "rice": ["dal", "sambar"],
            }
            pairing_categories = default_pairs.get(primary_category, ["rice", "roti"])

        # Search for recipes in pairing categories
        for category in pairing_categories:
            recipes = await self.recipe_repo.search_by_category(
                category=category,
                cuisine_type=prefs.cuisine_type,
                dietary_tags=prefs.dietary_tags,
                meal_type=meal_type,
                max_time_minutes=max_cooking_time,
                exclude_ids=used_recipe_ids,
                limit=10,
            )

            recipes = self._filter_by_excludes(recipes, exclude_ingredients)
            # Prefer recipes that don't repeat today's ingredients
            recipes = [r for r in recipes
                      if not self._recipe_uses_ingredient_today(r, used_ingredients_today)]

            if recipes:
                return self._recipe_to_meal_item(random.choice(recipes))

        # Fallback: search without ingredient restriction
        for category in pairing_categories:
            recipes = await self.recipe_repo.search_by_category(
                category=category,
                cuisine_type=prefs.cuisine_type,
                dietary_tags=prefs.dietary_tags,
                meal_type=meal_type,
                max_time_minutes=max_cooking_time,
                exclude_ids=used_recipe_ids,
                limit=10,
            )
            recipes = self._filter_by_excludes(recipes, exclude_ingredients)
            if recipes:
                return self._recipe_to_meal_item(random.choice(recipes))

        return None

    def _filter_by_excludes(self, recipes: list[dict], exclude_ingredients: set[str]) -> list[dict]:
        """Filter recipes that contain excluded ingredients."""
        filtered = []
        for recipe in recipes:
            if not self._recipe_matches_excludes(recipe, exclude_ingredients):
                filtered.append(recipe)
        return filtered

    def _recipe_matches_excludes(self, recipe: dict, exclude_ingredients: set[str]) -> bool:
        """Check if recipe contains any excluded ingredients."""
        if not exclude_ingredients:
            return False

        # Check recipe name
        name = recipe.get("name", "").lower()
        for excluded in exclude_ingredients:
            if excluded in name:
                return True

        # Check ingredients
        for ing in recipe.get("ingredients", []):
            if isinstance(ing, dict):
                ing_name = ing.get("name", "").lower()
            else:
                ing_name = str(ing).lower()

            for excluded in exclude_ingredients:
                if excluded in ing_name:
                    return True

        return False

    def _recipe_to_meal_item(self, recipe: dict) -> MealItem:
        """Convert a recipe dict to a MealItem."""
        nutrition = recipe.get("nutrition") or {}
        calories = nutrition.get("calories", 0) if isinstance(nutrition, dict) else 0

        return MealItem(
            id=str(uuid.uuid4()),
            recipe_id=recipe.get("id", ""),
            recipe_name=recipe.get("name", "Unknown Recipe"),
            recipe_image_url=recipe.get("image_url"),
            prep_time_minutes=recipe.get("prep_time_minutes") or recipe.get("total_time_minutes") or 30,
            calories=calories,
            is_locked=False,
            dietary_tags=recipe.get("dietary_tags", []),
            category=recipe.get("category", "other"),
        )
