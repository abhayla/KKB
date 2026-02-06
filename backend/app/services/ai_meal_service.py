"""AI-powered meal planning service using Gemini.

This service generates personalized meal plans using Google Gemini AI.
Recipes are generated freely by the AI (no database lookup required).
User preferences, rules, and festivals are passed as context to the AI.

Key features:
- AI-generated recipe names (no database dependency)
- Respects INCLUDE/EXCLUDE rules, allergies, dislikes
- Incorporates festival/fasting day context
- Post-processing validation and enforcement
- Retry logic with exponential backoff
"""

import asyncio
import json
import logging
import uuid
from dataclasses import dataclass, field
from datetime import date, timedelta
from typing import Any, Optional

from sqlalchemy import select

from app.ai.gemini_client import generate_text
from app.core.exceptions import ServiceUnavailableError
from app.db.postgres import async_session_maker
from app.models.recipe_rule import NutritionGoal, RecipeRule
from app.repositories.user_repository import UserRepository
from app.services.config_service import ConfigService
from app.services.festival_service import get_festivals_for_date_range

logger = logging.getLogger(__name__)


# ==============================================================================
# DATA CLASSES
# ==============================================================================


@dataclass
class MealItem:
    """A single item in a meal slot."""

    id: str
    recipe_name: str
    prep_time_minutes: int = 30
    dietary_tags: list[str] = field(default_factory=list)
    category: str = "other"
    is_locked: bool = False
    # For API compatibility (AI-generated meals don't have these)
    recipe_id: str = "AI_GENERATED"
    recipe_image_url: Optional[str] = None
    calories: int = 0


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
    cuisine_preferences: list[str] = field(default_factory=lambda: ["north"])
    allergies: list[dict] = field(default_factory=list)
    dislikes: list[str] = field(default_factory=list)
    weekday_cooking_time: int = 30
    weekend_cooking_time: int = 60
    busy_days: list[str] = field(default_factory=list)
    include_rules: list[dict] = field(default_factory=list)
    exclude_rules: list[dict] = field(default_factory=list)
    family_size: int = 4
    spice_level: str = "medium"


# ==============================================================================
# AI MEAL SERVICE
# ==============================================================================


