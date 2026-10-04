# Coverage and verification gaps

Appended by the QA Agent at every `qa_check`. The gate is 100 % line and branch
coverage across both suites; anything below, or any acceptance criterion not
verifiable by an automated test, is recorded here with the reason and the
manual check performed instead. "None for this slice" is a valid entry.

| Slice | Gap | Why | Compensating check | Status |
|---|---|---|---|---|
| 01-ping | Functional-only coverage 13/15 lines (86.67%); merged 15/15 (100%) | Functional suite does not invoke the two lines of UrlshortApplication.main; per-suite coverage is informational | Unit mainBootsWithoutAWebServer covers them; all three CSVs copied into coverage/01-ping | No merged coverage gap or exclusion; no waiver required |
| 01-ping @3886a04 | AC-7 live log contains 127.0.0.1 in Tomcat thread metadata | Required localhost bind adds the server address to the thread name; MockMvc with a distinct client address does not exercise this metadata | Real GET/canary captures and installed Tomcat bytecode; see 01-ping/findings.md QA-01 | Resolved on f286a10 by exclusion of process.thread.name; independently rechecked on the same bind; no waiver |
| 01-ping @f286a10 | None for the merged coverage gate or acceptance criteria | All 15 tests pass, merged lines 15/15, no branches; functional-only 13/15 remains informational | All AC-1–AC-8 observed live; startup-through-shutdown log has no canaries or loopback addresses; captures carry f286a10 suffix | No open verification gap; packaged artifact belongs to release checks |
| 01-create-redirect @a922f49 | None for this slice (merged coverage or AC verification) | Merged 185/185 lines and 56/56 branches; all 28 ACs have functional tests; unit-only 169/185 lines and functional-only 176/185 lines, 50/56 branches are informational | Live HTTP, correlated JSON logs, offline H2 exports, actual rejected audit insert with rollback, append-only snapshots and live OpenAPI diff; AC-19 uses the SPEC-authorized FunctionalClock | No coverage exclusion or waiver. Proof items 13–14 require later code-review/release records; tracked in qitem-20261003072643-917956c7. Packaged artifact and PostgreSQL are not claimed here |
| 02-analytics @862c52e | Unit-only 325/359 lines (90.53%); functional-only 324/359 lines (90.25%), 92/118 branches (77.97%); merged 359/359 lines and 118/118 branches | Per-suite coverage is informational; all 247 invocations pass, every AC and rule mapped | Independently copied unit/functional/all reports; complete gate and by-effect HTTP/storage/log/API checks | No merged coverage gap, exclusion or waiver |
| 02-analytics @862c52e | NFR-L3: added redirect p95 ≤2 ms not measured in QA | SPEC Non-functional and proof item 13 assign the number to release_prep after 03-operate supplies the bench; an isolated added share may require an honest release gap | AC-14 freshly passed on real Tomcat: every one of 20 redirects <250 ms while click writes sleep 2 s; AC-15 plus live rejected H2 insert preserves 302 | Pending release bench; allowed by the locked SPEC, never claimed as a measured p95 |
| 02-analytics @862c52e | Clock-boundary and lifetime observations use controlled tests; future salt security record pending | AC-5 and AC-9 explicitly authorize the suite clock; no natural UTC midnight was crossed. Salt lifetime is not observable through HTTP; item 12 requires design and security records | Reviewed concrete assertions and fresh functional effects for two addresses/day rotation/UTC groups; unit stale-callback/expiry/midnight tests; live no-PII exports and logs. Design review exists; security record follows qa_check | No untested AC or coverage gap; item 12 stays pending until its required security review is recorded |
| 02-analytics @5b3490c | No merged coverage or AC verification gap; NFR-L3 numeric added p95 remains pending release_prep | Test-only CR-01 setup repair passes isolated/full runs; merged CSV 359/359 lines, 118/118 branches. Per-suite misses remain informational. Production and functional trees identical to 862c52e | Fresh all-AC functional effects, 15 live exchanges/rejected insert, adopted prior comprehensive captures via exact source equivalence; salt security record and fresh actual-class disposal probe satisfy item 12 | Only item 13 remains pending under the locked SPEC and qitem-20261003103240-18e29a17; no exclusion/waiver |

| 03-operate @a7c533f | None for merged coverage or in-suite AC-1–AC-20 | 441/441 lines and 160/160 branches; 163 unit / 155 functional invocations, zero failures/errors/skips | All in-suite ACs independently observed with real HTTP; 2,303 captured exchanges; CSV/hash/traceability checks | No exclusion, lowered threshold or waiver; per-suite misses informational (unit 398/441 lines; functional 406/441 lines, 129/160 branches) |
| 03-operate @a7c533f | Controlled time, request peers and database-unavailable switch | Exact AC-3/4 boundaries and distinct peers use SPEC-authorized suite mechanisms; macOS rejected binding 127.0.0.2 | Disposable localhost launcher adds a Primary Clock, a request-peer wrapper and an H2 DataSource gate; actual candidate classes/resources, Tomcat, JDBC migrations and HTTP effects; separate unmodified jar smoke/env/drain checks | No product edit; no natural quiet-minute, natural database outage or real distinct-TCP-peer claim; test addresses 10.0.0.1/2 and 10.9.9.9 remain freshly verified in functional suite |
| 03-operate @a7c533f | AC-21 — one-command healthy container/readiness smoke | NFR-X1, NFR-R1, NFR-O3: locked SPEC assigns running-container proof to release_prep | QA compose config exit 0; unmodified jar smoke passes health, metric names, Prometheus; release: compose up --build, readiness health check, installed smoke | PENDING release container proof |
| 03-operate @a7c533f | AC-22 — loopback published port | NFR-X1: configuration cannot prove runtime binding | Compose pins 127.0.0.1:8080; release: compose port and docker inspect bindings | PENDING release inspection |
| 03-operate @a7c533f | AC-23 — non-root/read-only container and writable data | NFR-S5: no running-container claim in suites | Dockerfile/compose inspected; release: inspect User/ReadonlyRootfs/mounts and successful/failed writes | PENDING release probes |
| 03-operate @a7c533f | AC-24 — durable links after compose restart and down/up | NFR-R4: database persistence across container lifecycle is installed evidence | scripts/smoke.sh --restart is present; release checks original body/target after both lifecycles | PENDING release restart proof |
| 03-operate @a7c533f | AC-25 — final installed shutdown record | NFR-R3: release owns final judgment | QA unmodified jar --drain PASS: R0 201 within 1 s, probe refused, 62 complete / 20 refused / 0 boundary losses / 0 failures; functional test pins 10 s | Supplemental jar observation PASS; release record PENDING |
| 03-operate @a7c533f | AC-26 — final packaged configuration/smoke record | NFR-X1: release owns final installed-artifact judgment | QA unmodified jar uses env-overridden public URL, file path, 2/3 budgets and trusted peer; 429 at create limit and full smoke PASS | Supplemental jar observation PASS; release record PENDING |
| 03-operate @a7c533f | AC-27 — specified offered rates and NFR-L1/L2 latency | 60 s QA bench achieved 82.5 redirects/s and 16.5 creates/s, below 100/20; lower-load p95 values do not prove target | Captured zero-error mode output (redirect p95 2.3 ms, p99 4.1 ms; create p95 2.4 ms); release must establish specified rate before numeric judgment | PENDING release; QA-OPR-01, proof-sequencing.md; no latency-target claim |
| 03-operate @a7c533f | AC-28 — compose restart under load and stop timeout inspection | NFR-R3/X1: container proxy and runtime effects untested by suites | compose declares 20 s stop grace > 10 s phase; release: smoke --restart, R0/status classification and inspect StopTimeout | PENDING release proof |
| 03-operate @a7c533f | Proof item 11 needs downstream code/security records | Review steps follow qa_check | Unit memory-release tests and live response/log/metric/storage privacy observed; records requested through orchestration lead | PENDING review record, not pre-accepted; item 13 likewise pending release |


