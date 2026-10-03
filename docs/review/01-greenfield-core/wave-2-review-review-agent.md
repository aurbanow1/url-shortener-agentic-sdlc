# Mission 01 accumulated wave review — source and behavior

- Range: `7636264..8e9c065589e53385f60d6be3ddbc3683260285df` on `main`, covering w1 and w2 as assigned by the mission packet.
- Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03 UTC.
- Packet: `qitem-20261003135957-0f6e0c8b`; instance `01M3ZXEXAMS945ZS0QZ26KZK1V`.
- **Verdict: PASS with non-blocking carry-overs. No MUST-FIX or HIGH; one open MEDIUM and four LOW items below.** Both requested vantages are recorded. Release readiness is not asserted.

## Context and coverage proof

Doghouse: create and retire short links, redirect Visitors, count privacy-reduced clicks, expose aggregate statistics, and give the Operator rate limiting, health, metrics and a deployable single-node service. The merged structure delivers that scope without aliases, expiry, audit read, retention or speculative analytics. Confidence: 97/100 for the integrated application boundary; container effects and latency at the specified offered rates remain release-owned observations.

Read the accumulated product diff and source against the mission and three slice contracts, current architecture, accepted ADRs and the lead's shutdown/clock decisions. The range has **82 product/build/test/API files**, each with a verdict in [wave-2-file-ledger.md](wave-2-file-ledger.md). Twenty-five inherited test files retain their earlier full source reviews through exact Git blob identity; changed inherited tests were checked against those full reads and the complete delta. New operations tests and current production/configuration files were read. Every suite ran again on the combined product tree.

The unfiltered range also contains historical evidence, generated reports, governance changes, this reviewer's earlier reviews and separately assigned mission-02/03 documents: **1,442 paths** in total. [wave-2-range-audit.json](wave-2-range-audit.json) inventories every path, its target blob and its scope; it does not pretend that 32 MB of historical/generated artifacts received 1,442 new independent judgments. Prior findings remain settled except the explicit carry-overs. My own historical review artifacts are evidence of earlier source reads, not artifacts I am independently approving here.

The 82-file product tree at the merge is byte-identical to accepted `03-operate` candidate `1c8b2cff20ad8b73a060bc817c8d0011782f876f`, which includes the earlier slices. The root checkout's later documentation commits change none of those product inputs. This establishes the target of the fresh checks despite the main checkout advancing through documentation work.

## Verification and cross-slice claims

| Claim | Source and independent verification | Result |
|---|---|---|
| Create, idempotency, read and retire remain one audited transaction | `LinkService`, `LinkRepository`, `AuditLog`, V1; fresh functional failures/concurrency/regression tests. Own packaged-jar journey created once, replayed the same representation, then retired; offline H2 query found exactly one `link.create` and one `link.retire` row | PASS |
| A redirect's stored target and no-store response survive analytics and limiting | `RedirectController` → `ClickRecorder`; own real requests preserved exact `Location` and `Cache-Control`. Two admitted GETs plus one from another forwarded client produced three clicks; HEAD and rejected GET produced none; retired redirect returned 410 and statistics stayed unchanged | PASS |
| Rate limiting precedes validation/body consumption and keeps clients/budgets distinct | Filter orders and decoded-path classification inspected; fresh AC-1–AC-12 tests. Own jar with 4/3 budgets: invalid create consumed budget, oversized request over the limit returned safe 429, independent forwarded client still redirected; rejection counters each incremented once | PASS |
| Async clicks retain privacy and statistics consistency | Reduction, salt selection/disposal, bounded queue/report ownership and single grouped query inspected; unchanged accepted analytics source; fresh slow/failing/concurrent-store and salt tests. Own jar returned exactly three clicks, one reduced referrer origin and aggregate-only statistics | PASS, proxy identity limitation below |
| Error/log/API contracts remain joined | Advice, limiter writer and request-id filter inspected. All 17 captured journey requests had one matching completion with the actual status and a server-issued id; private canaries absent from the complete log. Live OpenAPI equals the committed JSON and every operation has 429 | PASS for runtime and document equality; schema defect retained |
| Coverage and traceability survive integration | Fresh `scripts/gw --log docs/review/01-greenfield-core/wave-2-check-8e9c065.txt --offline check --rerun-tasks`: 14 tasks executed, **165 unit / 155 functional**, no failures/errors/skips; Javadoc and coverage gate passed. All **186** source methods match QA's inventory, current traceability rows and fresh Gradle reports. All **321** current QA coverage copies match their recorded SHA256 | PASS |
| Shutdown and limiter decisions are implemented | R0 requires curl completion, 2xx/3xx and elapsed ≤10 s; drain reconciles dispatched ids against complete responses and reports backlog losses separately. RateLimiter reads time inside per-client compute, preserves `max(tat, now)`, and restores sweep scheduling after rollback. Fresh regressions pass; review2's original independent controls remain applicable because product blobs are identical | PASS against recorded decisions; final installed drain remains release-owned |
| Structure stays within the planned scale | Feature boundaries, filter order, one audit writer, one click writer, two versioned migrations and the existing Spring facilities agree across slices. The added Prometheus registry serves the required scrape surface | PASS; second vantage concurs |

