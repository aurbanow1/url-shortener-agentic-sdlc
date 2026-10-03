# Progress — Click analytics

> Durable acceptance state for this slice. In-process steps belong in the
> working agent's todo tool. See the `mission-slice-sop` skill and the
> conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo,
> `$OPENRIG_HOME/reference/sdlc-conventions.md` when installed).

## Acceptance

- [x] Implementation complete — candidate `862c52e` on `slice/02-analytics` (builder, 2026-10-03)
- [x] Tests passing — `scripts/gw --offline check --rerun-tasks` on `862c52e`: unit 121/121, functional 126/126, merged coverage 100 % line and branch, Javadoc gate green (`proof/builder-check-862c52e.txt`; builder run, QA re-runs independently)
- [ ] Review approved

## Builder-side proof-contract items

- [x] Captured exchange: three redirects with UA, Referer path/query and forwarding canaries, then the statistics (`proof/http-*-862c52e.txt`)
- [x] Stored click rows for that capture: origin, class, hash, no canary or raw address (`proof/click-rows-862c52e.txt`)
- [x] JSON log lines for those requests and their recording, `requestId` = header, no canary/address/hash/referrer (`proof/log-lines-862c52e.txt`)
- [x] `docs/api/openapi.json` regenerated on the candidate with the statistics operation and its example; drift fails the suite
- [x] `docs/diagrams/erd.mmd` shows `click` and its relation to `link` (on `main`, by the design step)
- [ ] QA: coverage reports, traceability, GAPS row (NFR-L3), live-vs-committed API document diff, independent captures
- [ ] Design/security review record on salt handling (rule 4); release bench for NFR-L3
