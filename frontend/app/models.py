import enum
from flask import url_for
from datetime import datetime, timedelta
from sqlalchemy.sql import func, and_, or_
from sqlalchemy.ext.hybrid import hybrid_property

from app import db
import config

class ErrorLevel(enum.Enum):
    CRITICAL = 0,
    ERROR = 1,
    WARNING = 2

    def name(self):
        NAMES = {
            ErrorLevel.CRITICAL: "critical",
            ErrorLevel.ERROR: "error",
            ErrorLevel.WARNING: "warning",
        }
        return NAMES[self]


class ZenodoTarget(enum.Enum):
    SANDBOX = 0
    RELEASE = 1

    def endpoint(self, path):
        return {
            ZenodoTarget.RELEASE: "https://zenodo.org/api/",
            ZenodoTarget.SANDBOX: "https://sandbox.zenodo.org/api/"
        }[self] + path.lstrip("/")

    def headers(self):
        token = {
            ZenodoTarget.RELEASE: config.ZENODO_TOKEN,
            ZenodoTarget.SANDBOX: config.ZENODO_SANDBOX_TOKEN
        }[self]
        return {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + token
        }

class Dump(db.Model):
    id = db.Column(db.Integer, primary_key=True, nullable=False)
    title = db.Column(db.UnicodeText, nullable=False)
    description = db.Column(db.UnicodeText, nullable=False, server_default=db.text("''"))
    spec = db.Column(db.Text, nullable=False)
    created_at = db.Column(db.TIMESTAMP, index=True, nullable=False, server_default=func.now())
    run_id = db.Column(db.Integer, db.ForeignKey("run.id"), nullable=True)
    compressed_size = db.Column(db.Integer, server_default=db.text("0"), nullable=False)
    entity_count = db.Column(db.Integer, server_default=db.text("0"), nullable=False)
    statement_count = db.Column(db.Integer, nullable=False, server_default=db.text("0"))
    triple_count = db.Column(db.Integer, nullable=False, server_default=db.text("0"))

    run = db.relationship("Run")
    zenodo_sandbox = db.relationship("Zenodo", primaryjoin='and_(Dump.id == Zenodo.dump_id, Zenodo.target=="SANDBOX")', uselist=False)
    zenodo_release = db.relationship("Zenodo", primaryjoin='and_(Dump.id == Zenodo.dump_id, Zenodo.target=="RELEASE")', uselist=False)

    @hybrid_property
    def in_queue(self):
        return self.run == None

    @property
    def done(self):
        return self.run is not None and self.run.finished_at is not None

    @property
    def download_link(self):
        return url_for('download', id=self.id)

    def has_zenodo(self, target):
        return Zenodo.query.filter_by(dump_id=self.id, target=target).limit(1).count() > 0

    @property
    def has_errors(self):
        return bool(self.errors)

    @property
    def errors(self):
        return DumpError.query.filter(or_(
            and_(self.run_id != None, DumpError.run_id == self.run_id),
            DumpError.dump_id == self.id
        )).all()

class Zenodo(db.Model):
    id = db.Column(db.Integer, primary_key=True, nullable=False)
    deposit_id = db.Column(db.Integer, nullable=False)
    dump_id = db.Column(db.Integer, db.ForeignKey("dump.id"), nullable=False)
    doi = db.Column(db.Text, nullable=False)
    target = db.Column(db.Enum(ZenodoTarget), nullable=False)

    created_at = db.Column(db.TIMESTAMP, nullable=False, server_default=func.now())
    started_at = db.Column(db.TIMESTAMP, nullable=True)
    completed_at = db.Column(db.TIMESTAMP, nullable=True)

    uploaded_bytes = db.Column(db.BigInteger, nullable=False, server_default=db.text("0"))

class Run(db.Model):
    id = db.Column(db.Integer, primary_key = True, nullable=False)
    started_at = db.Column(db.TIMESTAMP, nullable=True)
    finished_at = db.Column(db.TIMESTAMP, nullable=True)
    count = db.Column(db.Integer, nullable=False, server_default=db.text("0"))
    tool_version = db.Column(db.Text, nullable=True)
    wdtk_version = db.Column(db.Text, nullable=True)
    dump_date = db.Column(db.Text, nullable=True)

    @property
    def estimated_factor(self):
        expected_count = Run.query.filter(
            Run.finished_at != None
        ).order_by(Run.finished_at.desc()).limit(1).one().count
        return self.count / expected_count

    @property
    def estimated_percentage(self):
        return int(self.estimated_factor * 100)

    @property
    def estimated_remaining(self):
        current_seconds = (datetime.utcnow() - self.started_at).total_seconds()
        if self.estimated_factor == 0:
            r = Run.query.filter(Run.finished_at != None).order_by(Run.finished_at.desc()).limit(1).one()
            return r.finished_at - r.started_at
        return timedelta(seconds=current_seconds / self.estimated_factor - current_seconds)


class DumpError(db.Model):
    id = db.Column(db.Integer, primary_key = True, nullable=False)
    logged_at = db.Column(db.TIMESTAMP, nullable=False, server_default=func.now())
    dump_id = db.Column(db.Integer, db.ForeignKey("dump.id"), nullable=True)
    run_id = db.Column(db.Integer, db.ForeignKey("run.id"), nullable=True)
    zenodo_id = db.Column(db.Integer, db.ForeignKey("zenodo.id"), nullable=True)
    level = db.Column(db.Enum(ErrorLevel), nullable=False)
    message = db.Column(db.Text, nullable=False)

    @property
    def category(self):
        if self.dump_id is None:
            return "run"
        if self.zenodo_id is not None:
            return "upload"
        return "dump"