## Re-check 03-operate — 1c8b2cf

| Slice | Gap | Why | Compensating check | Status |
|---|---|---|---|---|
| 03-operate @1c8b2cf | None for merged coverage or AC-1–AC-20 | Fresh 165/155 gate; 443/443 lines and 162/162 branches; per-suite misses informational: unit 400/443 lines, functional 408/443 lines and 131/162 branches | Full independent HTTP journey, all 186 source methods/320 invocations traced; all 321 report hashes match | No exclusion, threshold change or waiver |
| 03-operate @1c8b2cf | Controlled time, request peers and JDBC availability | Same SPEC-authorized mechanisms as prior QA; no natural quiet-minute/outage or alternate TCP peer claim | Fresh actual candidate classes/resources/libs on Tomcat and migrated H2; distinct SPEC peers freshly pass functional suite; unmodified jar checks supplement | Explicit instrument boundary; product paths unchanged |
| 03-operate @1c8b2cf | Backward Clock step outside contract; arbitrary delayed concurrent sweep reads not stress-tested | Lead decision transition 726 assumes forward Clock and explicitly allows fail-closed waits after rollback; sweep's 2s margin does not cover every possible stalled thread | New deterministic regressions pass; unchanged review probe gives 60 reordering admissions and 2 post-rollback clients; read exact lead decision and candidate diff | Disclosed limit, no extra-budget claim; forthcoming review owns item 11 |
| 03-operate @1c8b2cf | AC-21 — healthy container/readiness | NFR-X1/R1/O3, release-level by locked SPEC | Fresh jar smoke PASS; unchanged Docker/compose configuration previously inspected; release compose up --build and installed smoke | PENDING container proof |
| 03-operate @1c8b2cf | AC-22 — runtime loopback publish | NFR-X1, configuration alone cannot prove binding | Configuration identical to inspected prior candidate; release compose port and docker inspect binding | PENDING runtime inspection |
| 03-operate @1c8b2cf | AC-23 — non-root/read-only container and writable data | NFR-S5, release installed effect | Configuration unchanged; release inspect User/ReadonlyRootfs/mounts and successful/failed write probes | PENDING container probes |
| 03-operate @1c8b2cf | AC-24 — durable links across restart and down/up | NFR-R4, installed lifecycle effect | Repaired shared R0 functions controlled 8/8; release scripts/smoke.sh --restart verifies original body/target across lifecycles | PENDING persistence/restart proof |
| 03-operate @1c8b2cf | AC-25 — final installed shutdown judgment | NFR-R3, release owns final record | Fresh unmodified jar: complete R0 201 at 532 ms, curl exit 0; new connection refused; 62 complete/16 refused/0 boundary losses/0 failures; strict complete/truncated/deadline controls 8/8 | Supplemental jar PASS with supported C locale; release record PENDING |
| 03-operate @1c8b2cf | AC-26 — final packaged config/smoke record | NFR-X1, release owns final installed environment | Fresh unmodified jar env public URL/data path/trusted peer/2-3 budgets and full smoke PASS | Supplemental jar PASS; release record PENDING |
| 03-operate @1c8b2cf | AC-27 — specified 100/20 offered rate and NFR-L1/L2 | Fresh 60s bench achieves only 82.1 redirects/s and 16.4 creates/s; p95 at lower load cannot prove target | Zero-error mode output retained: redirect p95 2.3 ms/p99 3.8 ms; create p95 2.6 ms. Release must establish actual specified rate and judge numbers | QA-OPR-01 MEDIUM carried to release, no numeric-target acceptance |
| 03-operate @1c8b2cf | AC-28 — compose restart under load and StopTimeout | NFR-R3/X1, no container execution here | Same corrected R0 functions reject bad/truncated/late responses; release actual --restart and docker inspect StopTimeout | PENDING runtime restart/inspection |
| 03-operate @1c8b2cf | Host Perl/Time::HiRes and supported locale for R0 modes; Linux host not tested | Inherited C.UTF-8 is unsupported by macOS Perl and panics at timestamp setup; new host dependency affects --drain/--restart | Standalone same Perl command reproduces exit 9 under inherited locale and exits 0 under C; failed attempts retained, strict supported-C controls 8/8 and jar drain PASS | QA-OPR-03 LOW, host requirement recorded; release verifies its actual host, no toolchain/product fix or target waiver |
| 03-operate @1c8b2cf | Anonymous disk-gauge working-directory path | Existing LOW observation W2-03 / QA-OPR-02, fixed by 03-dogfood-fix | Independent baseline reproduces path; exact 4fe7042 jar scrape and disk.free/disk.total retain values with no path, known working-directory path absent; proof/qa-4fe7042/http/ under 03-dogfood-fix and coverage/03-dogfood-fix/SUMMARY.md | **CLOSED by 03-dogfood-fix @4fe7042**; original observation retained as history |
| 03-operate @1c8b2cf | Proof items 11/13 require downstream evidence | QA PASS does not supply independent review records or final release judgment | Lead obligation qitem-20261003120849-f4cbfa97 accepted: 11 returns after new review, 13 after release_prep; proof-sequencing.md updated | PENDING by authored sequence; neither pre-accepted |

