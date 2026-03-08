"""API v1 router aggregator."""

from fastapi import APIRouter

from app.api.v1.endpoints import (
    auth,
    chat,
    family_members,
    festivals,
    grocery,
    households,
    meal_plans,
    notifications,
    photos,
    recipe_rules,
    recipes,
    stats,
    users,
)

api_router = APIRouter()

# Include all endpoint routers
api_router.include_router(auth.router)
api_router.include_router(users.router)
api_router.include_router(meal_plans.router)
api_router.include_router(recipes.router)
api_router.include_router(grocery.router)
api_router.include_router(festivals.router)
api_router.include_router(chat.router)
api_router.include_router(stats.router)
api_router.include_router(notifications.router)
api_router.include_router(recipe_rules.router)
api_router.include_router(recipe_rules.nutrition_goals_router)
api_router.include_router(family_members.router)
api_router.include_router(photos.router)
api_router.include_router(households.router)
