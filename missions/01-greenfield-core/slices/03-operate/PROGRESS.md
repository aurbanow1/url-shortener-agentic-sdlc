# Progress — Operate safely

> Durable acceptance state for this slice. In-process steps belong in the
> working agent's todo tool. See the `mission-slice-sop` skill and the
> conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo,
> `$OPENRIG_HOME/reference/sdlc-conventions.md` when installed).

## Acceptance

- [x] Implementation complete — candidate `a7c533f` on `slice/03-operate`, descends from 02's merge `091ff46` (builder, 2026-10-03)
- [x] Tests passing — `scripts/gw --offline check --rerun-tasks` on `a7c533f`: unit 163/163, functional 155/155, merged coverage 100 % line and branch, Javadoc green (`proof/builder-check-a7c533f.txt`; builder run, QA re-runs independently)
- [ ] Review approved

## Builder-side proof-contract items

- [x] Item 6: admitted create, the 429s after exhaustion, liveness and readiness UP, Prometheus excerpt with the rejection counter (`proof/http-*-a7c533f.txt`, `proof/prometheus-excerpt-a7c533f.txt`)
- [x] Item 7: one JSON log line per captured 429, `requestId` = header, no client address, forwarded value, user agent or URL (`proof/log-lines-429-a7c533f.txt`)
- [x] Item 8 (builder half): `docs/api/openapi.json` regenerated on a base containing 02's merge, 429 with `Retry-After` and an example on all six operations; QA's live-vs-committed diff pending
- [x] Item 9: `git merge-base --is-ancestor 091ff46 a7c533f` succeeds
- [x] Item 12: `scripts/smoke.sh` holds the release-level modes; `--bench` and `--drain` run on the candidate jar (`proof/smoke-bench-a7c533f.txt`, `proof/smoke-drain-a7c533f.txt`)
- [ ] QA: coverage reports, traceability, GAPS rows for AC-21 to AC-28, API-document diff
- [ ] Review: limiter memory bound and privacy record (item 11)
- [ ] Release: AC-21 to AC-28 against the container and the jar (item 13)


## Builder rework — 1c8b2cf (2026-10-03 UTC)

- [x] CR-01/SEC-01: locked `max(tat, now)` restored, clock read inside the per-client update; the reviewer's probe gives 60 total admissions (was 120)
- [x] CR-02: smoke R0 passes only on a complete 2xx/3xx within 10 s of the stop; the control gives 6/6 (`proof/smoke-r0-control-1c8b2cf.txt`); `--drain` on the jar OK
- [x] CR-03: the release also runs when its deadline is more than 2 s ahead, which only a backward step produces; the probe gives 2 clients (was 10,002)
- [x] Gate `check --rerun-tasks`: unit 165, functional 155, 443/443 lines, 162/162 branches (`proof/builder-check-1c8b2cf.txt`)
- [ ] QA and review re-check on `1c8b2cf`

## QA — a7c533f (2026-10-03 UTC)

- [x] Fresh independent gate: unit 163 / functional 155, zero failures/errors/skips; merged 441/441 lines and 160/160 branches; Javadoc passed.
- [x] All AC-1–AC-20 observed independently in 2,303 real HTTP exchanges, with controlled mechanisms disclosed; no product edits.
- [x] Coverage copies/hashes, all 184-method/318-invocation traceability, individual release gap rows and exact live API diff recorded.
- [x] Plain jar env/smoke/60 s bench/drain observed; apps stopped; candidate worktree clean and unchanged.
- [x] QA proof drop `proof/qa-evidence-a7c533f.md` covers items 1–10 and 12.
- [x] Attributed judgments for items 1–10 and 12: receipts `proof/judgments/00000001.md` through `00000012.md`, actor qa2-agent, subject commit a7c533ffef55650e5b422377ffe0c4e38d41400c. Live proof state confirms these accepted and only 11/13 pending.
- [ ] Item 11: code/security review records.
- [ ] Item 13: release AC-21–AC-28, container inspect/restarts and specified-rate latency judgment.

Pending records are retained by lead obligation `qitem-20261003120849-f4cbfa97` (`docs/qa/03-operate/proof-sequencing.md`). Bench input rate remains below 100/20; no numeric latency-target judgment.

The scope audit reports no 03-operate findings. Receipt 00000012 reaffirms item 6 after adding the required C1 header to the instrument disclosure; observed effects are unchanged.


## Review follow-up — 43cccf5

Current candidate a7c533f returned to implement on HIGH CR-01/SEC-01 and CR-02; CR-03 MEDIUM accompanies the limiter repair. Historical QA captures/gate remain evidence for their observed axes.

- [x] Prior QA shutdown claim corrected: the shipped script reported PASS but did not establish a complete R0 body.
- [x] Item 12 acceptance withdrawn: receipt `proof/judgments/00000013.md`; NOT-CLEAR drop `proof/qa-review-followup-a7c533f.md`.
- [ ] Repaired candidate QA, only after an assigned handoff packet.
- [ ] Review memory item 11 and release item 13 still unaccepted.

Only items 1–10 retain their narrower accepted artifact/test/effect claims; candidate remains not-ready.
