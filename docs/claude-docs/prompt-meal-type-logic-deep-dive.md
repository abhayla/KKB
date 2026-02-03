# Prompt: Deep Dive into Meal Type Logic

Use this prompt in a new Claude Code session to understand how meal types work across the entire system.

---

## Prompt

I want to understand in detail how meal types work in the RasoiAI codebase. Please explore and explain:

### 1. Recipe Import Flow
- How are recipes imported from Firebase (khanakyabanega) to PostgreSQL?
- How does `MEAL_TYPE_MAPPING` in `backend/scripts/import_recipes_from_kkb.py` transform source meal types?
- What happens to recipes tagged as "beverage", "dessert", "appetizer"?

### 2. Recipe Search & Filtering
- How does `backend/app/services/recipe_service.py` filter recipes by meal type?
- How does `Recipe.meal_types.contains([meal_type])` work in PostgreSQL?
- What's the difference between `meal_types` array and `course_type` field?

### 3. Add Recipe Sheet Flow (Android)
- Trace the flow from `HomeViewModel.showAddRecipeSheet(mealType)` to the API call
- How does `RecipeRepositoryImpl.searchRecipes()` pass the meal type filter?
- What happens when online vs offline?

### 4. Meal Generation Config
- How does `backend/config/reference_data/dishes.yaml` define meal types for dishes?
- How does `backend/config/meal_generation.yaml` use meal types for pairing?
- Why is config `meal_types` different from database `meal_types`?

### 5. Show Me Examples
For each meal type (BREAKFAST, LUNCH, DINNER, SNACKS):
- What categories of food should appear?
- Show sample SQL queries to verify what's in the database
- Explain any edge cases (beverages for breakfast, desserts for snacks, etc.)

### Key Files to Explore
- `backend/scripts/import_recipes_from_kkb.py` - MEAL_TYPE_MAPPING (line ~83-95)
- `backend/app/services/recipe_service.py` - search_recipes() (line ~171-219)
- `backend/app/models/recipe.py` - Recipe model with meal_types field
- `backend/config/reference_data/dishes.yaml` - Dish definitions with meal_types
- `backend/config/meal_generation.yaml` - Meal structure and pairing rules
- `android/data/src/main/java/com/rasoiai/data/repository/RecipeRepositoryImpl.kt` - Android search
- `android/app/src/main/java/com/rasoiai/app/presentation/home/HomeViewModel.kt` - fetchAddRecipeSuggestions()

Please provide a comprehensive explanation with code snippets and diagrams where helpful.

---

*Created for future reference - save this prompt to continue the meal type discussion later.*
