---
id: OPR.99.0.3.6
slice: 06-client-identity
mission: 02-brownfield
status: draft
stage: wip
tier: low
verified: 2026-10-04 against D21, baseline 5cfdf8a, shipped source and journeys
created: 2026-10-04
intent: "Client Identity"
depends_on: []
approved-spec-by: orchestration-lead@urlshort-factory
approved-spec-at: 2026-10-04T01:24:40.852Z
locked-artifacts:
  - name: SPEC.md
    path: SPEC.md
    kind: spec
  - name: design.md
    path: design.md
    kind: spec
provenance: transport:v1
approved-by: orchestration-lead@urlshort-factory
approved-at: 2026-10-04T05:26:35.350Z
---

# Slice 06 — Client Identity

## Intent

Client Identity. An Operator needs one maintained authority for client-identity rules so that rate limits, visitor counts and audit access cannot drift apart during maintenance. This refactor preserves existing responses, stored-data meaning and logs for Creators, Visitors and Analysts while consolidating the rules already decided in ADR-0015 and ADR-0019. Human decision D21 authorises this work and no new product behaviour.

## Mini-requirements

### Requirements covered

The mission SPEC's third amendment and D21 (`qitem-20261004003331-0b5245ad`) allocate this refactor. `5cfdf8a` is the dispatched pre-change baseline. Existing requirement owners remain unchanged; inherited rows below are regression obligations on the touched journeys.

| Id | Obligation in this slice | Proven by |
|---|---|---|
| FR-13 | Existing links and consumers keep working unchanged | AC-1–AC-15; unchanged baseline journeys and impact analysis |
| FR-10, NFR-R2 (inherited) | Rate budgets retain the existing trusted-proxy rule and refusals | AC-3, AC-4, AC-10 |
| FR-7, FR-8 (inherited, including shipped v2) | Clicks retain client grouping and per-day statistics | AC-5, AC-13 |
| FR-17, NFR-S6 (inherited) | Audit reads remain anonymous, read-only and restricted to direct loopback callers | AC-6–AC-11 |
| NFR-P1 (inherited) | Raw client identity stays private; daily hashes remain private | AC-5, AC-12 |
| NFR-O1, NFR-O2 (inherited) | Server-issued request correlation and structured, private logs stay unchanged | AC-12, AC-14 |
| NFR-R6 (inherited) | Audit read failure remains a sanitised error | AC-11 |
| NFR-M3 (inherited) | Live and committed API description stay unchanged | AC-15 |
| NFR-M1 (cross-cutting) | 100% line and branch coverage with honest gap reporting | Proof contract: build/report artifacts, not an HTTP criterion |
| NFR-M2 (cross-cutting) | Cross-cutting decisions describe the single authority before dependent code | Proof contract: ADR chronology and structural review, not an HTTP criterion |

NFR-X2 is not activated: D21 forbids a schema change. FR-14's defect fix and FR-15's separate improvements remain with their allocated slices.

### Personas

- **Primary:** Operator — runs the service: health, logs, metrics, audit trail, rate-limit tuning.
- **Secondary:** Creator — creates and manages short links; Visitor — opens a short link; Analyst — reads how a link performs. These are the personas in `docs/REQUIREMENTS.md` §1.

### User stories

- As an Operator, I want client-identity rules maintained in one place, so that their consumers cannot drift apart during maintenance.
- As a Creator, I want rate budgets to recognise the same client as before, so that proxy handling neither blocks unrelated clients nor lets forged headers evade limits.
- As a Visitor, I want existing links to redirect as before, so that internal maintenance does not interrupt access.
- As an Analyst, I want daily visitor counts to retain their meaning, so that reports stay comparable after the refactor.
- As an Operator, I want audit reads restricted to direct local requests, so that trusting a proxy for rate limits never grants it access to the trail.
- As a Visitor, I want client details to stay private, so that maintenance does not expose my address or browsing headers.
- As an Operator, I want the same correlated responses and logs, so that operational checks continue to work.

### Acceptance criteria

These are preservation criteria, not new API contracts. An unchanged existing journey may satisfy a criterion. Each case starts with fresh budget state or a dedicated peer; setup and statistics reads use separate peers so they do not consume the budget under test. Time and fixtures are controlled for exact assertions; asynchronous writes settle before statistics are read.

