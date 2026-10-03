# Design review — 01-create-redirect

- Candidate: `0aaab2fe1c77c81f83d25400dd7c07cfb7355abe`.
- SPEC: `0acbc9d4898a04df73cf61bab20570a75484629a`, unchanged at the candidate.
- Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03.
- Packet: `qitem-20261003055347-01d2d207`; instance `01M40149S2BGCAKVK83B6VCNKG`.
- **Verdict: FAIL — two HIGH findings, DR-01 and DR-02.** Two MEDIUM items
  should be fixed in passing with this revision; neither adds a separate gate.

## Context proof

This is the first persistent create/read/retire/redirect journey. Its invariants
are verbatim validated targets, unique opaque codes, a 24-hour idempotency
binding that survives rejection, atomic append-only audit, and private,
correlated error/log output. Confidence: 95% in the contract and boundaries,
90% in buildability after the findings below. The feature implementation does
not exist yet; repository mapping, audit rollback and the final OpenAPI
annotation output remain builder/QA obligations.

Read AGENTS.md, the approved SPEC and human constraints, the slice territory,
docs/DESIGN.md, architecture §3–§8, databases §8 and review guidance. The
amended mission leaves this slice and its human plan-lock intact. No brownfield
impact analysis is required. No new dependency or speculative layer is proposed;
the existing platform advice, JDBC and one audit writer are proportionate.

The main checkout was at `b52adb36ac01e53f1c325c48b4d3b3a23ca0f339`; the only
tracked difference after the candidate was mission NOTES. All reviewed design
files and product/build inputs matched the candidate; untracked rig-managed
files were untouched. This documentary review uses the main checkout by the
repository's document rule, not the implementation worktree.

## Coverage ledger

Every changed file in `0aaab2f^..0aaab2f` was read: **15/15**. The unchanged
probe init script, shipped configuration/filter and SPEC were also read as
supporting context.

| Changed file | Verdict |
|---|---|
| `docs/DESIGN.md` | DR-01/DR-02 propagate into the error/log contract; otherwise coherent with the slice |
| `docs/adr/0002-problem-details-via-platform-handler.md` | DR-01/DR-02: raw throwable logging and the narrower no-echo interpretation need correction |
| `docs/adr/0005-persistence-h2-flyway-spring-data-jdbc.md` | Pass: ownership, targeted updates, one clock and portability limit explicit |
| `docs/adr/0006-redirect-302-no-store.md` | Pass: stored String header, 302 and no-store |
| `docs/adr/0007-short-code-generation.md` | Pass: SecureRandom, reserved segment, unique constraint; collision failure explicitly bounded |
| `docs/adr/0008-audit-record-same-transaction.md` | Pass: insert-only writer, same transaction, no client identity/key |
| `docs/adr/0009-idempotency-key-binding.md` | Pass structurally: replay/mismatch/expiry preserve binding rules; DR-01 affects the declared race failure path's logging |
| `docs/adr/0010-committed-openapi-document.md` | Pass: one export/check mechanism, fixed server, examples and typed errors |
| `docs/diagrams/container.mmd` | Pass: matches the proposed boundaries |
| `docs/diagrams/create-link-sequence.mmd` | Pass: validation, replay, mismatch, insert and audit transaction represented |
| `docs/diagrams/erd.mmd` | Pass: matches DDL and deliberate absence of audit FK |
| `docs/diagrams/redirect-sequence.mmd` | Pass: 302/404/410 and guarded one-way retirement |
| `missions/01-greenfield-core/slices/01-create-redirect/design-probe/MechanismProbe.java` | Useful mechanism evidence; reused unchanged in independent probe; DR-01/DR-02 reproduced through its advice |
| `missions/01-greenfield-core/slices/01-create-redirect/design-probe/output.txt` | Read all recorded cases; evidence is scoped to mechanisms, not full feature acceptance |
| `missions/01-greenfield-core/slices/01-create-redirect/design.md` | DR-01/DR-02 HIGH; DR-03/DR-04 MEDIUM; remaining structural checks pass |

## Findings

Locations below refer to the candidate; `design.md` and `SPEC.md` are under
`missions/01-greenfield-core/slices/01-create-redirect/`.