## Release record 01-greenfield-core — main f090103 (product identical to 8e9c065)

Appended by the release agent at `release_prep` (2026-10-03). These rows record what the release-level
checks observed; the proof judgments (01 item 14, 02 item 13, 03 item 13) stay with their QA judges.
Evidence under `missions/01-greenfield-core/release/`; narrative in `missions/01-greenfield-core/RELEASE.md` §3.

| Slice | Gap | Why | Compensating check | Status |
|---|---|---|---|---|
| 03-operate @8e9c065 | **Accepted disclosed host gap; AC-28 amended under A-19**: held R0 through the Mac host's published `127.0.0.1:8080` gets an empty reply about 10.4 s after `docker compose restart` (6 of 6 recorded runs, including the original 5); the rest of its body never reaches Tomcat and the 10 s phase times out. This host-path result remains a failure of the original criterion, not a successful response | Host: macOS 15.2 (Apple M4 Pro), Lima VM Ubuntu 24.04, Docker Engine 28.4. Isolation points to the additional Mac-to-VM forwarding path; its precise connection-drop mechanism is unestablished. Native Linux Docker was not tested. The human changed the R0 measurement point to direct paths at ship sign-off: gate qitem-20261003165209-ce7abb0e, transition 1011, 2026-10-03T17:11:19Z; SPEC amendment 55a197a, A-19. Earlier lead decision qitem-20261003151205-a94f1d92 kept the original criterion until this human decision | Jar R0 complete 201 in 3/3 drains; VM's Docker-published port complete 201 in 2/2; namespace 2/2 reported, 1 retained complete response/log (first overwritten). Inspect stop timeout 20 s >10 s. Corrected restart counts: runs 3/4 148 attempts with 102/103 proxy failures; run 5 149/105; run 6 149/103, zero received responses outside 2xx/3xx or cut short. Evidence: missions/01-greenfield-core/RELEASE.md §3.3–3.6 and release/{smoke-restart-container-f090103-run6.txt,r0-vm-published-port-f090103-run1.txt,r0-vm-published-port-f090103-run2.txt,r0-in-namespace-control-f090103.txt,smoke-drain-f090103.txt,smoke-inspect-container-f090103.txt}; docs/RISKS.md records the host gap | Host gap accepted by human@kernel at transition 1011; AC-28 re-judged only against the amended direct-path criterion (A-19). The original rejection in receipt 27 and all failed host captures remain historical; no source fix or native-Linux result claimed |
| 03-operate @8e9c065 | AC-21 to AC-26 observed on the installed artifacts | Release-level by the locked SPEC | AC-21 compose up --build healthy via the readiness check + smoke; AC-22 `127.0.0.1:8080` only; AC-23 `User=urlshort` uid 10001, `ReadonlyRootfs=true`, one volume at `/app/data`, write under `/app` refused, under `/app/data` allowed; AC-24 link survives restart and down/up (3 of 3 runs that reached the check; runs 1 and 2 stopped at R0); AC-25 jar drain R0 201 at about 514 ms, probe refused, 0 boundary losses, 0 failures (3 of 3); AC-26 env base URL, data path, trusted proxy and create budget 10 observed | Observed PASS; item 13 judgment pending qa2-agent |
| 03-operate @8e9c065 | AC-27 / NFR-L1, L2 at the specified offered rate | QA-OPR-01: closed loops could not offer 100/20 | Open-loop `tools/bench.mjs` on the jar (budgets raised by command-line settings, shipped H2 file mode): 100.0 redirects/s p95 2.2 ms, p99 3.3 ms; 20.0 creates/s p95 2.8 ms; 0 errors; same machine as the service | Targets met on this run; one run, one laptop, not a capacity claim; the container was not benched |
| 02-analytics @8e9c065 | NFR-L3 added redirect p95 | The share is isolated as GET (records a click) minus HEAD (same path, returns before recording) at 100/s, 60 s each | GET p95 1.7 ms, HEAD p95 1.8 ms: no added p95 measurable above run-to-run noise; 12,000 GET clicks stored (statistics total), so recording was not skipped | ≤ 2 ms bound holds on this run; item 13 judgment pending qa-agent |
| 03-operate @8e9c065 | R0 host prerequisites (QA-OPR-03) | Perl panicked under an uninstalled locale on QA's host | This host's inherited `en_US.UTF-8` works; `scripts/smoke.sh` now runs Perl under `LC_ALL=C`, so the inherited locale no longer matters; Perl with Time::HiRes remains a prerequisite (script header) | Host prerequisite documented; locale dependency removed |

## QA release judgment — 02-analytics, merged 8e9c065

| Slice | Gap | Why | Compensating check | Status |
|---|---|---|---|---|
| 02-analytics @8e9c065 | NFR-L3: an isolated added click-recording p95 is not established by the release bench | The corrected sequential 60 s GET and HEAD runs measure separate response distributions. GET-minus-HEAD p95 is a comparison proxy, not a paired counterfactual or an isolated added-cost quantile; the earlier release row's "isolated" claim is limited accordingly | Open-loop GET with creates: 100.0/s, p95 2.2 ms; GET alone: 100.0/s, p95 1.7 ms; HEAD alone: 100.0/s, p95 1.8 ms; zero bad responses and exactly 12,000 GET clicks stored. QA independently reconciled all 19,205 completions, the 12,000-click statistics and the load tool's due-time measurement; AC-14's independent slow-store check remains applicable | Proof item 13's measurement/disclosure requirement is satisfied under its explicit allowance for an unisolated share. No isolated numerical-cost or capacity claim, threshold change or waiver. Audit: docs/qa/01-greenfield-core/release-judgments/audit-8e9c065.json |

## Deferred audit columns — human decision 2026-10-03

Human decision relayed in qitem-20261003175330-fb054f2f; QA documentation packet
qitem-20261003175815-19647350. Policy: docs/guidance/databases.md §2 and §8.
The shipped V1/V2 schema at 8e9c065 predates the policy. Every table requires
timezone-aware, non-null created_at/updated_at and actor columns where an actor
exists; updated_at is maintained on every write. These rows remain open until
their assigned slices merge. Migration effects and backfills are not yet verified.

