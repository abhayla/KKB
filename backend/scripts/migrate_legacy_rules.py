"""
Migration script for legacy recipe rules.

Migrates recipe rules from user_preferences.recipe_rules (JSON field)
to the new recipe_rules table (normalized schema).

Usage:
    PYTHONPATH=. python scripts/migrate_legacy_rules.py [--dry-run] [--user-id USER_ID]

Arguments:
    --dry-run: Preview changes without committing to database
    --user-id: Migrate only specific user (for testing)
"""

import argparse
import asyncio
import logging
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.postgres import get_db
from app.models.recipe_rule import RecipeRule
from app.models.user import User, UserPreferences

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


@dataclass
class MigrationStats:
    """Statistics for migration operation."""

    users_processed: int = 0
    rules_migrated: int = 0
    users_skipped: int = 0
    errors: int = 0
    error_details: list[str] = field(default_factory=list)

    def summary(self) -> str:
        """Return formatted summary of migration."""
        return (
            f"Migration Summary:\n"
            f"  Users processed: {self.users_processed}\n"
            f"  Rules migrated: {self.rules_migrated}\n"
            f"  Users skipped: {self.users_skipped}\n"
            f"  Errors: {self.errors}\n"
        )


def map_frequency_to_new_format(
    old_frequency: str, times_per_week: Optional[int]
) -> dict:
    """Map legacy frequency format to new schema.

    Args:
        old_frequency: DAILY, WEEKLY, TIMES_PER_WEEK, NEVER
        times_per_week: Number of times per week (1-7)

    Returns:
        dict with frequency_type, frequency_count, frequency_days
    """
    if old_frequency == "DAILY":
        return {
            "frequency_type": "DAILY",
            "frequency_count": None,
            "frequency_days": None,
        }
    elif old_frequency == "NEVER":
        return {
            "frequency_type": "NEVER",
            "frequency_count": None,
            "frequency_days": None,
        }
    elif old_frequency in ("WEEKLY", "TIMES_PER_WEEK"):
        return {
            "frequency_type": "TIMES_PER_WEEK",
            "frequency_count": times_per_week or 1,
            "frequency_days": None,
        }
    else:
        # Default fallback
        logger.warning(f"Unknown frequency: {old_frequency}, defaulting to TIMES_PER_WEEK")
        return {
            "frequency_type": "TIMES_PER_WEEK",
            "frequency_count": times_per_week or 1,
            "frequency_days": None,
        }


def map_meal_slot_to_new_format(meal_slots: list[str]) -> Optional[str]:
    """Map legacy meal_slot list to new single meal_slot field.

    Args:
        meal_slots: List of meal slots from legacy format

    Returns:
        Single meal slot or None if all slots (meaning no restriction)
    """
    if not meal_slots:
        return None

    # If all 4 slots are specified, it means no restriction
    all_slots = {"BREAKFAST", "LUNCH", "DINNER", "SNACKS"}
    if set(meal_slots) == all_slots:
        return None

    # Otherwise, take the first slot (or could be configurable)
    # For rules with multiple specific slots, we might need to create
    # multiple RecipeRule entries, but for MVP we'll just take the first
    return meal_slots[0] if meal_slots else None


async def migrate_user_rules(
    session: AsyncSession, user_id: str, dry_run: bool = False
) -> MigrationStats:
    """Migrate legacy rules for a single user.

    Args:
        session: Database session
        user_id: User ID to migrate
        dry_run: If True, don't commit changes

    Returns:
        MigrationStats with migration results
    """
    stats = MigrationStats()
    stats.users_processed = 1

    try:
        # Get user's preferences
        result = await session.execute(
            select(UserPreferences).where(UserPreferences.user_id == user_id)
        )
        prefs = result.scalar_one_or_none()

        if not prefs:
            logger.warning(f"No preferences found for user {user_id}")
            stats.users_skipped = 1
            return stats

        # Get legacy recipe_rules
        legacy_rules = prefs.recipe_rules
        if not legacy_rules or not isinstance(legacy_rules, dict):
            logger.info(f"User {user_id} has no legacy rules to migrate")
            stats.users_skipped = 1
            return stats

        # Check if user already has rules in new table (idempotency)
        existing_result = await session.execute(
            select(RecipeRule).where(RecipeRule.user_id == user_id)
        )
        existing_rules = existing_result.scalars().all()
        if existing_rules:
            logger.info(
                f"User {user_id} already has {len(existing_rules)} rules in new table, skipping"
            )
            stats.users_skipped = 1
            return stats

        # Process INCLUDE rules
        include_rules = legacy_rules.get("include", [])
        for rule_data in include_rules:
            new_rule = create_recipe_rule_from_legacy(user_id, rule_data, "INCLUDE")
            session.add(new_rule)
            stats.rules_migrated += 1
            logger.info(
                f"Migrated INCLUDE rule for {rule_data.get('target')} (user {user_id})"
            )

        # Process EXCLUDE rules
        exclude_rules = legacy_rules.get("exclude", [])
        for rule_data in exclude_rules:
            new_rule = create_recipe_rule_from_legacy(user_id, rule_data, "EXCLUDE")
            session.add(new_rule)
            stats.rules_migrated += 1
            logger.info(
                f"Migrated EXCLUDE rule for {rule_data.get('target')} (user {user_id})"
            )

        if not dry_run:
            await session.commit()
            logger.info(f"Committed {stats.rules_migrated} rules for user {user_id}")
        else:
            logger.info(f"[DRY RUN] Would migrate {stats.rules_migrated} rules for user {user_id}")

    except Exception as e:
        logger.error(f"Error migrating user {user_id}: {e}")
        stats.errors = 1
        stats.error_details.append(f"User {user_id}: {str(e)}")
        await session.rollback()

    return stats


