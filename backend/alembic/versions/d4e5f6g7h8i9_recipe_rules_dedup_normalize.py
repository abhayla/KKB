"""Recipe rules dedup, case normalization, and unique index

Revision ID: d4e5f6g7h8i9
Revises: c3d4e5f6g7h8
Create Date: 2026-02-06 14:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'd4e5f6g7h8i9'
down_revision: Union[str, None] = 'c3d4e5f6g7h8'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Step 1: Normalize all enum fields to UPPERCASE
    op.execute("""
        UPDATE recipe_rules SET
            action = UPPER(action),
            target_type = UPPER(target_type),
            frequency_type = UPPER(frequency_type),
            enforcement = UPPER(enforcement),
            meal_slot = UPPER(meal_slot)
        WHERE action != UPPER(action)
           OR target_type != UPPER(target_type)
           OR frequency_type != UPPER(frequency_type)
           OR enforcement != UPPER(enforcement)
           OR (meal_slot IS NOT NULL AND meal_slot != UPPER(meal_slot))
    """)

    # Step 2: Delete duplicate rules, keeping the oldest by created_at.
    # A duplicate is defined as same user_id + UPPER(target_name) + action + target_type + meal_slot.
    # Using a CTE with ROW_NUMBER to identify duplicates.
    op.execute("""
        DELETE FROM recipe_rules
        WHERE id IN (
            SELECT id FROM (
                SELECT id,
                    ROW_NUMBER() OVER (
                        PARTITION BY user_id, UPPER(target_name), action, target_type, COALESCE(meal_slot, 'ANY')
                        ORDER BY created_at ASC
                    ) AS rn
                FROM recipe_rules
            ) ranked
            WHERE rn > 1
        )
    """)

    # Step 3: Create unique index to prevent future duplicates
    op.create_index(
        'uq_recipe_rules_dedup',
        'recipe_rules',
        [
            'user_id',
            sa.text('UPPER(target_name)'),
            'action',
            'target_type',
            sa.text("COALESCE(meal_slot, 'ANY')"),
        ],
        unique=True,
    )


def downgrade() -> None:
    op.drop_index('uq_recipe_rules_dedup', table_name='recipe_rules')
