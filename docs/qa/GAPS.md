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
| 03-operate @1c8b2cf | Anonymous disk-gauge working-directory path | Existing LOW observation, not an AC-19 prohibited value | Fresh scrape inspected; all prohibited client/code/URL canaries absent; review retained current loopback scope | QA-OPR-02 LOW retained |
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
| click @8e9c065 | Missing created_at, updated_at, created_by and updated_by; clicked_at is the event timestamp, not the row-audit timestamp | Human audit-column policy applies to event tables; the Visitor's actor is anonymous | 02-brownfield / 02-click-retention, w1: **V3 expand migration**, SPEC 32b1ae2 AC-16. Preserve existing columns, backfill all four new columns, verify timezone-aware non-null timestamps with updated_at = created_at for insert-only rows and actor columns anonymous; preserve clicked_at and privacy reductions | **OPEN until 02-click-retention merges** |
| user_agent_class @8e9c065 | Missing created_at, updated_at, created_by and updated_by | Human audit-column policy applies to reference tables; seeded rows have actor system | 02-brownfield / 02-click-retention, w1: **V3 expand migration**, SPEC 32b1ae2 AC-16. Preserve tokens/constraints, backfill all four new columns, verify timezone-aware non-null timestamps and created_by = updated_by = system; updated_at = created_at for existing seed rows | **OPEN until 02-click-retention merges** |
| link @8e9c065 | created_at already exists (timezone-aware, non-null, without a database default); missing updated_at, created_by and updated_by | Human policy requires complete row-audit columns and maintenance on every write | 02-brownfield / 04-audit-columns, w2: **expand migration using the next free Flyway number after V3**. Retain existing created_at values, supply the policy's insert default/maintenance, backfill missing columns, verify actor columns and updated_at on subsequent link writes; existing links retain their meaning | **OPEN until 04-audit-columns merges** |
| audit_log @8e9c065 | Missing created_at, updated_at, created_by and updated_by; existing occurred_at/actor remain domain-event fields | Human audit-column policy applies to append-only audit tables too | 02-brownfield / 04-audit-columns, w2: **expand migration using the next free Flyway number after V3**. Preserve existing audit fields, backfill row-audit columns, verify inserts set updated_at = created_at and maintain actor columns; **audit_log stays append-only, with no application UPDATE/DELETE path** | **OPEN until 04-audit-columns merges** |

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
| AC-13 | First GitHub PR gate and main CD run, artifact uploads, action/cache/wrapper-validation and Dependabot execution not observed | Agents cannot push; candidate-local checks and the contracted builder network captures are complete. No GitHub URLs fabricated. Operator records run URLs/conclusions/artifact presence in PROOF.md after the human pushes | SPEC A-5 explicit accepted pending state (D2/D13); human/operator follow-up |
| AC-2 / A-6; BR-6 | A deliberately failing GitHub run was not exercised; failure upload/retention checked as configuration only | Parsed gate has no -x, continue-on-error or masked exit; explicit bash; reports/smoke upload always(),30 days. Local real check exit0 is captured | SPEC A-6 permits configuration evidence, not a claim of a red GitHub run |
| AC-12 lint | Native actionlint absent; independent YAML1.2 parse plus parsed/source AC checks used | All three candidate files parse without errors or warnings. Builder's separate actionlint1.7.12 capture and negative control retained. QA did not rerun that container | SPEC AC-12 expressly allows parsing when actionlint is not installed |
| AC-12 image | Initial --pull --no-cache attempt stopped at Dockerfile step5; a wholly uncached image build is not claimed | Supplemental builder --pull capture on the same SHA completed with both base images pulled, all15 steps and exit0. Steps5–7 actually ran, including bootJar; other layers cached. QA read both pull digests and success; C1 image/pin attachments committed ad79bb4 | AC-12 satisfied without waiver: cache is allowed by SPEC. A fully uncached/hosted-runner build remains unobserved; AC-13 pending |
| AC-5 / runner environment | GitHub's env.JAVA_HOME handoff and hosted Linux shell/runner not executed locally | Own installed jar smoke used local JDK21 default; parsed cd env passes JAVA_HOME; exact script unchanged. Local journey/log/shutdown observed on127.0.0.1:18105 | Actual GitHub effects remain in AC-13 pending; no clean-runner execution claim |
| Configuration-only traceability | Workflow ACs are file/local-command/run checks, not new product HTTP tests | SPEC explicitly selects this evidence strategy; .github-only diff leaves inherited product tests unchanged. All320 invocation names map to NFR-M1/rule1 and earlier product AC tables | Proof contract5 requires the13AC/7rule check table; no new product tests or test edits |

Instrument corrections: the first QA parser invocation used the wrong local
OpenRig module location and did not parse; the corrected installed path parsed
all three files. The first report audit reached copied coverage but its ps
check was denied by the sandbox; the permitted rerun verified all321 report
hashes, PID absence and refused port. Neither supplies a product finding.
Shared GAPS appends can change earlier receipts' evidence hashes; no judgment
on an unassigned closed mission is made here.
