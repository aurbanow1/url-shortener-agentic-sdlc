# Capturing requirements an agent can build from

A requirement here is a literal instruction to a builder who cannot ask you
questions and to a tester who must prove it from the outside. Write for both.

## 1. Shape of a slice SPEC

In this order, always (the projection and the audits depend on the headings):

1. **Intent** — one or two sentences: who feels what pain, what changes for them. Verbatim from the slice's `intent:` frontmatter plus the "why now".
2. **Mini-requirements**
   - *Requirements covered* — the `docs/REQUIREMENTS.md` ids this slice implements (`FR-n`, `NFR-n`); every allocated id gets at least one AC
   - *Personas* — the roles that touch this slice (primary / secondary). A persona is someone with a goal, not a system.
   - *User stories* — `As a <persona>, I want <capability>, so that <outcome>`. One story per outcome. If a story needs "and", split it.
   - *Acceptance criteria* — numbered `AC-n`, `GIVEN / WHEN / THEN`, each one behaviour, each observable from the public surface (HTTP status, headers, body fields, a log line, a stored row) — never "the service shall handle errors gracefully".
   - *Business rules* — the non-obvious logic as `When <condition>, then <behaviour>`; rules are where defects hide (alias uniqueness, expiry semantics, what counts as a click).
   - *Non-functional* — only what this slice must *prove*, quantified: limits, latency budgets, retention, logging and audit obligations.
   - *Scope* — in scope / **explicitly out of scope** (the second list prevents the next slice from being built by accident).
3. **Ambiguity log** — every question you had, the options, and the resolution: `decided` (with the reason), `assumed` (the safe default and why it is safe), or `parked on human` (packet id, then the recorded decision).
4. **Proof contract** — one checkbox per promised observable outcome; always include the coverage reports, the traceability rows, the gap entry and the by-effect captures.
5. **Self-check** — the author's own definition-of-done, recorded (see §5).

## 2. Acceptance criteria that work

- **Observable**: a test can see it without reading the implementation. "Returns `409 Conflict` as a problem detail with `title: Alias already taken`" — yes. "Validates the alias" — no.
- **Independent**: each AC passes or fails alone; no AC depends on the order of others.
- **Complete at the edges**: for every happy path, the failure paths are ACs too — invalid input, duplicates, not-found, expired, forbidden, rate-limited, wrong method. Privacy obligations (what must *not* be stored or logged) are ACs, proven with canaries.
- **Precise about data**: name fields and formats (`time` is an ISO-8601 UTC instant ending in `Z`), state exactness ("exactly these two fields") and tolerances (compare at seconds precision).
- **Derivable test name**: `AC03_duplicateAliasReturns409Problem` should be obvious from the text.
- **Framework-neutral**: no class names, schemas, libraries. Those are design.

Checklist before handing off: every AC maps to a user story or a business rule; every story has at least one AC; every error path the design will have to handle appears as an AC; no two ACs test the same thing.

## 3. Ambiguity policy

Ask of every open question: *would a wrong guess change what gets built, and is there no safe default?*

- If yes → park it on the human with the question, the options, your recommended default and the consequence of each; keep working on everything else.
- If a safe default exists → choose it, log it as `assumed` with the reason it is safe (usually: reversible, strict subset, no data impact), and move on.
- Never resolve ambiguity by widening scope ("we'll support both"). Narrow is safe; wide is a decision someone else owns.
- The human's recorded decisions (gate resolutions, mission brief) outrank your defaults; cite them in the log.

## 4. Non-functional requirements that mean something

| Weak | Usable |
|---|---|
| "fast" | p95 redirect latency ≤ 20 ms at 100 req/s on the reference laptop, measured by `scripts/smoke.sh --bench` |
| "secure" | only `http`/`https` targets accepted; `javascript:`/`data:` rejected with `400`; no raw client IP stored or logged (AC with canary) |
| "scalable" | out of scope for this slice (say so) |
| "audited" | every create/delete writes one `audit_log` row with actor, action, entity id, request id, before/after |

If you cannot name the number, the check, or the AC, it is not a requirement yet — park it or scope it out.

## 5. Definition of ready (the author's self-check)

Record under `## Self-check` in the SPEC, one honest line each:

- every AC observable from the public surface or logs; error and privacy paths present
- business rules cover the non-obvious logic; out-of-scope list explicit
- every ambiguity row resolved as decided / assumed / parked, with reasons
- proof contract names: coverage reports, traceability rows, gap entry, by-effect captures
- no design leaked (schema, class names, libraries)
- consistent with the mission brief and the human's recorded decisions
- a stranger could build and test this from `SPEC.md` alone

## 6. Reviewing a SPEC (for `requirements_review`)

Fail (HIGH) when: an FR/NFR id allocated to the slice (mission brief → `docs/REQUIREMENTS.md`) has no AC; an AC is not testable from outside; a failure path or privacy obligation is missing; an ambiguity was resolved by widening scope; a design decision is embedded; the proof contract omits coverage/traceability/gaps. Record everything else as MEDIUM/LOW with the fix.

## 7. Anti-patterns seen in the wild

- Aspirational language ("should ideally"), future phases, "nice to have" lists.
- ACs that restate the story ("user can create a link") instead of the observable result.
- Hidden requirements in the "Notes" section.
- A single giant AC covering the whole journey — it will produce one test that explains nothing when it fails.
- Treating the human as the first reviewer: the SPEC gets an independent review before any gate.
