-- V3: audit columns on click and user_agent_class (02-click-retention AC-16; the human's audit-column
-- policy, SPEC A-10). Expand only: four columns per table, nothing dropped, renamed or retyped.
-- created_at/updated_at: when the row was written, from the database clock (docs/guidance/databases.md
-- section 4); clicked_at keeps the click's time on the service clock. created_by/updated_by: the actor,
-- 'anonymous' for clicks (the unauthenticated Visitor) and 'system' for the seeded classes (SPEC A-11).
-- The defaults fill every new row, so the v1 insert (ClickStore.insert) is unchanged.
-- Backfill: a pre-existing click gets created_at = updated_at = clicked_at, the closest known write
-- time (the writer inserts within milliseconds of it); a pre-existing class gets this migration's time.
-- rollback (removes only what V3 added, then forget V3):
--   ALTER TABLE click DROP COLUMN updated_by; ALTER TABLE click DROP COLUMN created_by;
--   ALTER TABLE click DROP COLUMN updated_at; ALTER TABLE click DROP COLUMN created_at;
--   ALTER TABLE user_agent_class DROP COLUMN updated_by; ALTER TABLE user_agent_class DROP COLUMN created_by;
--   ALTER TABLE user_agent_class DROP COLUMN updated_at; ALTER TABLE user_agent_class DROP COLUMN created_at;
--   DELETE FROM "flyway_schema_history" WHERE "version" = '3';

ALTER TABLE user_agent_class ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE user_agent_class ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE user_agent_class ADD COLUMN created_by VARCHAR(16) DEFAULT 'system' NOT NULL;
ALTER TABLE user_agent_class ADD COLUMN updated_by VARCHAR(16) DEFAULT 'system' NOT NULL;
UPDATE user_agent_class SET updated_at = created_at;

ALTER TABLE click ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE click ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE click ADD COLUMN created_by VARCHAR(16) DEFAULT 'anonymous' NOT NULL;
ALTER TABLE click ADD COLUMN updated_by VARCHAR(16) DEFAULT 'anonymous' NOT NULL;
UPDATE click SET created_at = clicked_at, updated_at = clicked_at;
