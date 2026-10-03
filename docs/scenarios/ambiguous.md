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

## Dynamic re-planning `[final]`

What changed after the decision (SPEC revision, any slice split, `rig workflow
revise` receipt or new slice) and the record of it.

## Orchestration and validation `[final]`

Delegated plan-lock (D11), build on top of `02-analytics`, QA and combined
review on the other model family, wave review, release, ship sign-off.

## Metrics `[final]`
