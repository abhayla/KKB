"""Configuration service for loading meal generation config from Firestore.

This service loads config that was synced from YAML files by scripts/sync_config.py.
Config is cached in memory to avoid repeated Firestore reads.
"""

import logging
from dataclasses import dataclass, field
from typing import Any, Optional

from app.db.firestore import Collections, get_firestore_client

logger = logging.getLogger(__name__)


@dataclass
class MealStructure:
    """Meal structure configuration."""
    items_per_slot: int = 2
    expandable: bool = True
    time_based_items: dict[str, Any] = field(default_factory=dict)  # Variable items per cooking time


@dataclass
class RuleBehavior:
    """Rule behavior configuration."""
    action: str = ""
    description: str = ""
    allows_duplicates: bool = False
    keeps_pair: bool = False


@dataclass
class MealGenerationConfig:
    """Complete meal generation configuration."""
    meal_structure: MealStructure = field(default_factory=MealStructure)
    pairing_rules_flat: dict[str, list[str]] = field(default_factory=dict)
    meal_type_pairs: dict[str, list[str]] = field(default_factory=dict)
    recipe_categories: list[str] = field(default_factory=list)
    ingredient_aliases: dict[str, list[str]] = field(default_factory=dict)
    rule_behaviors: dict[str, RuleBehavior] = field(default_factory=dict)
    conflict_resolution: dict[str, str] = field(default_factory=dict)
    meal_type_settings: dict[str, bool] = field(default_factory=dict)


@dataclass
class IngredientInfo:
    """Ingredient information from reference data."""
    name: str
    aliases: list[str] = field(default_factory=list)
    category: str = ""


@dataclass
class DishInfo:
    """Dish information from reference data."""
    name: str
    category: str = ""
    pairs_with: list[str] = field(default_factory=list)
    meal_types: list[str] = field(default_factory=list)
    cuisines: list[str] = field(default_factory=list)


@dataclass
class CuisineInfo:
    """Cuisine information from reference data."""
    id: str
    name: str
    typical_pairings: dict[str, list[str]] = field(default_factory=dict)
    staple_ingredients: list[str] = field(default_factory=list)


@dataclass
class ReferenceData:
    """Reference data from Firestore."""
    ingredients: list[IngredientInfo] = field(default_factory=list)
    dishes: list[DishInfo] = field(default_factory=list)
    cuisines: list[CuisineInfo] = field(default_factory=list)


