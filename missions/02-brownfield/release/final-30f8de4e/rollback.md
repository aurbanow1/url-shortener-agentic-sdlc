# Rollback from the final mission02 candidate

Candidate: 30f8de4e647b05ff54cde09f1019ae519b00069d. Choose the smallest applicable rollback.
These are Operator commands; release preparation did not change main or the user database.

## Client-identity rollback only

D21's merge is b8d7fc165ad079eca1b321eb44015264f18493ec. It has no schema/dependency change.
The preserved pre-run image is urlshort:mission02-prerun-e227acf,
sha256:277703a6510ec2924fef9b423c0f48714c081fd2fe9da76461f54d702afd4b3b.
Its jar is /private/tmp/urlshort-mission02-e227acf.jar,
SHA256 fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2.
It retains V3/V4 and analytics v2. Stop the current process/container and take a stopped-data
backup first. Redeploy that preserved artifact against the unchanged schema and run the smoke.

For a source rollback, the integrator uses a throwaway branch first:

~~~sh
git worktree add -b rollback/mission02-client-identity .worktrees/rollback-02-refactor 30f8de4e
git -C .worktrees/rollback-02-refactor revert -m 1 --no-edit b8d7fc165ad079eca1b321eb44015264f18493ec
scripts/gw --offline -p .worktrees/rollback-02-refactor check bootJar
scripts/smoke.sh --jar .worktrees/rollback-02-refactor/build/libs/urlshort.jar 18243 /private/tmp/rollback-02-refactor.jsonl
~~~

No D21-specific source revert was executed in this release preparation. The earlier independent
integrator revert drill is recorded in ../../../../docs/scenarios/drills.md, DRILL2.

## Return to mission01 service and schema

Executable target: f09010396d584fdf41702bb99856aa17a1ec1206. The prepared jar is
/private/tmp/urlshort-mission02-rollback-f090103/build/libs/urlshort.jar;
its exact identity is in rollback-artifact.json. The local image is
urlshort:rollback-mission01-f090103, digest
sha256:2d4e63d1eea1418114875cbf614e4689e46d1f878daf29c9324d2e0f7583e611.
This returns the service to mission01, including its statistics v1 and absence of the later
audit reader/retention job. Link/click/audit domain rows remain; V3/V4 audit metadata is lost.
Previously purged rows cannot be recreated without an earlier stopped-data backup.

1. Stop every writer (docker compose stop urlshort, or SIGTERM the recorded jar PID). Wait for
   graceful shutdown and port refusal. Save the entire stopped H2 directory or named Compose
   volume to a new backup location. Never copy a live H2 file.
2. Build the prior service from its exact commit using the current pinned wrapper. Do not reuse
   a path containing uncommitted changes:

~~~sh
git worktree add --detach .worktrees/rollback-02-prior f09010396d584fdf41702bb99856aa17a1ec1206
scripts/gw --offline -p .worktrees/rollback-02-prior bootJar
docker build -f missions/02-brownfield/release/final-30f8de4e/rollback-runtime.Dockerfile -t urlshort:rollback-mission01-f090103 .worktrees/rollback-02-prior/build/libs
~~~

3. Run the literal V4 then V3 rollback SQL against a disposable stopped-data copy first.
   For the rehearsed copy, the exact commands were:

~~~sh
java -cp /private/tmp/urlshort-m02-h2.jar org.h2.tools.RunScript -url 'jdbc:h2:file:/private/tmp/urlshort-mission02-rollback-prior-data/urlshort;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE' -user sa -script missions/02-brownfield/release/pre-run-e227acf/rollback-v4.sql
java -cp /private/tmp/urlshort-m02-h2.jar org.h2.tools.RunScript -url 'jdbc:h2:file:/private/tmp/urlshort-mission02-rollback-prior-data/urlshort;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE' -user sa -script missions/02-brownfield/release/pre-run-e227acf/rollback-v3.sql
~~~

   The H2 driver is version2.4.240; get its jar from the resolved runtime cache or extract
   BOOT-INF/lib/h2-2.4.240.jar from the tested boot jar with a ZIP reader. Its location is an
   input to the command, not a required installed system path. Do not start the current
   candidate after rollback: its Flyway would reapply V3/V4.
4. Start the prior binary on loopback with Java21. The actual rehearsal used:

~~~sh
/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home/bin/java -jar /private/tmp/urlshort-mission02-rollback-f090103/build/libs/urlshort.jar --server.address=127.0.0.1 --server.port=18242 '--spring.datasource.url=jdbc:h2:file:/private/tmp/urlshort-mission02-rollback-prior-data/urlshort;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE' --logging.file.name=/private/tmp/urlshort-mission02-rollback-prior.jsonl
scripts/smoke.sh http://127.0.0.1:18242
~~~

   Adjust only the Java21 path and the stopped-data copy location on another host.
   For container deployment, publish only 127.0.0.1 and mount the copied directory at /app/data:

~~~sh
docker run -d --name urlshort-rollback-m02 -p 127.0.0.1:18243:8080 --read-only --tmpfs /tmp:rw,mode=1777 --mount type=bind,source=/private/tmp/urlshort-mission02-rollback-prior-data,target=/app/data --stop-timeout 20 urlshort:rollback-mission01-f090103
scripts/smoke.sh http://127.0.0.1:18243
~~~

   Ensure the bind directory is writable by UID10001 before container startup. The jar path
   was rehearsed; the rollback image was built/inspected but its separate startup was not.
5. Verify Flyway versions1,2, removed V3/V4 columns and preserved domain rows, then health,
   create, redirect, stats, errors and loopback publishing. Only after this passes should the
   Operator substitute the stopped production-data copy. Retain the original backup.
6. To undo the rollback, stop the prior service and start the final candidate on the copy.
   Flyway reapplication was observed in the historical pre-run and its smoke passed. Restore
   the untouched full backup to recover discarded audit metadata; restoration loses writes
   after that backup.

## Evidence and exact limits

rollback-raw-csv.tar.gz preserves all28 CSVs for before, afterV4, afterV3 and reapplication.
prerun-revalidation.json independently rechecks hashes and Flyway versions. Counts are **data rows
excluding headers**: before1203 links/1204 audit rows/12003 clicks; after reapplication/smoke
1204/1206/12004. The historical pre-run prose incorrectly called them header-inclusive; it is
preserved as history. rollback-prior-smoke.txt and rollback-prior.jsonl additionally prove actual
prior-version startup on a fresh rolled-back copy, followed by SIGTERM/graceful completion.

The historical Dockerfile's Gradle9.7.1 initialization stalled without visible progress and was
stopped by the agent (exit137); no root cause was established. The verified rollback image packages
the successfully built prior jar using the old runtime stage in rollback-runtime.Dockerfile.
It does not claim a successful historical distribution download or a separately smoked container.
