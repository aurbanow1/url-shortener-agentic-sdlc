---
slice: OPR.99.0.2.1
candidate_sha: a922f49144049db0228c316c474ac6e890742fa5
artifact_type: qa
verdict: PASS
money_evidence: "bootRun on a922f49: POST /api/links 201 Location
  /api/links/QUPpzC7N, GET /QUPpzC7N 302 Location verbatim with Cache-Control
  no-store, after DELETE 204 the Visitor gets a 410 problem detail; one ECS line
  per request with requestId equal to the header and no canary; audit rows
  link.create and link.retire carry those request ids"
evidences:
  - "6"
  - "7"
  - "8"
  - "9"
self_check: "I opened every capture: the 201/200 bodies are identical, the 302
  Location equals the stored url byte for byte with Cache-Control no-store, the
  410 and 400 are application/problem+json with instance urn:uuid of their own
  X-Request-Id; the six log lines carry exactly those ids; the grep for
  canaries, loopback addresses and curl/ over the run log returned 0; the two
  audit rows carry the create and retire request ids; the OSV file lists 0
  findings for 90 coordinates"
---

# Builder evidence — 01-create-redirect, candidate a922f49

Builder seat `development-agent@urlshort-factory`. Not a verdict: QA, code review and
security review judge independently. Narrative, commands and deviations: `PROOF.md` §Builder.

- Contract item 6: `20aef57` holds only the dependency overrides (`build.gradle.kts`), `scripts/gw check`
  green on it alone; fresh OSV run over its 90 runtime coordinates found 0 advisories
  (`osv-advisories-20aef57.json`, tree `runtime-dependencies-20aef57.txt`).
- Contract item 7: captured exchanges from `bootRun` on candidate `a922f49` — 201 create, 200 read,
  302 redirect (`Location` verbatim, `Cache-Control: no-store`), 204 retire, Visitor 410 problem detail
  without `Location`, 400 `url`/`scheme` problem detail.
- Contract item 8: the six JSON log lines, each `requestId` equal to its captured header; the whole run log
  contains no canary, user agent or client address.
- Contract item 9: the `link.create` and `link.retire` audit rows for the captured code, with actor,
  action, entity, before/after, request id and time.
- Supporting: `builder-check-a922f49.txt` (full gate, `--rerun-tasks`, unit 72/72, functional 87/87,
  100 % line/branch), `builder-red-functional-before-link-feature.txt` (64 of 85 journeys failing before
  the feature code existed).

## Media

![runtime-dependencies-20aef57.txt](runtime-dependencies-20aef57.txt)
![osv-advisories-20aef57.json](osv-advisories-20aef57.json)
![http-201-create-a922f49.txt](http-201-create-a922f49.txt)
![http-200-read-a922f49.txt](http-200-read-a922f49.txt)
![http-302-redirect-a922f49.txt](http-302-redirect-a922f49.txt)
![http-204-retire-a922f49.txt](http-204-retire-a922f49.txt)
![http-410-visitor-gone-a922f49.txt](http-410-visitor-gone-a922f49.txt)
![http-400-invalid-url-a922f49.txt](http-400-invalid-url-a922f49.txt)
![log-lines-a922f49.txt](log-lines-a922f49.txt)
![bootrun-log-a922f49.txt](bootrun-log-a922f49.txt)
![audit-rows-a922f49.txt](audit-rows-a922f49.txt)
![builder-check-a922f49.txt](builder-check-a922f49.txt)
![builder-red-functional-before-link-feature.txt](builder-red-functional-before-link-feature.txt)