Unless overridden, shipped settings apply: no trusted proxies, `server.forward-headers-strategy=none`, neither Tomcat remote-IP header setting enabled. A "problem" is the existing `application/problem+json` response with the HTTP status in its body and no stack trace, SQL, exception class, submitted header or client address. On a real server `HEAD` retains the corresponding status without a body. No identity diagnostic endpoint or response field is added.

- **AC-1 — Existing active links still redirect.** [FR-13]
  GIVEN an active link and its target recorded on the baseline, with available budget
  WHEN a Visitor opens its short URL after the refactor
  THEN the response is `302` with the same `Location` and cache-related headers as the baseline.

- **AC-2 — Existing link error outcomes stay unchanged.** [FR-13]
  GIVEN independent baseline fixtures and available budgets
  WHEN a client exercises each case below
  THEN the indicated status and existing problem fields are unchanged; none redirects or records a successful click.

  | Request | Outcome |
  |---|---|
  | `GET /{unknown-code}` | `404` problem |
  | `GET /{retired-code}` | `410` problem |
  | `POST /{active-code}` | `405` problem |
  | `POST /api/links` with `{"url":"javascript:alert(1)"}` | `400` problem with the existing `url` validation error |

- **AC-3 — Untrusted headers cannot evade default budgets.** [FR-10, NFR-R2]
  GIVEN a frozen clock, default limits and no trusted proxies
  WHEN one fresh peer sends 61 valid creates with changing `X-Forwarded-For`, `Forwarded` and `X-Real-IP` values, and independently another fresh peer opens an active link 601 times with changing values
  THEN creates 1–60 return `201` and create 61 returns `429`; redirects 1–600 return `302` and redirect 601 returns `429` without `Location`; both refusals have integer `Retry-After` of at least 1, create no link or click, and retain the baseline problem representation.

- **AC-4 — The identity matrix preserves budget sharing.** [FR-10, NFR-R2]
  GIVEN one independent identity-matrix row below, a frozen clock and both existing rate-limit settings set to 2 per minute
  WHEN its reference peer exhausts a budget with two headerless requests, then the row's request uses that budget, and unrelated fresh peer `192.0.2.200` uses it
  THEN the row's request returns `429` with `Retry-After: 30`, while the unrelated peer succeeds (`201` for valid creates, `302` for active redirects). Prove each row separately for both budgets.

- **AC-5 — Clicks preserve the same identity grouping.** [FR-7, FR-8, NFR-P1]
  GIVEN one independent identity-matrix row, a new link, one fixed UTC day and sufficient budgets
  WHEN the row's request, its headerless reference peer and unrelated peer `192.0.2.200` each open the link once as browsers, and the Analyst reads settled statistics
  THEN `totalClicks` is 3 and the sole `clicksPerDay` element has that date, `clicks: 3`, `uniqueVisitors: 2`, `botClicks: 0`; no response or click record contains the raw peer or forwarding values.

  **Identity matrix for AC-4 and AC-5.** "Reference peer" is the input for a separate headerless request, never a returned value. Substitute `P = 10.9.9.9`, `Q = 10.9.9.8`, `U = 203.0.113.7`, `V = 203.0.113.8` in header strings and lists.

  | Trusted-proxy list | Peer | `X-Forwarded-For` | Other identity headers | Reference peer |
  |---|---|---|---|---|
  | empty | P | U | `Forwarded: for=V`, `X-Real-IP: V` | P |
  | P | `10.0.0.5` | U | absent | `10.0.0.5` |
  | P | P | U | absent | U |
  | P | P | `198.51.100.1, U` | absent | U |
  | P, Q | P | `V, U, Q` | absent | U |
  | P | P | `  U ,  ` | absent | U |
  | P, Q | P | `P, Q` | absent | P |
  | P | P | absent | absent | P |
  | P | P | empty string | absent | P |
  | P | P | ` , ` | absent | P |
  | P | P | absent | `Forwarded: for=U`, `X-Real-IP: U` | P |
  | P | P | U | `Forwarded: for=V`, `X-Real-IP: V` | U |

