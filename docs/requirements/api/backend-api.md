# Backend API Requirements

## Summary Table

| ID | Endpoint | Method | Purpose | Status | Test Reference |
|----|----------|--------|---------|--------|----------------|
| API-001 | `/api/v1/auth/firebase` | POST | Exchange Firebase token for JWT | Implemented | `test_auth.py` |
| API-002 | `/api/v1/auth/refresh` | POST | Refresh access token | Implemented | `test_auth.py` |
| API-003 | `/api/v1/users/me` | GET | Get current user profile | Implemented | `test_auth.py` |
| API-004 | `/api/v1/users/preferences` | PUT | Update user preferences | Implemented | `test_preference_service.py` |
| API-005 | `/api/v1/meal-plans/generate` | POST | Generate AI meal plan | Implemented | `test_meal_generation_e2e.py` |
| API-006 | `/api/v1/meal-plans/current` | GET | Get current week's plan | Implemented | `test_meal_generation_e2e.py` |
| API-007 | `/api/v1/meal-plans/{plan_id}` | GET | Get meal plan by ID | Implemented | `test_meal_generation_e2e.py` |
| API-008 | `/api/v1/meal-plans/{plan_id}/items/{item_id}/swap` | POST | Swap meal item | Implemented | `test_meal_generation_e2e.py` |
| API-009 | `/api/v1/meal-plans/{plan_id}/items/{item_id}/lock` | PUT | Toggle item lock | Implemented | `test_meal_generation_e2e.py` |
| API-010 | `/api/v1/meal-plans/{plan_id}/items/{item_id}` | DELETE | Remove meal item | Implemented | `test_meal_generation_e2e.py` |
| API-011 | `/api/v1/recipes/search` | GET | Search recipes | Implemented | `test_recipe_cache.py` |
| API-012 | `/api/v1/recipes/{recipe_id}` | GET | Get recipe details | Implemented | `test_recipe_cache.py` |
| API-013 | `/api/v1/recipes/{recipe_id}/scale` | GET | Scale recipe servings | Implemented | `test_recipe_cache.py` |
| API-014 | `/api/v1/grocery` | GET | Get grocery list | Implemented | Various |
| API-015 | `/api/v1/grocery/whatsapp` | GET | Get WhatsApp format | Implemented | Various |
| API-016 | `/api/v1/festivals/upcoming` | GET | Get upcoming festivals | Implemented | Various |
| API-017 | `/api/v1/chat/message` | POST | Send chat message | Implemented | `test_chat_api.py` |
| API-018 | `/api/v1/chat/history` | GET | Get chat history | Implemented | `test_chat_api.py` |
| API-019 | `/api/v1/chat/image` | POST | Analyze food image | Implemented | `test_chat_api.py` |
| API-020 | `/api/v1/stats/streak` | GET | Get cooking streak | Implemented | Various |
| API-021 | `/api/v1/stats/monthly` | GET | Get monthly stats | Implemented | Various |
| API-022 | `/api/v1/notifications` | GET | Get notifications | Implemented | `test_notification_api.py` |
| API-023 | `/api/v1/notifications/read-all` | PUT | Mark all read | Implemented | `test_notification_api.py` |
| API-024 | `/api/v1/notifications/fcm-token` | POST | Register FCM token | Implemented | `test_notification_api.py` |
| API-025 | `/api/v1/notifications/fcm-token` | DELETE | Unregister FCM token | Implemented | `test_notification_api.py` |
| API-026 | `/api/v1/notifications/{id}/read` | PUT | Mark notification read | Implemented | `test_notification_api.py` |
| API-027 | `/api/v1/notifications/{id}` | DELETE | Delete notification | Implemented | `test_notification_api.py` |
| API-028 | `/api/v1/auth/logout` | POST | Invalidate refresh token | Implemented | `test_auth.py` |
| API-029 | `/api/v1/family-members` | GET | List family members | Implemented | `test_family_members_api.py` |
| API-030 | `/api/v1/family-members` | POST | Add family member | Implemented | `test_family_members_api.py` |
| API-031 | `/api/v1/family-members/{id}` | PUT | Update family member | Implemented | `test_family_members_api.py` |
| API-032 | `/api/v1/family-members/{id}` | DELETE | Delete family member | Implemented | `test_family_members_api.py` |
| API-033 | `/api/v1/recipe-rules` | GET | List recipe rules | Implemented | `test_recipe_rules_api.py` |
| API-034 | `/api/v1/recipe-rules` | POST | Create recipe rule | Implemented | `test_recipe_rules_api.py` |
| API-034a | `/api/v1/recipe-rules` | POST | 409 Conflict: family safety conflict with ConflictDetail | Implemented | `test_recipe_rule_family_conflict.py` |
| API-035 | `/api/v1/recipe-rules/{id}` | GET | Get recipe rule | Implemented | `test_recipe_rules_api.py` |
| API-036 | `/api/v1/recipe-rules/{id}` | PUT | Update recipe rule | Implemented | `test_recipe_rules_api.py` |
| API-037 | `/api/v1/recipe-rules/{id}` | DELETE | Delete recipe rule | Implemented | `test_recipe_rules_api.py` |
| API-038 | `/api/v1/recipe-rules/sync` | POST | Sync rules from Android | Implemented | `test_recipe_rules_api.py` |
| API-039 | `/api/v1/nutrition-goals` | GET | List nutrition goals | Implemented | `test_recipe_rules_api.py` |
| API-040 | `/api/v1/nutrition-goals` | POST | Create nutrition goal | Implemented | `test_recipe_rules_api.py` |
| API-041 | `/api/v1/nutrition-goals/{id}` | GET | Get nutrition goal | Implemented | `test_recipe_rules_api.py` |
| API-042 | `/api/v1/nutrition-goals/{id}` | PUT | Update nutrition goal | Implemented | `test_recipe_rules_api.py` |
| API-043 | `/api/v1/nutrition-goals/{id}` | DELETE | Delete nutrition goal | Implemented | `test_recipe_rules_api.py` |
| API-044 | `/api/v1/photos/analyze` | POST | Analyze food photo | Implemented | `test_chat_api.py` |
| API-045 | `/api/v1/recipes/{id}/rate` | POST | Rate a recipe | Implemented | Various |
| API-046 | `/api/v1/recipes/ai-catalog/search` | GET | Search AI recipe catalog | Implemented | `test_ai_recipe_catalog.py` |
| API-047 | `/api/v1/recipes/suggest-from-pantry` | POST | Suggest recipes from pantry | Implemented | Various |
| API-048 | `/api/v1/users/me` | DELETE | Delete account (GDPR) | Implemented | `test_auth.py` |
| API-049 | `/api/v1/users/me/export` | GET | Export user data (GDPR) | Implemented | `test_auth.py` |

