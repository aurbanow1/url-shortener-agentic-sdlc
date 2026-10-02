# 01-ping — independent design review

Reviewer: `review-agent@urlshort-factory` (Codex). Date: 2026-10-02 UTC.
Packet: `qitem-20261002225126-b5c94d4f`; instance: `01M3ZA8Q39QEB3R1QDQCVTER18`.
Candidate: `3931c8b51e4b3e486cae7218e43c206bd9188ba8`, incorporating the producer's
territory correction to the initial `44c13bb` handoff. SPEC: `4e581cc`.

## Verdict

**FAIL — one HIGH finding (DR-01).** Return to design before plan-lock.
The proposed controller and request-id filter are appropriately small, but
the functional-test configuration mechanism cannot establish AC-5 and AC-6
as written. No MUST-FIX, MEDIUM or LOW findings; no additional backlog items.

## Context proof

The slice proves the factory with one anonymous ping endpoint: exactly
`status` and UTC `time`, a server-issued response id, one JSON log event with
that id, no inbound-header or address disclosure, and a problem-detail 405.
The human requires a minimal correlation mechanism. The design uses one
filter, one controller and one record, with platform error handling and
logging; it adds no persistence or dependency.

Read project → mission → slice manifests, the mission decision and notes,
the complete slice SPEC and design, system design, all four ADRs, both
diagrams, repository conventions, build configuration and existing tests and
properties. No additional SDLC composition was selected by workflow guidance;
the explicit packet selects this design review. Confidence: intent and scope
100/100, architecture 95/100, configuration finding 100/100. Endpoint behavior
and output-capture behavior remain implementation/QA checks; no candidate
implementation was supplied and this review does not claim they passed.

## Findings

| ID | Severity | File:line | Evidence / consequence | Required change |
|---|---|---|---|---|
| DR-01 | HIGH | `missions/00-hello/slices/01-ping/design.md:194` (also `:170` and `:22`) | The recommended deletion does not restore the shipped configuration. On the real functional runtime classpath, its `application.properties` shadows the production resource. The read-only probe returns `logging.structured.format.console=<absent>` after the proposed deletion, while `spring.mvc.problemdetails.enabled=<absent>` both before and after it. The production-classpath control returns `ecs` and `true`. Thus the proposed suite does not load the ECS format needed by AC-6 or the property enabling the platform handler assumed by AC-5. This is a configuration-source problem, not the stated uncertainty about successive test contexts. | Replace the mechanism with an explicit, working way to load the shipped settings and retain the temporary H2 settings for functional tests. Verify both effective properties; update the related design/ADR guidance. Preserve the AC-6 fail-on-non-JSON rule and the AC-5 HTTP assertion. Coordinate any additional file territory with the lead. No custom error handler or encoder is required by this finding. |

## Empirical evidence

Ran with the repository's Java 21 / Spring Boot 4.1.1 dependencies:

```sh
scripts/gw --offline -I docs/review/01-ping/proof/config-probe.gradle reviewConfigProduction reviewConfigProbe reviewConfigAfterDeletion
```

Exit 0, `BUILD SUCCESSFUL`; three probe tasks executed. The probe uses Boot's
`ConfigDataEnvironmentPostProcessor.applyTo` on the actual Gradle runtime
classpaths. For the deletion variant it substitutes a generated copy of the
functional resource directory with only the recommended blank logging line
removed. It does not edit source/tests, start a service or touch a database.

| Effective property | Production control | Existing functional resources | Proposed deletion |
|---|---|---|---|
| `spring.application.name` | `urlshort` | absent | absent |
| `spring.mvc.problemdetails.enabled` | `true` | absent | absent |
| `logging.structured.format.console` | `ecs` | empty | absent |

Reproduction: [Gradle probe](proof/config-probe.gradle),
[Java probe](proof/ConfigProbe.java), [captured output](proof/config-probe.txt).
This is a configuration reproduction, not a ping HTTP test or coverage run.
The full product `check` gate belongs to the subsequent implementation review.

## Contract and design assessment

| Check | Assessment |
|---|---|
| AC-1 / AC-2 | Reachable through the exact two-field record and `Instant.now().toString()`; interval-based functional assertion is appropriate. |
| AC-3 / AC-4 / AC-8 | Reachable through a fresh server-issued UUID, header set before the chain and no read of the inbound id. |
| AC-5 | Error contract explicit; platform handler appropriate for production. Functional configuration prerequisite is missing (DR-01). |
| AC-6 | MDC, controller event and parse-every-matching-line assertion are appropriate. Recommended configuration mechanism fails (DR-01). |
| AC-7 | Header/address canaries and absence checks are specified; no component needs client PII. |
| Cleanup / errors | `finally` cleanup plus a throwing-chain unit check is specified; ASYNC/ERROR dispatches are explicitly deferred. |
| Data and rollback | Not applicable: no persistence or migration is introduced. |
| Threat coverage / audit | GET, wrong-method and inbound-header entry points covered; no mutation requires an audit event. Anonymous availability and clock exposure are explicitly accepted. |
| Scope / territory | Minimal components, no extra layer or dependency. Properties territory granted at `0160958`; manifest and producer's `3931c8b` correction read. No remaining territory finding. |
| Test mapping | All eight ACs and eight business rules mapped. Functional setup needs DR-01 corrected; runtime capture behavior still requires by-effect tests. |

