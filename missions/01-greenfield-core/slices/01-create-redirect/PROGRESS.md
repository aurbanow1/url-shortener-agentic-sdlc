# Progress — Create and redirect

> Durable acceptance state for this slice. In-process steps belong in the
> working agent's todo tool. See the `mission-slice-sop` skill and the
> conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo,
> `$OPENRIG_HOME/reference/sdlc-conventions.md` when installed).

## Acceptance

- [x] Implementation complete — candidate `a922f49` on `slice/01-create-redirect` (builder, 2026-10-03)
- [x] Tests passing — `scripts/gw check --rerun-tasks` on `a922f49`: unit 72/72, functional 87/87, merged coverage 100 % line and branch, Javadoc gate green (`proof/builder-check-a922f49.txt`; builder run, QA re-runs independently)
- [ ] Review approved

## Builder-side proof-contract items

- [x] First commit `20aef57` contains only the dependency overrides, green alone, fresh OSV run 0 advisories (`proof/osv-advisories-20aef57.json`)
- [x] Captured exchange 201/200/302/204/410/400 from the running service (`proof/http-*-a922f49.txt`)
- [x] JSON log lines for those requests, `requestId` = header, no canary/UA/address (`proof/log-lines-a922f49.txt`)
- [x] Audit rows for the captured create and retire (`proof/audit-rows-a922f49.txt`)
- [x] `docs/api/openapi.json` committed, generated key-sorted by `OpenApiDocumentTest`, drift fails the suite
- [ ] QA: coverage reports, traceability, GAPS row, live-vs-committed API document diff, independent captures
- [ ] Code review / security review: NFR-A2 and NFR-S4 records