class ConfigService:
    """Service for loading and caching meal generation configuration.

    Configuration is loaded from Firestore and cached in memory.
    Call refresh() to reload configuration from Firestore.
    """

    _instance: Optional["ConfigService"] = None
    _config: Optional[MealGenerationConfig] = None
    _reference_data: Optional[ReferenceData] = None

    def __new__(cls) -> "ConfigService":
        """Singleton pattern to ensure config is loaded once."""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        self.db = get_firestore_client()

    async def get_config(self) -> MealGenerationConfig:
        """Get meal generation config, loading from Firestore if not cached."""
        if self._config is None:
            await self._load_config()
        return self._config

    async def get_reference_data(self) -> ReferenceData:
        """Get reference data, loading from Firestore if not cached."""
        if self._reference_data is None:
            await self._load_reference_data()
        return self._reference_data

    async def refresh(self) -> None:
        """Force reload of all configuration from Firestore."""
        await self._load_config()
        await self._load_reference_data()
        logger.info("Configuration refreshed from Firestore")

    async def _load_config(self) -> None:
        """Load meal generation config from Firestore."""
        try:
            doc = await self.db.collection(Collections.SYSTEM_CONFIG).document("meal_generation").get()

            if not doc.exists:
                logger.warning("Meal generation config not found in Firestore, using defaults")
                self._config = MealGenerationConfig()
                return

            data = doc.to_dict()

            # Parse meal structure
            meal_structure_data = data.get("meal_structure", {})
            meal_structure = MealStructure(
                items_per_slot=meal_structure_data.get("items_per_slot", 2),
                expandable=meal_structure_data.get("expandable", True),
                time_based_items=meal_structure_data.get("time_based_items", {}),
            )

            # Parse rule behaviors
            rule_behaviors = {}
            for rule_type, behavior_data in data.get("rule_behaviors", {}).items():
                rule_behaviors[rule_type] = RuleBehavior(
                    action=behavior_data.get("action", ""),
                    description=behavior_data.get("description", ""),
                    allows_duplicates=behavior_data.get("allows_duplicates", False),
                    keeps_pair=behavior_data.get("keeps_pair", False),
                )

            self._config = MealGenerationConfig(
                meal_structure=meal_structure,
                pairing_rules_flat=data.get("pairing_rules_flat", {}),
                meal_type_pairs=data.get("meal_type_pairs", {}),
                recipe_categories=data.get("recipe_categories", []),
                ingredient_aliases=data.get("ingredient_aliases", {}),
                rule_behaviors=rule_behaviors,
                conflict_resolution=data.get("conflict_resolution", {}),
                meal_type_settings=data.get("meal_type_settings", {}),
            )

            logger.info(
                f"Loaded meal generation config: "
                f"{len(self._config.pairing_rules_flat)} pairing rules, "
                f"{len(self._config.recipe_categories)} categories"
            )

        except Exception as e:
            logger.error(f"Failed to load meal generation config: {e}")
            self._config = MealGenerationConfig()

    async def _load_reference_data(self) -> None:
        """Load reference data from Firestore."""
        try:
            ref_collection = self.db.collection(Collections.REFERENCE_DATA)

            # Load ingredients
            ingredients = []
            ing_doc = await ref_collection.document("ingredients").get()
            if ing_doc.exists:
                ing_data = ing_doc.to_dict()
                for item in ing_data.get("ingredients", []):
                    ingredients.append(IngredientInfo(
                        name=item.get("name", ""),
                        aliases=item.get("aliases", []),
                        category=item.get("category", ""),
                    ))

            # Load dishes
            dishes = []
            dish_doc = await ref_collection.document("dishes").get()
            if dish_doc.exists:
                dish_data = dish_doc.to_dict()
                for item in dish_data.get("dishes", []):
                    dishes.append(DishInfo(
                        name=item.get("name", ""),
                        category=item.get("category", ""),
                        pairs_with=item.get("pairs_with", []),
                        meal_types=item.get("meal_types", []),
                        cuisines=item.get("cuisines", []),
                    ))

            # Load cuisines
            cuisines = []
            cuisine_doc = await ref_collection.document("cuisines").get()
            if cuisine_doc.exists:
                cuisine_data = cuisine_doc.to_dict()
                for item in cuisine_data.get("cuisines", []):
                    cuisines.append(CuisineInfo(
                        id=item.get("id", ""),
                        name=item.get("name", ""),
                        typical_pairings=item.get("typical_pairings", {}),
                        staple_ingredients=item.get("staple_ingredients", []),
                    ))

            self._reference_data = ReferenceData(
                ingredients=ingredients,
                dishes=dishes,
                cuisines=cuisines,
            )

            logger.info(
                f"Loaded reference data: "
                f"{len(ingredients)} ingredients, "
                f"{len(dishes)} dishes, "
                f"{len(cuisines)} cuisines"
            )

        except Exception as e:
            logger.error(f"Failed to load reference data: {e}")
            self._reference_data = ReferenceData()

    def get_pairing_categories(self, cuisine: str, category: str) -> list[str]:
        """Get pairing categories for a given cuisine and category.

        Args:
            cuisine: Cuisine type (north, south, east, west)
            category: Recipe category (dal, sabzi, rice, etc.)

        Returns:
            List of compatible categories for pairing
        """
        if self._config is None:
            return []

        key = f"{cuisine}:{category}"
        pairings = self._config.pairing_rules_flat.get(key, [])

        # Fall back to any cuisine pairing if specific one not found
        if not pairings:
            for pair_key, pair_values in self._config.pairing_rules_flat.items():
                if pair_key.endswith(f":{category}"):
                    return pair_values

        return pairings

    def get_meal_type_pairs(self, meal_type: str) -> list[tuple[str, str]]:
        """Get default pairing pairs for a meal type.

        Args:
            meal_type: Meal type (breakfast, lunch, dinner, snacks)

        Returns:
            List of tuples (primary_category, accompaniment_category)
        """
        if self._config is None:
            return []

        pairs_list = self._config.meal_type_pairs.get(meal_type, [])
        result = []
        for pair_str in pairs_list:
            if ":" in pair_str:
                parts = pair_str.split(":")
                result.append((parts[0], parts[1]))
        return result

    def get_ingredient_aliases(self, ingredient: str) -> list[str]:
        """Get all aliases for an ingredient.

        Args:
            ingredient: Ingredient name

        Returns:
            List of aliases including the original name
        """
        if self._config is None:
            return [ingredient.lower()]

        ingredient_lower = ingredient.lower()
        aliases = [ingredient_lower]

        # Check config aliases
        for key, alias_list in self._config.ingredient_aliases.items():
            if ingredient_lower == key or ingredient_lower in alias_list:
                aliases.extend(alias_list)
                if key not in aliases:
                    aliases.append(key)

        # Check reference data aliases
        if self._reference_data:
            for ing_info in self._reference_data.ingredients:
                if ingredient_lower == ing_info.name.lower() or ingredient_lower in [a.lower() for a in ing_info.aliases]:
                    aliases.extend([a.lower() for a in ing_info.aliases])
                    if ing_info.name.lower() not in aliases:
                        aliases.append(ing_info.name.lower())

        return list(set(aliases))