- **AC-6 — Direct loopback audit reads remain admitted.** [FR-17, NFR-S6]
  GIVEN existing audit rows, safe settings and available budget
  WHEN headerless peers `127.0.0.1`, `127.0.0.2`, `127.255.255.254`, `::1`, `0:0:0:0:0:0:0:1` and `::ffff:127.0.0.1` independently send `GET /api/audit`
  THEN each returns `200 application/json` with the existing `items`/`next` representation, leaving every audit row unchanged. Admission also holds when that loopback peer is listed as trusted.

- **AC-7 — Non-loopback audit reads remain refused.** [NFR-S6]
  GIVEN existing audit rows, safe settings and available budget
  WHEN peers `192.0.2.10`, `10.0.0.7`, `::ffff:192.0.2.10` and `fe80::1` send `GET /api/audit` and `HEAD /api/audit`, each peer both absent from and present in the trusted list
  THEN every response is `403` without trail content. Trust for rate limiting never grants audit access.

- **AC-8 — Forwarding-header presence still closes audit access.** [NFR-S6]
  GIVEN a loopback peer, safe settings and available budget
  WHEN it sends `GET /api/audit` with either `X-Forwarded-For` or `Forwarded`, separately with a remote value, a forged loopback value, an empty value and whitespace only, both with and without that peer listed as trusted
  THEN all return `403` problems without trail content. The same refusals hold for a non-loopback peer; forwarding never turns refusal into admission.

- **AC-9 — Existing address-rewriting overrides still close audit access.** [NFR-S6]
  GIVEN each configuration below in isolation, existing audit rows and available budget
  WHEN a loopback client sends `GET /api/audit` and `HEAD /api/audit`, without headers and then with `X-Forwarded-For: 127.0.0.2`
  THEN all return `403` without trail content, including on a real server where a rewrite can consume the header.

  | Configuration change from shipped defaults |
  |---|
  | `server.forward-headers-strategy=native` |
  | `server.forward-headers-strategy=framework` |
  | Strategy stays `none`; `server.tomcat.remoteip.remote-ip-header=x-forwarded-for` |
  | Strategy stays `none`; `server.tomcat.remoteip.protocol-header=x-forwarded-proto` |
  | Strategy stays `none`; both header settings above enabled |

  Independently, with the shipped `none` pin and both remote-IP header settings unset, detecting a cloud platform preserves `200` for a plain loopback read and `403` for the forged-header read. Empty or whitespace-only remote-IP setting values retain the safe-setting behaviour (rule 3).

- **AC-10 — Audit rejection precedence stays unchanged.** [FR-10, NFR-R2, NFR-S6]
  GIVEN a request failing the audit-access rule, invalid `limit=0` and `Accept: text/html`
  WHEN it requests `/api/audit` with available budget, and independently with its create budget exhausted
  THEN the first response is a `403` problem and the second a `429` problem with `Retry-After`; neither is `400` or `406`, and neither exposes the trail.

- **AC-11 — Admitted audit reads retain their error paths.** [FR-17, NFR-R6]
  GIVEN a direct loopback caller, safe settings and available budget
  WHEN it sends `GET /api/audit?limit=0`, sends a valid read while the audit store is made to fail, and independently attempts `POST /api/audit`
  THEN respective outcomes are the baseline `400` validation problem, `500` sanitised problem without `items` or `next`, and `405` problem; each leaves the trail unchanged.

- **AC-12 — Client and browsing canaries stay private.** [NFR-P1, NFR-O2]
  GIVEN unique canaries in peer/forwarded addresses, full user agent and referrer path/query/fragment, plus a separate canary target in the audit trail
  WHEN successful redirects, rate refusals, admitted and refused audit reads run, then statistics and metrics are read
  THEN request logs, click records, statistics and metric names/tags contain no raw address, forwarding value, full user agent or referrer path/query/fragment; logs contain no audit canary or trail content; hashes appear only in existing private click data, never responses, logs or metrics. The authorised audit response may still return the stored target URL.

