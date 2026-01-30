"""initial_schema_postgresql_migration

Revision ID: 40d591ebbdfb
Revises:
Create Date: 2026-01-30 07:56:40.002229

This migration creates all tables for the RasoiAI PostgreSQL database,
migrating from Firestore.

Tables created:
- users (with preferences and family_members)
- recipes (with ingredients, instructions, nutrition)
- meal_plans (with items)
- grocery_lists (with items)
- festivals
- chat_messages
- cooking_streaks (with cooking_days)
- achievements (with user_achievements)
- system_config
- reference_data
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '40d591ebbdfb'
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # === USERS ===
    op.create_table(
        'users',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('firebase_uid', sa.String(128), unique=True, nullable=False, index=True),
        sa.Column('email', sa.String(255), nullable=True),
        sa.Column('name', sa.String(255), nullable=True),
        sa.Column('profile_picture_url', sa.Text(), nullable=True),
        sa.Column('is_onboarded', sa.Boolean(), default=False, nullable=False),
        sa.Column('is_active', sa.Boolean(), default=True, nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )

    # User preferences
    op.create_table(
        'user_preferences',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='CASCADE'), unique=True, nullable=False),
        sa.Column('dietary_type', sa.String(50), nullable=True),
        sa.Column('dietary_tags', sa.Text(), nullable=True),  # JSON array
        sa.Column('allergies', sa.Text(), nullable=True),  # JSON array
        sa.Column('disliked_ingredients', sa.Text(), nullable=True),  # JSON array
        sa.Column('cuisine_preferences', sa.Text(), nullable=True),  # JSON array
        sa.Column('cooking_time_preference', sa.String(50), nullable=True),
        sa.Column('spice_level', sa.String(20), nullable=True),
        sa.Column('weekday_cooking_time_minutes', sa.Integer(), nullable=True, default=30),
        sa.Column('weekend_cooking_time_minutes', sa.Integer(), nullable=True, default=60),
        sa.Column('busy_days', sa.Text(), nullable=True),  # JSON array
        sa.Column('recipe_rules', sa.Text(), nullable=True),  # JSON object
        sa.Column('last_change', sa.Text(), nullable=True),  # JSON object for undo
        sa.Column('family_size', sa.Integer(), default=2, nullable=False),
        sa.Column('cooking_skill_level', sa.String(20), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )

    # Family members
    op.create_table(
        'family_members',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('name', sa.String(100), nullable=False),
        sa.Column('age_group', sa.String(20), nullable=True),
        sa.Column('dietary_restrictions', sa.Text(), nullable=True),  # JSON array
        sa.Column('health_conditions', sa.Text(), nullable=True),  # JSON array
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index('ix_family_members_user_id', 'family_members', ['user_id'])

    # === RECIPES ===
    op.create_table(
        'recipes',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('name', sa.String(255), nullable=False, index=True),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('image_url', sa.Text(), nullable=True),
        sa.Column('cuisine_type', sa.String(20), nullable=False, index=True),
        sa.Column('meal_types', sa.Text(), nullable=False),  # JSON array
        sa.Column('dietary_tags', sa.Text(), nullable=False),  # JSON array
        sa.Column('course_type', sa.String(50), nullable=True),
        sa.Column('category', sa.String(50), nullable=True, index=True),  # dal, sabzi, rice, etc.
        sa.Column('prep_time_minutes', sa.Integer(), nullable=False, default=15),
        sa.Column('cook_time_minutes', sa.Integer(), nullable=False, default=30),
        sa.Column('total_time_minutes', sa.Integer(), nullable=False, default=45),
        sa.Column('servings', sa.Integer(), nullable=False, default=4),
        sa.Column('difficulty_level', sa.String(20), nullable=True),
        sa.Column('is_festive', sa.Boolean(), default=False, nullable=False),
        sa.Column('is_fasting_friendly', sa.Boolean(), default=False, nullable=False),
        sa.Column('is_quick_meal', sa.Boolean(), default=False, nullable=False),
        sa.Column('is_kid_friendly', sa.Boolean(), default=False, nullable=False),
        sa.Column('is_active', sa.Boolean(), default=True, nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index('ix_recipes_is_active', 'recipes', ['is_active'])

    # Recipe ingredients
    op.create_table(
        'recipe_ingredients',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('recipe_id', sa.String(36), sa.ForeignKey('recipes.id', ondelete='CASCADE'), nullable=False),
        sa.Column('name', sa.String(100), nullable=False),
        sa.Column('quantity', sa.Float(), nullable=False),
        sa.Column('unit', sa.String(30), nullable=False),
        sa.Column('category', sa.String(30), nullable=False),
        sa.Column('notes', sa.String(255), nullable=True),
        sa.Column('is_optional', sa.Boolean(), default=False, nullable=False),
        sa.Column('order', sa.Integer(), default=0, nullable=False),
    )
    op.create_index('ix_recipe_ingredients_recipe_id', 'recipe_ingredients', ['recipe_id'])

    # Recipe instructions
    op.create_table(
        'recipe_instructions',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('recipe_id', sa.String(36), sa.ForeignKey('recipes.id', ondelete='CASCADE'), nullable=False),
        sa.Column('step_number', sa.Integer(), nullable=False),
        sa.Column('instruction', sa.Text(), nullable=False),
        sa.Column('duration_minutes', sa.Integer(), nullable=True),
        sa.Column('timer_required', sa.Boolean(), default=False, nullable=False),
        sa.Column('tips', sa.Text(), nullable=True),
    )
    op.create_index('ix_recipe_instructions_recipe_id', 'recipe_instructions', ['recipe_id'])

    # Recipe nutrition
    op.create_table(
        'recipe_nutrition',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('recipe_id', sa.String(36), sa.ForeignKey('recipes.id', ondelete='CASCADE'), unique=True, nullable=False),
        sa.Column('calories', sa.Integer(), nullable=False),
        sa.Column('protein_grams', sa.Float(), nullable=False),
        sa.Column('carbohydrates_grams', sa.Float(), nullable=False),
        sa.Column('fat_grams', sa.Float(), nullable=False),
        sa.Column('fiber_grams', sa.Float(), nullable=False),
        sa.Column('sugar_grams', sa.Float(), nullable=True),
        sa.Column('sodium_mg', sa.Float(), nullable=True),
    )

    # === MEAL PLANS ===
    op.create_table(
        'meal_plans',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False, index=True),
        sa.Column('week_start_date', sa.Date(), nullable=False, index=True),
        sa.Column('week_end_date', sa.Date(), nullable=False),
        sa.Column('is_active', sa.Boolean(), default=True, nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )

    # Meal plan items
    op.create_table(
        'meal_plan_items',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('meal_plan_id', sa.String(36), sa.ForeignKey('meal_plans.id', ondelete='CASCADE'), nullable=False),
        sa.Column('recipe_id', sa.String(36), sa.ForeignKey('recipes.id', ondelete='SET NULL'), nullable=True),
        sa.Column('date', sa.Date(), nullable=False, index=True),
        sa.Column('meal_type', sa.String(20), nullable=False),
        sa.Column('servings', sa.Integer(), nullable=False, default=2),
        sa.Column('is_locked', sa.Boolean(), default=False, nullable=False),
        sa.Column('is_swapped', sa.Boolean(), default=False, nullable=False),
        sa.Column('festival_name', sa.String(100), nullable=True),
        sa.Column('recipe_name', sa.String(255), nullable=True),  # Cache for display
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index('ix_meal_plan_items_meal_plan_id', 'meal_plan_items', ['meal_plan_id'])

    # === GROCERY LISTS ===
    op.create_table(
        'grocery_lists',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False, index=True),
        sa.Column('meal_plan_id', sa.String(36), sa.ForeignKey('meal_plans.id', ondelete='SET NULL'), nullable=True),
        sa.Column('name', sa.String(100), nullable=False),
        sa.Column('is_active', sa.Boolean(), default=True, nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )

    # Grocery items
    op.create_table(
        'grocery_items',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('grocery_list_id', sa.String(36), sa.ForeignKey('grocery_lists.id', ondelete='CASCADE'), nullable=False),
        sa.Column('name', sa.String(100), nullable=False),
        sa.Column('quantity', sa.Float(), nullable=False),
        sa.Column('unit', sa.String(30), nullable=False),
        sa.Column('category', sa.String(30), nullable=False, index=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('is_checked', sa.Boolean(), default=False, nullable=False),
        sa.Column('is_in_pantry', sa.Boolean(), default=False, nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index('ix_grocery_items_grocery_list_id', 'grocery_items', ['grocery_list_id'])

    # === FESTIVALS ===
    op.create_table(
        'festivals',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('name', sa.String(100), nullable=False),
        sa.Column('name_hindi', sa.String(100), nullable=True),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('date', sa.Date(), nullable=True),
        sa.Column('year', sa.Integer(), nullable=False),
        sa.Column('regions', sa.Text(), nullable=False),  # JSON array
        sa.Column('is_fasting_day', sa.Boolean(), default=False, nullable=False),
        sa.Column('fasting_type', sa.String(50), nullable=True),
        sa.Column('special_foods', sa.Text(), nullable=True),  # JSON array
        sa.Column('avoided_foods', sa.Text(), nullable=True),  # JSON array
        sa.Column('is_active', sa.Boolean(), default=True, nullable=False),
    )
    op.create_index('ix_festivals_date', 'festivals', ['date'])
    op.create_index('ix_festivals_year', 'festivals', ['year'])

    # === CHAT MESSAGES ===
    op.create_table(
        'chat_messages',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False, index=True),
        sa.Column('role', sa.String(20), nullable=False),
        sa.Column('content', sa.Text(), nullable=False),
        sa.Column('message_type', sa.String(20), nullable=False, default='text'),
        sa.Column('tool_calls', sa.Text(), nullable=True),  # JSON array
        sa.Column('tool_results', sa.Text(), nullable=True),  # JSON array
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )

    # === COOKING STREAKS (Gamification) ===
    op.create_table(
        'cooking_streaks',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='CASCADE'), unique=True, nullable=False),
        sa.Column('current_streak', sa.Integer(), default=0, nullable=False),
        sa.Column('longest_streak', sa.Integer(), default=0, nullable=False),
        sa.Column('total_meals_cooked', sa.Integer(), default=0, nullable=False),
        sa.Column('last_cooking_date', sa.Date(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )

    # Cooking days
    op.create_table(
        'cooking_days',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('cooking_streak_id', sa.String(36), sa.ForeignKey('cooking_streaks.id', ondelete='CASCADE'), nullable=False),
        sa.Column('date', sa.Date(), nullable=False, index=True),
        sa.Column('meals_cooked', sa.Integer(), default=0, nullable=False),
        sa.Column('breakfast_cooked', sa.Boolean(), default=False, nullable=False),
        sa.Column('lunch_cooked', sa.Boolean(), default=False, nullable=False),
        sa.Column('dinner_cooked', sa.Boolean(), default=False, nullable=False),
    )
    op.create_index('ix_cooking_days_cooking_streak_id', 'cooking_days', ['cooking_streak_id'])

    # === ACHIEVEMENTS ===
    op.create_table(
        'achievements',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('name', sa.String(100), nullable=False, unique=True),
        sa.Column('description', sa.Text(), nullable=False),
        sa.Column('icon', sa.String(50), nullable=False),
        sa.Column('category', sa.String(50), nullable=False),
        sa.Column('requirement_type', sa.String(50), nullable=False),
        sa.Column('requirement_value', sa.Integer(), nullable=False),
        sa.Column('is_active', sa.Boolean(), default=True, nullable=False),
    )

    # User achievements (many-to-many)
    op.create_table(
        'user_achievements',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('achievement_id', sa.String(36), sa.ForeignKey('achievements.id', ondelete='CASCADE'), nullable=False),
        sa.Column('unlocked_at', sa.Date(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index('ix_user_achievements_user_id', 'user_achievements', ['user_id'])
    op.create_unique_constraint('uq_user_achievement', 'user_achievements', ['user_id', 'achievement_id'])

    # === SYSTEM CONFIG ===
    op.create_table(
        'system_config',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('key', sa.String(100), unique=True, nullable=False, index=True),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('config_data', sa.Text(), nullable=False),  # JSON
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )

    # === REFERENCE DATA ===
    op.create_table(
        'reference_data',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('category', sa.String(100), unique=True, nullable=False, index=True),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('data', sa.Text(), nullable=False),  # JSON
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )


def downgrade() -> None:
    # Drop tables in reverse order of creation (respect foreign keys)
    op.drop_table('reference_data')
    op.drop_table('system_config')
    op.drop_table('user_achievements')
    op.drop_table('achievements')
    op.drop_table('cooking_days')
    op.drop_table('cooking_streaks')
    op.drop_table('chat_messages')
    op.drop_table('festivals')
    op.drop_table('grocery_items')
    op.drop_table('grocery_lists')
    op.drop_table('meal_plan_items')
    op.drop_table('meal_plans')
    op.drop_table('recipe_nutrition')
    op.drop_table('recipe_instructions')
    op.drop_table('recipe_ingredients')
    op.drop_table('recipes')
    op.drop_table('family_members')
    op.drop_table('user_preferences')
    op.drop_table('users')
