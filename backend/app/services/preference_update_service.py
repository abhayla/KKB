"""Service for updating user preferences via chat.

This service handles preference updates triggered by LLM function calls.
It includes conflict detection, validation, and undo support.

Functions:
- update_recipe_rule: Add/remove/modify INCLUDE/EXCLUDE rules
- update_allergy: Add/remove allergies
- update_dislike: Add/remove dislikes
- update_preference: Update cooking time, busy days, dietary tags
- undo_last_change: Revert the last change
- show_config: Display current configuration
"""

import logging
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Optional

from app.repositories.user_repository import UserRepository

logger = logging.getLogger(__name__)


class RuleAction(str, Enum):
    ADD = "ADD"
    REMOVE = "REMOVE"
    MODIFY = "MODIFY"


class RuleType(str, Enum):
    INCLUDE = "INCLUDE"
    EXCLUDE = "EXCLUDE"


class Frequency(str, Enum):
    DAILY = "DAILY"
    WEEKLY = "WEEKLY"
    TIMES_PER_WEEK = "TIMES_PER_WEEK"
    NEVER = "NEVER"


class Severity(str, Enum):
    MILD = "MILD"
    MODERATE = "MODERATE"
    SEVERE = "SEVERE"


class PreferenceType(str, Enum):
    COOKING_TIME = "cooking_time"
    BUSY_DAYS = "busy_days"
    DIETARY_TAGS = "dietary_tags"
    SPICE_LEVEL = "spice_level"
    CUISINE = "cuisine"


@dataclass
class UpdateResult:
    """Result of a preference update operation."""
    success: bool
    message: str
    conflict: Optional[dict] = None
    updated_config: Optional[dict] = None


@dataclass
class ConflictInfo:
    """Information about a detected conflict."""
    existing_rule: dict
    new_rule: dict
    conflict_type: str  # "include_exclude" or "duplicate"
    resolution_options: list[str] = field(default_factory=list)