---

## Authentication Router

### API-001: Firebase Authentication

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /api/v1/auth/firebase` |
| **Purpose** | Exchange Firebase ID token for JWT access token |
| **Auth Required** | No |
| **Status** | Implemented |
| **Test** | `test_auth.py:test_firebase_auth` |

**Request:**
```json
{
  "firebase_token": "string"
}
```

**Response:**
```json
{
  "access_token": "string",
  "refresh_token": "string",
  "token_type": "bearer",
  "expires_in": 604800,
  "user": {
    "id": "string",
    "email": "string",
    "name": "string",
    "is_onboarded": false
  }
}
```

**Acceptance Criteria:**
- Given: Valid Firebase ID token
- When: POST to /auth/firebase
- Then: Returns JWT access token and refresh token
- And: Creates user if first login
- And: Returns user profile data

**Notes:**
- Accepts `fake-firebase-token` in debug mode for testing
- Access token expires in 7 days (10080 minutes) by default

---

### API-002: Refresh Token

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /api/v1/auth/refresh` |
| **Purpose** | Get new access token using refresh token |
| **Auth Required** | No (uses refresh token) |
| **Status** | Implemented |
| **Test** | `test_auth.py:test_refresh_token` |

**Request:**
```json
{
  "refresh_token": "string"
}
```

**Response:**
```json
{
  "access_token": "string",
  "token_type": "bearer",
  "expires_in": 604800
}
```

**Acceptance Criteria:**
- Given: Valid refresh token
- When: POST to /auth/refresh
- Then: Returns new access token
- And: Original refresh token remains valid

---

## Users Router

