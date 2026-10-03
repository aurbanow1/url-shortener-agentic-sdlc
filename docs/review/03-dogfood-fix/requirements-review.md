# 03-dogfood-fix — requirements review

**PASS on `8b63e5be8598212a9d892c2406081addc1c45d4b`. No open findings.**
One MEDIUM wording finding was corrected in passing before handoff.

Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03 UTC.
Packet: `qitem-20261003185900-adac5cf2`; instance: `01M41HGF6AHB6AZR3Q0KQ8MQR7`.
Initial SPEC: `fb489a91b482c7e3ce79d24346f54acd7524d439`.
Explicitly handed follow-ups: territory correction `06ad0bf`, wording correction `8b63e5b`.

## Context and coverage

This slice corrects the published problem schema to the existing HTTP bodies and removes
the installation path from disk metrics while retaining their values. Both defects were
observed in the installed-jar dogfood pass; neither is seeded. Existing wire behavior,
other metric exposure, link data and error producers stay unchanged. Confidence: high.

Read the whole SPEC and both follow-up deltas, mission brief and current amendments,
project/mission/slice manifests, relevant mission decisions and territory adoption `d5d9b14`,
PROGRESS/PROOF placeholders, FR-13/14/15 and inherited requirements, requirements/review
guidance and brownfield §7. Checked dogfood report `305dce5`, its raw problem/metric captures,
GAPS QA-OPR-02, existing error/OpenAPI producers and both named functional test files,
relevant system-design sections and ADR-0002/0010/0016, plus the accepted audit-read error
contract that this slice must inherit after w1 merges.

| Changed file | Verdict |
|---|---|
| `missions/02-brownfield/slices/03-dogfood-fix/SPEC.md` | PASS; full initial content and both correction deltas reviewed |

**1 unique changed file / 1 reviewed.** Review inputs were read at their named commits;
the shared main checkout was not switched or reset. No producer artifact was edited.

## Contract assessment

| Area | Assessment |
|---|---|
| Requirement allocation | FR-14 → AC-1/2/5/6; FR-15 → AC-7/8 and the gap closure; FR-13 → AC-3/4/9 plus required impact analysis. Inherited NFR-M3 → AC-3. Cross-cutting M1/M2 are explicit coverage and prior-ADR proof items. No migration, so X2 is not triggered. |
| AC-1–4 | Observable from live/committed documents and HTTP responses. Exact extension fields, optionality, removal of the phantom member, unchanged wire and restricted document delta are specified. Final AC-2 honestly names six representative requests, covering both body shapes. |
| AC-5/6 | Test-first obligation is verifiable from commits and captured failing runs. AC-6 separately observes anonymous Prometheus and disk.free output, absence of the path, and preservation of both disk gauges. This is the selected privacy correction. |
| AC-7–9 | Gap disposition and documentation accuracy have concrete file checks; compatibility requires the shipped suites, permitting only added assertions in the two named existing tests. No old test is removed to obtain a pass. |
| Rules and ambiguities | Five rules capture wire compatibility, shared schema, test-first work and retained gauges. All five ambiguity rows have reasons. A-1/A-4 address an existing retained defect and were accepted in the lead's adopted scope; no new authentication, metric restriction or migration is assumed. |
| Proof contract | Names per-suite coverage reports, AC/rule/requirement traceability, the slice gap entry and old gap closure, both red runs with SHAs, live error/document/metric captures, committed/live comparison, and prior indexed ADR amendments. Captures are scoped to what they show. |
| Boundaries | Explicit out-of-scope list; no new data model, dependency or implementation layer prescribed. Existing class/file names identify compatibility and the lead's grant; the implementation mechanism remains design work. Impact analysis must precede design and be checked again against the merged w1 base. |
| Shared custody | Final territory text agrees with the lead: DESIGN.md belongs to the design step; README/properties follow both w1 holders; the later plan-lock orders OpenAPI against analytics-v2. RateLimitFilter and its test remain excluded. Design selects the one W2-03 route and plan-lock narrows its grant. |

## Finding and resolution

| Id | Severity | File:line at `06ad0bf` | Evidence | Required change |
|---|---|---|---|---|
| RQ-01 | MEDIUM | `missions/02-brownfield/slices/03-dogfood-fix/SPEC.md:73`, `:208`, `:217` | AC-2 and the self-check claimed every problem status while naming six cases. The supplied dogfood captures include 405/406/413/415/500, and the audit design also includes 403. The six cases already exercise both extension shapes; the defect is an overstated coverage claim. | Describe a representative conformance matrix consistently, preserving the named cases and explicit scope. |

## Re-review 8b63e5b

**RQ-01: fixed.** The author retitled AC-2, named the untested statuses and corrected both
self-check claims. Independently compared the three-line delta and verified that the six
request cases are unchanged. No additional product obligation or open backlog item results.
Territory correction `06ad0bf` was also verified against adoption `d5d9b14`.

## Verification and limits

- [Source/capture inspection](proof/requirements-source-check.txt): parsed the committed
  problem schema and stored 400/422 bodies, confirming the original omission and actual
  field/rule/message shape. Parsed stored omitted-status bodies and inspected both disk
  samples carrying path labels. This rechecks supplied evidence; it is not a new live run.
- [Baseline gate](proof/requirements-baseline-check.txt):
  `scripts/gw --log docs/review/03-dogfood-fix/proof/requirements-baseline-check.txt --offline check`
  exited 0; all 14 tasks were UP-TO-DATE. This is baseline health, not proof of either future fix.

No claim that the fixes are implemented, the gaps are already closed, or every status was
separately validated. Exact-candidate regression red/green evidence, live captures and final
coverage remain implementation/QA obligations. Whether a property can remove just the tag
is correctly left to design, with an alternative explicitly subject to the grant.

## Self-check and handoff

Candidate identities and complete file coverage recorded; every AC, rule, allocation,
ambiguity and proof item checked. Finding cites reproducible stored evidence and its
resolution. Only review artifacts authored. PASS to design with no open findings; retain
the lead's w1 hold and OpenAPI ordering at plan-lock.
