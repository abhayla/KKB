"""Add recipe_rules and nutrition_goals tables

Revision ID: b2c3d4e5f6g7
Revises: a1b2c3d4e5f6
Create Date: 2026-02-05 10:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'b2c3d4e5f6g7'
down_revision: Union[str, None] = 'a1b2c3d4e5f6'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Create recipe_rules table
    op.create_table(
        'recipe_rules',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('target_type', sa.String(20), nullable=False),  # RECIPE, INGREDIENT, MEAL_SLOT
        sa.Column('action', sa.String(10), nullable=False),  # INCLUDE, EXCLUDE
        sa.Column('target_id', sa.String(255), nullable=True),  # Recipe ID (optional)
        sa.Column('target_name', sa.String(255), nullable=False),  # Display name
        sa.Column('frequency_type', sa.String(20), nullable=False),  # DAILY, TIMES_PER_WEEK, SPECIFIC_DAYS, NEVER
        sa.Column('frequency_count', sa.Integer(), nullable=True),  # For TIMES_PER_WEEK (1-7)
        sa.Column('frequency_days', sa.String(100), nullable=True),  # Comma-separated: "MONDAY,WEDNESDAY"
        sa.Column('enforcement', sa.String(10), nullable=False, server_default='REQUIRED'),
        sa.Column('meal_slot', sa.String(20), nullable=True),  # BREAKFAST, LUNCH, DINNER, SNACKS
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('sync_status', sa.String(20), nullable=False, server_default='SYNCED'),  # SYNCED, PENDING, CONFLICT
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index('ix_recipe_rules_user_id', 'recipe_rules', ['user_id'])
    op.create_index('ix_recipe_rules_updated_at', 'recipe_rules', ['updated_at'])
    op.create_index('ix_recipe_rules_target_type', 'recipe_rules', ['target_type'])
    op.create_index('ix_recipe_rules_action', 'recipe_rules', ['action'])

    # Create nutrition_goals table
    op.create_table(
        'nutrition_goals',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('food_category', sa.String(30), nullable=False),
        sa.Column('weekly_target', sa.Integer(), nullable=False, server_default='3'),
        sa.Column('current_progress', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('enforcement', sa.String(10), nullable=False, server_default='PREFERRED'),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('sync_status', sa.String(20), nullable=False, server_default='SYNCED'),  # SYNCED, PENDING, CONFLICT
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index('ix_nutrition_goals_user_id', 'nutrition_goals', ['user_id'])
    op.create_index('ix_nutrition_goals_food_category', 'nutrition_goals', ['food_category'])
    # Add unique constraint for user_id + food_category combination
    op.create_unique_constraint('uq_nutrition_goals_user_category', 'nutrition_goals', ['user_id', 'food_category'])


def downgrade() -> None:
    # Drop nutrition_goals table
    op.drop_constraint('uq_nutrition_goals_user_category', 'nutrition_goals', type_='unique')
    op.drop_index('ix_nutrition_goals_food_category', table_name='nutrition_goals')
    op.drop_index('ix_nutrition_goals_user_id', table_name='nutrition_goals')
    op.drop_table('nutrition_goals')

    # Drop recipe_rules table
    op.drop_index('ix_recipe_rules_action', table_name='recipe_rules')
    op.drop_index('ix_recipe_rules_target_type', table_name='recipe_rules')
    op.drop_index('ix_recipe_rules_updated_at', table_name='recipe_rules')
    op.drop_index('ix_recipe_rules_user_id', table_name='recipe_rules')
    op.drop_table('recipe_rules')