Evidence: [gate summary](wave-2-check-summary.json), [QA audit](wave-2-qa-audit.json), [HTTP captures](wave-2-http-8e9c065.json), [journey summary](wave-2-http-summary-8e9c065.json), [logs](wave-2-http-log-8e9c065.jsonl), [storage](wave-2-storage-8e9c065.txt), and the reproducible [probe](wave-2-http-probe.py). The probe started an unmodified jar on a temporary localhost port with a disposable H2 file, then stopped it before inspecting storage; it changed no product code. The initial sandbox attempt could not bind a port; the authorized retry produced the recorded observations.

JaCoCo's class-summed CSV gives 443/443 lines; the XML's distinct source-line aggregate gives 442/442 (`RequestBodyLimitFilter.java:39` is shared by the filter and its anonymous wrapper class: 28 class-summed versus 27 source lines). Both have zero missed lines and 162/162 branches. Per-suite percentages remain informational; no exclusion or lowered gate was introduced. The first audit parser incorrectly treated parameter display names and release-check rows as Java methods; the final audit uses source declarations, XML named methods plus Gradle's parameterized-method hierarchy, and Java-only traceability rows.

## Findings and forward disposition

| Id / existing reference | Severity | Location | Evidence and consequence | Disposition / required change |
|---|---|---|---|---|
| W2-01 / create CR-01 / W2D-01 | MEDIUM | `docs/api/openapi.json:81`, `src/main/java/dev/urlshort/web/OpenApiConfig.java:36` | Live validation returns top-level `errors[{field,rule,message}]`; the schema omits it and instead describes a nested `properties` object. Own `validation-errors` and `openapi` captures reproduce the mismatch. Equality with generated output does not establish an accurate client contract | **JUDGMENT-GAP**, retained. Lead routes a small forward fix for the existing OpenAPI component/customizer and a schema-shape assertion, then regeneration. No new error framework required |
| W2-02 / A-9 / W2D-03 | LOW | `src/main/java/dev/urlshort/click/ClickRecorder.java:86`, `src/main/java/dev/urlshort/web/RateLimitFilter.java:70` | Two forwarded clients behind the configured loopback proxy had independent limiter budgets, but three stored clicks had only one distinct hash. Current totals/referrers do not consume hashes; the default has no trusted proxy | **CONTEXT-GAP at the cross-slice boundary**, deliberately deferred by analytics A-9 and ADR-0015, not a newly violated AC. Preserve this limitation in release notes. Lead routes alignment before configured-proxy use or any unique-client/hash consumer; do not claim the old stored hashes can be repaired |
| W2-03 / QA-OPR-02 / W2D-04 | LOW | `src/main/resources/application.properties:42`; HTTP capture `prometheus` | Fresh scrape includes the working-directory disk-meter tag. No client/code/URL canary is present. Anonymous operator surface remains within the decided loopback exposure | **JUDGMENT-GAP**, retained. Remove the disk meter through the smallest supported Boot setting when that configuration is next touched, with a scrape assertion; otherwise keep the disclosed operator-scope limitation |
| W2-04 / QA-OPR-03 / W2D-07 | LOW | `scripts/smoke.sh:68` | The timestamp helper requires Perl/Time::HiRes and a supported locale. QA/review2's same-source controls reproduce failure under inherited macOS C.UTF-8 and pass under C. I read those records; I did not rerun that host failure | **JUDGMENT-GAP**, retained. Release verifies and documents its actual host prerequisites; a later script portability fix may remove the assumption. It must not turn a setup failure into a passing R0 verdict |
| W2-05 / W2D-05 | LOW | `src/main/java/dev/urlshort/click/ClickRecorder.java:92`; `src/test/java/dev/urlshort/click/ClickRecorderTest.java:128` | A reduction/HMAC failure reports reason `rejected`, while design §5 associates that reason with a full/closed queue. The existing induced GeneralSecurityException test freshly passed and asserts exactly that reason. One correlated WARN and fail-open behavior still hold | **CONTEXT-GAP**, narrow reason vocabulary omitted reduction failure. Widen the operator-facing definition or give the failure a distinct static reason when this area is next touched; do not add a new logging abstraction |

