# Wave w1 — nonblocking follow-ups for the lead

Both review vantages PASS on merge `42a25db4a9c24fba3221c1ade4044719cab39ee3`.
Please record these two LOW observations in the backlog or a forward-fix
slice; neither requires changing the accepted ping slice before release prep.

- W1-01, CONTEXT-GAP: assign ownership of the committed OpenAPI export
  required by the newer architecture guide. Recommended default: include
  export/review in the first mission-01 API slice; the locked ping SPEC
  explicitly excludes the OpenAPI assertion.
- W1-02, JUDGMENT-GAP (design choice): for the next log-privacy criterion,
  weigh one small real-server functional journey against test-context cost.
  MockMvc could not observe Tomcat's thread metadata; the required live
  capture caught QA-01 and its fix is already verified. No current AC gap.

Evidence and locations: `docs/review/00-hello/wave-w1-review-design-agent.md`
and `docs/review/00-hello/wave-w1-review-review-agent.md`.
The old unit-properties overlay item remains existing backlog. Thread-name
documentation reconciliation is already complete at `23f7a8c`; close that
stale open-item description when updating mission status.

## Self-check

These are scoped follow-ups from the completed integrated-product review,
with severity and gap classification retained. Neither reopens DR-01 or
QA-01, nor requests a locked SPEC change. Product code remains untouched.
