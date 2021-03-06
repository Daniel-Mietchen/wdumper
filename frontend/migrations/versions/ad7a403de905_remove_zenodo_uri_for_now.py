"""remove zenodo URI for now

Revision ID: ad7a403de905
Revises: dcef24d5ae1b
Create Date: 2019-07-21 16:16:52.717439

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import mysql

# revision identifiers, used by Alembic.
revision = 'ad7a403de905'
down_revision = 'dcef24d5ae1b'
branch_labels = None
depends_on = None


def upgrade():
    # ### commands auto generated by Alembic - please adjust! ###
    op.drop_column('dump', 'zenodo_uri')
    # ### end Alembic commands ###


def downgrade():
    # ### commands auto generated by Alembic - please adjust! ###
    op.add_column('dump', sa.Column('zenodo_uri', mysql.TEXT(collation='utf8mb4_unicode_ci'), nullable=True))
    # ### end Alembic commands ###