- **AC-13 — Click recording retains its success and failure semantics.** [FR-7, FR-8, FR-13]
  GIVEN an active link with available budget, both without a trusted proxy and behind one
  WHEN existing slow-store, failing-store, concurrent-redirect, `HEAD` and unsuccessful-redirect journeys run
  THEN their HTTP, click-count, timing-bound and lost-click log/counter assertions pass unchanged: slow or failed recording does not change the `302` target, successfully recorded `GET` redirects count once, and `HEAD` or unsuccessful redirects count no click.

- **AC-14 — Response correlation and log events stay unchanged.** [NFR-O1, NFR-O2]
  GIVEN equivalent baseline/candidate requests producing `201`, `302`, audit `200`, `400`, `403`, `404`, `405`, `410`, `429` and injected audit `500`, each carrying an inbound request-id canary
  WHEN responses and request log windows are captured
  THEN every response has a new server-issued `X-Request-Id`, each event for that request is one JSON object on one line carrying that id, and event count, level, message and stable fields match the baseline. Only documented run-specific values may be replaced under rule 6; a changed event or field cannot be hidden.

- **AC-15 — The published API description is identical.** [NFR-M3]
  GIVEN baseline `docs/api/openapi.json`
  WHEN the candidate serves `GET /v3/api-docs` and the existing generation journey regenerates the committed document
  THEN the live description equals the baseline under the existing canonicalisation, and the regenerated file is byte-for-byte identical, including paths, operation ids, examples, schemas and responses.

### Business rules

1. **One authority, existing decisions.** Rate limiting and click counting use the same resolved client. Audit access uses the original connection peer and forwarding-safety conditions, not the resolved visitor. Consolidation must not merge these deliberately different questions into one predicate. D21's structural move is recorded in `slice.yaml`; design owns its expression and API.
2. **Proxy trust preserves exact answers.** Only a peer listed in `urlshort.rate-limit.trusted-proxies` can supply `X-Forwarded-For` identity. Scan comma-separated entries from right to left, trim surrounding whitespace, skip empty/trusted entries, and use the first remaining value; otherwise use the peer. Trust matches exact configured text. `Forwarded` and `X-Real-IP` have no identity effect. Do not add validation, canonicalisation, CIDR matching, name resolution or trust defaults. Existing opaque non-empty forwarded tokens retain their treatment. None of this grants audit access.
3. **Audit access preserves all conditions.** Effective forwarding strategy must be `none`; neither `server.tomcat.remoteip.remote-ip-header` nor `server.tomcat.remoteip.protocol-header` may contain non-whitespace text; the original peer must be loopback; both `X-Forwarded-For` and `Forwarded` must be absent. Empty forwarding request headers refuse; empty remote-IP settings are disabled. An unset effective strategy, missing/empty peer or unparsable numeric peer stays refused. Trust cannot override a condition. A headerless local relay remains indistinguishable from a local Operator: deployment must not relay `/api/audit`, or must add a forwarding header. No stronger protection is claimed.
4. **Order and budgets.** Rate limiting precedes audit access, validation and content negotiation. Audit reads/refusals consume the existing create budget; exemptions, limits, refill and rounding stay unchanged. Consolidation does not rewrite the connection address or hide headers from another consumer.
5. **Clicks and privacy.** The same client string is hashed with the existing in-memory daily salt and UTC day. Same-day distinct counts, bot classification, totals, retention and failure reporting stay unchanged. Existing hashes/records are not rewritten. Absence of an already-resolved client retains the peer fallback; characterize that internal edge without adding a public rate-limit bypass.
6. **Honest comparison.** Capture equivalent requests/fixtures on named baseline and candidate SHAs. Control time, isolate budgets and await writes. Independent runs generate different ids, codes, timestamps, process/thread values and salts; list each necessary substitution and preserve correlation/equality relationships. Never normalise status, header presence, target `Location`, problem field names or stable values, access decision, grouping, event level/message/count, or a new/missing field. A problem's generated request id follows the same correlation substitution as its response header and logs. Hash bytes across independently salted processes are not an equality oracle; privacy and grouping are. Separate unrelated concurrent events by request id, not by silently dropping differences.

### Non-functional