| Table / shipped subject | Gap | Why | Planned migration and verification | Status |
|---|---|---|---|---|
| click @8e9c065 | Missing created_at, updated_at, created_by and updated_by; clicked_at is the event timestamp, not the row-audit timestamp | Human audit-column policy applies to event tables; the Visitor's actor is anonymous | 02-brownfield / 02-click-retention, w1: **V3 expand migration**, SPEC 32b1ae2 AC-16. Preserve existing columns, backfill all four new columns, verify timezone-aware non-null timestamps with updated_at = created_at for insert-only rows and actor columns anonymous; preserve clicked_at and privacy reductions | **CLOSED** at `02-click-retention`'s merge `ed2b940` (candidate `a2c34c1`, QA `qa2-agent`, AC-16 accepted): `V3__add_click_audit_columns.sql`; `ClickAuditColumnsTest`, `ClickRetentionStartupJourneyTest.AC13_AC16…`. Closed late, at `04-audit-columns`' merge, together with the rows below |
| user_agent_class @8e9c065 | Missing created_at, updated_at, created_by and updated_by | Human audit-column policy applies to reference tables; seeded rows have actor system | 02-brownfield / 02-click-retention, w1: **V3 expand migration**, SPEC 32b1ae2 AC-16. Preserve tokens/constraints, backfill all four new columns, verify timezone-aware non-null timestamps and created_by = updated_by = system; updated_at = created_at for existing seed rows | **CLOSED** at `02-click-retention`'s merge `ed2b940` (same V3 and tests as the `click` row) |
| link @8e9c065 | created_at already exists (timezone-aware, non-null, without a database default); missing updated_at, created_by and updated_by | Human policy requires complete row-audit columns and maintenance on every write | 02-brownfield / 04-audit-columns, w2: **expand migration using the next free Flyway number after V3**. Retain existing created_at values, supply the policy's insert default/maintenance, backfill missing columns, verify actor columns and updated_at on subsequent link writes; existing links retain their meaning | **CLOSED** at `04-audit-columns`' merge `d55a502` (candidate `305f804`, QA `qa2-agent`): `V4__add_link_audit_columns.sql`; `LinkAuditColumnsTest`, `LinkServiceStampTest`, `LinkUpgradeJourneyTest`, `LinkAuditColumnsJourneyTest`, `LinkAuditColumnsFailureJourneyTest` |
| audit_log @8e9c065 | Missing created_at, updated_at, created_by and updated_by; existing occurred_at/actor remain domain-event fields | Human audit-column policy applies to append-only audit tables too | 02-brownfield / 04-audit-columns, w2: **expand migration using the next free Flyway number after V3**. Preserve existing audit fields, backfill row-audit columns, verify inserts set updated_at = created_at and maintain actor columns; **audit_log stays append-only, with no application UPDATE/DELETE path** | **CLOSED** at `04-audit-columns`' merge `d55a502` (same V4 and tests as the `link` row) |

### Self-check

Read the human decision, policy, 02-click-retention AC-16 and 04-audit-columns
allocation. Inspected the exact shipped DDL in V1__create_link_and_audit_log.sql
and V2__create_click.sql at 8e9c065; four tables and their existing/missing
columns match these four rows. This is a GAPS-only documentation change, with
no migration, app, build or proof judgment. Changed hashes of closed mission-01
GAPS-citing receipts are factory backlog; no reaffirmation is made here.

## 01-audit-read — candidate 35590f06c852543c29097a42c43b7802be90ba40

Independent QA, 2026-10-03. No merged coverage shortfall, exclusion, threshold
change or product defect found. Qualified checks and downstream evidence are
explicit below; earlier mission rows remain their own records.

| Scope | Gap or limit | Reason and compensating evidence | Acceptance / owner |
|---|---|---|---|
| Per-suite coverage | Unit 436/492 lines (88.62%), 184/190 branches (96.84%); functional 455/492 lines (92.48%), 158/190 branches (83.16%) | Separate suites exercise complementary paths; direct merged CSV is 492/492 and 190/190. Fresh 200/200 and 200/200 invocations; copied HTML/XML/CSV hashes reconciled in proof/qa-verification-35590f0.json | SPEC NFR-M1 requires merged 100%; per-suite percentages informational; no waiver |
| AC-17 / FR-13 | Two unchanged shipped assertions fail solely because the API enumeration gains the audit operation | Original f6dd29e sources copied without edits and Git-blob hashes checked; 155 tests:153 pass, exactly OpenApiDocumentTest#AC28_liveDocumentDescribesTheSlice and #AC20_everyOperationDocumentsTheTooManyRequestsProblem fail. Their candidate versions pass in the full gate; all existing live operations/responses/examples equal the shipped jar document | Accepted lead grant qitem-20261003182833-40a842ff transition1156, commit428e9e1; only these two enumeration additions allowed. All155original assertions are not claimed green |
| AC-11..14 / NFR-S6 | Remote and IPv6/mapped peer classification is controlled Servlet input, not actual remote TCP traffic | SPEC expressly permits per-request peers; QA curl drives unchanged candidate handlers through a disclosed filter, covers remote GET/HEAD, all four loopback spellings, forwarded/empty headers, trusted-proxy settings and precedence. Native/framework overrides also refused on real Tomcat; all actual servers bound to127.0.0.1 | Qualification required by rule2; headerless local relay remains indistinguishable from Operator and is prohibited/documented by deployment boundary |
| AC-20/21 | Concurrent write and store fault deliberately induced | Actual JDBC autoCommit=false INSERT held across first page, then committed: original30 exactly once and fresh31. QA DataSource connection wrapper induces SQLException on actual audit SELECT preparation: safe500, correlated sanitized events, recovery200. Functional AC-21 uses an AuditTrail spy, independently supplemented by this JDBC effect | SPEC permits induction; no natural hardware failure, cross-process concurrency or crash-recovery claim |
| Proof item12 | Independent security-review record follows QA in this workflow | QA access/privacy effects are complete, but its own observations do not author the downstream review. Items1..11 and13 can be judged against this SHA; item12 must return after security review, before final acceptance | Sequencing obligation in proof/qa-proof-sequencing-35590f0.md routed to orchestration lead; no premature acceptance |