| Id | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| DR-01 | HIGH | `design.md:424`, `:476`; `docs/adr/0002-problem-details-via-platform-handler.md:72` | The design logs the original throwable and accepts leaked driver values until 03-operate. The independent probe passed `Idempotency-Key: review-key-canary-68fd` to two parameterised inserts under a UNIQUE constraint. The candidate's exact advice emitted that key in both ECS `error.message` and `error.stack_trace` (probe output:51), correlated to the 500 response (:52). This is the exception path explicitly chosen for a same-key race; SPEC rule 10 (:287) forbids keys/URLs/header values in every log event, including 500s. | Define a safe error log now: fixed event, request id, and only deliberately safe metadata; omit raw throwable/message/cause text, or prove any sanitised renderer removes values from the whole exception chain. Remove the deferred privacy exception from the threat model/self-check and align ADR/system docs. Add a canary check on the database-failure path, beyond AC-27's successful-create cases. |
| DR-02 | HIGH | `design.md:201`, `:210`; `docs/adr/0002-problem-details-via-platform-handler.md:68` | The design unilaterally accepts reflected path/media-type values despite SPEC rule 8 (:285). A request with `Content-Type: text/plain; note=review-media-canary-7d9e` received 415 with that submitted canary in `detail` (probe output:50). This remains a violation even under the design's proposed body/header-only interpretation. The author's P6 also demonstrates the arbitrary unknown segment in `instance` and `detail`. JSON escaping prevents injection but does not satisfy the no-echo contract. | Keep the existing no-echo contract: scrub client-derived detail and instance content for framework as well as domain errors, preserving status/media type, safe headers and typed validation errors. Use safe static wording and a non-client-derived instance when needed; add header/path canaries. Align the ADR and system design. No new human decision is necessary to implement the already-approved requirement. |
| DR-03 | MEDIUM | `design.md:524` and `:40` | Both the production and functional configurations declare a bean named `clock`. With the proposed `@Primary` on the functional method, Boot still rejects registration with `BeanDefinitionOverrideException` before type selection (probe output:61–62). A control changing only the functional method name to `functionalClock` starts and selects the primary functional clock (:66). | Name the functional bean distinctly (retaining `@Primary`) or document another explicit replacement mechanism. Verify the suite context with both configurations. Fix in passing; the clock architecture itself need not change. |
| DR-04 | MEDIUM | `design.md:47`, `:524`–`:530` | The all-events correlation strategy only describes MockMvc capture and adding the completion event. In the fresh real-server probe, the first HTTP request triggers three DispatcherServlet initialisation log events without `requestId` (output:35–37), before the proposed application filter can supply it. This is also visible in the author's original P1 capture. Warm requests/MockMvc miss that part of SPEC rule 10's every-request obligation. | Address cold-request logging, for example by eager servlet initialisation before requests are accepted; verify a first real request with the shipped logging settings. Keep its bootstrap logs outside the request window or otherwise guarantee correlation. Fix in passing with the observability changes, not by weakening the quantifier. |

DR-01 and DR-02 require a design revision before plan-lock. DR-03/DR-04 are
bounded test/configuration corrections, expected in that revision rather than
backlog. A smaller response implementation is preferable to a new logging or
error framework. No change to idempotency race semantics, the accepted scope,
or the human's gate decisions is requested.

## Verification and reachability

Executed through the pinned wrapper:

```sh
scripts/gw --log docs/review/01-create-redirect/proof/design-baseline-check.txt --offline check
scripts/gw --log docs/review/01-create-redirect/proof/design-boundary-probe.txt --offline -I docs/review/01-create-redirect/proof/design-boundary-probe.gradle designBoundaryProbe
```

Both commands exited 0. The baseline check reported 13 tasks, 3 executed and
10 up-to-date (the unit/functional tasks reused their unchanged outputs).
This establishes the current ping baseline gate, not coverage of the unbuilt
feature. The diagnostic probe exits successfully after recording expected
failures; its exit code alone is not a design PASS.

The review probe boots a fresh Tomcat on loopback with the candidate's exact
`ProbeAdvice` and `ProbeBodyLimitFilter`. A small record-bound JSON endpoint
matches the proposed request shape. It adds a deterministic database collision
to exercise the documented exception path; it does not claim to implement or
stress-test the future LinkService race. Only an in-memory H2 table is changed.
Clock checks use two minimal configurations with the proposed method names.

