# Release readiness guidance — prepare, prove, stop at the gate

The release package is the last artefact the human reads before signing off,
and the first the evaluator reads to judge the whole mission. Read by the
Release & Reliability Agent at `release_prep`, `evidence_export` and for the
final engineering summary; by the Review Agent at `release_review`; by the
lead at `mission_close`.

## 1. Principle

Release readiness is evidence on the **exact SHA**: a fresh gate run, an
installed smoke of the artefacts that would ship, the reviews that passed,
the gaps that remain and the way back. Nothing is published by an agent;
approval records stamps, the human ships.

## 2. `missions/<mission>/RELEASE.md` contract (sections, in this order)

1. **Decision brief for ship sign-off** — outcome in one paragraph; candidate
   SHA; what was proven and how; human decisions honoured (quote the stamps);
   the items that need the human's judgment; rollback in one line;
   recommended default and the alternative with its cost.
2. **Artifact and gate** — jar/image identifiers (SHA, digest), the CI and CD
   runs for the candidate SHA with URLs and conclusions (`ci-cd.md` §5), the gate
   command and its summary (tests, coverage), proof readiness (`rig proof show`).
3. **Installed smoke** — jar on loopback, container on loopback, what each
   answered, captured logs; what the smoke did **not** exercise.
4. **Dependency advisories** — OSV query on the runtime classpath
   (`tools/dep-advisories.mjs`), one row per advisory: affected artefact,
   severity, fixed-in, reachability on this release; remediation routed as a
   queue item with its id and the deadline rule (before the first slice that
   makes it reachable).
5. **Evidence per slice** — SPEC, design, PROOF, QA coverage/traceability/gaps,
   review files and ledger rows, with the SHAs.
6. **Metrics, read plainly** — the numbers from `docs/metrics/` and what they
   mean for this mission (retries and why, MTTR, human wait, E2E latency).
7. **Known gaps, complete** — every gap from `docs/qa/GAPS.md`, review LOWs,
   advisories, operational limits (single node, H2 file DB), with the owner.
8. **Rollback** — exact commands (`git revert -m 1 <merge>`, migration
   rollback, image rollback), what is lost, how to verify it worked.
9. **Self-check** — one honest line per section.

## 3. Installed smoke rules

- Smoke the artefact that would ship: `java -jar build/libs/urlshort.jar` and
  the image built by `docker compose build` — both bound to **loopback only**
  (`--server.address=127.0.0.1`, `docker run -p 127.0.0.1:<port>:8080`); an
  agent never publishes a port on all interfaces, even for a test.
- `scripts/smoke.sh <base-url>` exercises health/readiness and the public
  journey; capture status, headers, body and the structured log lines under
  `missions/<mission>/release/`.
- Stop and remove what you started; record the pid/container you stopped.
- State what the smoke did not cover (restart durability, load, concurrency)
  and where that is proven instead or recorded as a gap.

## 4. Advisories and reachability

A reachability argument ("authenticator not enabled", "no client input
parsed") is a reason to ship a dry run, not a fix. The remediation (version
overrides, full gate, fresh OSV run) is a routed queue item with a deadline
rule; the next release package reports it closed or still open.

## 5. Rollback plan

Written before the gate, executable by a stranger: the merge commit to
revert, the migrations to reverse and their scripts, the image/tag to
redeploy, the verification (gate + smoke) after rollback, and the data that
cannot be restored. Rehearse it once per mission in the drills (`git revert`
on a throwaway branch counts) and link the drill row.

## 6. Metrics

`node tools/sdlc-metrics.mjs` after the final evidence export; `docs/metrics/README.md`
explains every number's derivation and limits. In the release package,
translate: how many loops, caused by what, how long to repair, how long the
human held the packet. Never tune the counting to flatter the run; a
counting correction is a commit to the tool with its reason.

## 7. Evidence export and mission close

`tools/evidence-export.sh <mission>` at the final state: compiled graph, scope
audit, proof readiness, workflow list/status, every instance trace and show,
every packet's transitions and show (gates included — the human's decision
text must be in the export), usage, an `INDEX.md`. Commit it under
`docs/evidence/<mission>/`; the lead closes the mission with final `PROGRESS.md`
and `NOTES.md`, no worktrees left, backlog recorded.

## 8. The final engineering summary (`docs/FINAL-SUMMARY.md`)

Written once at the end for the evaluator, from the release packages:

1. Plan and rationale — what was built, why this architecture and this
   factory shape (link `PLAN.md`, `docs/ARCHITECTURE.md`, ADRs).
2. Artefact map — assignment item → artefact path → status (the table in
   `docs/guidance/README.md` §2, filled in).
3. The three scenarios — what each demonstrated, with the evidence paths and
   the governance events that actually happened.
4. Validation — gates, coverage, reviews, smoke, metrics; what the numbers say.
5. Risks and trade-offs — taken, rejected, and why (`docs/RISKS.md`,
   decision tables).
6. Assumptions — the `assumed` rows of `docs/REQUIREMENTS.md` and how the
   human resolved them.
7. Limitations — honestly: single-frontier engine and how parallelism was
   achieved, sandbox prompts, single-node product, advisories outstanding,
   anything not done.
8. How to verify in 15 minutes — commands to run the gate, the jar, the
   container, and where to read the evidence without installing OpenRig.

## 9. `release_review` and sign-off checklist (fail = HIGH)

1. Every claim in `RELEASE.md` links to evidence at the stated SHA.
2. Gate re-run fresh on that SHA; coverage at the policy; proof ready.
3. Jar **and** container smoked on loopback with captured logs; untested areas named.
4. Advisories listed with reachability and a routed remediation; none hidden.
5. Known gaps complete and consistent with `docs/qa/GAPS.md` and the review ledger.
6. Rollback commands present, rehearsed once, data loss stated.
7. Metrics generated from the engine records, with derivation notes.
8. Decision brief gives the human a default and the cost of the alternative; nothing was published.