No blocking repair is needed before release preparation. W2-01 should be fixed in the next suitable small slice; W2-02 is conditional on its explicit deployment/analytics trigger; the remaining LOWs can be fixed in passing or remain clearly owned backlog. All are routed through the lead in [wave-2-forward-fixes.md](wave-2-forward-fixes.md), queue item `qitem-20261003142206-ff6ad817`, not assigned as direct code changes by this reviewer.

## Second vantage and resolved item

The requested structural review is [wave-2-review-design-agent.md](wave-2-review-design-agent.md), commit `dad0a5a2b18facd85cafac67624e4459f0548064`, returned through `qitem-20261003140048-4a960682` (done). It independently inspected structure/drift and named its own design misses. No roundtable or new gate was added.

W2D-02, MEDIUM/JUDGMENT-GAP, is **fixed in passing** by the design author in that commit: current merged-state preamble, accepted ADR-0004 amendment statuses, recorded backward-clock policy and shared-clock testing convention. I read the exact three-document diff and checked it against the existing decisions and code. This changes documentation, not the reviewed product. It is resolved and excluded from the open finding counts. W2D-06 is a recorded testing convention, not a defect. The second vantage's container sentence is a statement of the configured shape; neither vantage claims the pending installed-container proof.

## Ponytail review

Lean already. Ship.

The feature boundaries and platform choices earn their size. The click ownership states protect an observed shutdown race; the client limiter needs no new dependency; the smoke uses curl's framing/completeness checks. No speculative layer or replacement abstraction is requested.

## Release obligations and limits

Keep the integration record's existing release work separate from wave findings: container AC-21–AC-28, non-root/read-only/binding/persistence inspections, final drain with observed boundary-loss count and achieved load rate, secret/advisory scans, NFR-L1/L2 at the **actual required 100/20 req/s**, and NFR-L3's isolated added-p95 measurement or honest gap. The existing closed-loop bench's roughly 82/16 req/s cannot prove the numeric latency targets; its printed shortfall is already explicit. No network advisory query, container execution or new benchmark was performed by this review.

Open downstream proof judgments and delivery stamps stay with their authored owners. QA2 has since recorded item 11's review evidence; release-owned items remain pending. This handoff authorizes the next workflow step, not publication, a release tag, or ship approval.

## Self-check

Assigned range and exact product identity checked; all 82 product-file verdicts recorded with retained-review provenance where applicable; fresh offline gate, 17-request HTTP journey, log/storage inspection and coverage/traceability audit completed; both vantages recorded; each open item has severity, evidence and a forward disposition. No product, test, SPEC or design edits by this reviewer. Ledger appended; `handoff` to release preparation after routing the forward items to the lead.