- **Coverage:** 100% merged unit/functional line and branch coverage, per-suite reports committed. No lowered threshold or silent new exclusion; a miss is a named gap (NFR-M1).
- **Privacy/access:** AC-6–AC-12 and AC-14 are mandatory. Security review also checks daily-salt lifetime and private hash use that HTTP alone cannot prove.
- **Redirect cost:** no new I/O, waiting or performance target. Existing slow-store/concurrency bounds remain mandatory (AC-13); this requirements step makes no new latency measurement claim.
- **Chronology/structure:** impact analysis precedes design; amend ADR-0015 and the moved audit decision in ADR-0019 before dependent code. Independent structural review verifies D21's single authority and all three consumers; HTTP tests alone cannot prove that (NFR-M2).

### Scope

**In scope:** D21's consolidation of existing resolution, sharing of the resolved client and the direct-loopback audit predicate; characterization before the move; preserved public behaviour/data; ADR/system-design updates, before/after proof, coverage and traceability.

**Explicitly out of scope:** new behaviour, setting, endpoint, dependency, schema or migration; new proxy/header syntax; normalising or rejecting previously accepted client tokens; changes to salts, analytics, limits, expiry or aliases; opening audit access, authentication or remote administration; unrelated defects or framework upgrades. Existing functional journey files remain byte-for-byte unchanged. Unit-test reference adjustments follow `slice.yaml`; changing a behavioural assertion needs the lead's recorded grant in `PROOF.md`, not a silent adaptation.

## Ambiguity log

| Id | Question and options | Resolution |
|---|---|---|
| A-1 | Does the short scaffold intent mean new identity behaviour or consolidation? | **decided:** consolidation only, D21 and mission third amendment. `intent` remains verbatim; the full outcome is above. |
| A-2 | Does one identity make audit access trust the resolved visitor, or preserve the direct peer? | **decided:** preserve both existing meanings under one authority. D21 names the audit predicate and CR-01; ADR-0015/0019 explain the distinction. |
| A-3 | May malformed forwarded tokens, IPv6 text or empty values be cleaned up? | **decided:** no; preserve exact-text/whitespace behaviour. D21 forbids new validation; characterize these edges before moving code. |
| A-4 | Must responses/logs have identical generated ids and timestamps across independent runs? | **assumed:** compare content using documented run-specific substitutions (rule 6), preserving correlations and structure. Safe: baseline runs already differ in those values; no caller-visible rule is relaxed. |
| A-5 | Which baseline applies if other seats commit during this slice? | **decided:** dispatched `5cfdf8a` is the initial comparator. Proof names actual baseline/candidate; a later integration rebase records upstream changes separately. The unrelated W2F-01 fix is not attributed to this refactor. |
| A-6 | Rename existing journeys to new ACs, or map their names in traceability? | **decided:** map existing names. D21/manifest require unchanged functional files; new characterization uses the granted new test territory. |

One safe assumption (A-4); no parked decision. Independent review and design precede the delegated plan-lock.

## Proof contract

