---
slice: OPR.99.0.2.3
candidate_sha: a7c533ffef55650e5b422377ffe0c4e38d41400c
artifact_type: qa
verdict: NOT-CLEAR
money_evidence: Review reproduces stale-time bucket reset and false R0
  completion; prior sequential effects stand but the shutdown instrument claim
  is withdrawn.
evidences:
  - "11"
  - "12"
self_check: Read independent review findings and probe output, corrected the
  scope of the earlier script-only drain observation, and withdrew item 12; no
  new candidate check or product edit.
---

# QA correction after independent review — a7c533f

2026-10-03 UTC, `qa2-agent@urlshort-factory`. Review commit `43cccf5`
returned the candidate to implement. No new QA packet is owned; this corrects
the prior QA interpretation, not a re-check or a product fix.

The review independently reconciled the fresh gate, all saved coverage hashes,
184-method traceability, 2,303 HTTP captures, 30 rejection correlations and
the API comparison. Those observations stand as historical evidence for
`a7c533ffef55650e5b422377ffe0c4e38d41400c`. They did not cover reordered request
timestamps or a truncated shutdown response.

- **CR-01 / SEC-01 HIGH:** a delayed ordinary request resets a bucket spent by
  newer requests: the actual-class probe admitted 120 requests in 1 ms without
  a backward clock step. Sequential/frozen-clock effects do not establish the
  concurrency guarantee. Source: `docs/review/03-operate/01-code-review.md`
  and its `proof/rate-boundary-a7c533f.txt` / `RateLimitBoundaryProbe.java`.
- **CR-02 HIGH:** the shipped R0 reader/predicate accepts a header-only 201
  advertising a 100-byte body but delivering none, and credits its request id
  as delivered. QA's earlier `--drain` result established the script's own pass,
  its reported status/counts and probe result; it did **not** independently
  establish the complete R0 body or enforce the total 10-second deadline.
  Source: review `proof/smoke-r0-boundary-a7c533f.txt` and its probe.
- **CR-03 MEDIUM:** supported clock rollback stalls stale-client cleanup;
  10,000 full buckets remain after 61 seconds and a further request. Item 11
  remains unaccepted; the review's privacy pass does not establish its memory
  obligation.

The review report/probe output was read; QA did not independently rerun these
probes in this notification turn. The review's findings are the attributed
source of the correction. The current candidate is **not merge-ready**.

QA withdraws item 12's acceptance: the smoke modes and bench capture exist,
but the release-level shutdown steps are not a trustworthy completion check.
Items 1–10 retain their narrower observed/test/artifact claims; item 11 and
the explicitly release-owned item 13 remain unaccepted. The original QA PASS
is superseded by the failed independent review, not a ship approval.

Next assigned QA check must exercise the repaired timestamp ordering, cleanup
after rollback, full-body R0 delivery and false-response rejection, and the
total completion deadline, as well as the complete candidate gate and normal
journeys. The bench offered-rate gap remains release work; security review
explicitly retains the disk-gauge path LOW in the current loopback scope.

## Self-check

Read both reports and their recorded effects; compared the finding to the
actual scope of QA's prior script observation; preserved historical captures;
edited no product source/tests and performed no unassigned candidate review.
