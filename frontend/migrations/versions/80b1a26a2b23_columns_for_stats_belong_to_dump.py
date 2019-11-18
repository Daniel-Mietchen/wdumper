"""columns for stats belong to dump

Revision ID: 80b1a26a2b23
Revises: 4cc940308e7b
Create Date: 2019-11-18 20:29:27.640883

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import mysql

# revision identifiers, used by Alembic.
revision = '80b1a26a2b23'
down_revision = '4cc940308e7b'
branch_labels = None
depends_on = None


def upgrade():
    # ### commands auto generated by Alembic - please adjust! ###
    op.add_column('dump', sa.Column('statement_count', sa.Integer(), server_default=sa.text('0'), nullable=False))
    op.add_column('dump', sa.Column('triple_count', sa.Integer(), server_default=sa.text('0'), nullable=False))
    op.drop_column('run', 'dump_statements')
    op.drop_column('run', 'dump_items')
    # ### end Alembic commands ###


def downgrade():
    # ### commands auto generated by Alembic - please adjust! ###
    op.add_column('run', sa.Column('dump_items', mysql.INTEGER(display_width=11), server_default=sa.text('0'), autoincrement=False, nullable=False))
    op.add_column('run', sa.Column('dump_statements', mysql.INTEGER(display_width=11), server_default=sa.text('0'), autoincrement=False, nullable=False))
    op.drop_column('dump', 'triple_count')
    op.drop_column('dump', 'statement_count')
    # ### end Alembic commands ###