### API-003: Get Current User

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/users/me` |
| **Purpose** | Get authenticated user's profile and preferences |
| **Auth Required** | Yes (Bearer token) |
| **Status** | Implemented |
| **Test** | `test_auth.py:test_get_current_user` |

**Response:**
```json
{
  "id": "string",
  "email": "string",
  "name": "string",
  "photo_url": "string",
  "is_onboarded": true,
  "preferences": {
    "dietary_restrictions": ["vegetarian"],
    "allergies": ["peanuts"],
    "dislikes": ["bitter_gourd"],
    "cuisine_preferences": ["north", "south"],
    "spice_level": "medium",
    "cooking_time_weekday": 30,
    "cooking_time_weekend": 60,
    "family_size": 4,
    "items_per_meal": 2
  }
}
```

**Acceptance Criteria:**
- Given: Valid JWT in Authorization header
- When: GET /users/me
- Then: Returns user profile with preferences
- And: Returns 401 if token invalid/expired

---

### API-004: Update Preferences

| Field | Value |
|-------|-------|
| **Endpoint** | `PUT /api/v1/users/preferences` |
| **Purpose** | Update user's meal planning preferences |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_preference_service.py` |

**Request:**
```json
{
  "dietary_restrictions": ["vegetarian"],
  "allergies": ["peanuts", "dairy"],
  "dislikes": ["bitter_gourd"],
  "cuisine_preferences": ["north", "south"],
  "spice_level": "medium",
  "cooking_time_weekday": 30,
  "cooking_time_weekend": 60,
  "family_size": 4,
  "items_per_meal": 2,
  "family_members": [
    {
      "name": "Priya",
      "age": 35,
      "dietary_restriction": "vegetarian"
    }
  ]
}
```

**Acceptance Criteria:**
- Given: Authenticated user
- When: PUT /users/preferences with updates
- Then: Preferences saved to database
- And: User marked as onboarded if first update
- And: Returns updated user profile

---

## Meal Plans Router

### API-005: Generate Meal Plan

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /api/v1/meal-plans/generate` |
| **Purpose** | Generate AI-powered 7-day meal plan |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_meal_generation_e2e.py` |

**Request:**
```json
{
  "week_start_date": "2025-01-20"
}
```

**Response:**
```json
{
  "id": "string",
  "week_start_date": "2025-01-20",
  "week_end_date": "2025-01-26",
  "days": [
    {
      "date": "2025-01-20",
      "day_name": "Monday",
      "meals": {
        "breakfast": [
          {
            "id": "string",
            "recipe_id": "string",
            "recipe_name": "Poha",
            "recipe_image_url": "string",
            "prep_time_minutes": 20,
            "calories": 250,
            "is_locked": false,
            "dietary_tags": ["vegetarian"]
          },
          {
            "id": "string",
            "recipe_id": "string",
            "recipe_name": "Chai",
            "prep_time_minutes": 10,
            "calories": 80,
            "is_locked": false,
            "dietary_tags": ["vegetarian"]
          }
        ],
        "lunch": [...],
        "dinner": [...],
        "snacks": [...]
      },
      "festival": null
    }
  ],
  "created_at": "2025-01-20T10:00:00Z",
  "updated_at": "2025-01-20T10:00:00Z"
}
```

**Acceptance Criteria:**
- Given: Authenticated user with preferences
- When: POST /meal-plans/generate
- Then: AI generates personalized 7-day plan
- And: Each meal slot has 2+ complementary items (default)
- And: INCLUDE rules are satisfied at required frequency
- And: EXCLUDE rules, allergies, dislikes are respected
- And: Cooking time limits enforced
- And: Festival days have appropriate dishes

**AI Integration:**
- Uses MealGenerationService with config-driven pairing
- Respects items_per_meal preference (1-4 items)
- Typical generation time: 45-90 seconds (Gemini AI + DB writes)

---

### API-006: Get Current Meal Plan

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/meal-plans/current` |
| **Purpose** | Get current week's active meal plan |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_meal_generation_e2e.py` |

**Acceptance Criteria:**
- Given: User has active meal plan
- When: GET /meal-plans/current
- Then: Returns current week's plan
- And: Returns 404 if no plan exists

---

### API-007: Get Meal Plan by ID

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/meal-plans/{plan_id}` |
| **Purpose** | Get specific meal plan by ID |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_meal_generation_e2e.py` |

