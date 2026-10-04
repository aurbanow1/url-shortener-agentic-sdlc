# Mission02 installed pre-run dogfood evidence

Subject e227acf04e1ea902406532fcebd741f8a86f7f65. Jar SHA-256 fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2; byte equivalence to mission03 is independently recorded. See [the report](../../02-brownfield.md).

settings.json records the two normal starts and the invalid-setting start, all from the same preserved jar. One disposable database was held at7 days, stopped and copied to a stopped backup, then restored at a different disposable path and started with the hold released. No installed binary or product source/test was changed.

http-ledger.json records every fresh request/response interval, method, URL, submitted headers, status and body. Request bodies are application/json. console.jsonl and file.jsonl both contain all30 request-completed events. The invalid-start output is separate because it handles no HTTP request.

raw-captures.tar.gz contains67 original HTTP and log members; raw-manifest.json binds their hashes. Display headers/logs, invalid-start text and Prometheus HELP whitespace are normalized for Git; original headers and bodies compare exactly with the unchanged ledger through the archive.

RUNBOOK-e227acf.md and pinned-openapi.json come from that exact Git subject. The live API and operator observations are checked against these copies. documentation-check.json records the precise Operator/local-relay qualification from pinned DESIGN/ADR-0019. The environment-variable names were read; the tested settings used their CLI property forms.

reused-verification.json points to the independently rechecked mission03 raw archive and405 wire/log exchanges, with selected observed contents. This explicit reuse is authorized by the packet and limited to identical jar bytes; documents and future D21 candidates are separate subjects.

Run python3 docs/qa/dogfood/02-brownfield/e227acf/verify.py from repository root. It verifies jar equivalence, exact pinned Runbook bytes, live/pinned API equality, all30 fresh responses against both sinks, all32 comparisons, purge events/invalid startup, raw-member hashes and shutdown records. Local preserved jars must still be available at jar-equivalence.json's paths.

No final candidate readiness, release benchmark or proof judgment is made by this ordinary pre-run artifact.