class AIMealService:
    """Service for generating meal plans using Gemini AI.

    The AI generates recipe names freely based on user preferences,
    without requiring a recipe database lookup.
    """

    def __init__(self):
        self.config_service = ConfigService()
        self.user_repo = UserRepository()

    async def generate_meal_plan(
        self,
        user_id: str,
        week_start_date: date,
    ) -> GeneratedMealPlan:
        """Generate a 7-day meal plan using Gemini AI.

        Args:
            user_id: User ID to generate plan for
            week_start_date: Start date of the week (usually Monday)

        Returns:
            GeneratedMealPlan with 7 days of AI-generated meals

        Raises:
            ServiceUnavailableError: If AI generation fails after retries
        """
        logger.info(f"Generating AI meal plan for user {user_id}")

        # Load user preferences
        prefs = await self._load_user_preferences(user_id)

        # Load festivals for the week
        festivals = await self._load_festivals(week_start_date)

        # Load pairing config
        config = await self.config_service.get_config()

        # Build prompt
        prompt = self._build_prompt(prefs, festivals, config, week_start_date)

        logger.debug(f"Prompt length: {len(prompt)} characters")

        # Generate with retry
        response_text = await self._generate_with_retry(prompt, max_retries=3)

        # Parse response
        plan = self._parse_response(
            response_text,
            week_start_date,
            prefs,
            festivals,
        )

        # Post-process: enforce rules
        plan = self._enforce_rules(plan, prefs)

        logger.info(
            f"Generated meal plan: {plan.week_start_date} to {plan.week_end_date}, "
            f"{len(plan.days)} days"
        )

        return plan

    async def _load_user_preferences(self, user_id: str) -> UserPreferences:
        """Load user preferences from PostgreSQL.

        Reads rules from the new recipe_rules table (normalized) with fallback
        to legacy JSON field for backward compatibility.
        """
        prefs_data = await self.user_repo.get_preferences(user_id)

        if not prefs_data:
            logger.warning(f"No preferences found for user {user_id}, using defaults")
            return UserPreferences()

        # Load rules from new recipe_rules table
        include_rules = []
        exclude_rules = []

        try:
            async with async_session_maker() as db:
                # Query active INCLUDE rules
                include_result = await db.execute(
                    select(RecipeRule).where(
                        RecipeRule.user_id == user_id,
                        RecipeRule.action == "INCLUDE",
                        RecipeRule.is_active == True,
                    )
                )
                include_db_rules = include_result.scalars().all()

                # Query active EXCLUDE rules
                exclude_result = await db.execute(
                    select(RecipeRule).where(
                        RecipeRule.user_id == user_id,
                        RecipeRule.action == "EXCLUDE",
                        RecipeRule.is_active == True,
                    )
                )
                exclude_db_rules = exclude_result.scalars().all()

                # Convert to dict format expected by the prompt builder
                for rule in include_db_rules:
                    include_rules.append({
                        "id": rule.id,
                        "type": rule.action,
                        "target": rule.target_name,
                        "frequency": rule.frequency_type,
                        "times_per_week": rule.frequency_count,
                        "specific_days": rule.frequency_days.split(",") if rule.frequency_days else [],
                        "meal_slot": [rule.meal_slot] if rule.meal_slot else ["breakfast", "lunch", "dinner", "snacks"],
                        "enforcement": rule.enforcement,
                    })

                for rule in exclude_db_rules:
                    exclude_rules.append({
                        "id": rule.id,
                        "type": rule.action,
                        "target": rule.target_name,
                        "frequency": rule.frequency_type,
                        "times_per_week": rule.frequency_count,
                        "specific_days": rule.frequency_days.split(",") if rule.frequency_days else [],
                        "meal_slot": [rule.meal_slot] if rule.meal_slot else ["breakfast", "lunch", "dinner", "snacks"],
                        "enforcement": rule.enforcement,
                    })

                logger.info(
                    f"Loaded {len(include_rules)} INCLUDE rules and "
                    f"{len(exclude_rules)} EXCLUDE rules from recipe_rules table"
                )

        except Exception as e:
            logger.warning(f"Failed to load rules from recipe_rules table: {e}")
            # Fallback to legacy JSON field
            recipe_rules = prefs_data.get("recipe_rules") or []
            include_rules = [r for r in recipe_rules if r.get("type") == "INCLUDE" and r.get("is_active", True)]
            exclude_rules = [r for r in recipe_rules if r.get("type") == "EXCLUDE" and r.get("is_active", True)]
            logger.info(f"Using legacy recipe_rules JSON field")

        return UserPreferences(
            dietary_tags=prefs_data.get("dietary_tags") or ["vegetarian"],
            cuisine_preferences=prefs_data.get("cuisine_preferences") or ["north"],
            allergies=prefs_data.get("allergies") or [],
            dislikes=prefs_data.get("disliked_ingredients") or [],
            weekday_cooking_time=prefs_data.get("weekday_cooking_time_minutes") or 30,
            weekend_cooking_time=prefs_data.get("weekend_cooking_time_minutes") or 60,
            busy_days=[d.upper() for d in (prefs_data.get("busy_days") or [])],
            include_rules=include_rules,
            exclude_rules=exclude_rules,
            family_size=prefs_data.get("family_size") or 4,
            spice_level=prefs_data.get("spice_level") or "medium",
        )

    async def _load_festivals(self, week_start: date) -> dict[date, dict]:
        """Load festivals for the week from PostgreSQL."""
        week_end = week_start + timedelta(days=6)

        try:
            async with async_session_maker() as db:
                festivals_map = await get_festivals_for_date_range(db, week_start, week_end)

                # Convert Festival models to dicts
                result = {}
                for d, festival in festivals_map.items():
                    result[d] = {
                        "name": festival.name,
                        "is_fasting_day": festival.is_fasting_day,
                        "special_foods": festival.special_foods or [],
                        "avoided_foods": festival.avoided_foods or [],
                    }
                return result
        except Exception as e:
            logger.error(f"Failed to load festivals: {e}")
            return {}

    def _build_prompt(
        self,
        prefs: UserPreferences,
        festivals: dict[date, dict],
        config: Any,
        week_start_date: date,
    ) -> str:
        """Build the prompt for Gemini."""

        # Build preferences section
        dietary_str = ", ".join(prefs.dietary_tags) if prefs.dietary_tags else "vegetarian"
        cuisine_str = ", ".join(prefs.cuisine_preferences) if prefs.cuisine_preferences else "north"

        # Allergies (STRICT - NEVER INCLUDE)
        allergies_str = ""
        if prefs.allergies:
            allergy_items = []
            for a in prefs.allergies:
                if isinstance(a, dict):
                    ing = a.get("ingredient", str(a))
                    severity = a.get("severity", "MODERATE")
                    allergy_items.append(f"{ing} ({severity})")
                else:
                    allergy_items.append(str(a))
            allergies_str = ", ".join(allergy_items)

        # Dislikes (AVOID)
        dislikes_str = ", ".join(prefs.dislikes) if prefs.dislikes else "none"

        # INCLUDE rules
        include_section = ""
        if prefs.include_rules:
            include_lines = []
            for rule in prefs.include_rules:
                target = rule.get("target", "")
                freq = rule.get("frequency", "WEEKLY")
                times = rule.get("times_per_week", 1)
                slots = rule.get("meal_slot", ["breakfast", "lunch", "dinner", "snacks"])
                slots_str = ", ".join(slots)

                if freq == "DAILY":
                    include_lines.append(f"- {target}: DAILY at {slots_str}")
                else:
                    include_lines.append(f"- {target}: {times}x/week at {slots_str}")

            include_section = "\n".join(include_lines)

        # EXCLUDE rules
        exclude_section = ""
        if prefs.exclude_rules:
            exclude_lines = []
            for rule in prefs.exclude_rules:
                target = rule.get("target", "")
                freq = rule.get("frequency", "NEVER")
                specific_days = rule.get("specific_days", [])

                if freq == "NEVER":
                    exclude_lines.append(f"- {target}: NEVER")
                elif freq == "SPECIFIC_DAYS" and specific_days:
                    days_str = ", ".join(specific_days)
                    exclude_lines.append(f"- {target}: On {days_str}")

            exclude_section = "\n".join(exclude_lines)

        # Cooking time limits
        busy_days_str = ", ".join(prefs.busy_days) if prefs.busy_days else "none"

        # Festivals section
        festivals_section = ""
        if festivals:
            festival_lines = []
            for d, f in festivals.items():
                day_name = d.strftime("%A")
                date_str = d.isoformat()
                name = f.get("name", "Festival")
                is_fasting = f.get("is_fasting_day", False)
                special = f.get("special_foods", [])
                avoided = f.get("avoided_foods", [])

                line = f"- {day_name} {date_str}: {name}"
                if is_fasting:
                    line += " (fasting day)"
                if special:
                    line += f"\n  - Special foods: {', '.join(special)}"
                if avoided:
                    line += f"\n  - Avoid: {', '.join(avoided)}"
                festival_lines.append(line)

            festivals_section = "\n".join(festival_lines)

        # Build date list for the week
        dates_section = []
        current = week_start_date
        for i in range(7):
            day_name = current.strftime("%A")
            is_weekend = day_name in ["Saturday", "Sunday"]
            is_busy = day_name.upper() in prefs.busy_days
            max_time = prefs.weekday_cooking_time
            if is_weekend:
                max_time = prefs.weekend_cooking_time
            if is_busy:
                max_time = min(max_time, prefs.weekday_cooking_time)

            dates_section.append(f"- {current.isoformat()} ({day_name}): max {max_time} min")
            current += timedelta(days=1)

        # Get pairing guidance from config
        pairing_section = """
- Dal pairs with: rice, roti, paratha, naan
- Sabzi pairs with: roti, paratha, rice
- Paratha pairs with: chai, raita, pickle, curd
- Dosa/Idli pairs with: sambar, chutney
- Curry pairs with: rice, roti, naan
- Biryani/Pulao pairs with: raita, salad
- Khichdi pairs with: curd, papad, pickle"""

        prompt = f"""You are RasoiAI, an Indian meal planning assistant. Generate a 7-day meal plan.

## USER PREFERENCES (STRICT - MUST FOLLOW)
### Dietary Tags: [{dietary_str}]
### Cuisines: [{cuisine_str}]
### Spice Level: {prefs.spice_level}
### Family Size: {prefs.family_size}

### Allergies (NEVER INCLUDE - STRICT): [{allergies_str if allergies_str else "none"}]
### Dislikes (AVOID): [{dislikes_str}]

### INCLUDE Rules (MUST APPEAR):
{include_section if include_section else "- None specified"}

### EXCLUDE Rules (NEVER INCLUDE):
{exclude_section if exclude_section else "- None specified"}

### Cooking Time Limits:
- Weekdays: Max {prefs.weekday_cooking_time} minutes
- Weekends: Max {prefs.weekend_cooking_time} minutes
- Busy days ({busy_days_str}): Max {prefs.weekday_cooking_time} minutes

### Dates for this week:
{chr(10).join(dates_section)}

## FESTIVALS THIS WEEK
{festivals_section if festivals_section else "No festivals this week."}

## PAIRING GUIDANCE
{pairing_section}

## IMPORTANT RULES
1. Each meal slot MUST have exactly 2 items (a main dish + accompaniment)
2. ALLERGIES are STRICT - NEVER include any dish containing allergen ingredients
3. INCLUDE rules MUST be satisfied at the specified frequency
4. EXCLUDE rules MUST be respected - never include on specified days
5. Prep time for EACH dish must be within the day's cooking time limit
6. Use authentic Indian dish names (e.g., "Aloo Paratha", "Masala Chai", "Dal Tadka")
7. Vary recipes across the week - avoid repeating the same dish

## OUTPUT FORMAT
Return ONLY valid JSON (no markdown, no explanation):
{{
  "days": [
    {{
      "date": "YYYY-MM-DD",
      "day_name": "Monday",
      "breakfast": [
        {{"recipe_name": "Aloo Paratha", "prep_time_minutes": 25, "dietary_tags": ["vegetarian"], "category": "paratha"}},
        {{"recipe_name": "Masala Chai", "prep_time_minutes": 10, "dietary_tags": ["vegetarian"], "category": "chai"}}
      ],
      "lunch": [
        {{"recipe_name": "Dal Tadka", "prep_time_minutes": 30, "dietary_tags": ["vegetarian", "vegan"], "category": "dal"}},
        {{"recipe_name": "Jeera Rice", "prep_time_minutes": 20, "dietary_tags": ["vegetarian", "vegan"], "category": "rice"}}
      ],
      "dinner": [
        {{"recipe_name": "Paneer Butter Masala", "prep_time_minutes": 30, "dietary_tags": ["vegetarian"], "category": "curry"}},
        {{"recipe_name": "Butter Naan", "prep_time_minutes": 15, "dietary_tags": ["vegetarian"], "category": "naan"}}
      ],
      "snacks": [
        {{"recipe_name": "Samosa", "prep_time_minutes": 20, "dietary_tags": ["vegetarian"], "category": "snack"}},
        {{"recipe_name": "Masala Chai", "prep_time_minutes": 10, "dietary_tags": ["vegetarian"], "category": "chai"}}
      ]
    }}
  ]
}}

Generate the complete 7-day meal plan now:"""

        return prompt

    async def _generate_with_retry(self, prompt: str, max_retries: int = 3) -> str:
        """Generate meal plan with retry logic.

        Args:
            prompt: The prompt to send to Gemini
            max_retries: Maximum number of retry attempts

        Returns:
            Raw response text from Gemini

        Raises:
            ServiceUnavailableError: If all retries fail
        """
        last_error = None

        for attempt in range(max_retries):
            try:
                logger.info(f"Gemini generation attempt {attempt + 1}/{max_retries}")
                response = await generate_text(prompt)

                # Basic validation - ensure it's valid JSON
                self._validate_response_structure(response)

                logger.info(f"Gemini generation succeeded on attempt {attempt + 1}")
                return response

            except json.JSONDecodeError as e:
                last_error = e
                logger.warning(f"Attempt {attempt + 1} failed - invalid JSON: {e}")
            except Exception as e:
                last_error = e
                logger.warning(f"Attempt {attempt + 1} failed: {e}")

            if attempt < max_retries - 1:
                wait_time = 2**attempt  # 1s, 2s, 4s
                logger.info(f"Waiting {wait_time}s before retry...")
                await asyncio.sleep(wait_time)

        raise ServiceUnavailableError(
            f"Meal plan generation failed after {max_retries} attempts. Last error: {last_error}"
        )

    def _validate_response_structure(self, response_text: str) -> None:
        """Validate that response is valid JSON with expected structure.

        Args:
            response_text: Raw response from Gemini

        Raises:
            ValueError: If structure is invalid
        """
        data = json.loads(response_text)

        if "days" not in data:
            raise ValueError("Response missing 'days' field")

        days = data["days"]
        if not isinstance(days, list) or len(days) != 7:
            raise ValueError(f"Expected 7 days, got {len(days) if isinstance(days, list) else 'not a list'}")

        for i, day in enumerate(days):
            for slot in ["breakfast", "lunch", "dinner", "snacks"]:
                if slot not in day:
                    raise ValueError(f"Day {i} missing '{slot}' field")
                items = day[slot]
                if not isinstance(items, list) or len(items) < 2:
                    raise ValueError(f"Day {i} {slot} should have 2 items, got {len(items) if isinstance(items, list) else 'not a list'}")

    def _parse_response(
        self,
        response_text: str,
        week_start_date: date,
        prefs: UserPreferences,
        festivals: dict[date, dict],
    ) -> GeneratedMealPlan:
        """Parse Gemini response into GeneratedMealPlan.

        Args:
            response_text: Raw JSON response from Gemini
            week_start_date: Start date of the week
            prefs: User preferences
            festivals: Festival data for the week

        Returns:
            GeneratedMealPlan object
        """
        data = json.loads(response_text)

        days = []
        current_date = week_start_date

        for day_data in data.get("days", []):
            # Parse meal items for each slot
            def parse_items(items_data: list) -> list[MealItem]:
                items = []
                for item in items_data:
                    items.append(
                        MealItem(
                            id=str(uuid.uuid4()),
                            recipe_name=item.get("recipe_name", "Unknown"),
                            prep_time_minutes=item.get("prep_time_minutes", 30),
                            dietary_tags=item.get("dietary_tags", []),
                            category=item.get("category", "other"),
                            is_locked=False,
                        )
                    )
                return items

            # Check for festival on this day
            festival = None
            if current_date in festivals:
                f = festivals[current_date]
                festival = {
                    "name": f.get("name", ""),
                    "is_fasting_day": f.get("is_fasting_day", False),
                    "special_foods": f.get("special_foods", []),
                }

            days.append(
                DayMeals(
                    date=current_date.isoformat(),
                    day_name=current_date.strftime("%A"),
                    breakfast=parse_items(day_data.get("breakfast", [])),
                    lunch=parse_items(day_data.get("lunch", [])),
                    dinner=parse_items(day_data.get("dinner", [])),
                    snacks=parse_items(day_data.get("snacks", [])),
                    festival=festival,
                )
            )

            current_date += timedelta(days=1)

        week_end = week_start_date + timedelta(days=6)

        return GeneratedMealPlan(
            week_start_date=week_start_date.isoformat(),
            week_end_date=week_end.isoformat(),
            days=days,
            rules_applied={
                "include_rules": len(prefs.include_rules),
                "exclude_rules": len(prefs.exclude_rules),
                "allergies_excluded": len(prefs.allergies),
                "dislikes_excluded": len(prefs.dislikes),
            },
        )

    def _enforce_rules(
        self,
        plan: GeneratedMealPlan,
        prefs: UserPreferences,
    ) -> GeneratedMealPlan:
        """Post-process to enforce rules that AI might have missed.

        Checks:
        1. Allergen violations - removes items containing allergens
        2. EXCLUDE violations - removes items on excluded days
        3. INCLUDE rule satisfaction - logs warnings if not met

        Args:
            plan: Generated meal plan
            prefs: User preferences

        Returns:
            Plan with violations removed
        """
        # Build allergen set (with variants)
        allergens = set()
        allergen_variants = {
            "peanut": ["peanuts", "groundnut", "groundnuts", "moongphali"],
            "peanuts": ["peanut", "groundnut", "groundnuts", "moongphali"],
            "dairy": ["milk", "cheese", "paneer", "curd", "yogurt", "cream", "butter", "ghee"],
            "gluten": ["wheat", "maida", "atta", "bread", "roti", "naan"],
            "shellfish": ["shrimp", "prawn", "crab", "lobster"],
            "tree nuts": ["almond", "cashew", "walnut", "pistachio", "kaju", "badam"],
            "egg": ["eggs", "anda", "omelette", "omelet"],
            "eggs": ["egg", "anda", "omelette", "omelet"],
        }

        for allergy in prefs.allergies:
            if isinstance(allergy, dict):
                ing = allergy.get("ingredient", "").lower()
            else:
                ing = str(allergy).lower()

            allergens.add(ing)
            if ing in allergen_variants:
                allergens.update(allergen_variants[ing])

        # Build EXCLUDE map: day_name -> set of excluded items
        exclude_by_day: dict[str, set[str]] = {}
        never_exclude = set()

        for rule in prefs.exclude_rules:
            target = rule.get("target", "").lower()
            freq = rule.get("frequency", "NEVER")
            specific_days = rule.get("specific_days", [])

            if freq == "NEVER":
                never_exclude.add(target)
            elif freq == "SPECIFIC_DAYS":
                for day in specific_days:
                    day_upper = day.upper()
                    if day_upper not in exclude_by_day:
                        exclude_by_day[day_upper] = set()
                    exclude_by_day[day_upper].add(target)

        # Check each day
        for day in plan.days:
            day_name_upper = day.day_name.upper()
            day_excludes = exclude_by_day.get(day_name_upper, set()) | never_exclude

            for slot in ["breakfast", "lunch", "dinner", "snacks"]:
                items = getattr(day, slot, [])
                filtered_items = []

                for item in items:
                    name_lower = item.recipe_name.lower()

                    # Check allergens
                    has_allergen = any(allergen in name_lower for allergen in allergens)
                    if has_allergen:
                        logger.warning(
                            f"Removed {item.recipe_name} from {day.date} {slot}: contains allergen"
                        )
                        continue

                    # Check EXCLUDE rules
                    is_excluded = any(excl in name_lower for excl in day_excludes)
                    if is_excluded:
                        logger.warning(
                            f"Removed {item.recipe_name} from {day.date} {slot}: matches EXCLUDE rule"
                        )
                        continue

                    filtered_items.append(item)

                setattr(day, slot, filtered_items)

        # Log INCLUDE rule fulfillment
        for rule in prefs.include_rules:
            target = rule.get("target", "").lower()
            freq = rule.get("frequency", "WEEKLY")
            times_needed = rule.get("times_per_week", 1) if freq != "DAILY" else 7
            slots = [s.lower() for s in rule.get("meal_slot", ["breakfast", "lunch", "dinner", "snacks"])]

            # Count occurrences
            count = 0
            for day in plan.days:
                for slot in slots:
                    items = getattr(day, slot, [])
                    for item in items:
                        if target in item.recipe_name.lower():
                            count += 1

            if count < times_needed:
                logger.warning(
                    f"INCLUDE rule '{rule.get('target')}' not fully satisfied: "
                    f"found {count}/{times_needed} occurrences"
                )

        return plan
