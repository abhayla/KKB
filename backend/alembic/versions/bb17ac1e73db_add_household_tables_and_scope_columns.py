"""add household tables and scope columns

Revision ID: bb17ac1e73db
Revises: h8i9j0k1l2m3
Create Date: 2026-03-08 13:04:10.454037

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'bb17ac1e73db'
down_revision: Union[str, None] = 'h8i9j0k1l2m3'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # --- Layer 1: New tables ---
    op.create_table('households',
        sa.Column('id', sa.String(length=36), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False),
        sa.Column('invite_code', sa.String(length=8), nullable=True),
        sa.Column('invite_code_expires_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('owner_id', sa.String(length=36), nullable=False),
        sa.Column('slot_config', sa.Text(), nullable=True),
        sa.Column('max_members', sa.Integer(), nullable=False, server_default='6'),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.ForeignKeyConstraint(['owner_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
    )
    op.create_index(
        op.f('ix_households_invite_code'), 'households', ['invite_code'],
        unique=True, postgresql_where=sa.text('is_active = true'),
    )

    op.create_table('household_members',
        sa.Column('id', sa.String(length=36), nullable=False),
        sa.Column('household_id', sa.String(length=36), nullable=False),
        sa.Column('user_id', sa.String(length=36), nullable=True),
        sa.Column('family_member_id', sa.String(length=36), nullable=True),
        sa.Column('role', sa.String(length=20), nullable=False),
        sa.Column('can_edit_shared_plan', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('is_temporary', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('join_date', sa.Date(), nullable=True),
        sa.Column('leave_date', sa.Date(), nullable=True),
        sa.Column('previous_household_id', sa.String(length=36), nullable=True),
        sa.Column('portion_size', sa.String(length=20), nullable=False, server_default='REGULAR'),
        sa.Column('active_meal_slots', sa.Text(), nullable=True),
        sa.Column('status', sa.String(length=20), nullable=False, server_default='ACTIVE'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.ForeignKeyConstraint(['family_member_id'], ['family_members.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['household_id'], ['households.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id'),
    )

    # --- Layer 3: Scope extension columns on existing tables ---

    # users: household membership links
    op.add_column('users', sa.Column('active_household_id', sa.String(length=36), nullable=True))
    op.add_column('users', sa.Column('passive_household_id', sa.String(length=36), nullable=True))
    op.create_foreign_key(
        'fk_users_active_household', 'users', 'households',
        ['active_household_id'], ['id'], ondelete='SET NULL',
    )
    op.create_foreign_key(
        'fk_users_passive_household', 'users', 'households',
        ['passive_household_id'], ['id'], ondelete='SET NULL',
    )

    # recipe_rules: household scoping
    op.add_column('recipe_rules', sa.Column('household_id', sa.String(length=36), nullable=True))
    op.add_column('recipe_rules', sa.Column('scope', sa.String(length=20), nullable=False, server_default='PERSONAL'))
    op.create_foreign_key(
        'fk_recipe_rules_household', 'recipe_rules', 'households',
        ['household_id'], ['id'], ondelete='CASCADE',
    )

    # meal_plans: household scoping
    op.add_column('meal_plans', sa.Column('household_id', sa.String(length=36), nullable=True))
    op.add_column('meal_plans', sa.Column('slot_scope', sa.String(length=20), nullable=False, server_default='ALL'))
    op.create_index(op.f('ix_meal_plans_household_id'), 'meal_plans', ['household_id'], unique=False)
    op.create_foreign_key(
        'fk_meal_plans_household', 'meal_plans', 'households',
        ['household_id'], ['id'], ondelete='SET NULL',
    )

    # meal_plan_items: per-item scoping
    op.add_column('meal_plan_items', sa.Column('scope', sa.String(length=20), nullable=False, server_default='FAMILY'))
    op.add_column('meal_plan_items', sa.Column('for_user_id', sa.String(length=36), nullable=True))
    op.add_column('meal_plan_items', sa.Column('meal_status', sa.String(length=20), nullable=False, server_default='PLANNED'))
    op.create_foreign_key(
        'fk_meal_plan_items_for_user', 'meal_plan_items', 'users',
        ['for_user_id'], ['id'], ondelete='SET NULL',
    )

    # notifications: household scoping
    op.add_column('notifications', sa.Column('household_id', sa.String(length=36), nullable=True))
    op.add_column('notifications', sa.Column('metadata_json', sa.Text(), nullable=True))
    op.create_index(op.f('ix_notifications_household_id'), 'notifications', ['household_id'], unique=False)
    op.create_foreign_key(
        'fk_notifications_household', 'notifications', 'households',
        ['household_id'], ['id'], ondelete='SET NULL',
    )


def downgrade() -> None:
    # notifications
    op.drop_constraint('fk_notifications_household', 'notifications', type_='foreignkey')
    op.drop_index(op.f('ix_notifications_household_id'), table_name='notifications')
    op.drop_column('notifications', 'metadata_json')
    op.drop_column('notifications', 'household_id')

    # meal_plan_items
    op.drop_constraint('fk_meal_plan_items_for_user', 'meal_plan_items', type_='foreignkey')
    op.drop_column('meal_plan_items', 'meal_status')
    op.drop_column('meal_plan_items', 'for_user_id')
    op.drop_column('meal_plan_items', 'scope')

    # meal_plans
    op.drop_constraint('fk_meal_plans_household', 'meal_plans', type_='foreignkey')
    op.drop_index(op.f('ix_meal_plans_household_id'), table_name='meal_plans')
    op.drop_column('meal_plans', 'slot_scope')
    op.drop_column('meal_plans', 'household_id')

    # recipe_rules
    op.drop_constraint('fk_recipe_rules_household', 'recipe_rules', type_='foreignkey')
    op.drop_column('recipe_rules', 'scope')
    op.drop_column('recipe_rules', 'household_id')

    # users
    op.drop_constraint('fk_users_passive_household', 'users', type_='foreignkey')
    op.drop_constraint('fk_users_active_household', 'users', type_='foreignkey')
    op.drop_column('users', 'passive_household_id')
    op.drop_column('users', 'active_household_id')

    # household_members
    op.drop_table('household_members')

    # households
    op.drop_index(op.f('ix_households_invite_code'), table_name='households')
    op.drop_table('households')
