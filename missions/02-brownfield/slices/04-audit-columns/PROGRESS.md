# Progress — Audit Columns

> Durable acceptance state for this slice. In-process steps belong in the
> working agent's todo tool. See the `mission-slice-sop` skill and the
> conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo,
> `$OPENRIG_HOME/reference/sdlc-conventions.md` when installed).

## Acceptance

- [ ] Implementation complete — built test-first as `49f0cee` on `main` `583278c`; the candidate is the rebase onto `02-click-retention`'s merge, which is pending (builder, 2026-10-03)
- [ ] Tests passing — `check --rerun-tasks` on `49f0cee`: unit 208/208, functional 211/211, 496/496 lines, 194/194 branches (`proof/builder-check-49f0cee.txt`); to be re-run on the rebased candidate
- [ ] Review approved

## Builder

- [x] Test first: `8f72b2f` red (`proof/builder-red-functional.txt`, `builder-red-unit.txt`), then `49f0cee` green
- [ ] Rebase onto `02-click-retention`'s merge commit, confirm V4 is the next Flyway number, re-run `check`, run the by-effect upgrade, hand off that SHA