**Acceptance Criteria:**
- Given: Plan ID exists and belongs to user
- When: GET /meal-plans/{plan_id}
- Then: Returns meal plan data
- And: Returns 404 if plan doesn't exist or belongs to another user

---

### API-008: Swap Meal Item

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /api/v1/meal-plans/{plan_id}/items/{item_id}/swap` |
| **Purpose** | Replace a meal item with alternative recipe |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_meal_generation_e2e.py` |

**Request:**
```json
{
  "specific_recipe_id": "string (optional)",
  "exclude_recipe_ids": ["string"]
}
```

**Acceptance Criteria:**
- Given: Unlocked meal item
- When: POST /swap with or without specific recipe
- Then: Item replaced with alternative
- And: Returns 404 if item is locked
- And: New recipe respects user preferences

---

### API-009: Toggle Item Lock

| Field | Value |
|-------|-------|
| **Endpoint** | `PUT /api/v1/meal-plans/{plan_id}/items/{item_id}/lock` |
| **Purpose** | Toggle lock status of meal item |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_meal_generation_e2e.py` |

**Acceptance Criteria:**
- Given: Meal item exists
- When: PUT /lock
- Then: Lock status toggled
- And: Locked items preserved during regeneration

---

### API-010: Remove Meal Item

| Field | Value |
|-------|-------|
| **Endpoint** | `DELETE /api/v1/meal-plans/{plan_id}/items/{item_id}` |
| **Purpose** | Remove a meal item from plan |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_meal_generation_e2e.py` |

**Acceptance Criteria:**
- Given: Unlocked meal item
- When: DELETE /items/{item_id}
- Then: Item removed from meal slot
- And: Returns 404 if item is locked

---

## Recipes Router

### API-011: Search Recipes

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/recipes/search` |
| **Purpose** | Search recipes with filters |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_recipe_cache.py` |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| q | string | Text search in name/description |
| cuisine | string | Filter: north, south, east, west |
| dietary | string | Filter: vegetarian, vegan, jain, etc. |
| mealType | string | Filter: breakfast, lunch, dinner, snacks |
| page | int | Page number (default: 1) |
| limit | int | Items per page (1-100, default: 20) |

**Acceptance Criteria:**
- Given: Search parameters
- When: GET /recipes/search
- Then: Returns matching recipes
- And: Supports pagination
- And: Filters are combinable

---

### API-012: Get Recipe Details

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/recipes/{recipe_id}` |
| **Purpose** | Get full recipe details |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_recipe_cache.py` |

**Response:**
```json
{
  "id": "string",
  "name": "Palak Paneer",
  "description": "string",
  "cuisine_type": "north",
  "dietary_tags": ["vegetarian"],
  "prep_time_minutes": 30,
  "cook_time_minutes": 20,
  "total_time_minutes": 50,
  "servings": 4,
  "difficulty": "medium",
  "image_url": "string",
  "ingredients": [
    {
      "name": "Spinach",
      "quantity": 500,
      "unit": "g",
      "notes": "fresh"
    }
  ],
  "instructions": [
    {
      "step_number": 1,
      "instruction": "Blanch spinach in boiling water",
      "duration_minutes": 5
    }
  ],
  "nutrition": {
    "calories": 300,
    "protein": 15,
    "carbs": 20,
    "fat": 18
  },
  "tips": ["Add kasuri methi for extra flavor"]
}
```

**Acceptance Criteria:**
- Given: Valid recipe ID
- When: GET /recipes/{recipe_id}
- Then: Returns full recipe with ingredients, instructions, nutrition
- And: Returns 404 if recipe doesn't exist

---

### API-013: Scale Recipe

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/recipes/{recipe_id}/scale` |
| **Purpose** | Scale recipe to target servings |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_recipe_cache.py` |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| servings | int | Target number of servings |

**Acceptance Criteria:**
- Given: Recipe ID and target servings
- When: GET /recipes/{id}/scale?servings=6
- Then: Returns recipe with adjusted quantities
- And: Ingredient amounts scaled proportionally
- And: Nutrition values adjusted

---

## Grocery Router