Instrument history retained: first temporary shipped-suite invocation had
NO-SOURCE because Python tar extraction failed; it supplies no test proof.
After safe extraction the real155-test replay ran. First live fixture used
an unsupported QA DATABASE_TO_LOWER option and create returned safe500;
that run was stopped, the documented H2 settings used on a fresh directory,
and all journeys repeated. Date/string normalization and stale in-memory
ledger entries were corrected in the evidence reconciler; raw current-slice
captures are independently archived. The paced61-read probe returned200
because GCRA refills continuously; a0.17s90-read burst then proved the first60
admitted and following30 refused with Retry-After. These are instrument and
probe corrections, not hidden product reruns or accepted product defects.

### 01-audit-read 35590f0 — correction after independent review

The original "no product defect found" and complete AC-13/14 qualification
above are superseded. **QA-AUD-01 HIGH:** explicit Tomcat
remoteip.remote-ip-header or remoteip.protocol-header enables a rewrite while
the strategy stays NONE. A loopback connection with XFF127.0.0.2 then receives
audit content200, and HEAD200, contrary to the mandatory403. Review found it;
QA independently reproduced both variants on the unchanged installed jar and
the default403 control (15 correlated requests). Prior QA/native/framework
and green functional checks omitted this configuration axis. This is an
unaccepted product failure and test gap requiring a builder fix and new
regression coverage, not a waiver or narrower scope. Findings and exact repro:
docs/qa/01-audit-read/findings.md and post-review-remoteip/verification.json.
Coverage/upgrade/read-only results remain historical evidence, not a PASS
of all requirements; security contract item12 remains unaccepted.


## 05-ci-cd — candidate add7ab5ca37dcd6f51aef3cd43c85455e1be6d14

Independent QA2, 2026-10-03. Merged CSV coverage is 443/443 lines and 162/162
branches (100%); no excluded code, changed threshold or merged coverage gap.

