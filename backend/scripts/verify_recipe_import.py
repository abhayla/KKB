"""
Verify recipe import into RasoiAI database.

Usage:
    cd backend
    python scripts/verify_recipe_import.py
"""

import asyncio
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from google.cloud.firestore_v1 import AsyncClient

TARGET_CREDENTIALS_PATH = "rasoiai-firebase-service-account.json"


def get_target_client() -> AsyncClient:
    """Get Firestore client for RasoiAI project."""
    creds_path = Path(__file__).parent.parent / TARGET_CREDENTIALS_PATH
    return AsyncClient.from_service_account_json(str(creds_path))


async def verify():
    print("="*60)
    print("RasoiAI Recipe Database Verification")
    print("="*60)

    client = get_target_client()
    collection = client.collection("recipes")

    # Count recipes
    total = 0
    cuisines = Counter()
    meal_types = Counter()
    diets = Counter()
    imported = 0

    async for doc in collection.stream():
        data = doc.to_dict()
        total += 1

        if data.get("imported_from") == "khanakyabanega":
            imported += 1

        cuisines[data.get("cuisine_type", "unknown")] += 1

        for mt in data.get("meal_types", []):
            meal_types[mt] += 1

        for dt in data.get("dietary_tags", []):
            diets[dt] += 1

    print(f"\nTotal recipes: {total}")
    print(f"Imported from khanakyabanega: {imported}")

    print("\nCuisine Distribution:")
    for cuisine, count in cuisines.most_common():
        print(f"  {cuisine}: {count}")

    print("\nMeal Types:")
    for mt, count in meal_types.most_common():
        print(f"  {mt}: {count}")

    print("\nDietary Tags:")
    for dt, count in diets.most_common():
        print(f"  {dt}: {count}")

    # Sample a few recipes
    print("\n" + "-"*40)
    print("SAMPLE RECIPES")
    print("-"*40)

    count = 0
    async for doc in collection.where("cuisine_type", "==", "south").limit(3).stream():
        data = doc.to_dict()
        print(f"\n[{data['cuisine_type'].upper()}] {data['name']}")
        print(f"  ID: {data['id']}")
        print(f"  Ingredients: {len(data.get('ingredients', []))}")
        print(f"  Instructions: {len(data.get('instructions', []))}")
        count += 1


if __name__ == "__main__":
    asyncio.run(verify())
