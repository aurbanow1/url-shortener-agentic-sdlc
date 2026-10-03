# Review guidance — every artefact, independently, with proof of coverage

The assignment asks for review results that prove all code was reviewed, with
issues and their resolutions. This guide is for every review step
(`requirements_review`, `design_review`, `code_review`, `security_review`,
`decomposition_review`, `wave_review`, `release_review`) and for the authors
who receive the findings. The role contract (`rig/agents/review-agent/guidance/role.md`)
says what each step writes; this says how to review so the record stands up.

## 1. Independence and the exact candidate

- The reviewer is never the author and runs on the other model family; it
  judges the exact SHA named in the packet, in the slice worktree, never a
  moving branch. *Check:* the review file starts with the SHA and `git
  rev-parse HEAD` matches.
- Reviewers do not edit product code or tests. A fix proposal is a finding
  with a required change; the author makes it.

## 2. Proof that everything was reviewed

- **File ledger**: every changed file (`git diff --stat main...slice/<s>`)
  with a verdict line; an unread file is a finding against the reviewer.
- **Context proof**: two or three lines on what the change is for and the
  confidence in that understanding; a review that misunderstood the intent is
  recorded as such and redone.
- **Empirical verification**: run the gate yourself (`scripts/gw --offline
  check`), reproduce any claimed defect (a described failing test or a
  `scripts/http` capture), cite `file:line`. Claims without reproduction are
  marked as such.
- **Audit of upstream evidence**: code review audits the QA evidence
  (coverage CSVs, traceability rows, captures) for consistency with the diff;
  security review audits the threat model against the entry points in the diff.

## 3. Findings: severity, resolution, convergence

| Severity | Meaning | Effect |
|---|---|---|
| MUST-FIX | breaks an AC, the gate, or a security/privacy obligation | `failed` |
| HIGH | wrong against SPEC/design/guide in a way that will cost later | `failed` |
| MEDIUM | should change; does not block | recorded, tracked |
| LOW / INFO | style, small simplification, observation | recorded |

- Finding format: `id | severity | file:line | evidence | required change`.
- The author answers **every** finding in a `## Review response` (fixed /
  disputed with reasoning / withdrawn by the reviewer); the reviewer appends
  `## Re-review <sha>` with each finding's status and the new verdict.
- Convergence: only MUST-FIX/HIGH fail; re-review is scoped to the findings
  and the fix; settled findings are not reopened without new evidence; a
  disagreement that survives one round goes to the lead with both positions.
- The ledger row in `docs/review/REVIEW-LEDGER.md` (slice, step, SHA, files
  changed/reviewed, counts by severity, verdict, reviewer) is the index the
  evaluator reads first.

## 4. What each review judges

| Step | Judge against | Fails on (HIGH) |
|---|---|---|
| requirements_review | `requirements.md` §2, §6; `docs/REQUIREMENTS.md` allocation | AC not testable from outside; missing failure/privacy AC; allocated id without an AC; design leaked; widened scope |
| design_review | `architecture.md` §3–§8 and §11 (the register), `databases.md` §8; brownfield: `brownfield.md` §2, §7 | AC unreachable; error AC without a problem detail; migration without rollback; threat model misses an entry point; speculative structure or dependency; a registered cross-cutting concern redefined without its ADR, or a needed consistency verdict missing |
| code_review | `java-spring.md` §6 and §8 (Javadoc), `qa.md` §2–§3, ponytail lens (§5 below) | AC without a test that would fail; leakage in errors; PII in logs; logic in the wrong layer; QA evidence inconsistent with the diff; Javadoc missing on a public type/method is a red gate (`javadoc -Xdoclint:all -Werror`), Javadoc that only restates the signature is MEDIUM |
| security_review | the checklist in the role file + §6 below | reachable open redirect/SSRF/injection; spoofable rate limit; raw PII; exposed internals; secret in repo |
| decomposition_review | `decomposition.md` §9 | layer slices; overlapping territories; unallocated requirement ids; tier without reason |
| wave_review | the integrated range on `main` vs. the SPECs, `architecture.md` §1–§2 and the §11 register (one line per concern) | drift from the doghouse; cross-slice inconsistency, including two code paths for one registered concern; coverage or traceability regressed after merge |
| release_review | `release.md` §9 | a RELEASE.md claim without evidence; smoke not on the exact SHA; rollback untested; known gap omitted |

## 5. The over-engineering lens (`ponytail-review`)

Walk the diff hunting only complexity: `delete:` (does not need to exist),
`stdlib:`/`native:` (the platform already does it), `yagni:` (speculative),
`shrink:` (same behaviour, less code). One line per finding in a
`## Ponytail review` section. Severity: a new dependency or layer the SPEC does
not need = HIGH; a hand-rolled JDK/Spring facility = MEDIUM; `shrink` = LOW; a
`// ponytail:` comment naming a real ceiling is accepted intent. The diff's
best outcome is getting shorter.

## 6. Security review — the shortener checklist, extended

The row-per-item checklist lives in the review role (`security_review`
section): redirect target and scheme allow-list, no server-side fetch,
parameterised SQL, alias/reserved words, trusted-proxy rule, PII and log
hygiene, error leakage, headers/CORS, actuator and H2 exposure,
dependencies, compliance tests. Add, per candidate:

- the request id is server-issued and an inbound `X-Request-Id` is ignored (canary test);
- short-code keyspace and rate limit make enumeration impractical (state the numbers);
- API documentation (`/v3/api-docs`, `/swagger-ui.html`) exposure is a decision per profile, recorded in the design;
- a dependency-advisory check at release (`tools/dep-advisories.mjs`, OSV) with a **reachability argument per advisory** — reachability is not a fix; the remediation is routed and dated;
- the audit table has no update/delete path; audit rows written in the change's transaction;
- no secrets in the repo or image; container runs non-root with a read-only filesystem except `data/`.

Status per item: pass / fail / n-a, each with its evidence path.

## 7. Anti-slop and drift

Duplication of something a few files over; divergence from the established
pattern for the same problem; abstractions with one implementation; tests
that assert implementation details; a slice that quietly grew beyond its SPEC
("while I was there"); documentation that describes the plan instead of the
behaviour. Each is a MEDIUM at least; drift from the SPEC is HIGH.

## 8. Reviewer's self-check (recorded at the end of every review file)

1. SHA stated and verified; worktree clean at that SHA.
2. Every changed file in the ledger with a verdict.
3. Gate run by me; each defect reproduced or marked unverified.
4. Each finding has severity, `file:line`, evidence, required change.
5. Verdict follows the severity rule; residual risks named in the handoff note.
6. Ledger row appended; nothing edited outside `docs/review/`.
