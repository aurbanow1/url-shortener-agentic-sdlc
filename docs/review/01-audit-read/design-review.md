# 01-audit-read — design review

Candidate: `b55c549738f5c2b24b32882d9497a616f7de199a` (design `c1be72844649ff87a9207f74bec46e2df8b135e2`,
impact analysis `a686b2a4a85830d4dcb5256a170e0cc2ee771f17`). Accepted SPEC: `7b753b7`.
Packet: `qitem-20261003174406-936f9d9c`; instance: `01M416ZY5N11CDGZBM2DT4GAXS`.
Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03 UTC.

**Verdict: FAIL — one HIGH (DR-01), one MEDIUM (DR-02).** Return to design.
DR-02 should be corrected in passing with the access-check revision. No new product capability,
dependency or human decision is needed to enforce the accepted closed-access contract.

## Context and coverage

The slice gives an Operator bounded, read-only pages of existing mutation records, while keeping
links, writers and statistics unchanged. The accepted ordering is assigned write sequence, with
an explicit limit on visibility of writes still in flight. The access rule admits a loopback peer
only when neither forwarding header exists, and no setting may open that rule. Confidence in
these requirements and the reviewed mechanisms is high; this is a design review, not approval
of an implementation that has not been built.

Read all ten unique files changed by the three handed commits, including both versions of the
ADR; the accepted SPEC, mission allocation/boundary, slice manifest, NOTES/PROGRESS/PROOF,
the earlier requirements findings/resolutions, and the relevant shared design and existing
request/error/audit paths. Judged against review guidance, architecture §§3–8, databases §8,
and brownfield §7. The impact analysis predates the design. Shared-checkout HEAD at verification
was `fbf2d5ee253d798c82bfbcf3b50aaf12b81f6b9a`; `src/` and Gradle build definitions have no diff
from the handed candidate. Document changes were reviewed from their exact commit objects,
without resetting the shared checkout or consuming another seat's uncommitted changes.

| Changed file | Verdict |
|---|---|
| `missions/02-brownfield/slices/01-audit-read/impact-analysis.md` | Read fully; complete module/data/rollback/test impact; forwarding-default limitation remains DR-01 |
| `missions/02-brownfield/slices/01-audit-read/design-probe/AuditProbe.java` | Read fully; P1–P5 exercise the claimed mechanisms, with stated prototype limits; no explicit override/strict-Accept cases |
| `missions/02-brownfield/slices/01-audit-read/design-probe/audit-probe.gradle` | Read fully; isolated Java source launcher on the functional runtime; no product mutation |
| `missions/02-brownfield/slices/01-audit-read/design-probe/output.txt` | Read fully; responses, concurrent-write example and bounded primary-key scan support the scoped claims |
| `missions/02-brownfield/slices/01-audit-read/design.md` | Read fully; FAIL DR-01; MEDIUM DR-02; other mechanisms/test mappings reachable |
| `docs/DESIGN.md` | Entire candidate delta read with surrounding shared context; access row must change with DR-01 |
| `docs/adr/0019-audit-read-loopback-keyset.md` | Read fully including `b55c549` follow-up; default enforcement DR-01; future wrapper/header obligation correctly explicit |
| `docs/diagrams/audit-read-sequence.mmd` | Read fully; sequence matches proposed flow; error precedence is qualified by DR-02 |
| `docs/diagrams/container.mmd` | Full changed diagram read; new read path and existing writer/storage boundaries correctly distinct |
| `missions/02-brownfield/NOTES.md` | Entire handed delta read; candidate/probe/custody continuation accurately recorded |

**10 changed files / 10 reviewed.** Only reviewer files were authored here.

## Requirements and design assessment

| Contract | Assessment |
|---|---|
| FR-17, AC-1–9; representation and paging | Reachable. One parameterized `SELECT`, descending primary key, limit+1 lookahead and a positive-id cursor support empty/null shape, exact stored content, bounded pages and per-field errors. Producer P2/P5 support the design; no redundant index or migration is needed. Strict-Accept errors have DR-02. |
| AC-8/20; concurrent writes | Reachable within the accepted bounded guarantee. Strictly decreasing positions cannot repeat a row. P3 holds an insert across page one, then confirms a fresh traversal sees it. This is not commit ordering or a snapshot guarantee. |
| NFR-A2, AC-10 | Reachable. Read-only `AuditTrail` has only a `SELECT`; existing transactional `AuditLog` remains the sole insert writer, with no update/delete path. Wrong methods remain framework 405; read preservation is explicitly tested. |
| NFR-S6, AC-11–14 | **DR-01:** the configuration default can be overridden into admission. Plain/default loopback and address classification are otherwise supported. Rate-limit trusted-proxy settings are correctly separate from audit authorization. |
| NFR-O1/O2, AC-15/16/21 | Reachable. Reuses request-id and sanitized ProblemDetail advice, no new audit/read log. Failed-query induction is explicit, with canaries, no partial/empty-success page, safe correlated ERROR/INFO and recovery. |
| FR-13/NFR-X2, AC-17/18 | Impact analysis first, baseline functional tests unchanged, no schema/data rewrite, merge-revert rollback stated. Synthetic V2 upgrade test is supplemented by the required old-jar-written data-directory capture. Candidate compatibility and rollback execution remain implementation/QA work. |
| NFR-M3, AC-19 | New operation, row/page examples, parameters and typed 400/403/500; existing 429 customizer and committed/live equality retained. JsonNode schema rendering is honestly left for the candidate check. |
| Threat model and scope | Covers GET/HEAD and other methods, spoofing, injection, disclosure, resource cost, anonymous/read-only access and failed reads. No new dependency or speculative layer; single handler/read component plus data records is proportionate. Shared configuration/OpenAPI custody is explicit; README grant remains for delegated plan-lock. ADR precedes code. |