## Files reviewed

Nine design-delivery files read in full (supporting SPEC, manifests and
baseline source/configuration read in addition):

| File | Verdict |
|---|---|
| `missions/00-hello/slices/01-ping/design.md` | DR-01; other design checks pass |
| `docs/DESIGN.md` | Architecture coherent; suite-format claim depends on DR-01 correction |
| `docs/adr/0001-spring-boot-4-java-21-gradle.md` | Consistent with the pinned build |
| `docs/adr/0002-problem-details-via-platform-handler.md` | Platform choice appropriate; functional proof depends on DR-01 correction |
| `docs/adr/0003-request-id-server-issued.md` | Minimal mechanism; cleanup and dispatch boundary explicit |
| `docs/adr/0004-structured-ecs-logs-no-client-pii.md` | Contract appropriate; mechanism depends on DR-01 correction |
| `docs/diagrams/container.mmd` | Consistent with the proposed components |
| `docs/diagrams/ping-sequence.mmd` | Success and 405 flows match the design |
| `missions/00-hello/NOTES.md` | Human constraint and producer's territory/handoff history understood |

## Self-check

- Reviewed the complete assigned design and supporting ADRs against all ACs;
  read the producer's later territory-only correction before judging it.
- Reproduced the sole blocking finding on the exact installed dependencies;
  cited the source line, runnable command and captured result.
- Severity reflects blocked proof of two required contracts, not style.
- No product source, tests, SPEC or design edited; only review evidence added.
- No settled findings reopened. This is DR-01's first failure; re-review must
  assess the producer's response, with escalation if the same finding fails twice.
- Ledger records all nine design-delivery files and this verdict. No claim
  that the future endpoint, full coverage gate or log capture has been verified.

## Re-review d0521deaa453d9e425b3ca9d06f2d10b65a16255

2026-10-02 UTC; packet `qitem-20261002230841-dc5cdf2b`.
**PASS — DR-01 fixed; no open findings.** Ready for human plan-lock.
This supersedes the initial verdict above. The SPEC remains at `4e581cc`.

| Finding | Producer response | Independent judgment |
|---|---|---|
| DR-01 (HIGH) | Accepted the cause; replace the shadowing suite file with `application-functional.properties` containing only the H2 overrides, and activate `functional` on the Gradle functional-test task. | **Fixed in design.** Independently reran the profile and no-profile probes: the active profile retains the in-memory database while loading the shipped `ecs` and `true` settings. The control without the profile selects the shipped file database, confirming why both the rename and activation are necessary. |

Ran the producer's inspected init script through `scripts/gw --offline -I
missions/00-hello/slices/01-ping/design-probe/config-probe-profile.gradle
designConfigProfileOverlay designConfigProfileOverlayNoProfile`.
Gradle reported `BUILD SUCCESSFUL`, with both probes executed.
The fresh reviewer capture is [config-profile-rereview.txt](proof/config-profile-rereview.txt).

With `functional` active, the result is `spring.application.name=urlshort`,
`spring.mvc.problemdetails.enabled=true`,
`logging.structured.format.console=ecs`,
`spring.datasource.url=jdbc:h2:mem:urlshort-functional;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`,
and `spring.flyway.enabled=true`. Both base and profile property sources are
listed. No service was started or database opened by this property probe.

The correction preserves the HTTP 405 assertion and the strict JSON-log
assertion. It removes the per-class override fallback and keeps the product
components unchanged. The two additional territory paths were granted in
`2b29248`; the build permission is limited to the functional-test block.

Eight changed delivery/supporting files read (unchanged design content retains
the initial review; no settled finding reopened):

| File | Verdict |
|---|---|
| `missions/00-hello/slices/01-ping/design.md` | DR-01 response accepted; mechanism and unchanged AC assertions sufficient |
| `docs/DESIGN.md` | Configuration-source rule matches the probe |
| `docs/adr/0004-structured-ecs-logs-no-client-pii.md` | Profile mechanism replaces shadowing; original privacy contract preserved |
| `missions/00-hello/slices/01-ping/design-probe/ConfigProbe.java` | Reads effective properties only; exposes database and profile selection |
| `missions/00-hello/slices/01-ping/design-probe/config-probe-profile.gradle` | Reproduces the proposed resource rename and JVM property on the real classpath |
| `missions/00-hello/slices/01-ping/design-probe/output.txt` | Producer's results agree with the independent rerun |
| `missions/00-hello/slices/01-ping/slice.yaml` | Required profile file and scoped build change granted |
| `missions/00-hello/NOTES.md` | Response, territory grant and unit-suite follow-up recorded |

The unit-suite shadowing is an existing, recorded follow-up for when unit
tests need shipped settings; it is not a new blocker or a required expansion
of this slice. No non-blocking review items require correction in passing.

## Self-check (re-review)

- Judged the producer's response to the only finding, with fresh successful
  configuration probes; all eight changed delivery/supporting files read.
- Confirmed the reviewed artifacts still match `d0521de` and the SPEC is
  unchanged; product source and tests were not edited.
- DR-01 is fixed in the design. The actual endpoint, output capture and
  coverage gate still require implementation and independent QA/code review.
- Recorded the passing ledger row and preserved the original failed review.
  Handoff proceeds to plan-lock; no stamp or human decision is claimed here.
