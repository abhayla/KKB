#!/usr/bin/env python3
"""
Sync Configuration to PostgreSQL

This script reads YAML configuration files and syncs them to PostgreSQL.
It's the bridge between version-controlled config files and runtime config.

Usage:
    python scripts/sync_config_postgres.py                    # Sync all config
    python scripts/sync_config_postgres.py --dry-run          # Preview changes
"""

import argparse
import asyncio
import json
import logging
import os
import sys
import uuid
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse

import asyncpg
import yaml
from dotenv import load_dotenv

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

# Load environment variables
load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Config paths
CONFIG_DIR = Path(__file__).parent.parent / "config"
MEAL_GENERATION_CONFIG = CONFIG_DIR / "meal_generation.yaml"
REFERENCE_DATA_DIR = CONFIG_DIR / "reference_data"


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


def load_yaml(file_path: Path) -> dict:
    """Load a YAML file and return its contents."""
    with open(file_path, 'r', encoding='utf-8') as f:
        return yaml.safe_load(f)


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


async def sync_meal_generation_config(conn, dry_run: bool = False) -> bool:
    """Sync meal_generation.yaml to PostgreSQL system_config table."""
    logger.info("=" * 60)
    logger.info("Syncing meal generation config...")

    if not MEAL_GENERATION_CONFIG.exists():
        logger.error(f"Config file not found: {MEAL_GENERATION_CONFIG}")
        return False

    config = load_yaml(MEAL_GENERATION_CONFIG)

    logger.info(f"  Loaded config with {len(config)} top-level keys:")
    for key in config.keys():
        if isinstance(config[key], dict):
            logger.info(f"    - {key}: {len(config[key])} items")
        elif isinstance(config[key], list):
            logger.info(f"    - {key}: {len(config[key])} items")
        else:
            logger.info(f"    - {key}: {config[key]}")

    if dry_run:
        logger.info("  [DRY RUN] Would write to: system_config (key: meal_generation)")
        return True

    now = datetime.now(timezone.utc)

    # Upsert to PostgreSQL
    await conn.execute('''
        INSERT INTO system_config (id, key, description, config_data, created_at, updated_at)
        VALUES ($1, $2, $3, $4, $5, $6)
        ON CONFLICT (key) DO UPDATE SET
            config_data = $4,
            updated_at = $6
    ''',
        str(uuid.uuid4()),
        'meal_generation',
        'Meal generation configuration including pairing rules and categories',
        json.dumps(config),
        now,
        now,
    )

    logger.info("  ✓ Written to: system_config (key: meal_generation)")
    return True


async def sync_reference_data(conn, dry_run: bool = False) -> bool:
    """Sync reference_data/*.yaml files to PostgreSQL reference_data table."""
    logger.info("=" * 60)
    logger.info("Syncing reference data...")

    if not REFERENCE_DATA_DIR.exists():
        logger.error(f"Reference data directory not found: {REFERENCE_DATA_DIR}")
        return False

    yaml_files = list(REFERENCE_DATA_DIR.glob("*.yaml"))
    logger.info(f"  Found {len(yaml_files)} reference data files")

    now = datetime.now(timezone.utc)

    for yaml_file in yaml_files:
        doc_name = yaml_file.stem  # e.g., "ingredients" from "ingredients.yaml"
        data = load_yaml(yaml_file)

        # Count items
        item_count = 0
        for key, value in data.items():
            if isinstance(value, list):
                item_count = len(value)
                break

        logger.info(f"  - {doc_name}: {item_count} items")

        if dry_run:
            logger.info(f"    [DRY RUN] Would write to: reference_data (category: {doc_name})")
            continue

        # Upsert to PostgreSQL
        await conn.execute('''
            INSERT INTO reference_data (id, category, description, data, created_at, updated_at)
            VALUES ($1, $2, $3, $4, $5, $6)
            ON CONFLICT (category) DO UPDATE SET
                data = $4,
                updated_at = $6
        ''',
            str(uuid.uuid4()),
            doc_name,
            f'{doc_name.capitalize()} reference data',
            json.dumps(data),
            now,
            now,
        )

        logger.info(f"    ✓ Written to: reference_data (category: {doc_name})")

    return True


async def verify_sync(conn) -> bool:
    """Verify that data was synced correctly."""
    logger.info("=" * 60)
    logger.info("Verifying sync...")

    # Check system_config
    row = await conn.fetchrow(
        "SELECT key, config_data FROM system_config WHERE key = $1",
        'meal_generation'
    )
    if row:
        data = json.loads(row['config_data'])
        logger.info(f"  ✓ system_config/meal_generation: {len(data)} keys")
    else:
        logger.error("  ✗ system_config/meal_generation: NOT FOUND")
        return False

    # Check reference_data
    ref_docs = ["ingredients", "dishes", "cuisines"]
    for doc_name in ref_docs:
        row = await conn.fetchrow(
            "SELECT category, data FROM reference_data WHERE category = $1",
            doc_name
        )
        if row:
            data = json.loads(row['data'])
            # Count items in first list field
            for key, value in data.items():
                if isinstance(value, list):
                    logger.info(f"  ✓ reference_data/{doc_name}: {len(value)} {key}")
                    break
        else:
            logger.warning(f"  ⚠ reference_data/{doc_name}: NOT FOUND (may be expected)")

    logger.info("=" * 60)
    logger.info("✓ Config sync verification complete!")
    return True


async def main():
    parser = argparse.ArgumentParser(description="Sync configuration to PostgreSQL")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Preview changes without writing to PostgreSQL"
    )
    parser.add_argument(
        "--verify-only",
        action="store_true",
        help="Only verify existing data, don't sync"
    )

    args = parser.parse_args()

    logger.info("=" * 60)
    logger.info("  MEAL GENERATION CONFIG SYNC (PostgreSQL)")
    logger.info("=" * 60)

    if args.dry_run:
        logger.info("  Mode: DRY RUN (no changes will be made)")
    else:
        logger.info("  Mode: LIVE (will write to PostgreSQL)")

    # Get PostgreSQL connection
    conn = await get_pg_connection()
    logger.info("  Connected to PostgreSQL")

    try:
        if args.verify_only:
            success = await verify_sync(conn)
            return 0 if success else 1

        success = True

        # Sync meal generation config
        if not await sync_meal_generation_config(conn, args.dry_run):
            success = False

        # Sync reference data
        if not await sync_reference_data(conn, args.dry_run):
            success = False

        # Verify sync (unless dry run)
        if not args.dry_run and success:
            success = await verify_sync(conn)

        return 0 if success else 1

    finally:
        await conn.close()


if __name__ == "__main__":
    exit_code = asyncio.run(main())
    sys.exit(exit_code)