| Check | Observed result / scope |
|---|---|
| JSON body at 16,384 bytes | Accepted in all four combinations: padding inside an ignored field or trailing whitespace, Content-Length or chunked |
| JSON body at 16,385 and 50,000 bytes | 413 problem detail in all eight combinations; suspected partial-read bypass was not reproduced and is not a finding |
| Framework 415 reflection | Submitted media-type parameter appears in detail (DR-02) |
| H2 duplicate-key failure | Bare 500 body, correct request id, submitted key in structured error log (DR-01) |
| Functional clock registration | Duplicate bean name rejected; distinct-name primary control starts (DR-03) |
| First real request | Lazy DispatcherServlet initialisation logs lack requestId (DR-04) |

| Criteria / design checks | Assessment |
|---|---|
| AC-1–AC-3 create/new codes/configured base | Reachable through validation, generator, repository, response; no request-derived host |
| AC-4–AC-7 invalid input/body size | Ordered field tokens and 400/415/413 explicit; no-echo boundary needs DR-02 |
| AC-8–AC-16 read/retire/redirect/routing | State from retired_at, conditional update, stored-only Location, constrained paths; explicit ProblemDetail errors |
| AC-17–AC-21 idempotency | One nullable unique binding, current representation, original 24-hour window and transactional expiry; DR-03 corrects the test clock wiring |
| AC-22–AC-25 audit | One insert inside each mutating transaction, rollback on failure, insert-only writer; DDL and destructive baseline rollback stated |
| AC-26–AC-27 observability/privacy | Completion event and 500 inside MVC are sound; DR-01 and DR-04 must correct the noted log paths |
| AC-28 API document | Controller annotations, fixed servers, deterministic export/live check; exact generated content remains an implementation check |
| Data/threat/territory/ADRs | Parameterised queries and indexes listed; UTC clock consistent; no fetch/SSRF; all new entry points considered; no territory extension; six ADRs plus one amendment |
| Remaining limits | No implemented JDBC mapping/transaction proof, no post-override stack execution or PostgreSQL run claimed; the builder must establish these or document applicable gaps |

## Self-check

- Exact design candidate and unchanged SPEC verified; 15/15 changed files read.
- Required guidance and author self-check audited, with no independent approval
  inferred from the author's plan-review.
- Baseline gate run; findings grounded in source, recorded inputs and fresh
  outputs; successful body-limit counter-evidence retained.
- No product, tests, SPEC or design changed. Probe sources/results and this
  report are confined to `docs/review/`.
- Ledger row appended. Exit `failed` to the design producer on DR-01/DR-02;
  the next pass resolves each response rather than reopening settled checks.

## Re-review 6367fa05bb16a6e843ba38301c1ecfcbb364e00d

**PASS — DR-01 through DR-04 fixed; no open findings.** Reviewed by
review-agent@urlshort-factory (Codex), 2026-10-03, on packet
`qitem-20261003062552-16fc9d48`. This verdict supersedes the initial FAIL above.

The revision consists of `d93e7d5` plus the builder-note correction `6367fa0`.
The approved SPEC remains byte-for-byte unchanged from `0acbc9d`. At execution,
the main checkout differed from this candidate only in mission NOTES; all
reviewed files and the probe's product/build inputs matched the candidate.
Confidence is high in the four resolutions and their design-level mechanisms;
feature implementation and full acceptance evidence remain ahead of this gate.

### Issue resolution

Locations refer to the revised candidate; `design.md` is in the slice directory.

