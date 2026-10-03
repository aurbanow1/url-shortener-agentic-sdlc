# Independent QA commands — 862c52e

Worktree: .worktrees/02-analytics, clean at 862c52eea8294e438b1f98b832ae4f64f7a16923 throughout.

Gate (from the worktree):

```sh
../../scripts/gw --log /Users/andrzej/Documents/projekty/test/openrig/url-shortener/missions/01-greenfield-core/slices/02-analytics/proof/qa-check-862c52e.txt --offline check --rerun-tasks
```

Live runs used the same candidate, explicit loopback bind, and a separate H2 file:

```sh
../../scripts/gw --log /Users/andrzej/Documents/projekty/test/openrig/url-shortener/missions/01-greenfield-core/slices/02-analytics/proof/qa-bootrun-862c52e.txt --offline bootRun --args='--server.port=18092 --server.address=127.0.0.1 --spring.datasource.url=jdbc:h2:file:./build/qa-h2/urlshort;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE'
```

Second run used qa-journey-bootrun-862c52e.txt as its log path. The first invocation with escaped argument syntax hit the sandbox's Gradle lock-socket restriction; the quoted-argument retry ran with the existing build authorization outside the sandbox. Product/toolchain files were not changed.

HTTP exchanges used scripts/http --silent --show-error --max-time 12 --include, method/header/body flags, and http://127.0.0.1:18092 plus each recorded path. qa-http-862c52e.json holds every submitted header/body and returned header/body with before/after instants. Twenty tool clients sent ten GETs each concurrently without following redirects.

Between runs, after stopping the app, H2 Shell on the disposable database installed this fault:

```sql
ALTER TABLE click ADD CONSTRAINT qa_fail_one_click
CHECK (link_id <> (SELECT id FROM link WHERE code = 'RqYAOyTJ'));
```

The second run's GET /RqYAOyTJ still returned 302, original Location, no-store and request id 1186692b-a9a2-4725-8b6c-8c9421283eb4. The click writer logged exactly one safe WARN for that id; stats remained 0. No application code or spy was used for this live fault.

Exports used org.h2.tools.Shell from the cached 2.4.240 jar, URL jdbc:h2:file:./.worktrees/02-analytics/build/qa-h2/urlshort;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE, user sa and empty password, with CSVWRITE over SELECT * FROM audit_log ORDER BY id and SELECT * FROM link ORDER BY id before/after. Click export: SELECT l.code, c.* FROM click c JOIN link l ON l.id=c.link_id ORDER BY c.id.

Cleanup after the second app stopped:

```sql
ALTER TABLE click DROP CONSTRAINT qa_fail_one_click;
SELECT COUNT(*) AS qa_constraints_left
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE CONSTRAINT_NAME = 'QA_FAIL_ONE_CLICK';
```

Returned 0. Both bootRun process groups were stopped via Ctrl-C (intentional exit 130). A subsequent scripts/http --silent --show-error --max-time 2 http://127.0.0.1:18092/actuator/health returned curl exit 7, connection refused. Worktree HEAD/status checked again, no product changes.
