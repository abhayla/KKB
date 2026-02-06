"""Add ai_recipe_catalog table

Revision ID: c3d4e5f6g7h8
Revises: b2c3d4e5f6g7
Create Date: 2026-02-06 10:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'c3d4e5f6g7h8'
down_revision: Union[str, None] = 'b2c3d4e5f6g7'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        'ai_recipe_catalog',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('display_name', sa.String(255), nullable=False),
        sa.Column('normalized_name', sa.String(255), nullable=False),
        sa.Column('dietary_tags', sa.Text(), nullable=True),
        sa.Column('cuisine_type', sa.String(20), nullable=True),
        sa.Column('meal_types', sa.Text(), nullable=True),
        sa.Column('category', sa.String(50), nullable=True),
        sa.Column('prep_time_minutes', sa.Integer(), nullable=True),
        sa.Column('calories', sa.Integer(), nullable=True),
        sa.Column('ingredients', sa.Text(), nullable=True),
        sa.Column('nutrition', sa.Text(), nullable=True),
        sa.Column('usage_count', sa.Integer(), nullable=False, server_default='1'),
        sa.Column('first_generated_by', sa.String(36),
                   sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    )

    op.create_index('ix_ai_recipe_catalog_normalized_name', 'ai_recipe_catalog',
                     ['normalized_name'], unique=True)
    op.create_index('ix_ai_recipe_catalog_usage_count', 'ai_recipe_catalog',
                     ['usage_count'])
    op.create_index('ix_ai_recipe_catalog_cuisine_type', 'ai_recipe_catalog',
                     ['cuisine_type'])


def downgrade() -> None:
    op.drop_index('ix_ai_recipe_catalog_cuisine_type', table_name='ai_recipe_catalog')
    op.drop_index('ix_ai_recipe_catalog_usage_count', table_name='ai_recipe_catalog')
    op.drop_index('ix_ai_recipe_catalog_normalized_name', table_name='ai_recipe_catalog')
    op.drop_table('ai_recipe_catalog')
