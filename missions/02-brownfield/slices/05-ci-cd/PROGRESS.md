# Progress — Ci Cd

> Durable acceptance state for this slice. In-process steps belong in the
> working agent's todo tool. See the `mission-slice-sop` skill and the
> conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo,
> `$OPENRIG_HOME/reference/sdlc-conventions.md` when installed).

## Acceptance

- [x] Implementation complete — candidate `add7ab5` (`.github/` only, copied byte for byte from the locked drafts)
- [x] Tests passing — local equivalents on `add7ab5`: actionlint 0 errors, YAML parsed, `check` green (165 unit / 155 functional, 100 % line and branch), `--jar` smoke OK, image built; AC-13 (GitHub runs) pending the human's push
- [x] Independent QA complete — exact `add7ab5ca37dcd6f51aef3cd43c85455e1be6d14`, fresh165/155, merged100/100, YAML/source AC-1..11, own jar smoke and stopped process; completed builder --pull image and attached pins checked. AC-13 explicitly PENDING under SPEC A-5; All seven proof-contract items accepted against this exact SHA (receipts00000001..00000007), current readiness ready7/7; evidencef10c796.
- [x] Review approved — code and security review PASS on `add7ab5` (`review2-agent`, `e0c0a12`; no findings)
- [x] Integrated — merged `--no-ff` into `main` as `0aa3695` (lead, 2026-10-03T20:32Z); gate re-run fresh on `main` with `--rerun-tasks`, green (`docs/evidence/02-brownfield/integrate-05-ci-cd-check-0aa3695.txt`); tag `slice/05-ci-cd/accepted` on `add7ab5`; worktree removed. Proof readiness at merge: 5 of 7 read accepted, items 5 and 6 `unknown` from shared-document drift (TRACEABILITY.md and GAPS.md edited for `01-audit-read` afterwards; review2 verified the `05-ci-cd` sections unchanged), re-judged by the scheduled final re-affirmation `qitem-20261003195138-8eb72ecb`
- [ ] AC-13 — the first GitHub runs (`gate` on the pull request, `cd` on `main`), pending the human's push; the operator records their URLs in `PROOF.md`
- [ ] Delivery stamp — after the mission's ship sign-off

- [x] AC-13 first GitHub runs recorded — operator b6b4a29: PR gate success;
  main CI/CD success with urlshort-jar and smoke-logs. QA follow-up uses the
  packet-permitted operator source because page/API retrieval failed; no
  independent GitHub fetch claim. Successful main runs contain Gradle9.8.0,
  adopted locally at f3e6b0b; deliberate red run remains unexercised.
