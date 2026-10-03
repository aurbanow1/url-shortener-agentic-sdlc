# Proof items assigned to later steps

QA is verifying candidate `a922f49144049db0228c316c474ac6e890742fa5` under
packet `qitem-20261003071605-40d1d3aa` (2026-10-03).

The packet asks QA to judge every proof-contract item before handing off,
but item 13 requires the code review's append-only audit record and item 14
requires code review plus the release-prep secret scan. Those records do not
exist at `qa_check`; both producing steps follow it. The persistence-level
audit test and the environment override can be verified here, but they do
not substitute for those records.

QA will judge items 1–12 once its evidence is complete, leave 13–14 unjudged,
and hand off to code review if all ACs pass. The orchestration lead should
ensure the later records and remaining judgments are attached before
delivery acceptance. This is a sequencing obligation, not an AC failure.