- [ ] All AC-1–AC-15 are green in the functional suite, with every matrix/settings case identified; unchanged existing journeys may satisfy them.
- [ ] AC-1/AC-2: before/after status, target, cache headers and problems for baseline-origin link fixtures.
- [ ] AC-3/AC-4: captured default and matrix budget decisions with `Retry-After`, demonstrating shared and independent clients.
- [ ] AC-5: settled statistics retain client grouping; raw identities are absent from rows/responses.
- [ ] AC-6–AC-9: admission/refusal captures include real-server CR-01 remote-IP and cloud-platform cases. Simulated peers alone do not prove server header-rewriting behaviour.
- [ ] AC-10/AC-11: sanitised precedence, validation, failed-read and wrong-method responses, with rows unchanged.
- [ ] AC-12: canary checks cover every named surface; review verifies private hash use and daily-salt lifetime.
- [ ] AC-13: existing click success, slow/failing-store and concurrency assertions pass unchanged, including trusted proxies, `HEAD`, loss logs/counters.
- [ ] AC-14: QA commits live baseline/candidate responses, JSON log captures and a comparison record listing every substitution under rule 6. Unexplained differences remain findings.
- [ ] AC-15: live API description matches baseline; regenerated `docs/api/openapi.json` has no byte difference.
- [ ] Characterization tests are committed and pass against baseline production code **before** the move: peer/header/trust matrix, loopback forms, exact-text trust, blank values, opaque forwarded tokens, strategy including unset, both remote-IP settings and resolved-client fallback. Preserve SHA/output and rerun the assertions after the move. Internal-only edges may use unit tests; HTTP-observable outcomes require functional evidence.
- [ ] Production move commits are separate from characterization/test changes. Commit-range review shows no existing functional file changed. Unit-test adaptations change API references only and retain behavioural assertions; any exception has the lead's explicit grant in `PROOF.md`.
- [ ] Complete existing functional suite passes unchanged, notably `AuditForwardedHeadersJourneyTest`, `AuditAccessSettingsJourneyTest`, rate-limit/trusted-proxy journeys, `StatsV2JourneyTest` and `TrustedProxyClickJourneyTest`. Record `scripts/gw --offline check` on the named candidate and the unchanged baseline-file comparison.
- [ ] Unit/functional coverage reports committed under `docs/qa/coverage/06-client-identity/{unit,functional}/`, with merged line/branch totals at 100%; no exclusions added to hide a miss.
- [ ] `docs/qa/TRACEABILITY.md` contains requirement-id ↔ AC ↔ exact test rows. `docs/qa/GAPS.md` names any unmet outcome or honest coverage target, or records no new gap.
- [ ] Impact analysis committed before design; amended ADR-0015/0019 precede dependent code; independent review verifies the single-authority outcome and all three consumers against D21. After merge, the register owner updates architecture guidance row 1 and the current system description.
- [ ] Candidate range adds no schema, setting, dependency or public API and rewrites no existing data. Captures/check outputs are linked from `PROOF.md`/`proof/` and registered with `rig proof add`; the manifest's QA seat remains the proof judge.

## Self-check

- Read scaffold/manifest, mission amendment, requirement baseline/personas, project SPEC, earlier audit/operations/analytics SPECs, ADR-0015/0019, relevant shipped source/journeys and committed API paths/responses. D21's original packet and closure confirm the assignment/baseline. The project SPEC's old alias/expiry summary is superseded by the mission's explicit decisions.
- Every AC is observable through HTTP, logs or stored effects; matrix identities are inputs, never a proposed response. M1/M2 and structural consolidation are artifact/review obligations, as in earlier slice SPECs.
- Errors/privacy cover `400`, `403`, `404`, `405`, `410`, `429`, injected audit `500`, canaries and settings that can erase forwarding evidence. Other shipped error paths retain the complete unchanged suite.
- Rules pin trust, exact text, blank-versus-absent, peer-versus-visitor, ordering, privacy and comparison limits. Out-of-scope work and functional-file rewrites are explicit.
- A-1–A-6 are decided or safely assumed; A-4 is the only assumption, none parked. Every allocated/inherited id is mapped; M1/M2 use artifact checks and X2 is inapplicable without a schema change.
- Proof names coverage, traceability, gaps, characterization chronology, separate move commits, structural review and by-effect captures, distinguishing real-server evidence from simulated peers.
- No new architecture, schema, class API or dependency is selected here. D21's structural instruction is linked through the manifest; test names identify evidence and setting names describe existing behaviour.
- `plan-review` self-check: strategy — scope equals D21, unrelated fixes excluded; interaction — preserved admission/refusal, spoofing, blank inputs and error precedence; engineering — external budget/aggregate oracles, isolated setup, real-server CR-01 proof, and documented substitutions avoid impossible random-value comparisons. No blocking issue found in this author review; independent requirements review remains required. Following the existing slice format, this is the review record, with no separate executive summary.
- Verification: `git diff --check` is clean; `rig scope audit --mission 02-brownfield --json` reports no frontmatter error or finding for `06-client-identity`, and no dependency advisory. Its seven findings concern pre-existing proof headers in other slices; this is not a claim that the whole mission audit is clear.
- Not verified at requirements: running baseline/candidate equivalence, build/coverage, live captures or the eventual single-authority implementation. These remain the design, builder, QA and review deliverables, not completed claims.