### API-014: Get Grocery List

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/grocery` |
| **Purpose** | Get aggregated grocery list for meal plan |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | Various |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| mealPlanId | string | Optional, defaults to current week |

**Response:**
```json
{
  "categories": [
    {
      "name": "Vegetables",
      "items": [
        {
          "id": "string",
          "name": "Spinach",
          "quantity": 1.5,
          "unit": "kg",
          "is_checked": false,
          "recipes_using": ["Palak Paneer", "Saag"]
        }
      ]
    }
  ],
  "total_items": 25,
  "checked_items": 0
}
```

**Acceptance Criteria:**
- Given: Active meal plan
- When: GET /grocery
- Then: Returns aggregated ingredients by category
- And: Quantities combined across meals
- And: Shows which recipes use each ingredient

---

### API-015: Get WhatsApp Format

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/grocery/whatsapp` |
| **Purpose** | Get grocery list formatted for WhatsApp |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | Various |

**Response:**
```text
🛒 *Weekly Grocery List*

🥬 *Vegetables*
- Spinach: 1.5 kg
- Tomatoes: 2 kg
- Onions: 1.5 kg

🥛 *Dairy*
- Paneer: 500 g
- Curd: 1 L

...
```

**Acceptance Criteria:**
- Given: Meal plan ID
- When: GET /grocery/whatsapp
- Then: Returns emoji-formatted text
- And: Suitable for sharing to kirana stores

---

## Festivals Router

### API-016: Get Upcoming Festivals

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/festivals/upcoming` |
| **Purpose** | Get upcoming Indian festivals |
| **Auth Required** | No |
| **Status** | Implemented |
| **Test** | Various |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| days | int | Days to look ahead (1-365, default: 30) |

**Response:**
```json
[
  {
    "id": "string",
    "name": "Makar Sankranti",
    "date": "2025-01-14",
    "is_fasting_day": false,
    "suggested_dishes": ["Til Ladoo", "Gur Chikki"],
    "description": "Harvest festival"
  }
]
```

**Acceptance Criteria:**
- Given: Days parameter
- When: GET /festivals/upcoming
- Then: Returns festivals within date range
- And: Includes fasting day indicator
- And: Suggests traditional dishes

---

## Chat Router

### API-017: Send Chat Message

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /api/v1/chat/message` |
| **Purpose** | Send message to AI cooking assistant |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_chat_api.py`, `test_chat_integration.py` |

**Request:**
```json
{
  "message": "I want chai every morning"
}
```

**Response:**
```json
{
  "message": {
    "id": "string",
    "role": "assistant",
    "content": "I've added Chai to your breakfast every day...",
    "message_type": "text",
    "created_at": "2025-01-20T10:00:00Z",
    "recipe_suggestions": null
  },
  "has_recipe_suggestions": false,
  "recipe_ids": []
}
```

**Tool Calling Capabilities:**
| Intent | Tool | Action |
|--------|------|--------|
| "I want chai every morning" | `update_recipe_rule` | ADD INCLUDE rule |
| "I don't eat mushrooms" | `update_recipe_rule` | ADD EXCLUDE rule |
| "I'm allergic to peanuts" | `update_allergy` | ADD allergy |
| "I don't like karela" | `update_dislike` | ADD dislike |
| "Show my settings" | `get_preferences` | Display config |
| "Undo that" | `undo_preference` | Revert change |

**Acceptance Criteria:**
- Given: User message
- When: POST /chat/message
- Then: AI responds with relevant answer
- And: Executes tool calls for preference updates
- And: Returns recipe suggestions when relevant

---

### API-018: Get Chat History

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/chat/history` |
| **Purpose** | Get conversation history |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_chat_api.py` |

**Response:**
```json
{
  "messages": [
    {
      "id": "string",
      "role": "user",
      "content": "How do I make dal tadka?",
      "message_type": "text",
      "created_at": "2025-01-20T10:00:00Z"
    },
    {
      "id": "string",
      "role": "assistant",
      "content": "Here's how to make dal tadka...",
      "message_type": "text",
      "created_at": "2025-01-20T10:00:05Z"
    }
  ],
  "total_count": 2
}
```

**Acceptance Criteria:**
- Given: User has chat history
- When: GET /chat/history
- Then: Returns text messages only
- And: Tool use/result messages filtered out
- And: Sorted by creation date

---

### API-019: Analyze Food Image

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /api/v1/chat/image` |
| **Purpose** | Analyze food photo using Gemini Vision |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_chat_api.py` |

**Request:**
```json
{
  "image_base64": "string (base64 encoded)",
  "media_type": "image/jpeg",
  "message": "What dish is this?"
}
```

**Response:**
```json
{
  "message": {
    "id": "string",
    "role": "assistant",
    "content": "This looks like Butter Chicken...",
    "message_type": "image_analysis",
    "created_at": "2025-01-20T10:00:00Z",
    "recipe_suggestions": ["butter-chicken-123"]
  },
  "has_recipe_suggestions": true,
  "recipe_ids": ["butter-chicken-123"]
}
```

**Acceptance Criteria:**
- Given: Base64 encoded food image
- When: POST /chat/image
- Then: Gemini Vision analyzes image
- And: Returns dish identification
- And: Suggests matching recipes from database

**Notes:**
- Max image size: 1MB after compression
- Supported formats: JPEG, PNG
- Uses Google Gemini Vision API

---

## Stats Router

### API-020: Get Cooking Streak

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/stats/streak` |
| **Purpose** | Get cooking streak statistics |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | Various |

