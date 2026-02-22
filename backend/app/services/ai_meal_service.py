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
from app.services.family_constraints import get_family_forbidden_keywords
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
    # Rich data for AI recipe catalog
    ingredients: Optional[list[dict]] = None
    nutrition: Optional[dict] = None
    instructions: Optional[list[dict]] = None


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
    items_per_meal: int = 2
    family_members: list[dict] = field(default_factory=list)
    dietary_type: str = "vegetarian"
    cooking_skill_level: str = "intermediate"
    allow_recipe_repeat: bool = False
    strict_allergen_mode: bool = True
    strict_dietary_mode: bool = True
    nutrition_goals: list[dict] = field(default_factory=list)


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
        # Cached after generation so the endpoint can reuse without a second DB call
        self.last_preferences: UserPreferences | None = None
        self.last_prefs_raw: dict | None = None

    def _filter_conflicting_rules(
        self, prefs: UserPreferences
    ) -> tuple[list[dict], list[dict]]:
        """Filter INCLUDE rules that conflict with family member constraints.

        Args:
            prefs: User preferences with include_rules and family_members.

        Returns:
            Tuple of (filtered_include_rules, removed_rules_report).
            removed_rules_report is a list of dicts with rule_target, member_name,
            constraint_keyword.
        """
        if not prefs.family_members or not prefs.include_rules:
            return prefs.include_rules, []

        forbidden_map = get_family_forbidden_keywords(prefs.family_members)
        if not forbidden_map:
            return prefs.include_rules, []

        filtered = []
        report = []

        for rule in prefs.include_rules:
            target = rule.get("target", "").lower()
            conflict_found = False

            for member_name, forbidden_keywords in forbidden_map.items():
                for keyword in forbidden_keywords:
                    if keyword in target:
                        report.append(
                            {
                                "rule_target": rule.get("target", ""),
                                "member_name": member_name,
                                "constraint_keyword": keyword,
                            }
                        )
                        logger.warning(
                            f"Filtered INCLUDE rule '{rule.get('target')}': "
                            f"conflicts with {member_name}'s constraint ({keyword})"
                        )
                        conflict_found = True
                        break
                if conflict_found:
                    break

            if not conflict_found:
                filtered.append(rule)

        return filtered, report

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

        # Load user preferences (cached for reuse by endpoint)
        prefs = await self._load_user_preferences(user_id)
        self.last_preferences = prefs

        # Filter INCLUDE rules that conflict with family member constraints
        filtered_include_rules, rules_filtered_report = self._filter_conflicting_rules(
            prefs
        )
        prefs.include_rules = filtered_include_rules

        # Load festivals for the week
        festivals = await self._load_festivals(week_start_date)

        # Load pairing config
        config = await self.config_service.get_config()

        # Build prompt
        prompt = self._build_prompt(
            prefs, festivals, config, week_start_date, rules_filtered_report
        )

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
        nutrition_goals = []

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
                    include_rules.append(
                        {
                            "id": rule.id,
                            "type": rule.action,
                            "target": rule.target_name,
                            "frequency": rule.frequency_type,
                            "times_per_week": rule.frequency_count,
                            "specific_days": (
                                rule.frequency_days.split(",")
                                if rule.frequency_days
                                else []
                            ),
                            "meal_slot": (
                                [rule.meal_slot]
                                if rule.meal_slot
                                else ["breakfast", "lunch", "dinner", "snacks"]
                            ),
                            "enforcement": rule.enforcement,
                        }
                    )

                for rule in exclude_db_rules:
                    exclude_rules.append(
                        {
                            "id": rule.id,
                            "type": rule.action,
                            "target": rule.target_name,
                            "frequency": rule.frequency_type,
                            "times_per_week": rule.frequency_count,
                            "specific_days": (
                                rule.frequency_days.split(",")
                                if rule.frequency_days
                                else []
                            ),
                            "meal_slot": (
                                [rule.meal_slot]
                                if rule.meal_slot
                                else ["breakfast", "lunch", "dinner", "snacks"]
                            ),
                            "enforcement": rule.enforcement,
                        }
                    )

                # Query active nutrition goals
                goals_result = await db.execute(
                    select(NutritionGoal).where(
                        NutritionGoal.user_id == user_id,
                        NutritionGoal.is_active == True,
                    )
                )
                nutrition_goals_db = goals_result.scalars().all()
                nutrition_goals = [
                    {
                        "food_category": g.food_category,
                        "weekly_target": g.weekly_target,
                        "enforcement": g.enforcement,
                    }
                    for g in nutrition_goals_db
                ]

                logger.info(
                    f"Loaded {len(include_rules)} INCLUDE rules, "
                    f"{len(exclude_rules)} EXCLUDE rules, and "
                    f"{len(nutrition_goals)} nutrition goals from database"
                )

        except Exception as e:
            logger.warning(f"Failed to load rules from recipe_rules table: {e}")
            # Fallback to legacy JSON field
            recipe_rules = prefs_data.get("recipe_rules") or []
            include_rules = [
                r
                for r in recipe_rules
                if r.get("type") == "INCLUDE" and r.get("is_active", True)
            ]
            exclude_rules = [
                r
                for r in recipe_rules
                if r.get("type") == "EXCLUDE" and r.get("is_active", True)
            ]
            logger.info("Using legacy recipe_rules JSON field")

        # Load family members
        family_members = []
        try:
            members = await self.user_repo.get_family_members(user_id)
            family_members = members or []
            logger.info(
                f"Loaded {len(family_members)} family members for user {user_id}"
            )
        except Exception as e:
            logger.warning(f"Failed to load family members: {e}")

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
            items_per_meal=prefs_data.get("items_per_meal") or 2,
            family_members=family_members,
            dietary_type=prefs_data.get("dietary_type") or "vegetarian",
            cooking_skill_level=prefs_data.get("cooking_skill_level") or "intermediate",
            allow_recipe_repeat=prefs_data.get("allow_recipe_repeat", False),
            strict_allergen_mode=prefs_data.get("strict_allergen_mode", True),
            strict_dietary_mode=prefs_data.get("strict_dietary_mode", True),
            nutrition_goals=nutrition_goals,
        )

    async def _load_festivals(self, week_start: date) -> dict[date, dict]:
        """Load festivals for the week from PostgreSQL."""
        week_end = week_start + timedelta(days=6)

        try:
            async with async_session_maker() as db:
                festivals_map = await get_festivals_for_date_range(
                    db, week_start, week_end
                )

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
        filtered_rules_report: list[dict] = None,
    ) -> str:
        """Build the prompt for Gemini."""

        # Build preferences section
        dietary_str = (
            ", ".join(prefs.dietary_tags) if prefs.dietary_tags else "vegetarian"
        )
        cuisine_str = (
            ", ".join(prefs.cuisine_preferences)
            if prefs.cuisine_preferences
            else "north"
        )

        # Family members section
        family_section = ""
        if prefs.family_members:
            family_lines = []
            for member in prefs.family_members:
                name = member.get("name", "Member")
                age_group = member.get("age_group", "adult")
                health = member.get("health_conditions") or []
                diet = member.get("dietary_restrictions") or []

                parts = [f"- {name} ({age_group})"]
                if health:
                    parts.append(f"Health: {', '.join(health)}")
                if diet:
                    parts.append(f"Diet: {', '.join(diet)}")
                family_lines.append(": ".join(parts) if len(parts) > 1 else parts[0])

            family_section = "\n".join(family_lines)

        # Mixed-diet household detection
        mixed_diet_section = ""
        if prefs.family_members:
            household_diet = prefs.dietary_type.lower()
            differing_members = []
            for member in prefs.family_members:
                member_diets = [
                    d.lower() for d in (member.get("dietary_restrictions") or [])
                ]
                for d in member_diets:
                    if d != household_diet and d in (
                        "vegetarian",
                        "non-vegetarian",
                        "vegan",
                        "eggetarian",
                        "non_vegetarian",
                        "jain",
                        "sattvic",
                    ):
                        differing_members.append((member.get("name", "Member"), d))
                        break
            if differing_members:
                mixed_lines = [
                    "## MIXED DIET HOUSEHOLD",
                    f"The household default is {prefs.dietary_type}, but some members have different preferences:",
                ]
                for member_name, restriction in differing_members:
                    mixed_lines.append(
                        f"- {member_name}: {restriction} — Include 2-3 {restriction} "
                        f'alternatives per week (mark as "[ALT: {member_name}]" '
                        f"so other members can use the default option)"
                    )
                mixed_diet_section = "\n".join(mixed_lines)

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
                slots = rule.get(
                    "meal_slot", ["breakfast", "lunch", "dinner", "snacks"]
                )
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

            dates_section.append(
                f"- {current.isoformat()} ({day_name}): max {max_time} min"
            )
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

        # Nutrition goals section
        nutrition_section = ""
        if prefs.nutrition_goals:
            goal_lines = []
            for goal in prefs.nutrition_goals:
                cat = goal.get("food_category", "unknown")
                target = goal.get("weekly_target", 3)
                enf = goal.get("enforcement", "PREFERRED")
                goal_lines.append(f"- {cat}: {target} servings/week ({enf})")
            nutrition_section = "\n".join(goal_lines)

        # Conflict warnings section
        conflict_section = ""
        if filtered_rules_report:
            conflict_lines = []
            for item in filtered_rules_report:
                conflict_lines.append(
                    f"- REMOVED '{item['rule_target']}': conflicts with "
                    f"{item['member_name']}'s constraint ({item['constraint_keyword']})"
                )
            conflict_section = "\n".join(conflict_lines)

        # Recipe repeat and strictness rules
        repeat_text = (
            "Allow repeating favorite recipes across the week"
            if prefs.allow_recipe_repeat
            else "Vary recipes across the week - avoid repeating the same dish"
        )
        allergen_strictness = (
            "ZERO TOLERANCE - absolutely no allergens in any dish, ingredient, or garnish"
            if prefs.strict_allergen_mode
            else "Avoid allergens where possible but minor traces acceptable"
        )
        dietary_strictness = (
            "STRICT enforcement - every dish must comply with dietary type"
            if prefs.strict_dietary_mode
            else "Flexible - occasional exceptions to dietary type acceptable"
        )

        prompt = f"""You are RasoiAI, an Indian meal planning assistant. Generate a 7-day meal plan.

## USER PREFERENCES (STRICT - MUST FOLLOW)
### Primary Diet: {prefs.dietary_type.upper()}
### Dietary Tags: [{dietary_str}]
### Cuisines: [{cuisine_str}]
### Spice Level: {prefs.spice_level}
### Cooking Skill: {prefs.cooking_skill_level}
### Family Size: {prefs.family_size}

## FAMILY MEMBERS (Adapt meals to accommodate ALL members)
{family_section if family_section else "No individual family members specified."}

### Family Member Meal Guidance:
- Diabetic members: avoid high sugar/GI foods (sweets, white rice heavy dishes, sugary drinks)
- Soft food needs: no hard/crunchy items, prefer soft-cooked dishes (khichdi, dal, soft roti)
- No spicy: use mild preparations, avoid green chilies and hot spices
- Low salt: reduce salt in recipes, avoid pickles and papad
- High protein: include paneer, dal, eggs, chicken, sprouts dishes
- Low oil: prefer steamed, grilled, or dry preparations over deep-fried
- Jain diet: no root vegetables (potato, onion, garlic, ginger)
- Ensure meals are safe and appropriate for ALL family members listed above.

{mixed_diet_section}

### Allergies (NEVER INCLUDE - STRICT): [{allergies_str if allergies_str else "none"}]
### Dislikes (AVOID): [{dislikes_str}]

### INCLUDE Rules (MUST APPEAR):
{include_section if include_section else "- None specified"}

### EXCLUDE Rules (NEVER INCLUDE):
{exclude_section if exclude_section else "- None specified"}

### Filtered Rules (removed due to family member conflicts):
{conflict_section if conflict_section else "- None removed"}

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

## NUTRITION GOALS
{nutrition_section if nutrition_section else "No nutrition goals set."}

## IMPORTANT RULES
1. Each meal slot MUST have exactly {prefs.items_per_meal} items (a main dish + accompaniment{'s' if prefs.items_per_meal > 2 else ''})
2. ALLERGIES: {allergen_strictness}
3. DIETARY TYPE: {dietary_strictness}
4. INCLUDE rules MUST be satisfied at the specified frequency
5. EXCLUDE rules MUST be respected - never include on specified days
6. Prep time for EACH dish must be within the day's cooking time limit
7. Use authentic Indian dish names (e.g., "Aloo Paratha", "Masala Chai", "Dal Tadka")
8. {repeat_text}

## OUTPUT FORMAT
Return ONLY valid JSON (no markdown, no explanation).
Each item must include: recipe_name, prep_time_minutes, dietary_tags, category, calories, ingredients, nutrition.
{{
  "days": [
    {{
      "date": "YYYY-MM-DD",
      "day_name": "Monday",
      "breakfast": [
        {{"recipe_name": "Aloo Paratha", "prep_time_minutes": 25, "dietary_tags": ["vegetarian"], "category": "paratha", "calories": 350, "ingredients": [{{"name": "Wheat Flour", "quantity": 2, "unit": "cup", "category": "grains"}}, {{"name": "Potato", "quantity": 3, "unit": "medium", "category": "vegetables"}}, {{"name": "Ghee", "quantity": 2, "unit": "tbsp", "category": "dairy"}}], "nutrition": {{"protein_g": 8, "carbs_g": 45, "fat_g": 15, "fiber_g": 3}}}},
        {{"recipe_name": "Masala Chai", "prep_time_minutes": 10, "dietary_tags": ["vegetarian"], "category": "chai", "calories": 80, "ingredients": [{{"name": "Tea Leaves", "quantity": 2, "unit": "tsp", "category": "other"}}, {{"name": "Milk", "quantity": 1, "unit": "cup", "category": "dairy"}}], "nutrition": {{"protein_g": 3, "carbs_g": 10, "fat_g": 3, "fiber_g": 0}}}}
      ],
      "lunch": [
        {{"recipe_name": "Dal Tadka", "prep_time_minutes": 30, "dietary_tags": ["vegetarian", "vegan"], "category": "dal", "calories": 250, "ingredients": [{{"name": "Toor Dal", "quantity": 1, "unit": "cup", "category": "pulses"}}, {{"name": "Ghee", "quantity": 2, "unit": "tbsp", "category": "dairy"}}], "nutrition": {{"protein_g": 12, "carbs_g": 35, "fat_g": 8, "fiber_g": 6}}}},
        {{"recipe_name": "Jeera Rice", "prep_time_minutes": 20, "dietary_tags": ["vegetarian", "vegan"], "category": "rice", "calories": 200, "ingredients": [{{"name": "Basmati Rice", "quantity": 1, "unit": "cup", "category": "grains"}}, {{"name": "Cumin Seeds", "quantity": 1, "unit": "tsp", "category": "spices"}}], "nutrition": {{"protein_g": 4, "carbs_g": 40, "fat_g": 3, "fiber_g": 1}}}}
      ],
      "dinner": [
        {{"recipe_name": "Paneer Butter Masala", "prep_time_minutes": 30, "dietary_tags": ["vegetarian"], "category": "curry", "calories": 350, "ingredients": [{{"name": "Paneer", "quantity": 250, "unit": "g", "category": "dairy"}}, {{"name": "Tomato", "quantity": 3, "unit": "medium", "category": "vegetables"}}], "nutrition": {{"protein_g": 18, "carbs_g": 12, "fat_g": 25, "fiber_g": 2}}}},
        {{"recipe_name": "Butter Naan", "prep_time_minutes": 15, "dietary_tags": ["vegetarian"], "category": "naan", "calories": 260, "ingredients": [{{"name": "Maida", "quantity": 2, "unit": "cup", "category": "grains"}}, {{"name": "Butter", "quantity": 2, "unit": "tbsp", "category": "dairy"}}], "nutrition": {{"protein_g": 7, "carbs_g": 40, "fat_g": 8, "fiber_g": 1}}}}
      ],
      "snacks": [
        {{"recipe_name": "Samosa", "prep_time_minutes": 20, "dietary_tags": ["vegetarian"], "category": "snack", "calories": 250, "ingredients": [{{"name": "Maida", "quantity": 1, "unit": "cup", "category": "grains"}}, {{"name": "Potato", "quantity": 2, "unit": "medium", "category": "vegetables"}}], "nutrition": {{"protein_g": 4, "carbs_g": 30, "fat_g": 12, "fiber_g": 2}}}},
        {{"recipe_name": "Masala Chai", "prep_time_minutes": 10, "dietary_tags": ["vegetarian"], "category": "chai", "calories": 80, "ingredients": [{{"name": "Tea Leaves", "quantity": 2, "unit": "tsp", "category": "other"}}, {{"name": "Milk", "quantity": 1, "unit": "cup", "category": "dairy"}}], "nutrition": {{"protein_g": 3, "carbs_g": 10, "fat_g": 3, "fiber_g": 0}}}}
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
            raise ValueError(
                f"Expected 7 days, got {len(days) if isinstance(days, list) else 'not a list'}"
            )

        for i, day in enumerate(days):
            for slot in ["breakfast", "lunch", "dinner", "snacks"]:
                if slot not in day:
                    raise ValueError(f"Day {i} missing '{slot}' field")
                items = day[slot]
                if not isinstance(items, list) or len(items) < 2:
                    raise ValueError(
                        f"Day {i} {slot} should have 2 items, got {len(items) if isinstance(items, list) else 'not a list'}"
                    )

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
                            calories=item.get("calories", 0),
                            ingredients=item.get("ingredients"),
                            nutrition=item.get("nutrition"),
                            instructions=item.get("instructions"),
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
            "dairy": [
                "milk",
                "cheese",
                "paneer",
                "curd",
                "yogurt",
                "cream",
                "butter",
                "ghee",
            ],
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
            slots = [
                s.lower()
                for s in rule.get(
                    "meal_slot", ["breakfast", "lunch", "dinner", "snacks"]
                )
            ]

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

        # Family constraint enforcement (post-processing safety net)
        forbidden_map = get_family_forbidden_keywords(prefs.family_members)
        if forbidden_map:
            for day in plan.days:
                for slot in ["breakfast", "lunch", "dinner", "snacks"]:
                    items = getattr(day, slot, [])
                    safe_items = []

                    for item in items:
                        name_lower = item.recipe_name.lower()
                        # Also check ingredient names if available
                        ingredient_text = ""
                        if item.ingredients:
                            ingredient_text = " ".join(
                                ing.get("name", "").lower()
                                for ing in item.ingredients
                                if isinstance(ing, dict)
                            )

                        is_unsafe = False
                        for member_name, forbidden_keywords in forbidden_map.items():
                            for keyword in forbidden_keywords:
                                if keyword in name_lower or keyword in ingredient_text:
                                    logger.warning(
                                        f"Removed {item.recipe_name} from {day.date} {slot}: "
                                        f"unsafe for {member_name} (contains '{keyword}')"
                                    )
                                    is_unsafe = True
                                    break
                            if is_unsafe:
                                break

                        if not is_unsafe:
                            safe_items.append(item)

                    setattr(day, slot, safe_items)

        return plan
