# PROOF — OPR.99.0.4.1 01 Analytics V2

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.4.1 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

Closed by: <seat>   Date: <date>   Verdict: <pass | pass-with-residue | ...>

## What this proves

<1-3 sentences: the claim the slice made, now demonstrated>

## Artifacts (media in proof/)

Dropped via `rig proof add … --evidences … --media …` (one drop per verdict; media attached, never only hand-listed):

- proof/screenshot-01.png — <what it shows>
- proof/capture-behavior.gif — <what it shows>
- proof/command-output.txt — <what it proves>

## Residue / caveats (if any)

<documented residue: what's not covered + where it's tracked>

## Builder (dev2-agent@urlshort-factory)

**Candidate `ec466da`** (handed off 2026-10-03 23:1xZ). It descends from `02-click-retention`'s merge
`ed2b940`, so it carries `01-audit-read` and `03-dogfood-fix` too.

- **Final rebase.** `git rebase --onto 2566c38 a8fc8b6` on `main` at `2566c38`. The six analytics
  commits applied without conflict (`a8fc8b6` was click-retention's pre-merge tip, the cut point).
  `git merge-base --is-ancestor ed2b940 slice/01-analytics-v2` holds.
- **`66d0e1b` test, the granted AC-14 shape update** (`slice.yaml` grant, lead 21:57Z):
  `AuditUpgradeJourneyTest.AC18`'s one expected statistics body. Its single `clicksPerDay` element
  gains `"uniqueVisitors":1,"botClicks":0`, and nothing else in the test changes. It joins the AC-14
  list beside `StatsJourneyTest.AC09` and `ClickRecordingJourneyTest.AC17`. The impact analysis could
  not name it, because audit-read merged after it was written.
- **`ec466da` docs, design §13 step 6:** `docs/api/openapi.json` regenerated last, from
  `build/openapi/openapi.json`, on top of audit-read's and dogfood-fix's documents. The diff is only
  `DayClicks` gaining `uniqueVisitors` and `botClicks` with their descriptions, plus the statistics
  example and its description.
- **Gate on `ec466da`:** `scripts/gw --offline check --rerun-tasks`, log at
  [`proof/builder-check-candidate.txt`](proof/builder-check-candidate.txt). BUILD SUCCESSFUL. Unit 221,
  functional 241, 0 failures or skips; `OpenApiDocumentTest` is green now. Merged lines 580/580,
  branches 206/206 (100 %). Javadoc green. Reports are in `docs/qa/coverage/01-analytics-v2/{unit,functional,all}/`.
- **By effect on `ec466da`** ([`proof/builder-by-effect-ec466da.txt`](proof/builder-by-effect-ec466da.txt)):
  - Three forwarded clients give `{"date":"2026-10-03","clicks":4,"uniqueVisitors":3,"botClicks":1}`.
  - Prometheus shows `urlshort_clicks_recorded_total 4.0`, and all five `lost` reasons at `0.0`.
  - The live `DayClicks` schema has exactly the four properties.
  - No client address appears in the log.

Earlier status, kept for the record: built on the stacked base first. Design §13 steps 2–5 were on
`slice/01-analytics-v2` at `12fe427`. The branch was rebased onto `slice/02-click-retention` at
`056c8db` (the lead's plan-lock note), and I built on top of it. Still to do, in this order:
1. `02-click-retention` merges to `main`, after `01-audit-read` and `03-dogfood-fix` have merged.
2. `git rebase --onto main 056c8db` (or the click-retention merge commit, once it is there).
3. Regenerate `docs/api/openapi.json` last (step 6).
4. Run `scripts/gw check` and hand off that SHA.

Commits (red first, then green):

| Commit | What | Red observed |
|---|---|---|
| `0294f8b` test | the journeys and unit cases of design §7, and the two AC-14 shape updates (`StatsJourneyTest.AC09`, `ClickRecordingJourneyTest.AC17`) | red against signature-only stubs (not committed: a four-field `DayClicks` filled with zeros, `ClickStore.stats` without the day branch, a `MeterRegistry` constructor that registered nothing, `CLIENT_ATTRIBUTE` declared but never set). 9 of 53 unit and 11 of 55 functional tests failed on assertions (`uniqueVisitors` 0 where 3 was expected, `Wanted but not invoked: stamp("198.51.100.5")`, no `urlshort.clicks.*` meter) |
| `3054978` test | AC-8's peer: `203.0.113.87`, not the SPEC's `203.0.113.77` | v1's `ClickRecordingJourneyTest.AC05` sends from `.77` on a September clock in the same shared context. AC-8 sending from it at today's clock first left its rate-limit bucket ahead, and AC05 answered `429`. The SPEC's peer is illustrative, as design §7 already says for AC-3 |
| `ef2c4e0` feat | `ClickStore.stats` (the `UNION ALL`; `countByDayAndReferrer` removed), the `LinkStats` fold, the `DayClicks` `@Schema`s, the `StatsController` example | green |
| `e8ee098` feat | `RateLimitFilter` (grant `c78500e`): `public`, `CLIENT_ATTRIBUTE`, set before charging; `ClickRecorder` hashes it, else the peer | green |
| `39afc9c` test | `ClickMetricsJourneyTest` gains `@AutoConfigureMetrics`. Without it, Boot's test support turns the Prometheus export off, and the scrape answered `404`. `ClickRetentionJourneyTest` (click-retention, the stacked base) has three per-day assertions moved to the v2 shape | these are my test faults, found when the product went green |
| `12fe427` feat | the two counters in `ClickRecorder`, reason constants, `package-info` (the hash's single use) | green |

**Gate on `12fe427`** (`scripts/gw --offline check --continue` in the worktree): 189 functional and
unit tests completed. **1 failure, expected:** `OpenApiDocumentTest.NFRM3_committedDocumentEqualsTheLiveOne`,
because `DayClicks` changed and `docs/api/openapi.json` is regenerated last, after the rebase (design
§13 step 6). Then `javadoc jacocoAllReport jacocoTestCoverageVerification -x test -x functionalTest
--rerun-tasks` ran on that run's execution data. Javadoc passed with doclint, including the now-public
`RateLimitFilter`. Merged coverage is lines 515/515 and branches 174/174 (100 %).

**Deviations from the design, all test-side:**
- AC-3 uses peer `203.0.113.31` and AC-8 uses `203.0.113.87` (dedicated peers, as above).
- AC-9 and AC-12 sit in `TrustedProxyClickJourneyTest`, because AC-9's input includes AC-7's
  proxied clients, and those need the trusted-proxy context.
- AC-13's per-day schema check is a new test in `StatsV2JourneyTest`, so the shipped `StatsJourneyTest`
  document test stays unchanged (AC-14).
- **Outside the impact analysis's AC-14 list:** the three `ClickRetentionJourneyTest` assertions. That
  test did not exist when the analysis was written; it is `02-click-retention`'s, on the stacked base.

**Rebased onto click-retention's candidate** at 21:44Z: `git rebase --onto slice/02-click-retention 056c8db`
applied cleanly, giving tip `e64fb51` on `a8fc8b6`. `check --continue` still showed only the expected
`OpenApiDocumentTest` failure.

**Trial rebase onto `01-audit-read`'s merge `cb148c4`** (21:53Z), in a throwaway detached worktree, so
no slice branch moved. The whole stack applied **without conflict**:
`application.properties` and `README.md` merged in custody order (audit-read's lines first). The gate
on it ran 236 tests with 2 failures:
- `OpenApiDocumentTest`, expected;
- `AuditUpgradeJourneyTest.AC18` (audit-read's, new on `main`), which asserts v1's exact statistics
  body. The lead granted that one assertion at `35e5951` (my request `qitem-20261003215452-d915f3ab`).
  It changes on the final rebase as an AC-14-style shape update.

**By effect on `e64fb51` (interim, retaken on the candidate):**
[`proof/builder-by-effect-e64fb51.txt`](proof/builder-by-effect-e64fb51.txt). The jar runs on 127.0.0.1
with 127.0.0.1 as trusted proxy, and three clients are named in `X-Forwarded-For`:
- Statistics: `{"date":"2026-10-03","clicks":4,"uniqueVisitors":3,"botClicks":1}`.
- Prometheus: `urlshort_clicks_recorded_total 4.0`, and `urlshort_clicks_lost_total` for all five
  reasons, each at `0.0`.
- No client address in the log.

**Still to do on the candidate:** the coverage reports in `docs/qa/coverage/01-analytics-v2/`, and the
captures retaken.

## Self-check (interim)

- Diff re-read against design §1. `recorded` increments right after `insert` returns, before
  `compareAndSet` (unit: a write that returns after the claim is counted). All five `lost` counters
  are registered at construction (unit: present at zero, one tag each). The fold has no fallback for
  a missing day row. `countByDayAndReferrer` is deleted. `client_hash` appears only in the `COUNT(DISTINCT …)`.
- Grant conditions: in `RateLimitFilter`, only the class modifier, the constant, the attribute line
  and the Javadoc changed. `clientOf` and the constructor are still package-private. Every existing
  `RateLimitFilterTest` case is unchanged, with one case added. No other `web/` file is touched.
- No migration, no `application.properties`, no README.

## QA — exact candidate ec466da8da4b1efde9d612c6c8692070cc6fc4b9

Independent checker: qa-agent@urlshort-factory (Codex), 2026-10-03. Verdict: PASS for qa_check. Read the SPEC, design, impact analysis, builder proof and required QA guidance; independently verified the effects rather than adopting the builder's captures. Proof items1–10 are covered here. Item11 awaits the security-review record and item12 awaits release_prep; the lead's a12a0e2 records both returns before their acceptance stages.

Fresh exact-candidate offline gate: 221unit/241functional, no failures/errors/skips; merged CSV580/580 lines206/206 branches100/100. Reports copied and372 SHA-256 comparisons passed. Details: [coverage summary](../../../../docs/qa/coverage/01-analytics-v2/SUMMARY.md); independent log [qa-check-ec466da.txt](proof/qa-check-ec466da.txt).

Three isolated actual-Tomcat processes used the immutable candidate jar/classes. Port18170 ran the unmodified jar with actual loopback configured as a trusted proxy. Ports18171/18172 ran unchanged candidate classes with external QA-only Clock/Servlet-peer controls and an actual H2 BEFORE INSERT trigger for faults and delay. [AnalyticsQaLauncher.java](proof/qa-ec466da/AnalyticsQaLauncher.java) discloses these fixtures. Product code and shipped tests were never edited. The jar's SHA-256 is a1f85572fd35035385d50696ceae7ff25a230a712a42631c46f57134e0a39050.

| AC | Independently observed effect | Capture under proof/qa-ec466da/ |
|---|---|---|
|1| Exact four top-level fields and four daily fields; one browser1/1/0; untouched link zero/empty | http/ac01-stats.body; ac01-empty-stats.body |
|2| SPEC three clients3+2+1 give6clicks/3uniques; installed three clients, one twice, one bot give4/3/1 | http/ac02-stats.body; installed-stats.body |
|3| Same exact SPEC peer at23:59:59 and00:00:01 gives one unique each UTC day; stored hashes differ; no combined figure | http/ac03-stats.body; default-figures-rows.json; controls.json |
|4| Six UA rows from six clients give6/6/3; bots remain in raw totals | http/ac04-stats.body; default-initial-rows.json |
|5| Same client's browser and bot give2/1/1 | http/ac05-stats.body |
|6| V1 seven-click total, exact2/3/1 UTC series with absent day3, exact26-click top10/tie order; totals consistent | http/ac06-seven-stats.body; ac06-days-stats.body; ac06-referrers-stats.body |
|7| Trusted10.9.9.9 and exact four SPEC headers give4/3/0; forged left entry ignored; no address in any stored click column | http/ac07-stats.body; trusted-ac07-rows.json |
|8| Default peer203.0.113.77 and varied XFF/Forwarded/X-Real-IP give3/1/0; no forwarded address stored | http/ac08-stats.body; default-initial-rows.json |
|9| V1 exact privacy canaries, forwarded inputs and stored hashes absent from aggregate-only bodies | http/default-v1-canaries-stats.body; trusted-v1-canaries-stats.body; stored snapshots; verify.py |
|10| Both contexts: three actual inserts give recorded+3; two physical failed inserts give lost+2/write failed, no rows and unchanged302 | http/default-after-recorded.body; trusted-after-recorded.body; default-after-write-failed.body; trusted-after-write-failed.body; JSON logs |
|11| Both counter families present, lost has only static reason labels; no client/hash/link/referrer/UA value | default-click-counters.txt; trusted-click-counters.txt; http/*-prometheus.body |
|12| All716 responses match requestId/status exactly once in default console and file; each failure has one correlated class-only loss WARN; no private values in either sink | http-ledger.json; head-wire.json; *-console.jsonl; *.jsonl; verification-summary.json |
|13| Live installed OpenAPI equals committed document; DayClicks exactly four properties/example; every other baseline operation equal | http/installed-openapi.body; openapi-diff.txt; verify.py |
|14| Original f6dd29e replay155/155 with only four explicitly authorized expectations across three files; initial literal153/155 retained | v1-authorized-summary.json; shipped-functional-source-provenance-authorized.json; v1-replay-summary.json; lead a12a0e2 |
|15| Two-second actual H2 insertion delay:20redirects all below250ms in each context; two failed writes still302;200concurrent each give200 stored reduced rows | http/*slow-retry-burst-timings.txt; *-slow-before-settle.json; *-concurrent-rows.json; verification-summary.json |

Additional failure/regression effects: invalid scheme400, idempotent replay201/same code, mismatched key422, exact24-hour expiry201/new code, unknown/long statistics code404, wrong method405, retired410/statistics remain readable, HEAD zero body bytes on real wire, OPTIONS defaults, and the61st API request429/Retry-After. Every error has application/problem+json and urn:uuid response correlation. Reads and HEAD/OPTIONS leave complete database snapshots byte-equivalent; installed create/retire audit rows match their response IDs with no duplicate replay audit row. After controlled advance beyond90days, the inherited autonomous purge removes old click rows and the statistics become empty while links remain.

AC-14 qualification: the first literal replay has only the two inherited audit enumeration failures. QA requested a ruling before accepting them; lead a12a0e2 carries forward audit-read grant428e9e1. The repeat changes only those two enumerations plus the two allowed per-day expectations; all other original assertions remain unchanged. The replay fixture disables purge for the older v1 timestamps, as in the preceding retention replay; the canonical candidate gate and manual inherited purge observation use enabled purge. The newly merged AuditUpgradeJourneyTest exact-body change is separately granted35e5951 and named by the builder; three click-retention exact-day assertions belong to its later dependency tests. The impact analysis names both original f6dd29e day expectations and redirect/click/migration/API custody, with these later additions disclosed in the builder section and lead records.

Observation limits: Servlet peers and Clock are controlled inputs, not a remote TCP boundary or natural midnight. The H2 trigger is an isolated physical-failure fixture, not a natural disk crash. The slow/concurrency captures establish resilience, not NFR-L1 percentile results. Hash single-use construction and salt non-persistence remain the required downstream security judgment. No Docker, production load, queue-capacity saturation, crash recovery, restart upper-bound manual exercise, or shutdown-under-load claim. Existing unit regressions cover reason vocabulary, salt disposal and restart/day identity behavior; QA does not infer a missing review from those green tests.

### Self-check

- Every AC exercised by observable effects or its recorded original-suite/document comparison; required failure cases tried. Observed status/headers/body, actual stored rows, correlated default-console/file JSON and audit rows. Controlled mechanisms and unchecked limits explicitly declared.
- Read merged coverage from the copied CSV, verified372 report hashes and the independent gate; no exclusion or threshold change. All282 source-method/context rows and462 invocations map both ways;41 parameterized methods have disclosed group attribution. Five inherited resilience methods execute under each configuration.
- TRACEABILITY and mandatory GAPS entry written, including pending NFR-L1. Raw captures and initial instrument errors retained; corrected referrer expectation, timing parser and direct-wire HEAD probe verified before the verdict.
- Proof drop attests items1–10 and names the actual candidate. Security11/release12 remain pending under authored return sequencing, not accepted absent evidence.
- All three processes stopped normally with exit130; every post-stop loopback probe returned curl7. Exact candidate HEAD rechecked and worktree clean. No product/test edits.
