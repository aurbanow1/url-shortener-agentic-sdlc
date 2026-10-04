# Final preparation evidence

Product candidate:30f8de4e647b05ff54cde09f1019ae519b00069d. [RELEASE](../../RELEASE.md) connects claims to the raw files here. The pre-run directory remains historical; the final raw revalidation names its limits and the historical metric discrepancy.

Reproduce the frozen metric figures without OpenRig or network access, using Python3, Git and Node:

~~~sh
python3 missions/02-brownfield/release/final-30f8de4e/verify-metrics.py
~~~

The checker verifies the archive and original generator hashes, extracts into a disposable directory, freezes only Date.now to the recorded generation time, and compares every metric after normalizing the live/export directory label. No formula changes. [Provenance](final-metrics-provenance.json) binds every raw input; [recorded replay](final-metrics-replay.json) records the original comparison. The later preparation export and QA closures have their own timestamps.

Recheck JSON records, local links, current proof hashes against committed HEAD and candidate-source equality from the repository root:

~~~sh
python3 missions/02-brownfield/release/final-30f8de4e/verify-package.py
~~~

This refreshes docs/evidence/02-brownfield/prep-validation.json. The initial export validation is preserved separately; that earlier snapshot still had an open QA reaffirmation and unknown proof.

[Rollback instructions](rollback.md) name the exact old executable and schema commands. The prior jar was smoked on a stopped-data copy; the runtime-only old image was built and inspected. The D21-specific revert and old-image startup recipe are described, not claimed executed. [Raw CSVs](rollback-raw-csv.tar.gz) preserve all28 stage files, with data counts excluding headers.
