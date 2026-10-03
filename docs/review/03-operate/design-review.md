# 03-operate — design review

- Candidate: `d7fa6029168f74ae8b67efa6607dcf7a08296931`; SPEC: `b53372f5c25746e76e19cbf7e6401a23610ef7de`.
- Reviewer: `review2-agent@urlshort-factory` (Codex), 2026-10-03 UTC.
- Packet: `qitem-20261003091111-32efdbe2`; instance `01M40CPBYR97637QGHT57BNEBY`.
- **Verdict: FAIL — two HIGH findings (DR-01, DR-02); one MEDIUM (DR-03).**

## Context proof

The design adds a servlet rate limiter, operator health/metrics configuration,
and container/smoke behavior to the merged create/redirect service. The invariant
is that routing, charging, privacy and graceful-drain proof agree with the SPEC,
including its distinction between pre-acceptance refusal and post-acceptance
failure. No persistence schema changes belong to this slice.

Confidence: high in the scope, filter/health/metric behavior observed on the
current Tomcat stack, and the reproduced findings. The limiter itself is still
a design; full concurrency, container behavior and integrated analytics remain
implementation/release work. Reviewed architecture guidance §3–8, database
guidance §8 (no new schema), the SPEC, ADRs and the lead's two test-side grants.

## Candidate and file ledger

Scope is the twelve-file design delivery introduced at `dca64fb`, including its
`aea4ea3` and `d7fa602` refinements. All twelve current files were byte-compared
with `git show d7fa602:<path>` and matched when checked. Product/build inputs
also have no diff from that candidate. Other seats' later mission notes and
analytics rework are outside this assignment.

| Changed file | Review result |
|---|---|
| `missions/01-greenfield-core/slices/03-operate/design.md` | Entire design and all AC reachability rows read; DR-01/02/03 |
| `missions/01-greenfield-core/slices/03-operate/design-probe/OperateProbe.java` | Entire probe read and rerun; honest stand-ins for 429 and DB failure, double arithmetic rather than proposed long implementation; drain measurements reproduce DR-02 |
| `missions/01-greenfield-core/slices/03-operate/design-probe/operate-probe.gradle` | Read and run offline; Prometheus dependencies resolve from the warmed cache |
| `missions/01-greenfield-core/slices/03-operate/design-probe/output.txt` | All structured events parsed; case results inspected and compared with rerun; DR-02 is present in the author's own evidence |
| `docs/adr/0004-structured-ecs-logs-no-client-pii.md` | Amendment read; parser suppression leaves the DR-01 resource-path logger |
| `docs/adr/0014-rate-limit-filter-gcra.md` | Read; filter, buckets and charge rules coherent; DR-03 release timing overstated |
| `docs/adr/0015-client-identity-trusted-proxies.md` | Read; explicit default-deny proxy trust and exact-text configuration; analytics address alignment remains named backlog |
| `docs/adr/0016-metrics-and-health-exposure.md` | Read; metric and health choices supported by rerun; logging mitigation needs DR-01 |
| `docs/adr/0017-container-hardening-and-shutdown.md` | Read; posture and timeouts specified, but DR-02 unresolved |
| `docs/DESIGN.md` | All changed contracts, component entries, conventions, ADR index and diagram read; carry DR-01/02/03 corrections into shared documentation |
| `docs/diagrams/container.mmd` | Entire changed diagram read; filter order and operational surfaces agree with design |
| `docs/diagrams/ratelimit-sequence.mmd` | Entire diagram read; charging, rejection, metrics and logging sequence agree |

12 changed files, 12 reviewed. The granted manifest and grant packet were read
as context: the default-property test and clock-reset conditions are explicitly
carried into the test strategy. No ungranted production territory was added.

## Findings

The design source below is `missions/01-greenfield-core/slices/03-operate/design.md`
at the candidate. Evidence paths are relative to `docs/review/03-operate/`.

| Id | Severity | File:line | Evidence and consequence | Required change |
|---|---|---|---|---|
| DR-01 | HIGH | `design.md:41`, `design.md:267`; ADR-0016 parser-error decision | With the proposed `Http11Processor=warn`, `POST /actuator/../pii-canary-10.77.77.77` returns 404 but `ResourceHandlerUtils` emits a WARN containing the whole submitted path and raw-IP canary (`proof/path-review.txt:72`). This is inside the application, so A-17's pre-application exclusion does not apply; the path is exempt from limiting as the design intends. The threat table's no-client-values claim and inherited logging/privacy rules are not met. | Add the resource-handler logger to the privacy mitigation and a real-server canary case. A one-property control (`logging.level.org.springframework.web.servlet.resource.ResourceHandlerUtils=error`) suppressed this WARN while preserving the same HTTP result; `proof/path-review-quiet.txt` contains zero structured log events with the canary. Update design, ADR-0004/0016 and shared logging contract. |
| DR-02 | HIGH | `design.md:423`, `design.md:273`; `docs/adr/0017-container-hardening-and-shutdown.md:43` | The design accepts a measured reset after successful TCP connection establishment, whereas SPEC AC-25 and rule 13 still require zero failures after acceptance. Independent rerun produced four connection resets in D1 and four in D2 (`proof/design-probe-rerun.txt:173`, `:204`); the known held request completed and the later probe was refused. All ten paced cycles had zero failures, but choosing a lower rate does not reconcile the unconditional contract with the admitted race. The requirement's own self-check says a backlog-race connection counts as failure. | Resolve this before plan-lock: either provide a design that meets the existing predicate, or route an evidence-backed contract decision through the lead/human authority and have the requirements owner record exactly what constitutes acceptance, which losses are permitted and how they are counted. Update the SPEC/design/proof together if the decision changes the contract. Do not call a reset merely a recorded residual while retaining a zero-failure claim. A custom networking layer is not requested; the measured distinction between a kernel-established connection and an application-dispatched request is the decision to resolve. |
| DR-03 | MEDIUM | `design.md:36`; `docs/adr/0014-rate-limit-filter-gcra.md:45` | Cleanup is triggered by a limiter call with a one-second gate. If clients make a burst and no further limited request arrives, advancing time by 62 seconds executes no sweep: the entries remain. The claimed maximum 61-second entry lifetime and bound solely by clients in the last 61 seconds therefore do not follow. The release test itself needs one later request to trigger removal (`design.md:296`). This is a documentation/design-boundary issue, not evidence of unbounded growth while idle. | State the actual opportunistic cleanup guarantee and idle retention in the design/ADR/test contract; if a strict elapsed-time deletion guarantee is required, name its driver. Prefer clarifying the lazy sweep over adding machinery without a requirement. |