| Finding | Resolution | Evidence |
|---|---|---|
| DR-01 HIGH | **Fixed.** The 500 event records bounded exception class names and one code location, never the throwable or exception messages. Completion events omit the client-selected method, and the framework's method-echoing WARN is suppressed. ADR-0002/0004/0009 and the system design agree. | `design.md:449`, `:466`, `:594`; fresh probe R7 produces a real H2 unique violation and R8 an exception chain with message canaries: neither leaks to body or logs. Each has one safe failure event and one completion event. The proposed feature test additionally verifies transaction rollback. |
| DR-02 HIGH | **Fixed.** One final response hook clears detail and replaces instance with a server-generated request-id URN; static typed validation extensions and response headers remain intact. | `design.md:211`, `:570`; fresh R1–R6/R9 cover media-type, unknown-path, domain-error and method cases. Every body omits detail and uses the response request id as instance; 415 retains Accept and 405 retains Allow. |
| DR-03 MEDIUM | **Fixed.** The primary functional clock bean has a distinct name, `functionalClock`. | `design.md:54`, `:592`; this is the exact distinct-name configuration already verified by the independent control in `proof/design-boundary-probe.txt:66`. No new clock experiment is claimed. |
| DR-04 MEDIUM | **Fixed.** Eager servlet initialization moves bootstrap messages before traffic; the test strategy adds a first real-request check and waits for the completion event. | `design.md:51`, `:571`, `:595`; fresh probe initialization precedes Tomcat startup, and R1 captures only its correctly correlated completion event. |

No finding is disputed or withdrawn. Both MEDIUM items are resolved in this
revision; none is deferred to backlog. No settled check is reopened. The fixes
retain the existing scope, data model, rollback and AC-to-suite mapping, and
add no dependency or architectural layer. D11 in mission NOTES delegates this
slice's plan-lock to the orchestration lead; the earlier human-gate description
above is historical.

### Revision coverage ledger

All **9/9** producer files changed since the original candidate were read,
including the full new probe and its captured output. Reviewer artifacts and
the orchestration lead's unrelated NOTES updates are outside this revision.

| Changed file | Verdict |
|---|---|
| `docs/DESIGN.md` | Pass: error and logging contracts match the revision |
| `docs/adr/0002-problem-details-via-platform-handler.md` | Pass: shared no-echo response hook and safe failure metadata |
| `docs/adr/0004-structured-ecs-logs-no-client-pii.md` | Pass: logging tradeoff, method suppression and eager initialization explicit |
| `docs/adr/0009-idempotency-key-binding.md` | Pass: race failure retains its existing semantics with safe diagnostics |
| `docs/diagrams/create-link-sequence.mmd` | Pass: failure event aligned with the text |
| `missions/01-greenfield-core/slices/01-create-redirect/design-probe/RevisionProbe.java` | Pass: nine mechanism cases, actual H2 failure and bounded request captures; not a full feature suite |
| `missions/01-greenfield-core/slices/01-create-redirect/design-probe/revision-output.txt` | Pass: all recorded cases inspected and independently parsed |
| `missions/01-greenfield-core/slices/01-create-redirect/design-probe/revision-probe.gradle` | Pass: pinned Java 21 and existing functional runtime classpath; no dependency addition |
| `missions/01-greenfield-core/slices/01-create-redirect/design.md` | Pass: all four responses resolved, regression strategy and evidence limits explicit |

### Verification and limits

Fresh reviewer execution, after inspecting the producer's probe source:

```sh
scripts/gw --log docs/review/01-create-redirect/proof/design-rereview-6367fa0.txt --offline -I missions/01-greenfield-core/slices/01-create-redirect/design-probe/revision-probe.gradle designRevisionProbe
```

Exit 0, BUILD SUCCESSFUL, six tasks (one executed, five up-to-date). The probe
prints verdicts rather than enforcing assertions, so its exit code was not
used alone. A separate parser checked both the author's record and the fresh
record: nine expected statuses (415/404/404/404/405/405/500/500/413), problem
media type, exact instance/request-id equality, no detail or canary values,
safe 500 metadata, preserved Allow/Accept, and 11/11 correlated JSON request
events. Initialization occurred before Tomcat started and the first request.

This verifies the revised advice/configuration mechanism on real loopback
Tomcat with in-memory H2. It does not implement the feature or prove its
transactions, full functional clock wiring, generated OpenAPI, typed field
errors or final cold-start test. Those remain explicit builder/QA obligations;
the existing independent clock control and original body-limit checks remain
supporting evidence. The baseline quality gate was already run in the initial
review; its unchanged product inputs did not warrant another baseline run.

### Self-check

- Candidate and unchanged SPEC verified; all nine revision files read.
- Each producer response judged against its original finding and observed
  behavior; the fresh output agrees with the recorded evidence.
- No product, test, SPEC or design edited. Only review evidence/report/ledger
  are added by this pass; rig-managed files remain untouched.
- Ledger appended; exit `handoff` to plan-lock with no open review findings.
