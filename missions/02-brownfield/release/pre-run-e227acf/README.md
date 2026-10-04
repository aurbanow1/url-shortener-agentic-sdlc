# Mission 02 pre-run — e227acf

Prepared against `e227acf04e1ea902406532fcebd741f8a86f7f65` on `main`.
Authorized by ordinary item `qitem-20261004005451-9e2c091d`; this is not the
mission lifecycle's release_prep completion. Five mission-02 slices, analytics
v2 and the W2F-01 scheduling-test fix are present. D21's `06-client-identity`
refactor is absent. The product/build/scripts/API diff from `50ad9c3` is empty.

Jar port `18220`, container port `18221`; release2 uses `18230`/`18231`.
The root build is available after release2 captured its pinned jar. Use a
separate image tag and disposable databases; do not overwrite its evidence.
Every result here must retain this candidate SHA. The final mission package
must freshly bind gate and installed smoke to its later merged candidate and
review the refactor delta before carrying any pre-run result forward.

## Status

Fresh gate/bootJar passed: 226 unit + 250 functional invocations. Canonical
merged coverage from only those two suites is 581/581 lines and 206/206 branches.
The copied jar and image both passed the installed baseline smoke and analytics
v2 fields/counter probes; OSV reported 97 dependencies and zero advisories.
The jar runs on 18220 (PID 89865), container on 18221 (name
urlshort-mission02-prerun-e227acf). Both use disposable data.

The benchmark completed after release2's rollback gate: combined GET100/s +
create20/s, GET100/s alone and HEAD100/s alone all ran for 60 seconds with
zero bad responses; see `bench.txt`. The V3/V4 rollback rehearsal and
candidate reapplication smoke passed on disposable data; see
`rollback-verification.md`. QA's bounded installed dogfood passed with no new
defects in `docs/qa/dogfood/02-brownfield.md` (`d83bbc5`). Mission evidence
export and coordinated metrics refresh are complete. No release readiness or
human ship decision is claimed: D21 and the lifecycle release gate remain.