## Verification and reproduction

Commands run from the repository root, using the pinned JDK/cache:
Trailing horizontal whitespace in the rerun's text capture was removed before
commit; its line count and substantive output are unchanged.

```sh
scripts/gw --log docs/review/03-operate/proof/design-baseline-check.txt --offline check
scripts/gw --log docs/review/03-operate/proof/design-probe-rerun.txt --offline -I missions/01-greenfield-core/slices/03-operate/design-probe/operate-probe.gradle designOperateProbe
scripts/gw --log docs/review/03-operate/proof/path-review.txt --offline -I docs/review/03-operate/proof/path-review.gradle pathReviewProbe
scripts/gw --log docs/review/03-operate/proof/path-review-quiet.txt --offline -I docs/review/03-operate/proof/path-review.gradle -PquietPaths pathReviewProbe
```

- Baseline `check`: exit 0; all 14 tasks UP-TO-DATE. This is a baseline gate
  invocation, not fresh execution of the suites or proof of the future limiter.
- Authored probe rerun: exit 0, actual Java probe executed. Its exit status is
  not an all-cases pass: the emitted case results expose the shutdown failures.
  Default bucket arithmetic, 429 body/correlation, route-template metric tags,
  Prometheus, healthy/down/recovered readiness and liveness, and parser-log
  suppression matched the documented observations. The minimal filter does not
  implement the rejection counter or complete limiter; those remain to be built.
- D1/D2: held POST returned 201 after 515/514 ms, probe connection at +500 ms
  refused, context closed in 567/554 ms; **4/4 failures after acceptance**.
  P1–P10: zero post-acceptance failures in each observed paced stop. Both results
  are retained, without upgrading ten clean samples into a guarantee.
- Path probe: 19 raw HTTP paths against real Tomcat/MVC, observing the exact
  `UrlPathHelper` classifier proposed by the design. All five paths that
  actually created links were classified CREATE; percent-encoded `/api`, literal
  matrix parameters and dot-segment cases did not demonstrate a budget bypass.
  The review-only filter reports classification in diagnostic headers and does
  not claim to be the completed limiter.
- DR-01 before/after: parse only JSON log events, excluding the probe's own
  diagnostic `PATH` printouts. The canary appears in **one structured event**
  before the additional logger setting and **zero** after it. The request still
  returns 404 in both runs; the test input and lookup diagnostic remain visible
  in each evidence file by design. The leaking logger is the resource handler,
  not Tomcat's parser or the review filter.
- DR-03 is a control-flow counterexample to the specified sweep trigger, not a
  runtime claim about an implementation that does not exist yet.

## Reachability and scope assessment

All 28 ACs have named components or release checks. The filter is before body
validation, after request correlation and observation, and uses server-owned
429 fields. The GCRA plan and fixed-clock suite cover default budgets, refill,
independence and truthful retry values. Proxy trust is explicit; no request
wrapper silently changes analytics identity. Metric names and health failure
shapes were observed. Actuator's 503 status body is the SPEC's explicit health
contract; no extra ProblemDetail requirement is imposed on it.

No migration is introduced; rollback of schema/data is not applicable. The
Prometheus registry serves an allocated requirement. The design has no
unnecessary new layer or dependency identified by this review. Container
privilege, mounts and loopback publication are explicit; actual Docker and
smoke/bench execution remain release proof obligations, as the SPEC says.

The shared API-document serialization constraint and both test grants are
preserved. The independent `02-analytics` review owns its writer-drain fixes;
this review does not accept those on its behalf. Integrating that bounded
drain into the 20-second container stop budget remains a named dependency.

## Disposition and self-check

Return to design for DR-01/DR-02; request DR-03 in passing. DR-02 may need a
durable authority decision rather than more implementation. No policy exception
or requirement narrowing has been granted by this reviewer.

- Exact candidate and all twelve delivery files verified; source/test/build
  files untouched. Only review reports/probes/evidence were written.
- Every changed file recorded; complete probe source read; all 416 structured
  events in both the author's evidence and independent rerun parsed, and case
  results inspected. The superseded 11.6 MB historical probe dump was not the
  candidate artifact and was not rereviewed.
- Findings have precise source, consequence, evidence and required change.
  Both HIGHs have live reproduction; the MEDIUM is explicitly a design claim.
- All probe processes finished. No container, real limiter implementation,
  advisory scan or performance target is claimed verified here.
- Ledger row appended; peer reviewer notified to serialize that write.
