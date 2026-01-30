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
    is_generic: bool = False  # True if no database recipe (user makes their own)


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
    # Meal generation settings
    items_per_meal: int = 2  # Number of items per meal slot (1-4)
    strict_allergen_mode: bool = True  # Strictly exclude allergens
    strict_dietary_mode: bool = True  # Strictly enforce dietary tags
    allow_recipe_repeat: bool = False  # Allow same recipe multiple times per week


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
            f"exclude_rules={len(prefs.exclude_rules)}, "
            f"items_per_meal={prefs.items_per_meal}, "
            f"strict_allergen={prefs.strict_allergen_mode}, "
            f"allow_repeat={prefs.allow_recipe_repeat}"
        )

        # Track used recipes to avoid duplicates (unless allow_recipe_repeat is True)
        used_recipe_ids: set[str] = set()
        # If allow_recipe_repeat is True, we pass empty set to exclude_ids to allow repetition
        exclude_recipe_ids = set() if prefs.allow_recipe_repeat else used_recipe_ids

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
                exclude_recipe_ids=exclude_recipe_ids,
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
            # Meal generation settings
            items_per_meal=prefs_data.get("items_per_meal", 2),
            strict_allergen_mode=prefs_data.get("strict_allergen_mode", True),
            strict_dietary_mode=prefs_data.get("strict_dietary_mode", True),
            allow_recipe_repeat=prefs_data.get("allow_recipe_repeat", False),
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

    def _calculate_items_for_slot(
        self,
        config: MealGenerationConfig,
        max_cooking_time: int,
        slot: str,
        user_items_per_meal: int = 2,
    ) -> tuple[int, int]:
        """Calculate number of main and total items based on cooking time.

        Implements Design Decision #1: Variable items per meal.
        - ≤30 min → 1 main + 1 complementary = 2 items
        - 30-45 min → 2 mains + 1 complementary = 3 items (but we return 2 for now)
        - >45 min → 2+ mains + complementary = 3+ items

        User preference (items_per_meal) overrides the config default.

        Args:
            config: Meal generation config with time thresholds
            max_cooking_time: Maximum cooking time for this slot/day
            slot: Meal slot (breakfast, lunch, dinner, snacks)
            user_items_per_meal: User's preferred items per meal (1-4)

        Returns:
            Tuple of (main_items_count, total_items_count)
        """
        # Use user preference as the cap (1-4 items)
        items_cap = max(1, min(4, user_items_per_meal))

        # Check if time-based items is enabled in config
        time_based = config.meal_structure.time_based_items if hasattr(config.meal_structure, 'time_based_items') else None

        if not time_based or not time_based.get("enabled", False):
            # Use user's items_per_meal setting
            return (1, items_cap)

        thresholds = time_based.get("thresholds", [])
        if not thresholds:
            return (1, items_cap)

        # Find matching threshold (thresholds should be sorted by max_time ascending)
        main_items = 1
        for threshold in sorted(thresholds, key=lambda t: t.get("max_time", 0)):
            if max_cooking_time <= threshold.get("max_time", 9999):
                main_items = threshold.get("main_items", 1)
                break

        # Total items = main_items + 1 complementary, capped at user preference
        total_items = min(main_items + 1, items_cap)

        logger.debug(
            f"Slot {slot}: cooking_time={max_cooking_time}min → "
            f"main_items={main_items}, total_items={total_items} (user_cap={items_cap})"
        )

        return (main_items, total_items)

    async def _generate_day_meals(
        self,
        config: MealGenerationConfig,
        prefs: UserPreferences,
        day_index: int,
        day_name: str,
        max_cooking_time: int,
        include_tracker: dict[str, dict],
        used_recipe_ids: set[str],
        exclude_recipe_ids: set[str] = None,
    ) -> dict[str, list[MealItem]]:
        """Generate all meals for a single day with pairing."""
        meals = {
            "breakfast": [],
            "lunch": [],
            "dinner": [],
            "snacks": [],
        }

        # Use exclude_recipe_ids for filtering (may be empty if allow_recipe_repeat is True)
        # We still track used_recipe_ids for reference, but exclude_recipe_ids controls filtering
        if exclude_recipe_ids is None:
            exclude_recipe_ids = used_recipe_ids

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

            # Calculate items needed for this slot based on cooking time
            # User's items_per_meal preference overrides config default
            main_items_count, items_per_slot = self._calculate_items_for_slot(
                config=config,
                max_cooking_time=slot_max_time,
                slot=slot,
                user_items_per_meal=prefs.items_per_meal,
            )

            # Check for INCLUDE rules that apply to this slot
            include_items = await self._process_include_rules(
                slot=slot,
                day_index=day_index,
                include_tracker=include_tracker,
                prefs=prefs,
                max_cooking_time=slot_max_time,
                exclude_ingredients=exclude_ingredients,
                used_recipe_ids=exclude_recipe_ids,  # Use exclude set (empty if allow_repeat)
            )

            if include_items:
                # INCLUDE rules satisfied - add them plus any needed pairs
                # Use calculated items_per_slot instead of fixed config value

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
                        # Infer category from recipe name if database category is "other" or empty
                        primary_category = primary.category
                        if not primary_category or primary_category == "other":
                            primary_category = self._infer_category_from_name(
                                primary.recipe_name, default_category="other"
                            )
                        pair_item = await self._get_complementary_item(
                            primary_category=primary_category,
                            config=config,
                            prefs=prefs,
                            meal_type=slot,
                            max_cooking_time=slot_max_time,
                            exclude_ingredients=exclude_ingredients,
                            used_recipe_ids=exclude_recipe_ids,  # Use exclude set (empty if allow_repeat)
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
                    used_recipe_ids=exclude_recipe_ids,  # Use exclude set (empty if allow_repeat)
                    used_ingredients_today=used_ingredients_today,
                    items_per_slot=items_per_slot,
                )
                meals[slot] = paired_items
                for item in paired_items:
                    used_recipe_ids.add(item.recipe_id)
                    self._track_main_ingredient(item.recipe_name, used_ingredients_today)

        return meals

    def _infer_category_from_name(self, recipe_name: str, default_category: str = "other") -> str:
        """Infer recipe category from name when database category is missing.

        This is needed because the recipe database has category=NULL for most recipes.
        We use the recipe name to determine the category for pairing purposes.
        """
        name_lower = recipe_name.lower()

        # Category keywords in priority order (more specific first)
        category_keywords = {
            "chai": ["chai", "tea", "masala tea"],
            "coffee": ["coffee", "kaapi"],
            "paratha": ["paratha", "parantha"],
            "roti": ["roti", "chapati", "phulka", "naan", "kulcha"],
            "rice": ["rice", "chawal", "pulao", "biryani", "khichdi"],
            "dal": ["dal", "daal", "sambhar", "sambar", "rasam"],
            "sabzi": ["sabzi", "sabji", "curry", "bhaji", "fry"],
            "dosa": ["dosa", "dosai"],
            "idli": ["idli", "idly"],
            "poha": ["poha", "pohe"],
            "upma": ["upma", "uppuma"],
            "samosa": ["samosa"],
            "pakora": ["pakora", "pakoda", "bhajia", "bhaji"],
        }

        for category, keywords in category_keywords.items():
            for keyword in keywords:
                if keyword in name_lower:
                    return category

        return default_category

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
        """Build set of ingredients to exclude from exclude rules, allergies, dislikes.

        Respects user preferences:
        - strict_allergen_mode: If True (default), allergies are strictly excluded
        - If False, allergies are not excluded (user accepts risk)
        """
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

        # From allergies - respect strict_allergen_mode setting
        if prefs.strict_allergen_mode:
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
        else:
            logger.warning(
                f"strict_allergen_mode is OFF - allergies will NOT be excluded: {prefs.allergies}"
            )

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

            # OPTIMIZATION: Single broader search, prefer user's cuisine in memory
            # This reduces 2-4 API calls to 1 (with caching, subsequent calls are free)
            if frequency == "DAILY":
                # For DAILY items: search without cuisine filter, prefer matches in memory
                # This avoids the 2-call pattern and ensures we always find daily items
                recipes = await self.recipe_repo.search_by_ingredient(
                    ingredient=target,
                    ingredient_aliases=aliases,
                    cuisine_type=None,  # Broader search
                    dietary_tags=prefs.dietary_tags,
                    meal_type=None,  # Don't filter by meal type for daily items
                    max_time_minutes=max_cooking_time,
                    exclude_ids=None,  # Allow reusing recipes for daily items
                    limit=40,  # Fetch more to have cuisine variety
                )
                recipes = self._filter_by_excludes(recipes, exclude_ingredients)

                # Prefer user's cuisine but don't require it
                if recipes and prefs.cuisine_type:
                    cuisine_matches = [
                        r for r in recipes
                        if r.get("cuisine_type", "").lower() == prefs.cuisine_type.lower()
                    ]
                    if cuisine_matches:
                        recipes = cuisine_matches
            else:
                # For non-DAILY rules: single broader search without meal_type filter
                # Filter/prefer meal_type matches in memory to reduce API calls
                recipes = await self.recipe_repo.search_by_ingredient(
                    ingredient=target,
                    ingredient_aliases=aliases,
                    cuisine_type=prefs.cuisine_type,
                    dietary_tags=prefs.dietary_tags,
                    meal_type=None,  # Broader search - filter in memory
                    max_time_minutes=max_cooking_time,
                    exclude_ids=used_recipe_ids,
                    limit=30,  # Fetch more to filter in memory
                )
                recipes = self._filter_by_excludes(recipes, exclude_ingredients)

                # Prefer recipes that match the slot's meal_type, but don't require it
                if recipes:
                    meal_matches = [
                        r for r in recipes
                        if slot in r.get("meal_types", [])
                    ]
                    if meal_matches:
                        recipes = meal_matches

                # FALLBACK: If no recipes found within time constraint, try without time limit
                # This ensures INCLUDE rules can be satisfied even on busy days
                if not recipes:
                    logger.debug(f"INCLUDE rule '{target}': no recipes in {max_cooking_time}min, trying without time limit")
                    recipes = await self.recipe_repo.search_by_ingredient(
                        ingredient=target,
                        ingredient_aliases=aliases,
                        cuisine_type=prefs.cuisine_type,
                        dietary_tags=prefs.dietary_tags,
                        meal_type=None,
                        max_time_minutes=None,  # No time limit
                        exclude_ids=used_recipe_ids,
                        limit=30,
                    )
                    recipes = self._filter_by_excludes(recipes, exclude_ingredients)

                    # Prefer recipes that match the slot's meal_type
                    if recipes:
                        meal_matches = [
                            r for r in recipes
                            if slot in r.get("meal_types", [])
                        ]
                        if meal_matches:
                            recipes = meal_matches

            if recipes:
                # Prefer recipes with the target ingredient in the name
                # This ensures "Paneer" rule picks recipes like "Paneer Tikka" over
                # recipes that just have paneer as an ingredient
                target_lower = target.lower()
                name_matches = [r for r in recipes if target_lower in r.get("name", "").lower()]
                if name_matches:
                    recipe = random.choice(name_matches)
                else:
                    recipe = random.choice(recipes)
                item = self._recipe_to_meal_item(recipe)
                items.append(item)
                tracker["times_assigned"] += 1
                logger.debug(f"INCLUDE rule '{target}' assigned to {slot}")
            else:
                # FINAL FALLBACK: Create generic suggestion when no database recipe found
                # This ensures INCLUDE rules are always satisfied, even if as a "make your own"
                logger.info(f"INCLUDE rule '{target}': no database recipes, using generic suggestion")
                generic_item = MealItem(
                    id=str(uuid.uuid4()),
                    recipe_id="GENERIC",
                    recipe_name=target.title(),  # Use the target name (e.g., "Paneer", "Dal")
                    recipe_image_url=None,
                    prep_time_minutes=30,
                    calories=0,
                    is_locked=False,
                    dietary_tags=prefs.dietary_tags or [],
                    category="other",
                    is_generic=True,
                )
                items.append(generic_item)
                tracker["times_assigned"] += 1
                logger.debug(f"INCLUDE rule '{target}' assigned as generic to {slot}")

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
        items_per_slot: int = None,
    ) -> list[MealItem]:
        """Generate a meal with complementary paired items.

        Args:
            slot: Meal slot (breakfast, lunch, dinner, snacks)
            config: Meal generation config
            prefs: User preferences
            max_cooking_time: Maximum cooking time for this slot
            exclude_ingredients: Set of ingredients to exclude
            used_recipe_ids: Set of recipe IDs already used this week
            used_ingredients_today: Set of main ingredients used today
            items_per_slot: Number of items to generate (if None, uses config default)

        Returns:
            List of MealItem objects for the slot
        """
        if used_ingredients_today is None:
            used_ingredients_today = set()

        # Use passed items_per_slot or fall back to config
        target_items = items_per_slot if items_per_slot is not None else config.meal_structure.items_per_slot

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

            if len(items) >= target_items:
                break

        # If we still don't have enough items, fall back to any recipes
        if len(items) < target_items:
            needed = target_items - len(items)
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
        if len(items) < target_items:
            logger.warning(f"Slot {slot} has {len(items)} items, retrying with relaxed time constraint")
            needed = target_items - len(items)

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

        # FINAL FALLBACK: If still not enough items, use generic suggestions
        # This happens when database lacks recipes for user's cuisine/preferences
        if len(items) < target_items:
            needed = target_items - len(items)
            logger.warning(
                f"Slot {slot} still needs {needed} items, using generic suggestions"
            )

            generic_dishes = self._get_generic_dishes_for_slot(
                slot=slot,
                cuisine_type=prefs.cuisine_type,
                exclude_ingredients=exclude_ingredients,
            )

            # Add generic items
            for dish in generic_dishes[:needed]:
                items.append(self._create_generic_meal_item(dish, slot))
                logger.info(f"Added generic suggestion: {dish['name']} to {slot}")

        return items[:target_items]

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
                # Breakfast beverages - pair with snacks/breads
                "chai": ["paratha", "poha", "toast", "biscuit", "samosa"],
                "tea": ["paratha", "poha", "toast", "biscuit", "samosa"],
                "coffee": ["toast", "dosa", "idli", "poha"],
                # Fallback for unknown categories
                "other": ["paratha", "roti", "rice"],
            }
            pairing_categories = default_pairs.get(primary_category, ["paratha", "roti", "rice"])

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
            is_generic=False,
        )

    def _get_generic_dishes_for_slot(
        self,
        slot: str,
        cuisine_type: Optional[str],
        exclude_ingredients: set[str],
    ) -> list[dict]:
        """Get generic dish suggestions when database recipes unavailable.

        Returns dish information that can be shown as "make your own" suggestions.
        Uses reference data from dishes.yaml.

        Args:
            slot: Meal slot (breakfast, lunch, dinner, snacks)
            cuisine_type: User's preferred cuisine
            exclude_ingredients: Set of ingredients to exclude

        Returns:
            List of dish info dicts with name, category, and pairs_with
        """
        # Generic dishes by slot and cuisine - used when no database recipes found
        # These are common Indian dishes organized by meal type
        generic_dishes = {
            "breakfast": {
                "north": [
                    {"name": "Aloo Paratha", "category": "paratha", "pairs_with": ["chai", "curd"]},
                    {"name": "Poha", "category": "poha", "pairs_with": ["chai", "sev"]},
                    {"name": "Chole Bhature", "category": "one_pot", "pairs_with": ["lassi"]},
                    {"name": "Halwa Puri", "category": "breakfast_main", "pairs_with": ["aloo sabzi"]},
                ],
                "south": [
                    {"name": "Idli", "category": "idli", "pairs_with": ["sambar", "chutney"]},
                    {"name": "Dosa", "category": "dosa", "pairs_with": ["sambar", "chutney"]},
                    {"name": "Upma", "category": "upma", "pairs_with": ["chutney", "coffee"]},
                    {"name": "Pongal", "category": "breakfast_main", "pairs_with": ["sambar", "chutney"]},
                ],
                "east": [
                    {"name": "Luchi", "category": "bread", "pairs_with": ["aloor dom", "cholar dal"]},
                    {"name": "Paratha", "category": "paratha", "pairs_with": ["chai", "curry"]},
                ],
                "west": [
                    {"name": "Thepla", "category": "paratha", "pairs_with": ["chai", "curd", "pickle"]},
                    {"name": "Dhokla", "category": "snack", "pairs_with": ["chutney", "chai"]},
                    {"name": "Fafda", "category": "snack", "pairs_with": ["jalebi", "chai"]},
                    {"name": "Poha", "category": "poha", "pairs_with": ["chai", "sev"]},
                ],
            },
            "lunch": {
                "north": [
                    {"name": "Dal", "category": "dal", "pairs_with": ["rice", "roti"]},
                    {"name": "Sabzi", "category": "sabzi", "pairs_with": ["roti", "rice"]},
                    {"name": "Rajma Chawal", "category": "curry", "pairs_with": ["salad"]},
                    {"name": "Chole Chawal", "category": "curry", "pairs_with": ["onion salad"]},
                ],
                "south": [
                    {"name": "Sambar Rice", "category": "sambar", "pairs_with": ["papad", "pickle"]},
                    {"name": "Rasam Rice", "category": "rasam", "pairs_with": ["papad"]},
                    {"name": "Curd Rice", "category": "rice", "pairs_with": ["pickle"]},
                    {"name": "Vegetable Curry", "category": "curry", "pairs_with": ["rice"]},
                ],
                "east": [
                    {"name": "Dal Bhaat", "category": "dal", "pairs_with": ["rice", "sabzi"]},
                    {"name": "Fish Curry", "category": "curry", "pairs_with": ["rice"]},
                    {"name": "Shukto", "category": "sabzi", "pairs_with": ["rice"]},
                ],
                "west": [
                    {"name": "Dal Dhokli", "category": "dal", "pairs_with": ["salad"]},
                    {"name": "Undhiyu", "category": "sabzi", "pairs_with": ["puri"]},
                    {"name": "Gujarati Kadhi", "category": "curry", "pairs_with": ["rice"]},
                ],
            },
            "dinner": {
                "north": [
                    {"name": "Dal", "category": "dal", "pairs_with": ["roti", "paratha"]},
                    {"name": "Paneer Curry", "category": "curry", "pairs_with": ["roti", "naan"]},
                    {"name": "Mix Veg", "category": "sabzi", "pairs_with": ["roti"]},
                    {"name": "Khichdi", "category": "khichdi", "pairs_with": ["curd", "papad"]},
                ],
                "south": [
                    {"name": "Sambar", "category": "sambar", "pairs_with": ["rice", "dosa"]},
                    {"name": "Vegetable Curry", "category": "curry", "pairs_with": ["rice", "appam"]},
                    {"name": "Kootu", "category": "curry", "pairs_with": ["rice"]},
                ],
                "east": [
                    {"name": "Dal", "category": "dal", "pairs_with": ["rice", "roti"]},
                    {"name": "Sabzi", "category": "sabzi", "pairs_with": ["rice", "roti"]},
                ],
                "west": [
                    {"name": "Dal", "category": "dal", "pairs_with": ["rice", "roti", "bhakri"]},
                    {"name": "Sabzi", "category": "sabzi", "pairs_with": ["roti", "bhakri"]},
                ],
            },
            "snacks": {
                "north": [
                    {"name": "Samosa", "category": "snack", "pairs_with": ["chai", "chutney"]},
                    {"name": "Pakora", "category": "snack", "pairs_with": ["chai", "chutney"]},
                    {"name": "Sandwich", "category": "snack", "pairs_with": ["chai"]},
                ],
                "south": [
                    {"name": "Vada", "category": "vada", "pairs_with": ["sambar", "chutney"]},
                    {"name": "Bonda", "category": "snack", "pairs_with": ["chutney"]},
                    {"name": "Murukku", "category": "snack", "pairs_with": ["coffee"]},
                ],
                "east": [
                    {"name": "Singara", "category": "snack", "pairs_with": ["chai"]},
                    {"name": "Beguni", "category": "snack", "pairs_with": ["chai"]},
                ],
                "west": [
                    {"name": "Kachori", "category": "snack", "pairs_with": ["chai"]},
                    {"name": "Khaman", "category": "snack", "pairs_with": ["chutney"]},
                    {"name": "Vada Pav", "category": "snack", "pairs_with": ["chai"]},
                ],
            },
        }

        # Get dishes for the slot
        slot_dishes = generic_dishes.get(slot, {})

        # Prefer user's cuisine, fall back to north
        cuisine = (cuisine_type or "north").lower()
        cuisine_dishes = slot_dishes.get(cuisine, slot_dishes.get("north", []))

        # Filter out dishes that match excluded ingredients
        filtered_dishes = []
        for dish in cuisine_dishes:
            name_lower = dish["name"].lower()
            is_excluded = any(excl in name_lower for excl in exclude_ingredients)
            if not is_excluded:
                filtered_dishes.append(dish)

        # If all dishes filtered out, try other cuisines
        if not filtered_dishes:
            for alt_cuisine in ["north", "south", "west", "east"]:
                if alt_cuisine == cuisine:
                    continue
                alt_dishes = slot_dishes.get(alt_cuisine, [])
                for dish in alt_dishes:
                    name_lower = dish["name"].lower()
                    is_excluded = any(excl in name_lower for excl in exclude_ingredients)
                    if not is_excluded:
                        filtered_dishes.append(dish)
                        if len(filtered_dishes) >= 3:
                            break
                if filtered_dishes:
                    break

        return filtered_dishes

    def _create_generic_meal_item(self, dish: dict, slot: str) -> MealItem:
        """Create a MealItem for a generic dish suggestion.

        Args:
            dish: Dish info dict with name, category, pairs_with
            slot: Meal slot

        Returns:
            MealItem with is_generic=True and recipe_id="GENERIC"
        """
        return MealItem(
            id=str(uuid.uuid4()),
            recipe_id="GENERIC",  # Special marker for generic items
            recipe_name=f"{dish['name']} (make your own)",
            recipe_image_url=None,
            prep_time_minutes=30,  # Default estimate
            calories=0,  # Unknown
            is_locked=False,
            dietary_tags=[],  # Unknown
            category=dish.get("category", "other"),
            is_generic=True,  # Mark as generic suggestion
        )