def create_recipe_rule_from_legacy(
    user_id: str, legacy_rule: dict, action: str
) -> RecipeRule:
    """Create a RecipeRule from legacy rule format.

    Args:
        user_id: User ID
        legacy_rule: Legacy rule dict
        action: INCLUDE or EXCLUDE

    Returns:
        RecipeRule instance
    """
    target_name = legacy_rule.get("target", "Unknown")
    old_frequency = legacy_rule.get("frequency", "WEEKLY")
    times_per_week = legacy_rule.get("times_per_week", 1)
    meal_slots = legacy_rule.get("meal_slot", [])
    is_active = legacy_rule.get("is_active", True)

    # Map frequency
    frequency_mapping = map_frequency_to_new_format(old_frequency, times_per_week)

    # Map meal slot
    meal_slot = map_meal_slot_to_new_format(meal_slots)

    # Create new rule
    return RecipeRule(
        user_id=user_id,
        target_type="RECIPE",  # Legacy rules are all RECIPE type
        action=action,
        target_id=None,  # Legacy rules didn't have IDs
        target_name=target_name,
        frequency_type=frequency_mapping["frequency_type"],
        frequency_count=frequency_mapping["frequency_count"],
        frequency_days=frequency_mapping["frequency_days"],
        enforcement="REQUIRED",  # Default to REQUIRED
        meal_slot=meal_slot,
        is_active=is_active,
        sync_status="SYNCED",  # Migrated rules are already synced
    )


async def migrate_all_users(
    session: AsyncSession, dry_run: bool = False
) -> MigrationStats:
    """Migrate all users' legacy rules.

    Args:
        session: Database session
        dry_run: If True, don't commit changes

    Returns:
        MigrationStats with migration results
    """
    total_stats = MigrationStats()

    # Get all users
    result = await session.execute(select(User))
    users = result.scalars().all()

    logger.info(f"Found {len(users)} users to process")

    for user in users:
        stats = await migrate_user_rules(session, user.id, dry_run)
        total_stats.users_processed += stats.users_processed
        total_stats.rules_migrated += stats.rules_migrated
        total_stats.users_skipped += stats.users_skipped
        total_stats.errors += stats.errors
        total_stats.error_details.extend(stats.error_details)

    return total_stats


async def main():
    """Main migration entry point."""
    parser = argparse.ArgumentParser(
        description="Migrate legacy recipe rules to new schema"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Preview changes without committing",
    )
    parser.add_argument(
        "--user-id",
        type=str,
        help="Migrate only specific user (for testing)",
    )
    args = parser.parse_args()

    logger.info("Starting recipe rules migration...")
    if args.dry_run:
        logger.info("DRY RUN MODE - No changes will be committed")

    async for session in get_db():
        try:
            if args.user_id:
                # Migrate single user
                logger.info(f"Migrating user: {args.user_id}")
                stats = await migrate_user_rules(session, args.user_id, args.dry_run)
            else:
                # Migrate all users
                logger.info("Migrating all users...")
                stats = await migrate_all_users(session, args.dry_run)

            # Print summary
            logger.info("\n" + stats.summary())

            if stats.errors > 0:
                logger.error("Errors encountered:")
                for error in stats.error_details:
                    logger.error(f"  - {error}")

        except Exception as e:
            logger.error(f"Migration failed: {e}")
            raise


if __name__ == "__main__":
    asyncio.run(main())
