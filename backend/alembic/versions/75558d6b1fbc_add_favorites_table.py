"""add_favorites_table

Revision ID: 75558d6b1fbc
Revises: bb17ac1e73db
Create Date: 2026-03-21 18:20:09.748782

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '75558d6b1fbc'
down_revision: Union[str, None] = 'bb17ac1e73db'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Create favorites table
    op.create_table('favorites',
        sa.Column('id', sa.String(length=36), nullable=False),
        sa.Column('user_id', sa.String(length=36), nullable=False),
        sa.Column('recipe_id', sa.String(length=36), nullable=False),
        sa.Column('created_at', sa.DateTime(), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['user_id'], ['users.id']),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('user_id', 'recipe_id', name='uq_user_recipe_favorite')
    )
    op.create_index('ix_favorites_user_id', 'favorites', ['user_id'], unique=False)

    # Add preferences_updated_at for conflict detection
    op.add_column('users', sa.Column('preferences_updated_at', sa.DateTime(timezone=True), nullable=True))


def downgrade() -> None:
    op.drop_column('users', 'preferences_updated_at')
    op.drop_index('ix_favorites_user_id', table_name='favorites')
    op.drop_table('favorites')
