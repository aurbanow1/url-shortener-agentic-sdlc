# Progress — Audit Columns

> Durable acceptance state for this slice. In-process steps belong in the
> working agent's todo tool. See the `mission-slice-sop` skill and the
> conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo,
> `$OPENRIG_HOME/reference/sdlc-conventions.md` when installed).

## Acceptance

- [x] Implementation complete — candidate `305f804`, rebased onto `02-click-retention`'s merge `ed2b940` (`main` `2566c38`); V4 follows V3 (builder, 2026-10-03)
- [x] Tests passing — `check --rerun-tasks` on `305f804`: unit 218/218, functional 233/233, 557/557 lines, 200/200 branches (`proof/builder-check-305f804.txt`; builder run, QA re-runs independently)
- [ ] Review approved

## Builder

- [x] Test first: `8f72b2f` red (`proof/builder-red-functional.txt`, `builder-red-unit.txt`), then `49f0cee` green
- [x] Rebased onto `02-click-retention`'s merge. V4 is the next Flyway number. Two shipped tests pinned to V3 under the lead's grant `132a884`. `check` re-run green.
- [x] By-effect upgrade from the real `f6dd29e` jar (`proof/upgrade-0` to `upgrade-4`, `jar-log-upgrade.txt`)
