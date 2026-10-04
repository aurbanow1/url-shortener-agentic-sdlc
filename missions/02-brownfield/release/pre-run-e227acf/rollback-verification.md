# V3/V4 rollback rehearsal — e227acf pre-run

The rehearsal used a copy of the stopped jar database at
`/private/tmp/urlshort-m02-prerun-e227acf-data/urlshort`; the original
pre-run directory was retained. The service was stopped before each DDL step.

1. `snapshot-before.sql` recorded complete link, audit_log, click and
   user_agent_class rows plus columns, constraints and Flyway history.
2. `rollback-v4.sql` ran the literal V4 header statements, then
   `snapshot-after-v4.sql`. Flyway history changed from versions 1,2,3,4 to
   1,2,3; domain row CSV SHA-256 values were unchanged.
3. `rollback-v3.sql` ran the literal V3 header statements, then
   `snapshot-after-v3.sql`. Flyway history changed to 1,2; domain row CSV
   SHA-256 values remained unchanged.
4. The pinned candidate jar was started on port 18222, bound to 127.0.0.1,
   against that rolled-back copy. Flyway reapplied V3 and V4, and
   `rollback-reapplied-smoke.txt` records a complete installed smoke pass.
   The final snapshot contains the expected smoke additions (one link, two
   audit rows, one click); the pre-existing rows were not rewritten.

Evidence CSVs live under the corresponding temporary stage directories. The
before/after domain hashes were:

| Projection | Before / after V4 / after V3 SHA-256 |
|---|---|
| link.csv | `ce7846db820dd380db42632712c9462a4e89fd2e6a4967d8624d9e8225814ba5` |
| audit_log.csv | `434f798418ad0d9bcc0081579a0eb35824c7be1b73ae3713ec86c7eb60551fc1` |
| click.csv | `5f41c89e4e040a7dc31631e5b9b533e2830f14224f9f5f570e40ad16e62b7c61` |
| user_agent_class.csv | `c7143b0ca2e90c912d376d5b7acc30f1519638b92493f4fae3bbce1e9574485f` |

Before counts were 1,203 links, 1,204 audit rows and 12,003 clicks (CSV
header included). After reapplication and smoke they were 1,204 links, 1,206
audit rows and 12,004 clicks. The final Flyway history was 1,2,3,4.

Rollback removes the added row-audit metadata, while the older rows remain.
Purged clicks cannot be reconstructed; recovery requires the stopped-data
backup and loses writes after that backup. The rehearsal used a disposable
copy and did not alter the original pre-run directory.
