# QA instrumentation and limits

QA product boundary: no src/, tests, build.gradle.kts or scripts changed. The
Java harness is compiled by the saved external Gradle init task against candidate
classes/runtime dependencies. Public effects use real Tomcat and scripts/http;
file commands only control disposable fixtures and inspect the real DataSource.
The normal installed upgrade uses actual java -jar on rebuilt f6dd29e then X.
The baseline build used the root wrapper (Gradle 9.8.0) on exact f6dd29e sources;
the candidate gate/replay used its worktree wrapper (9.7.1), all offline/JDK21.

Probe chronology is retained, so errors in QA tools cannot be confused with
product failures:

- An initial bare java launch selected JDK11 and rejected class-file version 61;
  the corrected executable is /opt/homebrew/opt/openjdk@21/bin/java. A preliminary
  original-service PID 77763 was stopped with scoped approval after sandbox kill
  permission was denied. It is not the final upgrade capture.
- qa-effects-sandbox-denied: child JVM could not bind localhost (SocketException,
  Operation not permitted). It stopped; the driver was retried with scoped
  approval, not by changing product/toolchain configuration.
- qa-effects-probe-sql-correction: QA forgot quotes on lower-case Flyway columns.
- qa-effects-probe-history-correction: QA included Flyway's null schema-marker
  version in the ['1','2'] expectation.
- qa-effects-a8fc8b6: upgrade/invalid starts passed; QA later assumed stats used
  day/count, while the specified fields are date/clicks.
- qa-effects-continued-a8fc8b6: all remaining retention effects passed after the
  field correction.
- qa-effects-probe-replay-correction: metadata comparison passed; QA expected
  replay 200 instead of the specified unchanged 201.
- qa-effects-public-detail-a8fc8b6: public failure/expiry/rate and detailed schema
  checks passed with the corrected replay assertion.
- qa-effects-final-a8fc8b6: corrected --all driver completed end to end, 249/249
  assertions, 207 captured responses correlated with JSON product logs. This is
  the primary by-effect evidence. All its owned processes stopped.

The controls use an external primary UTC Clock, trusted loopback proxy plus
separate documentation-range peer addresses, and higher budgets only for long
shifted-clock retention fixtures. The public failure fixture keeps the shipped
60/600 budgets. Invalid settings and pause/period are actual environment values.
The concurrency probe sets disposable database LOCK_TIMEOUT=10000 and blocks
old-row locks to observe the real DELETE mid-statement; it does not establish a
load/latency ceiling. The keyed-hash probe changes only an in-memory fixture key
by reflection, restores it immediately, and exercises the real reduction path.

## Self-check

Read actual output, rows, full legacy column/constraint metadata, request IDs and
JSON events. Record only observed scopes. Copy and hash reports before auxiliary
replay data. Stop every owned app and independently check ports. Exact candidate
HEAD retained; proof 9 deferred by the lead's written custody amendment.
