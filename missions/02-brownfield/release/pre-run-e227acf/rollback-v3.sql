ALTER TABLE click DROP COLUMN updated_by;
ALTER TABLE click DROP COLUMN created_by;
ALTER TABLE click DROP COLUMN updated_at;
ALTER TABLE click DROP COLUMN created_at;
ALTER TABLE user_agent_class DROP COLUMN updated_by;
ALTER TABLE user_agent_class DROP COLUMN created_by;
ALTER TABLE user_agent_class DROP COLUMN updated_at;
ALTER TABLE user_agent_class DROP COLUMN created_at;
DELETE FROM "flyway_schema_history" WHERE "version" = '3';
