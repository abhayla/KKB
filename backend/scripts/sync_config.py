#!/usr/bin/env python3
"""
Sync Configuration to Firestore

This script reads YAML configuration files and syncs them to Firestore.
It's the bridge between version-controlled config files and runtime config.

Usage:
    python scripts/sync_config.py                    # Sync all config
    python scripts/sync_config.py --dry-run          # Preview changes
    python scripts/sync_config.py --collection meals # Sync specific collection
"""

import argparse
import logging
import sys
from pathlib import Path

import yaml

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.db.firestore import get_firestore_client

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Config paths
CONFIG_DIR = Path(__file__).parent.parent / "config"
MEAL_GENERATION_CONFIG = CONFIG_DIR / "meal_generation.yaml"
REFERENCE_DATA_DIR = CONFIG_DIR / "reference_data"


def load_yaml(file_path: Path) -> dict:
    """Load a YAML file and return its contents."""
    with open(file_path, 'r', encoding='utf-8') as f:
        return yaml.safe_load(f)


async def sync_meal_generation_config(db, dry_run: bool = False) -> bool:
    """Sync meal_generation.yaml to Firestore system_config collection."""
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
        logger.info("  [DRY RUN] Would write to: system_config/meal_generation")
        return True

    # Write to Firestore
    doc_ref = db.collection("system_config").document("meal_generation")
    await doc_ref.set(config)

    logger.info("  ✓ Written to: system_config/meal_generation")
    return True


async def sync_reference_data(db, dry_run: bool = False) -> bool:
    """Sync reference_data/*.yaml files to Firestore reference_data collection."""
    logger.info("=" * 60)
    logger.info("Syncing reference data...")

    if not REFERENCE_DATA_DIR.exists():
        logger.error(f"Reference data directory not found: {REFERENCE_DATA_DIR}")
        return False

    yaml_files = list(REFERENCE_DATA_DIR.glob("*.yaml"))
    logger.info(f"  Found {len(yaml_files)} reference data files")

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
            logger.info(f"    [DRY RUN] Would write to: reference_data/{doc_name}")
            continue

        # Write to Firestore
        doc_ref = db.collection("reference_data").document(doc_name)
        await doc_ref.set(data)

        logger.info(f"    ✓ Written to: reference_data/{doc_name}")

    return True


async def verify_sync(db) -> bool:
    """Verify that data was synced correctly."""
    logger.info("=" * 60)
    logger.info("Verifying sync...")

    # Check system_config
    doc = await db.collection("system_config").document("meal_generation").get()
    if doc.exists:
        data = doc.to_dict()
        logger.info(f"  ✓ system_config/meal_generation: {len(data)} keys")
    else:
        logger.error("  ✗ system_config/meal_generation: NOT FOUND")
        return False

    # Check reference_data
    ref_docs = ["ingredients", "dishes", "cuisines"]
    for doc_name in ref_docs:
        doc = await db.collection("reference_data").document(doc_name).get()
        if doc.exists:
            data = doc.to_dict()
            # Count items in first list field
            for key, value in data.items():
                if isinstance(value, list):
                    logger.info(f"  ✓ reference_data/{doc_name}: {len(value)} {key}")
                    break
        else:
            logger.error(f"  ✗ reference_data/{doc_name}: NOT FOUND")
            return False

    logger.info("=" * 60)
    logger.info("✓ All config synced successfully!")
    return True


async def main():
    parser = argparse.ArgumentParser(description="Sync configuration to Firestore")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Preview changes without writing to Firestore"
    )
    parser.add_argument(
        "--collection",
        choices=["all", "meals", "reference"],
        default="all",
        help="Which collection to sync"
    )
    parser.add_argument(
        "--verify-only",
        action="store_true",
        help="Only verify existing data, don't sync"
    )

    args = parser.parse_args()

    logger.info("=" * 60)
    logger.info("  MEAL GENERATION CONFIG SYNC")
    logger.info("=" * 60)

    if args.dry_run:
        logger.info("  Mode: DRY RUN (no changes will be made)")
    else:
        logger.info("  Mode: LIVE (will write to Firestore)")

    # Get Firestore client
    db = get_firestore_client()

    if args.verify_only:
        success = await verify_sync(db)
        return 0 if success else 1

    success = True

    # Sync meal generation config
    if args.collection in ["all", "meals"]:
        if not await sync_meal_generation_config(db, args.dry_run):
            success = False

    # Sync reference data
    if args.collection in ["all", "reference"]:
        if not await sync_reference_data(db, args.dry_run):
            success = False

    # Verify sync (unless dry run)
    if not args.dry_run and success:
        success = await verify_sync(db)

    return 0 if success else 1


if __name__ == "__main__":
    import asyncio
    exit_code = asyncio.run(main())
    sys.exit(exit_code)
