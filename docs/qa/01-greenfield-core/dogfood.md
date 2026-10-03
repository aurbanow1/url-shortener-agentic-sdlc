# Installed-artifact dogfood — 01-greenfield-core

QA Agent/Codex; packet `qitem-20261003151220-a22accf5`; 2026-10-03.
Candidate: `f090103` (product inputs matching merge `8e9c065`).
Completed: two known defects confirmed (one MEDIUM, one LOW); no additional
defect or severe failure identified. Session 15:13–15:41Z, including setup,
exploration and stored-effect checks; jar lifetime 15:16:18–15:41:29Z.
The packaged JDK 21 jar ran on loopback with disposable H2 databases.
The release narrative appeared during this pass; its existing AC-28 host gap
was read as context, not independently tested or accepted here.

Evidence: [artifact and hash](dogfood/artifact.json),
[HTTP ledger](dogfood/http-ledger.json),
[reconciliation](dogfood/verification.json),
[correlated JSON events](dogfood/correlated-events.jsonl), raw exchanges and
complete app logs under dogfood/. There are **112 individually captured HTTP
calls / 114 response hops**, all with one matching completion and status.
Four early sandbox-client connection failures and two post-stop refusals are
separate; one diagnostic health call and the passing smoke's requests are not
included in that per-request claim. HEAD body files contain curl's header
dump, not an asserted HTTP payload.
The wire-captures.tar.gz archive preserves original exchange bytes, with
SHA-256 values in wire-captures-manifest.json. Display header files use LF
and omit trailing whitespace/terminal blank lines; Prometheus display text
omits line-end spaces. Other bodies are unchanged. All 230 original archive
members were checked against their saved hashes.

This product has HTTP interfaces. Raw request/response and JSON-log captures
provide reproduction evidence. No browser binary is installed in this seat;
Swagger UI rendering and screenshot/video evidence are outside this pass.

| ID | Severity | Status | Expected / observed | Reproduction and evidence |
|---|---|---|---|---|
| W2-01 | MEDIUM | Already found; independently reproduced twice | Validation errors should be described by OpenAPI; runtime top-level errors[] is absent from its ProblemDetail schema, which instead describes a nested properties member | dogfood/exchanges/19-invalid-url.body, 20-invalid-url-repeat.body, 21-live-openapi.body |
| W2-03 | LOW | Already found; independently reproduced twice | Anonymous metrics should avoid unnecessary installation details; disk gauge exposes its working-directory path tag | dogfood/exchanges/22-prometheus.body, 23-prometheus-repeat.body; release container capture confirms /app/. |

## W2-01 — Validation-error schema omits the wire extension

Creator/client-contract defect, MEDIUM. On the running packaged jar:

1. `scripts/http -sS -i -X POST -H 'Content-Type: application/json' --data-binary '{"url":"javascript:dogfood-url-canary"}' http://127.0.0.1:18121/api/links`
2. `scripts/http -sS http://127.0.0.1:18121/v3/api-docs`
3. Compare the response with `components.schemas.ProblemDetail`.

Expected: schema describes optional top-level `errors` entries with field,
rule and message. Observed twice: 400 response contains that array; schema
has no errors property and instead includes a nested properties object.
The 422 conflicting-key response also carries errors[]. The submitted
canary is absent from the problem response, so this is documentation drift,
not an observed input leak. Evidence: exchanges 18–21 above, with headers
and bodies retained. Previously recorded as W2-01 / create CR-01 / W2D-01.

## W2-03 — Anonymous disk metric exposes an installation path

Operator/privacy polish defect, LOW under the loopback deployment boundary.

1. `scripts/http -sS http://127.0.0.1:18121/actuator/prometheus`
2. Find `disk_free_bytes` and `disk_total_bytes`; repeat the scrape.

Expected: no unnecessary filesystem-layout details on the anonymous scrape.
Observed on both independent jar scrapes: disk meters include the full
working-directory `path` tag. Existing installed-container evidence shows
the same meter with `/app/.`. No client canary appears in those metrics.
Evidence: exchanges 22/23 above and the packet's container capture.
Previously recorded as W2-03 / QA-OPR-02 / W2D-04.

## Observed journeys

| Persona | Observed effect | Evidence exchanges |
|---|---|---|
| Creator | Exact create/read; distinct unkeyed duplicates; keyed replay and conflict preserve binding; corrected invalid first use succeeds; Host spoof cannot alter configured shortUrl | 09/10, 17/18, 29/30, 50, 54–57, 66–72 |
| Creator mistakes | Safe 400 for invalid URLs/JSON/keys; 415 media; 16,384-byte body 201 and 16,385 bytes 413; missing/wrong-method/Accept 404/405/406 | 19/20, 31–49, 73/74 |
| Visitor | Original-target no-store 302; short-link query ignored; local follow reaches 200 through 302; HEAD adds no click; retired visit 410 | 12–15, 25, 52, 58, 89, 92 |
| Analyst | Empty/three-click stats and reduced origins; history after retire/restart; new visits increase one to two then five, ranking community=2/newsletter=1; HEAD/OPTIONS do not add clicks | 11/16, 27, 53, 60–62, 88–93, 96–99 |
| Operator | Status-only probes and metrics; hidden H2/env 404; Swagger landing reachable, rendering untested; configured budgets 2 API/3 redirect then safe 429; forwarded spoof cannot bypass; exempt surfaces 200; natural quiet interval refills; later retry returns original code | 05–08, 48/49, 63/64, 75–85, 100–103 |
| Operator restart | Active/retired details, idempotency binding and prior statistics survive restarting the same jar/database | 86–95, db/*-before.csv and *-after.csv |

Primary reconciliation: **12 links and 14 audit rows unchanged** across restart,
reads and replays. Click rows grew from six to ten, matching four new visits.
Each of four concurrent keys has one stored link and one create audit row.
The small-budget database has one link/one audit/six admitted clicks; four
rejections added no records and were counted by the rejection meter. Clicks
hold origin, UA class and 64-hex hash only; supplied header/path/query/address
canaries are absent. Logs contain no such canary or stored client hash.

Concurrent same-key double submit was explored four times: one 201 and one
safe 500 each, then retry returns the winner. This is the explicitly accepted
at-most-one-link boundary (01-create-redirect rule 5 and ADR-0007), not a newly
failed AC. Offline rows confirm that no loser mutation survived. This is a
client retry boundary, not a promise that both concurrent calls return 201.

The release agent's updated main smoke helper also passed against the saved
candidate jar; its blob is recorded in dogfood/smoke-helper.json. The saved
f090103 helper copy was not the script executed. The jar product hash remained
fixed and product/config/API inputs still matched the packet.

## Self-check

Exact product/config/API inputs compared with f090103; saved jar hash unchanged.
Raw effects, all 114 response-hop ids, safe problems, privacy canaries,
idempotency winners, stored audits/clicks and live API document checked.
Both defects reproduced twice and recorded incrementally. All three jar runs
completed graceful shutdown (intentional exit 130); both ports then refused
connections. No product/test/threshold/release edits by QA.

Not checked: browser rendering/video; Docker/port-forwarder shutdown; latency
targets; natural midnight or 24-hour expiry; alternate actual TCP peers,
live reverse proxy, PostgreSQL, backups or adversarial capacity. CLI settings
supplied the small budgets; release's environment/container proofs are separate.
No complete AC gate or ship approval is claimed. W2-01/W2-03 are real mission-02
inputs; no bug was seeded.
