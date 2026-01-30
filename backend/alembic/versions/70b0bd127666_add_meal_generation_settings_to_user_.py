"""Add meal generation settings to user preferences

Revision ID: 70b0bd127666
Revises: 40d591ebbdfb
Create Date: 2026-01-30 14:58:42.450644

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '70b0bd127666'
down_revision: Union[str, None] = '40d591ebbdfb'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Add meal generation settings columns with defaults for existing rows
    op.add_column('user_preferences', sa.Column('items_per_meal', sa.Integer(), nullable=False, server_default='2'))
    op.add_column('user_preferences', sa.Column('strict_allergen_mode', sa.Boolean(), nullable=False, server_default='true'))
    op.add_column('user_preferences', sa.Column('strict_dietary_mode', sa.Boolean(), nullable=False, server_default='true'))
    op.add_column('user_preferences', sa.Column('allow_recipe_repeat', sa.Boolean(), nullable=False, server_default='false'))


def downgrade() -> None:
    op.drop_column('user_preferences', 'allow_recipe_repeat')
    op.drop_column('user_preferences', 'strict_dietary_mode')
    op.drop_column('user_preferences', 'strict_allergen_mode')
    op.drop_column('user_preferences', 'items_per_meal')
