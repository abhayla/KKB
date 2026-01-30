#!/usr/bin/env python3
"""
Import recipes from khanakyabanega Firebase project to RasoiAI PostgreSQL.

This script reads recipes from the source Firebase database and transforms them
to match the RasoiAI schema before inserting into PostgreSQL.

Usage:
    cd backend
    source venv/Scripts/activate  # Windows
    source venv/bin/activate       # Linux/Mac

    # Dry run (preview only)
    python scripts/import_recipes_postgres.py --dry-run

    # Import first 100 recipes
    python scripts/import_recipes_postgres.py --limit 100

    # Import all recipes
    python scripts/import_recipes_postgres.py --all

    # Import only missing recipes (not already in database)
    python scripts/import_recipes_postgres.py --missing-only
"""

import argparse
import asyncio
import json
import os
import re
import sys
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Optional
from urllib.parse import urlparse

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from dotenv import load_dotenv
from google.cloud.firestore_v1 import AsyncClient
import asyncpg

# Import mappings from the original script
from scripts.import_recipes_from_kkb import (
    CUISINE_MAPPING,
    DIET_MAPPING,
    MEAL_TYPE_MAPPING,
    DIFFICULTY_MAPPING,
    INGREDIENT_CATEGORIES,
    parse_ingredient_string,
    transform_recipe,
)

# Load environment variables from .env file
load_dotenv()

# Source Firebase credentials (khanakyabanega project)
SOURCE_CREDENTIALS_PATH = "khanakyabanega-firebase-adminsdk-fbsvc-4351e5a2e3.json"


def get_pg_config():
    """Get PostgreSQL connection config from DATABASE_URL environment variable."""
    database_url = os.getenv("DATABASE_URL")
    if not database_url:
        raise ValueError(
            "DATABASE_URL environment variable is required. "
            "Format: postgresql+asyncpg://user:password@host:port/database"
        )

    # Parse the URL (handle both postgresql:// and postgresql+asyncpg://)
    url = database_url.replace("postgresql+asyncpg://", "postgresql://")
    parsed = urlparse(url)

    return {
        "host": parsed.hostname,
        "port": parsed.port or 5432,
        "user": parsed.username,
        "password": parsed.password,
        "database": parsed.path.lstrip("/"),
    }


def get_source_client() -> AsyncClient:
    """Get Firestore client for the source (khanakyabanega) project."""
    creds_path = Path(__file__).parent.parent / SOURCE_CREDENTIALS_PATH
    if not creds_path.exists():
        raise FileNotFoundError(f"Source credentials not found at: {creds_path}")
    return AsyncClient.from_service_account_json(str(creds_path))


async def get_pg_connection():
    """Get PostgreSQL connection."""
    config = get_pg_config()
    return await asyncpg.connect(
        host=config["host"],
        port=config["port"],
        user=config["user"],
        password=config["password"],
        database=config["database"],
    )


async def get_existing_recipe_ids(conn) -> set:
    """Get all recipe IDs already in PostgreSQL."""
    rows = await conn.fetch("SELECT id FROM recipes")
    return {row['id'] for row in rows}


def safe_print(text: str) -> None:
    """Print text safely, handling Unicode encoding errors on Windows console."""
    try:
        print(text)
    except UnicodeEncodeError:
        # Replace problematic characters with '?' for Windows console
        print(text.encode('ascii', errors='replace').decode('ascii'))