class PreferenceUpdateService:
    """Service for managing user preference updates via chat.

    Handles CRUD operations on:
    - Recipe rules (INCLUDE/EXCLUDE)
    - Allergies
    - Dislikes
    - General preferences (cooking time, dietary tags, etc.)

    Includes:
    - Conflict detection between INCLUDE and EXCLUDE rules
    - Last change tracking for undo support
    - Config summary formatting
    """

    def __init__(self):
        self.user_repo = UserRepository()

    async def update_recipe_rule(
        self,
        user_id: str,
        action: str,
        rule_type: str,
        target: str,
        frequency: Optional[str] = None,
        times_per_week: Optional[int] = None,
        meal_slots: Optional[list[str]] = None,
        reason: Optional[str] = None,
    ) -> UpdateResult:
        """Add, remove, or modify a recipe rule.

        Args:
            user_id: User ID
            action: ADD, REMOVE, or MODIFY
            rule_type: INCLUDE or EXCLUDE
            target: Recipe or ingredient name
            frequency: DAILY, WEEKLY, TIMES_PER_WEEK, or NEVER
            times_per_week: Number of times per week (for WEEKLY/TIMES_PER_WEEK)
            meal_slots: List of meal slots (BREAKFAST, LUNCH, DINNER, SNACKS)
            reason: Optional reason for the rule

        Returns:
            UpdateResult with success status and message
        """
        # Strip whitespace from target name
        target = target.strip()

        prefs = await self.user_repo.get_preferences(user_id) or {}

        # Recipe rules are stored as {include: [], exclude: []}
        recipe_rules = prefs.get("recipe_rules", {})
        if not isinstance(recipe_rules, dict):
            recipe_rules = {"include": [], "exclude": []}
        if "include" not in recipe_rules:
            recipe_rules["include"] = []
        if "exclude" not in recipe_rules:
            recipe_rules["exclude"] = []

        # Get the appropriate list based on rule type
        rule_key = "include" if rule_type == RuleType.INCLUDE.value else "exclude"
        opposite_key = "exclude" if rule_type == RuleType.INCLUDE.value else "include"
        rules_list = recipe_rules[rule_key]
        opposite_list = recipe_rules[opposite_key]

        # Store previous state for undo
        previous_state = {
            "type": "recipe_rules",
            "data": {"include": list(recipe_rules["include"]), "exclude": list(recipe_rules["exclude"])},
        }

        # Determine frequency
        freq = frequency or (Frequency.NEVER.value if rule_type == RuleType.EXCLUDE.value else Frequency.WEEKLY.value)

        # Build new rule
        new_rule = {
            "id": f"rule_{uuid.uuid4().hex[:8]}",
            "type": rule_type,
            "target": target,
            "frequency": freq,
            "times_per_week": times_per_week or 1,
            "meal_slot": meal_slots or ["BREAKFAST", "LUNCH", "DINNER", "SNACKS"],
            "reason": reason,
            "is_active": True,
            "created_at": datetime.now(timezone.utc).isoformat(),
        }

        if action == RuleAction.ADD.value:
            # Check for conflicts with opposite type
            conflict = self._check_rule_conflict_nested(opposite_list, new_rule)
            if conflict:
                return UpdateResult(
                    success=False,
                    message=f"Conflict detected: {conflict['conflict_type']}",
                    conflict=conflict,
                )

            # Check for duplicates in same type
            existing = self._find_existing_rule(rules_list, target, rule_type)
            if existing:
                return UpdateResult(
                    success=False,
                    message=f"A {rule_type} rule for '{target}' already exists.",
                    conflict={
                        "existing_rule": existing,
                        "conflict_type": "duplicate",
                    }
                )

            rules_list.append(new_rule)
            message = f"Added {rule_type} rule: {target}"

        elif action == RuleAction.REMOVE.value:
            # Find and remove the rule
            found = False
            for i, rule in enumerate(rules_list):
                if rule.get("target", "").lower() == target.lower():
                    rules_list.pop(i)
                    found = True
                    break

            if not found:
                return UpdateResult(
                    success=False,
                    message=f"No {rule_type} rule found for '{target}'.",
                )

            message = f"Removed {rule_type} rule: {target}"

        elif action == RuleAction.MODIFY.value:
            # Find and update the rule
            found = False
            for rule in rules_list:
                if rule.get("target", "").lower() == target.lower():
                    if frequency:
                        rule["frequency"] = frequency
                    if times_per_week is not None:
                        rule["times_per_week"] = times_per_week
                    if meal_slots:
                        rule["meal_slot"] = meal_slots
                    if reason:
                        rule["reason"] = reason
                    rule["updated_at"] = datetime.now(timezone.utc).isoformat()
                    found = True
                    break

            if not found:
                return UpdateResult(
                    success=False,
                    message=f"No {rule_type} rule found for '{target}'.",
                )

            message = f"Modified {rule_type} rule: {target}"

        else:
            return UpdateResult(success=False, message=f"Invalid action: {action}")

        # Save changes
        prefs["recipe_rules"] = recipe_rules
        prefs["last_change"] = {
            "action": f"{action}_RECIPE_RULE",
            "previous_state": previous_state,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }

        await self.user_repo.save_preferences(user_id, prefs)

        return UpdateResult(
            success=True,
            message=message,
            updated_config=self._summarize_rules(recipe_rules),
        )

    async def update_allergy(
        self,
        user_id: str,
        action: str,
        ingredient: str,
        severity: Optional[str] = None,
    ) -> UpdateResult:
        """Add or remove an allergy.

        Args:
            user_id: User ID
            action: ADD or REMOVE
            ingredient: Ingredient name
            severity: MILD, MODERATE, or SEVERE

        Returns:
            UpdateResult with success status and message
        """
        prefs = await self.user_repo.get_preferences(user_id) or {}
        allergies = prefs.get("allergies", [])

        # Store previous state
        previous_state = {
            "type": "allergies",
            "data": list(allergies),
        }

        if action == RuleAction.ADD.value:
            # Check if already exists
            for allergy in allergies:
                if isinstance(allergy, dict):
                    if allergy.get("ingredient", "").lower() == ingredient.lower():
                        return UpdateResult(
                            success=False,
                            message=f"Allergy for '{ingredient}' already exists.",
                        )
                elif str(allergy).lower() == ingredient.lower():
                    return UpdateResult(
                        success=False,
                        message=f"Allergy for '{ingredient}' already exists.",
                    )

            allergies.append({
                "ingredient": ingredient,
                "severity": severity or Severity.MODERATE.value,
            })
            message = f"Added allergy: {ingredient} ({severity or 'MODERATE'})"

        elif action == RuleAction.REMOVE.value:
            found = False
            for i, allergy in enumerate(allergies):
                if isinstance(allergy, dict):
                    if allergy.get("ingredient", "").lower() == ingredient.lower():
                        allergies.pop(i)
                        found = True
                        break
                elif str(allergy).lower() == ingredient.lower():
                    allergies.pop(i)
                    found = True
                    break

            if not found:
                return UpdateResult(
                    success=False,
                    message=f"No allergy found for '{ingredient}'.",
                )

            message = f"Removed allergy: {ingredient}"

        else:
            return UpdateResult(success=False, message=f"Invalid action: {action}")

        # Save changes
        prefs["allergies"] = allergies
        prefs["last_change"] = {
            "action": f"{action}_ALLERGY",
            "previous_state": previous_state,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }

        await self.user_repo.save_preferences(user_id, prefs)

        return UpdateResult(
            success=True,
            message=message,
            updated_config={"allergies": allergies},
        )

    async def update_dislike(
        self,
        user_id: str,
        action: str,
        ingredient: str,
    ) -> UpdateResult:
        """Add or remove a disliked ingredient.

        Args:
            user_id: User ID
            action: ADD or REMOVE
            ingredient: Ingredient name

        Returns:
            UpdateResult with success status and message
        """
        prefs = await self.user_repo.get_preferences(user_id) or {}
        dislikes = prefs.get("disliked_ingredients", [])

        # Store previous state
        previous_state = {
            "type": "disliked_ingredients",
            "data": list(dislikes),
        }

        ingredient_lower = ingredient.lower()

        if action == RuleAction.ADD.value:
            # Check if already exists
            if any(d.lower() == ingredient_lower for d in dislikes):
                return UpdateResult(
                    success=False,
                    message=f"'{ingredient}' is already in dislikes.",
                )

            dislikes.append(ingredient)
            message = f"Added to dislikes: {ingredient}"

        elif action == RuleAction.REMOVE.value:
            found = False
            for i, d in enumerate(dislikes):
                if d.lower() == ingredient_lower:
                    dislikes.pop(i)
                    found = True
                    break

            if not found:
                return UpdateResult(
                    success=False,
                    message=f"'{ingredient}' is not in dislikes.",
                )

            message = f"Removed from dislikes: {ingredient}"

        else:
            return UpdateResult(success=False, message=f"Invalid action: {action}")

        # Save changes
        prefs["disliked_ingredients"] = dislikes
        prefs["last_change"] = {
            "action": f"{action}_DISLIKE",
            "previous_state": previous_state,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }

        await self.user_repo.save_preferences(user_id, prefs)

        return UpdateResult(
            success=True,
            message=message,
            updated_config={"dislikes": dislikes},
        )

    async def update_preference(
        self,
        user_id: str,
        preference_type: str,
        action: str,
        value: str,
    ) -> UpdateResult:
        """Update general preferences (cooking time, busy days, dietary tags, etc.).

        Args:
            user_id: User ID
            preference_type: cooking_time, busy_days, dietary_tags, spice_level, cuisine
            action: SET, ADD, or REMOVE
            value: The value to set/add/remove

        Returns:
            UpdateResult with success status and message
        """
        prefs = await self.user_repo.get_preferences(user_id) or {}

        # Store previous state
        previous_state = {
            "type": "preference",
            "preference_type": preference_type,
            "data": prefs.get(self._get_pref_key(preference_type)),
        }

        message = ""

        if preference_type == PreferenceType.COOKING_TIME.value:
            # Parse value like "weekday:30" or "weekend:60"
            if ":" in value:
                time_type, minutes = value.split(":")
                minutes = int(minutes)
                if time_type.lower() == "weekday":
                    prefs["weekday_cooking_time_minutes"] = minutes
                    message = f"Set weekday cooking time to {minutes} minutes"
                elif time_type.lower() == "weekend":
                    prefs["weekend_cooking_time_minutes"] = minutes
                    message = f"Set weekend cooking time to {minutes} minutes"
            else:
                minutes = int(value)
                prefs["weekday_cooking_time_minutes"] = minutes
                prefs["weekend_cooking_time_minutes"] = minutes
                message = f"Set cooking time to {minutes} minutes"

        elif preference_type == PreferenceType.BUSY_DAYS.value:
            busy_days = prefs.get("busy_days", [])
            day = value.upper()

            if action == "ADD":
                if day not in busy_days:
                    busy_days.append(day)
                    message = f"Added {day} as a busy day"
                else:
                    return UpdateResult(success=False, message=f"{day} is already a busy day")
            elif action == "REMOVE":
                if day in busy_days:
                    busy_days.remove(day)
                    message = f"Removed {day} from busy days"
                else:
                    return UpdateResult(success=False, message=f"{day} is not in busy days")
            elif action == "SET":
                busy_days = [d.upper() for d in value.split(",")]
                message = f"Set busy days to: {', '.join(busy_days)}"

            prefs["busy_days"] = busy_days

        elif preference_type == PreferenceType.DIETARY_TAGS.value:
            dietary_tags = prefs.get("dietary_tags", [])
            tag = value.lower()

            if action == "ADD":
                if tag not in dietary_tags:
                    dietary_tags.append(tag)
                    message = f"Added dietary tag: {tag}"
                else:
                    return UpdateResult(success=False, message=f"'{tag}' is already in dietary tags")
            elif action == "REMOVE":
                if tag in dietary_tags:
                    dietary_tags.remove(tag)
                    message = f"Removed dietary tag: {tag}"
                else:
                    return UpdateResult(success=False, message=f"'{tag}' is not in dietary tags")
            elif action == "SET":
                dietary_tags = [t.lower() for t in value.split(",")]
                message = f"Set dietary tags to: {', '.join(dietary_tags)}"

            prefs["dietary_tags"] = dietary_tags

        elif preference_type == PreferenceType.SPICE_LEVEL.value:
            prefs["spice_level"] = value.lower()
            message = f"Set spice level to: {value}"

        elif preference_type == PreferenceType.CUISINE.value:
            cuisines = prefs.get("cuisine_preferences", [])
            cuisine = value.lower()

            if action == "ADD":
                if cuisine not in cuisines:
                    cuisines.append(cuisine)
                    message = f"Added cuisine preference: {cuisine}"
                else:
                    return UpdateResult(success=False, message=f"'{cuisine}' is already in preferences")
            elif action == "REMOVE":
                if cuisine in cuisines:
                    cuisines.remove(cuisine)
                    message = f"Removed cuisine preference: {cuisine}"
                else:
                    return UpdateResult(success=False, message=f"'{cuisine}' is not in preferences")
            elif action == "SET":
                cuisines = [c.lower() for c in value.split(",")]
                message = f"Set cuisine preferences to: {', '.join(cuisines)}"

            prefs["cuisine_preferences"] = cuisines

        else:
            return UpdateResult(success=False, message=f"Unknown preference type: {preference_type}")

        # Save changes
        prefs["last_change"] = {
            "action": f"UPDATE_{preference_type.upper()}",
            "previous_state": previous_state,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }

        await self.user_repo.save_preferences(user_id, prefs)

        return UpdateResult(
            success=True,
            message=message,
            updated_config=self._summarize_preferences(prefs),
        )

    async def undo_last_change(self, user_id: str) -> UpdateResult:
        """Undo the last configuration change.

        Args:
            user_id: User ID

        Returns:
            UpdateResult with success status and message
        """
        prefs = await self.user_repo.get_preferences(user_id)

        if not prefs or "last_change" not in prefs:
            return UpdateResult(
                success=False,
                message="No changes to undo.",
            )

        last_change = prefs["last_change"]
        previous_state = last_change.get("previous_state", {})

        if not previous_state:
            return UpdateResult(
                success=False,
                message="No previous state available to restore.",
            )

        # Restore previous state
        state_type = previous_state.get("type")
        state_data = previous_state.get("data")

        if state_type == "recipe_rules":
            prefs["recipe_rules"] = state_data
            message = "Restored previous recipe rules"
        elif state_type == "allergies":
            prefs["allergies"] = state_data
            message = "Restored previous allergies"
        elif state_type == "disliked_ingredients":
            prefs["disliked_ingredients"] = state_data
            message = "Restored previous dislikes"
        elif state_type == "preference":
            pref_type = previous_state.get("preference_type")
            pref_key = self._get_pref_key(pref_type)
            prefs[pref_key] = state_data
            message = f"Restored previous {pref_type}"
        else:
            return UpdateResult(
                success=False,
                message="Unknown state type, cannot undo.",
            )

        # Clear last_change to prevent double undo
        del prefs["last_change"]

        await self.user_repo.save_preferences(user_id, prefs)

        return UpdateResult(
            success=True,
            message=f"Undo successful: {message}",
            updated_config=self._summarize_preferences(prefs),
        )

    async def show_config(
        self,
        user_id: str,
        section: Optional[str] = None,
    ) -> UpdateResult:
        """Show current configuration.

        Args:
            user_id: User ID
            section: all, rules, allergies, dislikes, preferences

        Returns:
            UpdateResult with configuration summary
        """
        prefs = await self.user_repo.get_preferences(user_id) or {}

        section = section or "all"
        config_summary = {}

        if section in ["all", "rules"]:
            config_summary["recipe_rules"] = self._summarize_rules(prefs.get("recipe_rules", []))

        if section in ["all", "allergies"]:
            config_summary["allergies"] = prefs.get("allergies", [])

        if section in ["all", "dislikes"]:
            config_summary["dislikes"] = prefs.get("disliked_ingredients", [])

        if section in ["all", "preferences"]:
            config_summary["preferences"] = {
                "dietary_tags": prefs.get("dietary_tags", []),
                "cuisine_preferences": prefs.get("cuisine_preferences", []),
                "spice_level": prefs.get("spice_level", "medium"),
                "weekday_cooking_time": prefs.get("weekday_cooking_time_minutes", 30),
                "weekend_cooking_time": prefs.get("weekend_cooking_time_minutes", 60),
                "busy_days": prefs.get("busy_days", []),
            }

        return UpdateResult(
            success=True,
            message=f"Current configuration ({section}):",
            updated_config=config_summary,
        )

    async def resolve_conflict(
        self,
        user_id: str,
        resolution: str,
        new_rule: dict,
    ) -> UpdateResult:
        """Resolve a conflict between rules.

        Args:
            user_id: User ID
            resolution: "keep_existing" or "replace"
            new_rule: The new rule that caused the conflict

        Returns:
            UpdateResult with success status
        """
        if resolution == "keep_existing":
            return UpdateResult(
                success=True,
                message="Kept existing rule, new rule was not added.",
            )

        elif resolution == "replace":
            prefs = await self.user_repo.get_preferences(user_id) or {}
            recipe_rules = prefs.get("recipe_rules", [])

            # Remove conflicting rules
            target = new_rule.get("target", "").lower()
            recipe_rules = [
                r for r in recipe_rules
                if r.get("target", "").lower() != target
            ]

            # Add new rule
            recipe_rules.append(new_rule)

            prefs["recipe_rules"] = recipe_rules
            await self.user_repo.save_preferences(user_id, prefs)

            return UpdateResult(
                success=True,
                message=f"Replaced existing rule with new {new_rule.get('type')} rule for '{new_rule.get('target')}'",
                updated_config=self._summarize_rules(recipe_rules),
            )

        return UpdateResult(success=False, message=f"Unknown resolution: {resolution}")

    def _find_existing_rule(self, rules: list[dict], target: str, rule_type: str = None) -> Optional[dict]:
        """Find an existing rule for a target in a list."""
        target_lower = target.lower()
        for rule in rules:
            if rule.get("target", "").lower() == target_lower:
                # If rule_type specified, check it matches
                if rule_type is None or rule.get("type") == rule_type:
                    return rule
        return None

    def _check_rule_conflict_nested(self, opposite_rules: list[dict], new_rule: dict) -> Optional[dict]:
        """Check if a new rule conflicts with rules in the opposite list.

        Args:
            opposite_rules: List of rules of the opposite type (e.g., exclude rules when adding include)
            new_rule: The new rule being added

        Returns:
            Conflict dict if found, None otherwise
        """
        target = new_rule.get("target", "").lower()
        rule_type = new_rule.get("type")
        opposite_type = "EXCLUDE" if rule_type == "INCLUDE" else "INCLUDE"

        for rule in opposite_rules:
            rule_target = rule.get("target", "").lower()

            if rule_target == target:
                return {
                    "existing_rule": rule,
                    "new_rule": new_rule,
                    "conflict_type": "include_exclude",
                    "message": f"Conflict: '{target}' has an existing {opposite_type} rule, "
                               f"but you're adding an {rule_type} rule.",
                }

        return None

    def _summarize_rules(self, rules: dict) -> dict:
        """Summarize recipe rules for display.

        Args:
            rules: Dict with 'include' and 'exclude' lists
        """
        if isinstance(rules, list):
            # Handle legacy flat list format
            include_rules = [r for r in rules if r.get("type") == "INCLUDE"]
            exclude_rules = [r for r in rules if r.get("type") == "EXCLUDE"]
        else:
            # Handle nested dict format
            include_rules = rules.get("include", [])
            exclude_rules = rules.get("exclude", [])

        return {
            "include": [
                {
                    "target": r.get("target"),
                    "frequency": r.get("frequency"),
                    "meal_slots": r.get("meal_slot"),
                }
                for r in include_rules
            ],
            "exclude": [
                {
                    "target": r.get("target"),
                    "reason": r.get("reason"),
                }
                for r in exclude_rules
            ],
            "total": len(include_rules) + len(exclude_rules),
        }

    def _summarize_preferences(self, prefs: dict) -> dict:
        """Summarize preferences for display."""
        return {
            "dietary_tags": prefs.get("dietary_tags", []),
            "cuisine_preferences": prefs.get("cuisine_preferences", []),
            "allergies": prefs.get("allergies", []),
            "dislikes": prefs.get("disliked_ingredients", []),
            "cooking_time": {
                "weekday": prefs.get("weekday_cooking_time_minutes", 30),
                "weekend": prefs.get("weekend_cooking_time_minutes", 60),
            },
            "busy_days": prefs.get("busy_days", []),
        }

    def _get_pref_key(self, preference_type: str) -> str:
        """Get the database key for a preference type."""
        key_map = {
            "cooking_time": "weekday_cooking_time_minutes",
            "busy_days": "busy_days",
            "dietary_tags": "dietary_tags",
            "spice_level": "spice_level",
            "cuisine": "cuisine_preferences",
        }
        return key_map.get(preference_type, preference_type)
