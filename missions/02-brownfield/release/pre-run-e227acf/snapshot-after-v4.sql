CALL CSVWRITE('/private/tmp/urlshort-m02-rollback-e227acf/after-v4/link.csv', 'SELECT id,code,url,created_at,retired_at,idempotency_key FROM link ORDER BY id');
CALL CSVWRITE('/private/tmp/urlshort-m02-rollback-e227acf/after-v4/audit_log.csv', 'SELECT id,occurred_at,actor,action,entity,entity_id,request_id,before_state,after_state FROM audit_log ORDER BY id');
CALL CSVWRITE('/private/tmp/urlshort-m02-rollback-e227acf/after-v4/click.csv', 'SELECT id,link_id,clicked_at,clicked_on,referrer,user_agent_class,client_hash FROM click ORDER BY id');
CALL CSVWRITE('/private/tmp/urlshort-m02-rollback-e227acf/after-v4/user_agent_class.csv', 'SELECT token FROM user_agent_class ORDER BY token');
CALL CSVWRITE('/private/tmp/urlshort-m02-rollback-e227acf/after-v4/columns.csv', 'SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,IS_NULLABLE,COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=''PUBLIC'' ORDER BY TABLE_NAME,ORDINAL_POSITION');
CALL CSVWRITE('/private/tmp/urlshort-m02-rollback-e227acf/after-v4/constraints.csv', 'SELECT TABLE_NAME,CONSTRAINT_NAME,CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA=''PUBLIC'' ORDER BY TABLE_NAME,CONSTRAINT_NAME');
CALL CSVWRITE('/private/tmp/urlshort-m02-rollback-e227acf/after-v4/flyway.csv', 'SELECT "version","description","success" FROM "flyway_schema_history" ORDER BY "installed_rank"');
