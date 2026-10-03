# 02-analytics DR-01 — focused convergence escalation

Review packet: `qitem-20261003092345-52dd8a6e`.
Instance: `01M40CP0JVZZWR8226MNN8XCCV`.
Candidate: `45c98c469e7f18f7cdf4b098a03a851fa56d073d`.
Review: `docs/review/02-analytics/design-review.md`, appended re-review.

DR-01 remains HIGH after one correction round; DR-02/03/04 are fixed. Under
the role's repeat-finding convergence rule, this asks the lead to direct a
focused correction, not to change product scope or waive the finding.

The original required bounded shutdown and queued/in-flight loss accounting.
The revision fixes the bound but assumes pool shutdown forces the in-flight
daemon writer to report. The real-H2 probe disproves that assumption. A
normal child-JVM exit after the five-second drain accounts only five of six
clicks: two written, three queued losses reported, one active write still
blocked and never reported. Evidence and reproducible Gradle tasks:

- `docs/review/02-analytics/proof/design-exit-boundary.txt`
- `docs/review/02-analytics/proof/ExitBoundaryProbe.java`
- `docs/review/02-analytics/proof/design-revision-boundary.txt`
- `docs/review/02-analytics/proof/revision-boundary.gradle`

Producer position (design-agent, 09:33Z): the pool-close claim was unprobed
and is wrong; proposes an atomic in-flight task reference, shutdown taking
ownership of its report, completion suppressing duplicates, and a test with
an interruption-ignoring store. The proposed reason explicitly says the
in-flight database outcome is unknown. No corrected artifact has yet been
handed off, so that proposal is not treated as implemented evidence.

Recommendation: authorize that focused design correction and one re-review
on a concrete SHA, preserving the five-second bound, request-correlated
private reporting, honest unknown-outcome wording and duplicate suppression.
Verify worker-start/completion races as well as a latch-blocked store and
normal process exit. Do not reopen DR-02/03/04 or redesign the analytics
feature. If the lead instead parks the residual risk, record the rationale
and any requirement consequence explicitly.

Continuation: resolve this escalation with the candidate/evidence path and
return it to the current review packet. Review stays waiting meanwhile.
