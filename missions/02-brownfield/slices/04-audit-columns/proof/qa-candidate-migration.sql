-- V4: audit columns on link and audit_log (04-audit-columns; the human's audit-column policy, ADR-0020 and
-- its 04-audit-columns amendment). Expand only: nothing dropped, renamed or retyped.
-- link: created_at already exists (the shipped creation time on the service clock) and is the policy's
-- created_at. updated_at is set by the application on the service clock at each of the three link writes
-- (create, retire, key release); its default only fills rows inserted without it (v1-shaped inserts).
-- created_by/updated_by: 'anonymous', the actor of every request (NFR-S6).
-- audit_log: created_at/updated_at say when the row was written, by the database clock; occurred_at keeps
-- the event's time on the service clock. created_by/updated_by equal the row's actor ('anonymous' today,
-- so the default equals it; a writer with another actor must set them).
-- Backfill:
--   a link's updated_at is its latest known write: retired_at when retired, otherwise created_at;
--   an audit row's created_at = updated_at = its occurred_at, never later than this migration, and its
--   created_by = updated_by = its actor.
-- rollback (removes only what V4 added, then forget V4):
--   ALTER TABLE link DROP COLUMN updated_by; ALTER TABLE link DROP COLUMN created_by;
--   ALTER TABLE link DROP COLUMN updated_at;
--   ALTER TABLE audit_log DROP COLUMN updated_by; ALTER TABLE audit_log DROP COLUMN created_by;
--   ALTER TABLE audit_log DROP COLUMN updated_at; ALTER TABLE audit_log DROP COLUMN created_at;
--   DELETE FROM "flyway_schema_history" WHERE "version" = '4';

ALTER TABLE link ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE link ADD COLUMN created_by VARCHAR(64) DEFAULT 'anonymous' NOT NULL;
ALTER TABLE link ADD COLUMN updated_by VARCHAR(64) DEFAULT 'anonymous' NOT NULL;
UPDATE link SET updated_at = COALESCE(retired_at, created_at);

ALTER TABLE audit_log ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE audit_log ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE audit_log ADD COLUMN created_by VARCHAR(64) DEFAULT 'anonymous' NOT NULL;
ALTER TABLE audit_log ADD COLUMN updated_by VARCHAR(64) DEFAULT 'anonymous' NOT NULL;
UPDATE audit_log SET created_at = LEAST(occurred_at, CURRENT_TIMESTAMP), updated_at = LEAST(occurred_at, CURRENT_TIMESTAMP),
    created_by = actor, updated_by = actor;