async def insert_recipe_to_postgres(conn, recipe: dict) -> bool:
    """Insert a transformed recipe into PostgreSQL.

    Creates entries in:
    - recipes
    - recipe_ingredients
    - recipe_instructions
    - recipe_nutrition
    """
    now = datetime.now(timezone.utc)

    try:
        # Insert recipe
        await conn.execute('''
            INSERT INTO recipes (
                id, name, description, image_url, cuisine_type, meal_types,
                dietary_tags, course_type, category, prep_time_minutes, cook_time_minutes,
                total_time_minutes, servings, difficulty_level, is_festive,
                is_fasting_friendly, is_quick_meal, is_kid_friendly, is_active,
                created_at, updated_at
            ) VALUES (
                $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15,
                $16, $17, $18, $19, $20, $21
            )
            ON CONFLICT (id) DO NOTHING
        ''',
            recipe['id'],
            recipe['name'],
            recipe.get('description', ''),
            recipe.get('image_url'),
            recipe['cuisine_type'],
            json.dumps(recipe['meal_types']),
            json.dumps(recipe['dietary_tags']),
            recipe.get('course_type', 'main'),
            recipe.get('category'),  # Will be None initially, categorize_recipes.py can update
            recipe['prep_time_minutes'],
            recipe['cook_time_minutes'],
            recipe['total_time_minutes'],
            recipe['servings'],
            recipe.get('difficulty_level', 'medium'),
            recipe.get('is_festive', False),
            recipe.get('is_fasting_friendly', False),
            recipe.get('is_quick_meal', False),
            recipe.get('is_kid_friendly', False),
            True,
            now,
            now,
        )

        # Insert ingredients
        for i, ing in enumerate(recipe.get('ingredients', [])):
            await conn.execute('''
                INSERT INTO recipe_ingredients (
                    id, recipe_id, name, quantity, unit, category, notes, is_optional, "order"
                ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                ON CONFLICT (id) DO NOTHING
            ''',
                ing.get('id') or str(uuid.uuid4()),
                recipe['id'],
                ing['name'],
                ing['quantity'],
                ing['unit'],
                ing.get('category', 'other'),
                ing.get('notes'),
                ing.get('is_optional', False),
                i,
            )

        # Insert instructions
        for inst in recipe.get('instructions', []):
            await conn.execute('''
                INSERT INTO recipe_instructions (
                    id, recipe_id, step_number, instruction, duration_minutes, timer_required, tips
                ) VALUES ($1, $2, $3, $4, $5, $6, $7)
                ON CONFLICT (id) DO NOTHING
            ''',
                inst.get('id') or str(uuid.uuid4()),
                recipe['id'],
                inst['step_number'],
                inst['instruction'],
                inst.get('duration_minutes'),
                inst.get('timer_required', False),
                inst.get('tips'),
            )

        # Insert nutrition
        nutrition = recipe.get('nutrition', {})
        if nutrition:
            await conn.execute('''
                INSERT INTO recipe_nutrition (
                    id, recipe_id, calories, protein_grams, carbohydrates_grams,
                    fat_grams, fiber_grams, sugar_grams, sodium_mg
                ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                ON CONFLICT (recipe_id) DO NOTHING
            ''',
                str(uuid.uuid4()),
                recipe['id'],
                nutrition.get('calories', 0),
                nutrition.get('protein_grams', 0),
                nutrition.get('carbohydrates_grams', 0),
                nutrition.get('fat_grams', 0),
                nutrition.get('fiber_grams', 0),
                nutrition.get('sugar_grams'),
                nutrition.get('sodium_mg'),
            )

        return True

    except Exception as e:
        safe_print(f"  Error inserting {recipe['name']}: {e}")
        return False