## Independent controls

Run from the repository root:

```sh
scripts/gw --log docs/review/01-audit-read/proof/access-controls.txt --offline -I docs/review/01-audit-read/proof/audit-access.gradle reviewAuditAccess
scripts/gw --log docs/review/01-audit-read/proof/baseline-check.txt --offline check
```

Both exited 0. The baseline gate was entirely **UP-TO-DATE**; it is not a fresh candidate suite
or proof of the unbuilt endpoint. The access probe executed freshly against real embedded
Tomcat and the shipped error advice. It uses a minimal handler with the design's guard and
`produces` condition, a synthetic audit canary, and a properties file supplying the proposed
`none` default. It does not test the future product's SQL or return real audit records. All
listeners/clients stayed on `127.0.0.1`; all databases were temporary in-memory databases.

| Control | Observed |
|---|---|
| File default `none`, plain loopback | 200 with synthetic row |
| File default `none`, either forwarding header | 403 ProblemDetail |
| File default `none`, CLI `native`, `X-Forwarded-For: 127.0.0.2` | **200 with synthetic row** |
| File default `none`, CLI `framework`, either `X-Forwarded-For: 127.0.0.2` or `Forwarded: for=127.0.0.2` | **200 with synthetic row** |
| Either override, `X-Forwarded-For: 192.0.2.10` | 403; shows the forged loopback value causes admission |
| Default `none`, bad limit / refused header, wildcard or browser Accept | 400 with field/rule / 403, both ProblemDetail |
| Same bad limit / refused header, strict `Accept: text/html` or `application/problem+json` | **406 ProblemDetail**, before handler validation/guard |

Evidence: [probe source](proof/AuditAccessProbe.java), [task](proof/audit-access.gradle),
[property default](proof/forwarding-default.properties), [control output](proof/access-controls.txt),
[baseline gate](proof/baseline-check.txt).

## Findings

`design.md` and `SPEC.md` below refer to this slice; ADR paths are repository-relative.

| Id | Severity | File:line | Evidence and consequence | Required change |
|---|---|---|---|---|
| DR-01 | HIGH | `design.md:23`, `:36`, `:198`, `:225`; `docs/adr/0019-audit-read-loopback-keyset.md:119` | The property file only supplies an overridable default. The independent native/framework controls above return 200 with a synthetic confidential row for forged loopback forwarding headers; default none refuses them. This breaks AC-13/14 and BR-2's no-setting-opens rule, including the documented safety of a local relay that adds a refused header. ADR-0019 explicitly treats overriding the property as voiding the rule, but no decision authorizes that exception. At 17:52Z the author acknowledged the gap and absence of a decision. This is a reproduced design mechanism, not a claim that an unbuilt product endpoint is already deployed. | Enforce fail-closed access even when the forwarding strategy is overridden; a file default and operator warning are insufficient. Add real-server controls for both `native` and `framework`, including forged XFF/Forwarded and plain loopback, and keep unset/cloud-default behavior closed or demonstrably safe. Update the design, threat residual, ADR/shared contract and test mapping together. The author's proposed check that the effective strategy is explicitly NONE is a narrow candidate fix, to be reviewed when handed back. |
| DR-02 | MEDIUM | `design.md:31`, `:93`, `:94`, `:242` | The mapping's `produces=application/json` condition runs before the claimed first guard. The independent strict-Accept controls get 406 instead of the table's 403 or per-field 400. Bodies remain safe ProblemDetail and no trail is exposed, so this is non-blocking response/precedence drift, not another disclosure. Browser Accept with a wildcard works as described. | Reconcile the handler/error precedence with the promised 403/400 responses and add strict-Accept controls. Removing the mapping condition while leaving successful JSON conversion to Spring is a small option; preserve ProblemDetail regardless of Accept. Describe any remaining intentional 406 behavior accurately, without claiming the guard runs on every GET/HEAD first. |

## Self-check and continuation

The ten-file review is complete, both findings have independent observed responses and exact
locations, and the verdict follows DR-01's security consequence. No source, product test, SPEC,
design or producer evidence was edited. Reviewer probe output and baseline-gate limits are
explicit. Append one ledger row and return through `failed` to the design author. Re-review
the producer's response to DR-01/02 and any consequences of that fix; retain settled paging,
scope and requirements decisions. Implementation still owes full candidate suites, upgrade,
OpenAPI equality and actual log evidence.
