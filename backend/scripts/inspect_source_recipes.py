"""
Inspect recipe data structure from khanakyabanega Firebase project.

This script connects to the source Firebase project and analyzes the recipe
collection structure to understand the data schema for migration.

Usage:
    cd backend
    source venv/bin/activate  # or venv\\Scripts\\activate on Windows
    python scripts/inspect_source_recipes.py
"""

import asyncio
import json
import os
import sys
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Any

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from google.cloud.firestore_v1 import AsyncClient


# Source Firebase credentials (khanakyabanega project)
SOURCE_CREDENTIALS_PATH = "khanakyabanega-firebase-adminsdk-fbsvc-4351e5a2e3.json"


def get_source_client() -> AsyncClient:
    """Get Firestore client for the source (khanakyabanega) project."""
    creds_path = Path(__file__).parent.parent / SOURCE_CREDENTIALS_PATH
    if not creds_path.exists():
        raise FileNotFoundError(
            f"Source credentials not found at: {creds_path}\n"
            "Please ensure the khanakyabanega service account JSON is in the backend folder."
        )
    return AsyncClient.from_service_account_json(str(creds_path))


def analyze_field(value: Any, field_name: str = "") -> dict:
    """Analyze a field value and return type info."""
    if value is None:
        return {"type": "null", "example": None}
    elif isinstance(value, bool):
        return {"type": "bool", "example": value}
    elif isinstance(value, int):
        return {"type": "int", "example": value}
    elif isinstance(value, float):
        return {"type": "float", "example": value}
    elif isinstance(value, str):
        return {"type": "string", "example": value[:100] if len(value) > 100 else value}
    elif isinstance(value, datetime):
        return {"type": "datetime", "example": str(value)}
    elif isinstance(value, list):
        if len(value) == 0:
            return {"type": "list[empty]", "example": []}
        else:
            # Analyze first item
            item_type = analyze_field(value[0], f"{field_name}[0]")
            return {
                "type": f"list[{item_type['type']}]",
                "length": len(value),
                "example": value[:2] if len(value) > 2 else value,
            }
    elif isinstance(value, dict):
        return {
            "type": "dict",
            "fields": {k: analyze_field(v, f"{field_name}.{k}") for k, v in value.items()},
        }
    else:
        return {"type": str(type(value).__name__), "example": str(value)[:100]}


async def inspect_collection(client: AsyncClient, collection_name: str, limit: int = 5):
    """Inspect a collection and analyze its structure."""
    print(f"\n{'='*60}")
    print(f"Collection: {collection_name}")
    print('='*60)

    collection = client.collection(collection_name)

    # Get a few sample documents
    docs = []
    count = 0
    async for doc in collection.limit(limit).stream():
        data = doc.to_dict()
        data["__doc_id__"] = doc.id
        docs.append(data)
        count += 1

    if count == 0:
        print("  (empty collection)")
        return None

    # Count total documents
    total_count = 0
    async for _ in collection.stream():
        total_count += 1

    print(f"Total documents: {total_count}")
    print(f"Sample size: {count}")

    # Analyze schema from all samples
    all_fields = set()
    field_types = {}
    field_examples = {}

    for doc in docs:
        for field, value in doc.items():
            all_fields.add(field)
            analysis = analyze_field(value, field)

            if field not in field_types:
                field_types[field] = Counter()
                field_examples[field] = []

            field_types[field][analysis["type"]] += 1
            if len(field_examples[field]) < 3:
                field_examples[field].append(analysis.get("example"))

    print("\nSchema:")
    print("-" * 40)
    for field in sorted(all_fields):
        types = field_types[field].most_common()
        type_str = ", ".join([f"{t}({c})" for t, c in types])
        print(f"  {field}: {type_str}")

        # Show example for simple types
        examples = field_examples[field]
        if examples and not isinstance(examples[0], dict):
            print(f"    Examples: {examples[:2]}")

    # Return first document for detailed view
    return docs[0] if docs else None


async def inspect_subcollections(client: AsyncClient, parent_path: str, doc_id: str):
    """Check for subcollections under a document."""
    print(f"\nChecking subcollections under {parent_path}/{doc_id}...")

    # Note: Firestore Admin SDK can list subcollections
    parent_ref = client.document(f"{parent_path}/{doc_id}")

    try:
        collections = parent_ref.collections()
        subcoll_names = []
        async for coll in collections:
            subcoll_names.append(coll.id)

        if subcoll_names:
            print(f"  Found subcollections: {subcoll_names}")
            for subcoll in subcoll_names:
                await inspect_collection(client, f"{parent_path}/{doc_id}/{subcoll}", limit=3)
        else:
            print("  No subcollections found")
    except Exception as e:
        print(f"  Could not list subcollections: {e}")


async def main():
    """Main inspection routine."""
    print("="*60)
    print("KhanaKyaBanega Recipe Database Inspector")
    print("="*60)

    try:
        client = get_source_client()
        print("[OK] Connected to khanakyabanega Firebase project")
    except Exception as e:
        print(f"[ERROR] Failed to connect: {e}")
        return

    # List all root collections
    print("\n" + "-"*40)
    print("Root Collections:")
    print("-"*40)

    root_collections = []
    try:
        async for coll in client.collections():
            root_collections.append(coll.id)
            print(f"  - {coll.id}")
    except Exception as e:
        print(f"Error listing collections: {e}")
        # Try known collection names
        root_collections = ["recipes", "users", "mealPlans", "ingredients"]
        print(f"Trying known collections: {root_collections}")

    # Inspect recipes collection specifically
    print("\n" + "="*60)
    print("RECIPE COLLECTION ANALYSIS")
    print("="*60)

    # Try different possible collection names for recipes
    recipe_collections = ["recipes", "Recipes", "recipe", "Recipe"]

    for coll_name in recipe_collections:
        try:
            sample = await inspect_collection(client, coll_name, limit=10)
            if sample:
                # Show detailed view of first recipe
                print(f"\n{'='*60}")
                print("SAMPLE RECIPE (Full Document)")
                print('='*60)

                # Pretty print the sample
                def serialize(obj):
                    if isinstance(obj, datetime):
                        return obj.isoformat()
                    return str(obj)

                print(json.dumps(sample, indent=2, default=serialize, ensure_ascii=False))

                # Check for subcollections (ingredients, instructions might be subcollections)
                doc_id = sample.get("__doc_id__")
                if doc_id:
                    await inspect_subcollections(client, coll_name, doc_id)

                break
        except Exception as e:
            print(f"Collection '{coll_name}' not accessible: {e}")

    # Inspect other common collections
    other_collections = ["users", "mealPlans", "ingredients", "categories", "tags"]

    for coll_name in other_collections:
        try:
            await inspect_collection(client, coll_name, limit=3)
        except Exception as e:
            pass  # Collection might not exist

    print("\n" + "="*60)
    print("Inspection Complete!")
    print("="*60)


if __name__ == "__main__":
    asyncio.run(main())