async def import_recipes(
    limit: Optional[int] = None,
    dry_run: bool = True,
    offset: int = 0,
    batch_size: int = 100,
    missing_only: bool = False,
):
    """Import recipes from source to PostgreSQL database."""
    print("=" * 60)
    print("Recipe Import: khanakyabanega -> PostgreSQL")
    print("=" * 60)
    print(f"Mode: {'DRY RUN (no changes)' if dry_run else 'LIVE IMPORT'}")
    if missing_only:
        print("Filter: MISSING ONLY (skipping existing recipes)")
    print(f"Limit: {limit if limit else 'ALL'}")
    print(f"Offset: {offset}")
    print(f"Batch size: {batch_size}")
    print()

    # Connect to source
    try:
        source_client = get_source_client()
        print("[OK] Connected to source (khanakyabanega Firebase)")
    except Exception as e:
        print(f"[ERROR] Failed to connect to source: {e}")
        return

    # Connect to PostgreSQL (required for missing_only or live import)
    pg_conn = None
    existing_ids = set()
    if not dry_run or missing_only:
        try:
            pg_conn = await get_pg_connection()
            print("[OK] Connected to PostgreSQL")

            # Check current recipe count
            count = await pg_conn.fetchval("SELECT COUNT(*) FROM recipes")
            print(f"[INFO] Current recipes in PostgreSQL: {count}")

            # Get existing recipe IDs if filtering for missing only
            if missing_only:
                existing_ids = await get_existing_recipe_ids(pg_conn)
                print(f"[INFO] Existing recipe IDs loaded: {len(existing_ids)}")
        except Exception as e:
            print(f"[ERROR] Failed to connect to PostgreSQL: {e}")
            return

    # Fetch recipes from source
    print("\nFetching recipes from source...")
    source_recipes = []
    source_collection = source_client.collection("recipes")

    # For missing_only, we need to fetch all and filter (can't filter by ID in Firestore efficiently)
    query = source_collection.order_by("createdAt")
    if offset > 0 and not missing_only:
        query = query.offset(offset)
    # Don't apply limit when missing_only - we'll filter and then apply limit
    if limit and not missing_only:
        query = query.limit(limit)

    fetched_count = 0
    skipped_existing = 0
    async for doc in query.stream():
        fetched_count += 1
        # Skip existing recipes if missing_only is enabled
        if missing_only and doc.id in existing_ids:
            skipped_existing += 1
            continue
        data = doc.to_dict()
        data["id"] = doc.id
        source_recipes.append(data)
        # Apply limit after filtering for missing_only
        if missing_only and limit and len(source_recipes) >= limit:
            break

    print(f"Fetched {fetched_count} recipes from source")
    if missing_only:
        print(f"Skipped {skipped_existing} existing recipes")
        print(f"Found {len(source_recipes)} missing recipes to import")

    if not source_recipes:
        print("No recipes to import.")
        return

    # Transform recipes
    print("\nTransforming recipes...")
    transformed = []
    errors = []

    for i, source in enumerate(source_recipes):
        try:
            recipe = transform_recipe(source)
            transformed.append(recipe)
        except Exception as e:
            errors.append({
                "id": source.get("id"),
                "title": source.get("title"),
                "error": str(e),
            })

    print(f"Successfully transformed: {len(transformed)}")
    if errors:
        print(f"Errors during transformation: {len(errors)}")
        for err in errors[:5]:
            safe_print(f"  - {err['title']}: {err['error']}")

    # Preview first few
    print("\n" + "-" * 40)
    print("SAMPLE TRANSFORMED RECIPES")
    print("-" * 40)
    for recipe in transformed[:3]:
        safe_print(f"\n[{recipe['cuisine_type'].upper()}] {recipe['name']}")
        safe_print(f"  Meal types: {recipe['meal_types']}")
        safe_print(f"  Diet: {recipe['dietary_tags']}")
        safe_print(f"  Time: {recipe['total_time_minutes']} min")
        safe_print(f"  Ingredients: {len(recipe['ingredients'])}")
        safe_print(f"  Instructions: {len(recipe['instructions'])} steps")

    # Statistics
    cuisine_counts = {}
    diet_counts = {}
    for r in transformed:
        c = r['cuisine_type']
        cuisine_counts[c] = cuisine_counts.get(c, 0) + 1
        for d in r['dietary_tags']:
            diet_counts[d] = diet_counts.get(d, 0) + 1

    print("\n" + "-" * 40)
    print("STATISTICS")
    print("-" * 40)
    print("By cuisine:")
    for c, count in sorted(cuisine_counts.items(), key=lambda x: -x[1]):
        print(f"  {c}: {count}")
    print("By diet:")
    for d, count in sorted(diet_counts.items(), key=lambda x: -x[1]):
        print(f"  {d}: {count}")

    if dry_run:
        print("\n" + "=" * 60)
        print("DRY RUN COMPLETE - No changes made")
        print("=" * 60)
        print(f"\nTo import, run: python scripts/import_recipes_postgres.py --all")
        if pg_conn:
            await pg_conn.close()
        return

    # Import to PostgreSQL
    print("\n" + "-" * 40)
    print("IMPORTING TO POSTGRESQL...")
    print("-" * 40)

    imported = 0
    skipped = 0
    import_errors = []

    for i, recipe in enumerate(transformed):
        success = await insert_recipe_to_postgres(pg_conn, recipe)
        if success:
            imported += 1
        else:
            import_errors.append(recipe['name'])

        if (i + 1) % batch_size == 0:
            print(f"  Progress: {i + 1}/{len(transformed)} recipes...")

    # Final count
    final_count = await pg_conn.fetchval("SELECT COUNT(*) FROM recipes")

    print(f"\n{'=' * 60}")
    print("IMPORT COMPLETE")
    print(f"{'=' * 60}")
    print(f"  Imported: {imported}")
    print(f"  Errors: {len(import_errors)}")
    print(f"  Total recipes in database: {final_count}")

    if import_errors:
        print(f"\nFailed recipes:")
        for name in import_errors[:10]:
            safe_print(f"  - {name}")
        if len(import_errors) > 10:
            print(f"  ... and {len(import_errors) - 10} more")

    # Close connection
    await pg_conn.close()


def main():
    parser = argparse.ArgumentParser(description="Import recipes from khanakyabanega to PostgreSQL")
    parser.add_argument("--dry-run", action="store_true", help="Preview only, don't import")
    parser.add_argument("--limit", type=int, help="Maximum recipes to import")
    parser.add_argument("--offset", type=int, default=0, help="Skip first N recipes")
    parser.add_argument("--all", action="store_true", help="Import all recipes")
    parser.add_argument("--missing-only", action="store_true", help="Import only recipes not already in database")
    parser.add_argument("--batch-size", type=int, default=100, help="Progress update interval")

    args = parser.parse_args()

    if args.all or args.missing_only:
        limit = args.limit  # Allow optional limit with --missing-only
    else:
        limit = args.limit or 10  # Default to 10 for safety

    # Default to dry-run if not explicitly importing all or missing-only
    dry_run = args.dry_run or (not args.all and not args.missing_only and args.limit is None)

    asyncio.run(import_recipes(
        limit=limit,
        dry_run=dry_run,
        offset=args.offset,
        batch_size=args.batch_size,
        missing_only=args.missing_only,
    ))


if __name__ == "__main__":
    main()
