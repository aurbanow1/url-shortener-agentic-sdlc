# Scenario: ambiguous — mission `03-ambiguous-analytics`

> **Draft — completed at mission close.** `[final]` marks what is filled in from
> `missions/03-ambiguous-analytics/RELEASE.md` and the evidence export.

## The ask

"Marketing says the analytics are not good enough." No numbers, no reader, no
definition of a click. The mission brief said what to do with that: surface
the real questions, decide them with the human before anything is designed,
then build to the decided scope and prove it on top of the shipped v1.

## Requirement understanding

The lead decomposed one slice (`01-analytics-v2`) whose first act is to ask;
the decomposition review passed (9/9 artefacts); the human approved the shape
at the mission plan-lock. The Requirements Agent then wrote the ambiguity log
and parked six decisions on the human (`qitem-20261003154347-19e96a75`), each
with options, a recommended default and the consequence of each:

| # | Question | Decided (2026-10-03T16:10Z, human) |
|---|---|---|
| Q1 | What is counted | B — add unique visitors per UTC day beside raw clicks |
| Q2 | What the daily-salted client hash may be used for | B — distinct visitors within its own UTC day only, never exposed or joined |
| Q3 | Bots | B — bot clicks per day shown beside the unchanged figures |
| Q4 | Retention | A — keep 90 days then delete (mission 02's purge); totals become a rolling window |
| Q5 | Who reads it | A — the existing stats API, fields added |
| Q6 | Day boundary | A — UTC only, so per-day uniques stay exact |

The decision text is recorded verbatim in the queue transition and in the
SPEC; the privacy consequence of Q2 ties click identity to the rate limiter's
trusted-proxy rule (wave-review W2-02).

## Dynamic re-planning

The human answered the six questions 22 minutes after the park (16:10Z,
verbatim in the queue transition: "accept all recommended"). The answer fired
no re-plan trigger. No slice was added or split, and mission 02's 90-day purge
was unchanged, because Q4 A matches it. The plan still moved three times
afterwards, and each move is in `missions/03-ambiguous-analytics/NOTES.md`:

- **A cross-mission human decision.** The audit-column policy (18:01Z) applies
  to any new table. The slice's design adds none, so it is met by mission 02's
  `04-audit-columns`. That is recorded rather than built twice.
- **A design-time grant.** Q2 B ties a visitor's identity to the rate
  limiter's client rule (wave-review finding W2-02). The design needed
  `RateLimitFilter` to expose that rule, so the lead granted the file and its
  test at 18:36Z: additive only, recorded in the slice manifest with a
  revision receipt.
- **An earlier, stacked build.** The plan-lock was held behind mission 02's
  `02-click-retention`, which changes the same `click/` code. On the human's
  request to start waiting work, the lead released it at 20:03Z, stacked on
  click-retention's reviewed branch and built on the second builder seat. The
  candidate must descend from click-retention's merge commit, checked at
  integration.

The compiled graph's revision receipts are listed in the mission notes;
`[final]`: anything that changes between now and the mission close.

## Orchestration and validation

The slice ran the full workflow, with no failed review at any step. Times
are UTC, from the instance trace (`01M416Z3CM54YQTX93V4KG0CPS`):

| Step | Closed | Seat | Note |
|---|---|---|---|
| requirements, then the ambiguity park | 16:13 | requirements-agent | six questions parked on the human 15:48; answered 16:10 |
| requirements_review | 16:41 | review-agent | pass |
| design | 18:42 | design-agent | waited behind mission 02's two designs on the single design seat; the second design seat came later (D16) |
| design_review | 18:56 | review-agent | pass, one MEDIUM fixed in passing |
| plan_lock | 20:03 | orchestration-lead | delegated (D11); held behind click-retention, then released early on the human's request |
| implement | 23:14 | dev2-agent | built stacked on click-retention's reviewed branch; waited four times for that base to settle |
| qa_check | 00:01 | qa-agent | pass |
| code_review, with security | 00:18 | review-agent | pass, including the privacy judgment of Q2 B |
| integrate | 00:24 | orchestration-lead | rebased onto `main` after audit-columns; a range-diff showed all 8 patches identical to the judged `ec466da`; QA re-judged ancestry on `22fc8e2`; fresh gate; merge `c9b66dd` |

What validated the human's decisions:
- **Q1 B and Q6 A:** `uniqueVisitors` is counted per UTC day, as an upper bound across restarts.
- **Q2 B:** the client hash is never exposed, and the daily salt is never persisted.
- **Q3 B:** `botClicks` is reported beside the unchanged figures.
- **Q5 A:** the existing statistics API gains the fields; the regenerated API document was diffed against the live one.

The slice also closed wave-review finding W2-02: click identity now follows the
rate limiter's trusted-proxy rule. `[final]`: wave review, release and ship
sign-off for mission 03.

## Metrics `[final]`