**Response:**
```json
{
  "current_streak": 12,
  "longest_streak": 23,
  "weekly_progress": [true, true, true, false, true, true, true],
  "last_cooked_date": "2025-01-20"
}
```

**Acceptance Criteria:**
- Given: User has cooking history
- When: GET /stats/streak
- Then: Returns current and longest streaks
- And: Weekly progress array for current week

---

### API-021: Get Monthly Stats

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/stats/monthly` |
| **Purpose** | Get monthly cooking statistics |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | Various |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| month | string | Month in yyyy-MM format |

**Response:**
```json
{
  "month": "2025-01",
  "meals_cooked": 45,
  "new_recipes_tried": 12,
  "average_rating": 4.2,
  "cuisine_breakdown": {
    "north": 20,
    "south": 15,
    "east": 5,
    "west": 5
  },
  "achievements_earned": ["7-day-streak", "50-meals"]
}
```

**Acceptance Criteria:**
- Given: Month parameter in yyyy-MM format
- When: GET /stats/monthly
- Then: Returns aggregated stats for month
- And: Includes cuisine breakdown
- And: Lists achievements earned that month

---

## Notifications Router

### API-022: Get Notifications

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /api/v1/notifications` |
| **Purpose** | Get user's notifications |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_notification_api.py` |

**Response:**
```json
{
  "notifications": [
    {
      "id": "string",
      "type": "festival",
      "title": "Makar Sankranti in 3 days!",
      "body": "Plan your festive meals now",
      "is_read": false,
      "action_url": "/festivals/makar-sankranti",
      "created_at": "2025-01-11T10:00:00Z"
    }
  ],
  "unread_count": 3,
  "total_count": 10
}
```

**Acceptance Criteria:**
- Given: Authenticated user
- When: GET /notifications
- Then: Returns notifications sorted by date
- And: Excludes expired notifications
- And: Includes unread count

---

### API-023: Mark All as Read

| Field | Value |
|-------|-------|
| **Endpoint** | `PUT /api/v1/notifications/read-all` |
| **Purpose** | Mark all notifications as read |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_notification_api.py` |

**Response:**
```json
{
  "success": true,
  "message": "Marked 5 notifications as read"
}
```

**Acceptance Criteria:**
- Given: User has unread notifications
- When: PUT /notifications/read-all
- Then: All notifications marked as read
- And: Returns count of affected notifications

---

### API-024: Register FCM Token

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /api/v1/notifications/fcm-token` |
| **Purpose** | Register device for push notifications |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_notification_api.py` |

**Request:**
```json
{
  "fcm_token": "string",
  "device_type": "android"
}
```

**Acceptance Criteria:**
- Given: FCM token from Firebase
- When: POST /notifications/fcm-token
- Then: Token registered for user
- And: Can receive push notifications

---

### API-025: Unregister FCM Token

| Field | Value |
|-------|-------|
| **Endpoint** | `DELETE /api/v1/notifications/fcm-token` |
| **Purpose** | Unregister device from push notifications |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_notification_api.py` |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| fcm_token | string | Token to unregister |