| Scope | Gap or limit | Reason / evidence | Acceptance / owner |
|---|---|---|---|
| Per-suite coverage | Unit400/443 lines90.2935%,162/162 branches100%; functional408/443 lines92.0993%,131/162 branches80.8642% | Fresh165/155 invocations all pass; complementary suites merge to443/443 and162/162. 321 copied report files SHA-256 checked in proof/qa-report-audit-add7ab5.json | Existing NFR-M1/build gate applies100% to merged data; no waiver |
| AC-13 | **Runs occurred and succeeded according to the operator record at b6b4a29; no longer pending. QA could not fetch the run/job/artifact data directly.** | [PR ci37153245436](https://github.com/aurbanow1/url-shortener-agentic-sdlc/actions/runs/37153245436): pull_request, job gate, success. [main ci37153380482](https://github.com/aurbanow1/url-shortener-agentic-sdlc/actions/runs/37153380482): push main, success. [main cd37153380418](https://github.com/aurbanow1/url-shortener-agentic-sdlc/actions/runs/37153380418): push main, success, urlshort-jar and smoke-logs attached. Browser cache misses and gh API404 retained in docs/qa/05-ci-cd/github-access-attempts.json | Judged from the operator's committed conclusions/artifact record under the packet's explicit fallback, not QA's own GitHub fetch; follow-up docs/qa/05-ci-cd/github-run-judgment.md |
| AC-2 / A-6; BR-6 | A deliberately failing GitHub run was not exercised; failure upload/retention checked as configuration only | Parsed gate has no -x, continue-on-error or masked exit; explicit bash; reports/smoke upload always(),30 days. Local real check exit0 is captured | SPEC A-6 permits configuration evidence, not a claim of a red GitHub run |
| AC-12 lint | Native actionlint absent; independent YAML1.2 parse plus parsed/source AC checks used | All three candidate files parse without errors or warnings. Builder's separate actionlint1.7.12 capture and negative control retained. QA did not rerun that container | SPEC AC-12 expressly allows parsing when actionlint is not installed |
| AC-12 image | Initial --pull --no-cache attempt stopped at Dockerfile step5; a wholly uncached image build is not claimed | Supplemental builder --pull capture on the same SHA completed with both base images pulled, all15 steps and exit0. Steps5–7 actually ran, including bootJar; other layers cached. QA read both pull digests and success; C1 image/pin attachments committed ad79bb4 | AC-12 satisfied without waiver: cache is allowed by SPEC. Operator now records hosted CD image-build success for AC-13; a wholly uncached build is not independently corroborated by QA |
| AC-5 / runner environment | GitHub's env.JAVA_HOME handoff and hosted Linux shell/runner not executed locally | Own installed jar smoke used local JDK21 default; parsed cd env passes JAVA_HOME; exact script unchanged. Local journey/log/shutdown observed on127.0.0.1:18105 | Operator now records hosted CD bootJar/smoke/image steps successful (AC-13); QA did not fetch job logs or independently inspect the JAVA_HOME handoff |
| Configuration-only traceability | Workflow ACs are file/local-command/run checks, not new product HTTP tests | SPEC explicitly selects this evidence strategy; .github-only diff leaves inherited product tests unchanged. All320 invocation names map to NFR-M1/rule1 and earlier product AC tables | Proof contract5 requires the13AC/7rule check table; no new product tests or test edits |

Instrument corrections: the first QA parser invocation used the wrong local
OpenRig module location and did not parse; the corrected installed path parsed
all three files. The first report audit reached copied coverage but its ps
check was denied by the sandbox; the permitted rerun verified all321 report
hashes, PID absence and refused port. Neither supplies a product finding.
Shared GAPS appends can change earlier receipts' evidence hashes; no judgment
on an unassigned closed mission is made here.


AC-13 follow-up (qitem-20261003211407-91a03acb): read the exact operator
section at b6b4a29, checked all three recorded successful conclusions, PR gate
job and both CD artifact names, and retained unsuccessful direct access
attempts. Earlier pending rows are superseded by this entry. Successful main
runs are on a3d6867 under Gradle9.8.0, after Dependabot #8; the original main
runs on ecf8dfd were cancelled. PR gate used9.7.1. Local wrapper adoption
f3e6b0b is context only, not a new local gate result. **A-6 remains unchanged:
a deliberately red GitHub run was not exercised.** Artifact downloads,
artifact-content review, hosted logs and fully uncached-build verification
are not claimed by QA. No threshold or coverage gap changed.

## Re-check 01-audit-read — candidate 7ac8af56ed04c27bbefbd416b3976c544d2f274a

No merged coverage shortfall or exclusion: 494/494 lines and 194/194 branches.
Per-suite informational misses: unit56 lines/6 branches, functional37
lines/32 branches; the complementary suite covers every one.

QA-AUD-01 / CR-01 is **fixed on this candidate**, independently observed with
the installed jar: either explicit Tomcat remote-IP or protocol-header
setting refuses plain and forged GET/HEAD403 with no stored canary.
Default/empty-setting loopback200, forwarded403, and native/framework403
controls also observed. Prior35590f0 item1 remains rejected; no waiver was
used for that defect. Fresh regression has two real-Tomcat functional
invocations and three unit methods. Evidence: proof/qa-recheck-7ac8af5/,
docs/qa/01-audit-read/findings.md re-check.

Qualified AC-17 result unchanged in scope: all25 shipped f6dd29e files
verified byte-equal to Git blobs; 155 tests,153 pass and only the exact two
enumeration failures accepted by lead grant428e9e1, transition1156. Their
candidate versions pass. This grant permits the new audit path/operation;
it does not authorize any behavioural regression.

Observation limits and compensating checks:
- Remote peer/IPv6 tests use the SPEC-authorized external Servlet wrapper.
  These establish peer/header inputs seen by the service, not an actual
  remote TCP-client boundary. Real installed Tomcat separately verifies all
  known rewriting modes. Headerless local relays remain indistinguishable
  from the Operator and prohibited or required to add a forwarding header.
- The guard mirrors Boot4.1.1's RemoteIpValve trigger list. A future Boot
  upgrade must revisit new triggers; known-trigger real-container cases
  cannot prove future configuration semantics.
- JDBC seeding, a held transaction and an actual SQLException are external
  QA controls, documented in QaLauncher.java. No natural disk/H2 crash or
  concurrent writer stress is claimed. Exact row comparisons prove the
  held-write/read-failure outcomes; no product/test edits.
- Capture setup error: QA added logging.file.name without an ECS file
  encoder, so the first sink was plain text although the default console
  was JSON. Original captures are retained under plain-file-captures/.
  QA repeated observability/privacy and installed two-page/identical
  forwarded-page exchange with logging.structured.format.file=ecs, leaving
  the default console unchanged. All23 repeated requests correlate to
  24 JSON events in both console and file. Earlier235 captures are not
  claimed to have retained JSON correlation; whole-run privacy checked in
  both capture sets. No AC remains unverified because of this setup error.
- Security item12 is downstream. The existing lead sequencing obligation
  qitem-20261003194346-b74b8081 returns the corrected independent review
  before acceptance. QA does not judge an absent security record.


## 03-dogfood-fix — candidate 4fe70427bd0d182e886d6a19b217daa1d9e39f5d

| Slice | Gap / qualification | Why | Compensating check | Status |
|---|---|---|---|---|
| 03-dogfood-fix @4fe7042 | None for merged coverage or required AC verification | Merged CSV508/508 lines194/194 branches; fresh 204/207 gate and unchanged baseline 203/202 both green | All nine ACs observed through installed effects, historical red runs or recorded document checks;354 copied report hashes checked | No exclusion, threshold change, failing AC or waiver; W2-01 fixed, not open |
| 03-dogfood-fix @4fe7042 | Unit-only441/508 lines188/194 branches; functional-only471/508 lines162/194 branches | Policy gates merged coverage; per-suite views informational | Independent copied unit/functional/all CSV sums | Merged 100% line/branch; no coverage gap |
| 03-dogfood-fix @4fe7042 | AC-3 full baseline diff, AC-4 full jar equality, AC-5 history, AC-7 gaps and AC-8 documents use recorded checks alongside functional regressions | A current functional test cannot establish prior Git history or close a living gap row; SPEC proof item 1 explicitly permits named tests or recorded checks | verify.py; empty committed-vs-live.diff; six full bodies compared in wire-comparison.json; two independently red historical runs; ADR ancestry/index, DESIGN/README read, QA-OPR-02 row closed | Required checks complete; no claim that document/history ACs are all JUnit-only |
| 03-dogfood-fix @4fe7042 | Default single disk path; a future second disk path would collapse series after tag removal | MeterFilter ignores path on every meter; only the two default disk gauges carry it | Baseline path-bearing scrape compared to candidate pathless numeric gauges; unit preserves unrelated tag | Configuration ceiling in ADR-0016; future extra disk path must add a non-sensitive distinguishing tag |
| 03-dogfood-fix @4fe7042 | Consumers selecting disk metrics by removed path must change that selector | Deliberate privacy correction extends installation-detail protection | Installed old tag selector baseline 200 / candidate 404; unfiltered free/total stay200 and numeric | Expected compatibility change documented in ADR-0016; AC-4 problem bodies unchanged |
| 03-dogfood-fix @4fe7042 | Some parameterized JUnit XML names identify arguments without source method | Gradle9.8/JUnit default display omits enclosing method in those XML cases | All 411 invocations green; all 233 source methods separately inventoried, every XML invocation attributed to method or green class parameterized group | Honest result-attribution boundary; no skipped or omitted source method |
| 03-dogfood-fix @4fe7042 | Inherited gradlew.bat worktree modification is line endings only | Wrapper bump and eol attributes; later main renormalisation outside candidate | Normalized bytes equal candidate blob; worktree-note.json; exact HEAD preserved | No product change or toolchain repair by QA |
| 03-dogfood-fix @4fe7042 | No new Docker, load, migration, Swagger rendering or natural expiry/midnight exercise | Slice fixes metadata and metric tagging; outside nine ACs | Original shipped suites run unchanged; installed retired410, mismatch422, malformed400, missing404 and create-budget429 independently checked | Explicit scope, no broader claim |

## 02-click-retention — QA2, a8fc8b6 (2026-10-03)

| Gap / scope | Why / evidence | Compensating check / disposition |
|---|---|---|
| Unit line 91.22% (447/490); branch 100% (168/168). Functional line 93.47% (458/490); branch 80.36% (135/168). | Suites exercise complementary paths; committed per-suite CSVs. | Merged canonical gate 490/490 lines and 168/168 branches, 100/100. No exclusion or lowered threshold. |
| Proof item 9 pending X′. | X is deliberately based before audit-read's cb148c4 merge under lead NOTES §2 21:42Z. | qitem-20261003221121-7686465a returns ancestry/Flyway judgment to QA on X′ before acceptance, after authorized range-diff and fresh gate. No ancestry PASS on X. |
| Daily/pause observations use a controlled service Clock. | SPEC AC-8/15 explicitly require the suite clock; the external harness leaves the real five-second executor and JDBC unchanged. | Autonomous no-trigger daily run observed in 3.651 real seconds; pause checked at both +60 service-second points with >5 real seconds per observation. Actual installed default startup and real environment binding verified separately. |
| Fault/concurrency probes use disposable JDBC/HMAC controls. | A one-shot physical prepareStatement exception, actual empty keyed-hash key and old-row transaction blocker make failures reproducible. | Actual HTTP handlers, writer and global DELETE inspected; 302/current row during pending DELETE; class-only WARN and correlated reduction reason; no product/test edit. 5,000 rows is a concurrency fixture, not a performance benchmark. |
| Large catch-up, Docker/PostgreSQL, SIGTERM while purge is running not checked here. | Outside these AC observations; design ADR-0018/0020 probes remain separate evidence. | No QA performance/shutdown-budget claim. A shutdown timeout returns without interrupting JDBC; this QA establishes only normal process cleanup. |
| QA harness corrections before clean run. | Quoted lower-case Flyway columns; ignored its null schema-marker version; corrected stats fields to date/clicks and replay status to specified 201. Sandbox denied child binding; direct default java was JDK11. | Interrupted attempts retained; JDK21 and scoped sandbox approval used; corrected --all run passed all 249 assertions. No product defect asserted from instrument errors. |

Every AC is functionally covered and independently observed by effect. No remaining
AC verification gap or merged coverage deficit on X; custody item 9 is an explicit
later judgment obligation, not an accepted gap or completed acceptance.

## 01-analytics-v2 — candidate ec466da8da4b1efde9d612c6c8692070cc6fc4b9

| Gap / qualification | Why | Compensating observation / disposition |
|---|---|---|
| NFR-L1 p95/p99 pending release_prep | This slice changes redirect identity work; the SPEC assigns percentile measurements to the release benchmark | Fresh v1 slow/failing/concurrent tests pass in both contexts. Actual physical two-second insert delay left all20 redirects below250ms; each200-concurrent sequence stored200 rows. No percentile claim. Proof12 returns after release_prep under a12a0e2 and qitem-20261003195138-8eb72ecb. |
| Unit87.24% lines506/580,96.12% branches198/206; functional94.14% lines546/580,83.01% branches171/206 | Suites exercise complementary paths; policy gates merged execution data | CSV merged580/580 lines206/206 branches100/100.372 copied report hashes checked. No exclusion, lowered threshold or merged coverage gap. |
| Controlled peers, UTC Clock and H2 insert trigger | Exact proxy, midnight, expiry, fault and delay inputs need repeatable isolated fixtures | Actual unchanged candidate classes/Tomcat/JDBC/Flyway, external Servlet peer wrapper, primary Clock and physical H2 trigger. Unmodified installed jar separately verifies actual-loopback trusted XFF4/3/1, privacy, metrics, API, audit and errors. No remote TCP-client boundary, natural midnight, real disk crash or load benchmark claimed. |
| Rule5 construction/security judgment remains downstream item11 | HTTP cannot prove that a hash is never joined or used across days, or that a salt is never persisted | Same client has different stored hashes on the two UTC days; no hash/address/canary appears in statistics, counters or either log sink. Existing salt regression tests pass. Design states the restrictions; independent security review must record them before item11 is judged and integration occurs (lead a12a0e2). |
| Original-suite replay setup and authorized expectations disclosed | Literal153/155 has only the two merged audit enumeration failures; old fixture clicks otherwise run under the earlier retention replay hold | Lead a12a0e2 carries audit grant428e9e1 forward for those two assertions. Authorized repeat155/155 changes only those enumerations and the two allowed per-day expectations; original/replayed hashes retained. No remaining AC-14 gap or blanket waiver. Purge-enabled candidate gate and independent inherited purge observation both pass. |
|41 parameterized source methods have XML group attribution | Default JUnit parameter displays omit the source method in some invocation names | All282 source/context methods mapped; all462 invocations green and attributed to a named method or its green class parameterized group. Five resilience methods execute under both default and trusted contexts. No unique per-method XML join asserted for those groups. |
| QA instrument corrections retained | Initial referrer expectation mistakenly included none; timing parser initially expected literal backslash-n; curl-I output file holds HEAD headers | Corrected top10 exactly matches v1. Initial20 slow responses retained and repeated on a new link with a corrected timing parser. Direct JDK HTTP probe observes zero HEAD body bytes. Raw captures, parser correction note and superseded manual assertion retained; no product defect inferred from these instrument errors. |

No uncovered merged line/branch, failed required AC, or excluded test remains at qa_check. Security and release benchmark items are pending evidence obligations, not accepted absent records. All713 curl responses plus three HEAD wire checks correlate in both console/file sinks; all apps stopped and the worktree left clean at the exact candidate.

### Release judgment — NFR-L1, candidate 50ad9c3

The pending percentile measurement above is closed for this candidate. The
release benchmark used the preserved jar with one service instance and a
disposable file-H2 database. Its open-loop redirect phase sustained 100
redirects/s for 60 s alongside 20 creates/s: 6,000 redirects, zero bad
responses, p95 3.7 ms and p99 9.5 ms. These meet NFR-L1's p95 ≤20 ms and p99
≤50 ms limits at the specified rate and duration. This is a result for this
run only; the load generator shared the host with the service, unrelated host
activity may have overlapped, and no capacity or container-performance claim
is made. The measured configuration trusts the loopback proxy and raises
both rate budgets to 1,000,000/minute, as recorded in the release manifest.
Percentiles come from the captured load-generator output; per-request client
latency samples were not retained, so QA independently reconciled completion
counts and the measurement method but did not recompute those quantiles.
The restored-seat recheck of `797f8fb` confirms its numbers and bounded
closure; these added qualifications preserve the measurement's scope.
Evidence: `missions/03-ambiguous-analytics/RELEASE.md` §3,
`missions/03-ambiguous-analytics/release/bench-50ad9c3.txt`, and
`missions/03-ambiguous-analytics/slices/01-analytics-v2/proof/qa-release-50ad9c3/audit.json`.


## 04-audit-columns — QA2, candidate 305f8045d45b19a9e3287d5fe3508af6e04db9a4

| Gap / qualification | Why | Compensating check / disposition |
|---|---|---|
| None for merged coverage or required AC verification | Fresh557/557 lines200/200 branches; all11 ACs observed | No exclusion, lowered threshold or failing acceptance criterion |
| Unit490/557 lines (87.97%),194/200 branches (97%); functional523/557 lines (93.90%),166/200 branches (83%) | The gate specifies merged coverage; suites complement one another | Independent CSV sums,366 copied hashes rechecked; merged100/100 |
| AC1/11 schema/rollback tests are unit, as explicitly allowed in SPEC proof item1/design§7 | There is no dedicated JUnit functional rollback method; QA's generic every-AC-functional convention cannot be represented as an invented test | Functional LinkUpgradeJourneyTest covers in-place application/rows; external real candidate jar/JDBC driver runs the literal rollback on a copy, verifies all schema/legacy values and restarts/reapplies V4. No unobserved AC or waiver requested |
| AC9 two baseline tests differ by migration pins | Grant132a884 explicitly permits stopping the retention tests at their intended V3 instead of latestV4 |57/59 baseline files byte-equal; exact two diffs retain every assertion; all437 inherited invocations green plus14 new |
| External Clock and JDBC controls; no natural24h wait or spontaneous audit-store failure | SPEC-controlled time and reproducible transaction failure | Real HTTP paths/repositories/DataSource; actual H2 CHECK rejects one audit INSERT; exact link/audit snapshots prove rollback. F6 expiry fixture moves one creation time back2d while stopped, then real shipped endpoint releases its key |
| Synthetic future audit row on the shipped directory | Verifies the explicit LEAST cap independently of the real-clock f6 writes | All legacy values preserved; future occurred_at unchanged, new row clocks equal and earlier; all genuine old rows backfill exact event time |
|41 source methods have only class-level parameterized XML attribution | JUnit display names omit method names for those arguments | All451 invocations green;271 source methods inventoried; nested classes resolved against their actual XML;230 individually attributed |
| QA setup interruptions retained | Child socket bind denied; H2 offsets/fractions needed normalization; initial limiter probe incorrectly used the exempt audit route | Scoped sandbox approval; fresh directories after correction; final94 requests/787 assertions all green. Only final runmissions/02-brownfield/slices/04-audit-columns/proof/qa-305f804/run-20261003T234116500899Z is claimed complete |
| PostgreSQL, Docker, large-directory startup cost not independently run | Outside required H2 behavioral verification; separate design32-control probe/timing retained | No QA performance/portability claim; normal shutdown observed for all7 processes |

The existing interim link/audit_log policy rows at GAPS lines84–85 close only on this slice's merge;
QA does not mark a future merge as done. All new and upgraded columns are observed here, and the
integrator can close that interim row when the exact candidate lands.

## 06-client-identity — QA2, candidate fb63a88a9b92c1fec97ba74686af1a2f30304160

| Gap / qualification | Why | Compensating check / disposition |
|---|---|---|
| None for merged coverage or required AC effects |584/584 lines206/206 branches; all15 ACs observed| No new exclusion, lower threshold or unobserved acceptance outcome |
| Unit87.33% lines96.12% branches; functional94.18% lines83.50% branches | Gate requires merged100/100; suites complement | CSV sums and378 copied report hashes verified |
| Controlled servlet peers and exact whitespace request-header values | Host cannot bind127.0.0.2; curl omits all-space headers; simulated peers do not prove container rewrite safety | External BEFORE-limiter filter supplies explicit Servlet inputs; all other headers and responses use real HTTP. Separate unmodified jars cover actual connection CR-01/cloud and blank settings; empty forwarding header wire trace is retained. No physical nonlocal client or whitespace wire-header claim |
|56 parameterized methods have class-only XML attribution | JUnit display names omit their method |326 mappings across74 reports;321 declarations plus five inherited methods. All590 invocations green; exact source/report inventory retained |
| First candidate gate had two401s from another listener | Pre-existing pgAdmin IPv4 listener occupied50898; failed Tomcat wildcard requests reached another service. No simultaneous listener snapshot while failed test alive | Original failures retained, same candidate isolated19-test class and fresh full590 gate passed. Owner process untouched. Lead records LOW localhost collision backlog outside slice; no QA/toolchain fix or waiver |
| Broad final metric comparator rejected two scrapes | Runtime values differ, baseline has extra health503 series and candidate an existing JVM concurrent-GC phase meter; neither is an AC-12 equality promise | Preserve original false comparator and all differing names/tags/values; scoped saved analysis checks business values and metric privacy. Health503 startup-poll attribution is an inference (raw readiness probe responses were not captured). All1017 non-metric HTTP/log pairs are exactly equal under Rule6 |
| Springdoc duration message293ms versus162ms | Existing API initialization measurement varies | Retain both INFO messages/one extra event; all other fields/events and live API body match. It is outside AC-14's named audit200 comparison, whose events match exactly; no log message normalization |
| External mutable Clock, JDBC and held writer | Frozen budgets/day, repeatable expiry and storage failures | Same external fixture with original and candidate production classes; physical H2 table rename causes failures, latch delays real JDBC writer; normal callbacks/data paths unchanged. Controls are not product endpoints or edited tests |
| Proof16 downstream clauses pending | Independent review and post-merge register/system description happen after qa_check | Lead transition1882 explicitly returns16 after review/merge/design-owner edits; QA judges only1–15/17 now. No premature acceptance or scope waiver |
| No Docker/PostgreSQL, NAT network, broad latency or fresh in-flight shutdown experiment | Outside this refactor's required outcomes | Actual H2/original databases, jar contexts and unchanged functional timing assertions verified. All own apps stopped; no new portability/capacity/shutdown guarantee |

### 06-client-identity post-handoff review finding (02:47Z)

Review2 independently reproduced a HIGH test-only race: the new AC-5 settledStats helper polls using a frozen create60 budget, receives429 and NPEs after170ms while its writer is held; after release a fresh peer sees correct3clicks/2uniques. QA did not delay this actual helper in its own checks (its external delay check settles before stats), so the prior green590 gate establishes its observed scheduling only. No product grouping failure or coverage waiver. Proof1/11 withdrawn; review2 owns formal failed routing, and a new assigned candidate must resolve it. Source, hashes and attribution: docs/qa/06-client-identity/review-polling-race.md.