**Acceptance Criteria:**
- Given: Registered FCM token
- When: DELETE /notifications/fcm-token
- Then: Token deactivated
- And: Device stops receiving pushes

---

### API-026: Mark Notification as Read

| Field | Value |
|-------|-------|
| **Endpoint** | `PUT /api/v1/notifications/{notification_id}/read` |
| **Purpose** | Mark single notification as read |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_notification_api.py` |

**Acceptance Criteria:**
- Given: Notification ID
- When: PUT /notifications/{id}/read
- Then: Notification marked as read
- And: Returns 404 if not found or wrong user

---

### API-027: Delete Notification

| Field | Value |
|-------|-------|
| **Endpoint** | `DELETE /api/v1/notifications/{notification_id}` |
| **Purpose** | Delete a notification |
| **Auth Required** | Yes |
| **Status** | Implemented |
| **Test** | `test_notification_api.py` |

**Acceptance Criteria:**
- Given: Notification ID
- When: DELETE /notifications/{id}
- Then: Notification removed
- And: Returns 404 if not found or wrong user

---

## Cross-Cutting Concerns

### Authentication

All endpoints except `/auth/*` and `/festivals/upcoming` require JWT authentication:

```http
Authorization: Bearer <access_token>
```

**Token Expiration:**
- Access token: 10080 minutes (7 days)
- Refresh token: 30 days (opaque, with reuse detection)

**Test Mode:**
- Backend accepts `fake-firebase-token` when `DEBUG=true`

### Error Responses

All endpoints return consistent error format:

```json
{
  "detail": "Error message",
  "status_code": 400
}
```

**Common Status Codes:**
| Code | Meaning |
|------|---------|
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Invalid/expired token |
| 404 | Not Found - Resource doesn't exist |
| 500 | Internal Error - Server failure |

### Rate Limiting

| Endpoint Type | Limit |
|---------------|-------|
| AI Generation | 5/hour |
| Chat Messages | 30/minute |
| Chat Image | 10/hour |
| Auth Firebase | 10/minute |
| Auth Refresh | 20/minute |
| Photo Analyze | 10/hour |

---

## Implementation Files

| Router | File Path |
|--------|-----------|
| Auth | `backend/app/api/v1/endpoints/auth.py` |
| Users | `backend/app/api/v1/endpoints/users.py` |
| Meal Plans | `backend/app/api/v1/endpoints/meal_plans.py` |
| Recipes | `backend/app/api/v1/endpoints/recipes.py` |
| Grocery | `backend/app/api/v1/endpoints/grocery.py` |
| Festivals | `backend/app/api/v1/endpoints/festivals.py` |
| Chat | `backend/app/api/v1/endpoints/chat.py` |
| Stats | `backend/app/api/v1/endpoints/stats.py` |
| Notifications | `backend/app/api/v1/endpoints/notifications.py` |
| Recipe Rules | `backend/app/api/v1/endpoints/recipe_rules.py` |
| Nutrition Goals | `backend/app/api/v1/endpoints/recipe_rules.py` (separate router) |
| Family Members | `backend/app/api/v1/endpoints/family_members.py` |
| Photos | `backend/app/api/v1/endpoints/photos.py` |

## Test Files

| Test Category | File Path | Tests |
|---------------|-----------|-------|
| Auth Tests | `backend/tests/test_auth.py` | 3 |
| Preference Tests | `backend/tests/test_preference_service.py` | 26 |
| Chat Integration | `backend/tests/test_chat_integration.py` | 27 |
| Chat API | `backend/tests/test_chat_api.py` | 12 |
| Meal Generation | `backend/tests/test_meal_generation.py` | 22 |
| Meal Gen Integration | `backend/tests/test_meal_generation_integration.py` | 29 |
| Meal Gen E2E | `backend/tests/test_meal_generation_e2e.py` | 14 |
| Recipe Cache | `backend/tests/test_recipe_cache.py` | 35 |
| Notification Service | `backend/tests/test_notification_service.py` | 19 |
| Notification API | `backend/tests/test_notification_api.py` | 10 |

**Total Backend Tests: ~539 (43 files)**

---

*Requirements derived from: Backend API exploration and endpoint source code analysis*
