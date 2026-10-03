# Mission 01 — wave integration record

Written by the orchestration lead at the end of `wave_integration`
(2026-10-03T14:00Z) as the starting point for `wave_review`. Every SHA below can be
checked with `git log`; every verdict lives in the review and QA files named.

## Range to review

`main` from `7636264` (the base at w1 launch, after the mission plan-lock) to
`8e9c065` (the last slice merge). Product code changed only through the three
merges below; everything else in the range is documents and evidence.

## Slices

| Wave | Slice | Tier / plan-lock | Accepted candidate | Merge on `main` | Gate log on `main` | Hops | Review loops |
|---|---|---|---|---|---|---|---|
| w1 | `01-create-redirect` | high / human gate (decided by me under D11) | `a922f49` (tag `slice/01-create-redirect/accepted`) | `16c355f` | `integrate-01-create-redirect-check-16c355f.txt` | 12 of 30 | requirements RQ-01; design DR-01/DR-02 |
| w2 | `02-analytics` | low / delegated | `5b3490c` (tag `slice/02-analytics/accepted`) | `091ff46` | `integrate-02-analytics-check-091ff46.txt` | 13 of 30 | design DR-01/02/04 (two rounds on DR-01); code CR-01 (test timing) |
| w2 | `03-operate` | low / delegated | `1c8b2cf` (tag `slice/03-operate/accepted`) | `8e9c065` | `integrate-03-operate-check-8e9c065.txt` | 15 of 30 | requirements RQ-01; design DR-01/DR-02; code CR-01/CR-02 (limiter race, smoke false pass) |

For every slice, QA and the combined code and security review passed the same SHA
that was merged. `03-operate`'s candidate descends from `02-analytics`' merge,
which was checked with `git merge-base --is-ancestor 091ff46 1c8b2cf`. The gate after the
last merge ran `check --rerun-tasks`: unit 165, functional 155, 0 failures or
skips, coverage verification passed, Javadoc green.

## Open items handed to `wave_review`

- **CR-01 from `01-create-redirect`'s code review (MEDIUM, lead backlog).** The
  generated ProblemDetail schema in `docs/api/openapi.json` describes a generic
  property bag and omits the runtime `errors[{field, rule, message}]` member.
  Still open on `8e9c065`: the document has no `errors` key. No w2 slice took it.
- **A-9 proxy alignment (lead backlog, from `02-analytics` SPEC A-9).** The click
  hook hashes `request.getRemoteAddr()` (`ClickRecorder.java:86`). `03-operate`'s
  limiter uses the trusted-proxy rule for `X-Forwarded-For`. With the shipped
  default (no trusted proxy) both see the same client. Behind a configured proxy,
  every click would hash the proxy's address. The statistics endpoint reports
  totals, per-day counts and referrers, not unique visitors, so nothing shipped
  reads the hash today. Mission 03's analytics work is where that changes.
- **LOWs kept by the slice reviews:** Boot's disk-space gauge shows the working
  directory path on `/actuator/prometheus` (loopback operator scope, NFR-S5), and
  `smoke.sh` needs a supported C locale on macOS (`docs/qa/03-operate/findings.md`).
- **Written lead decisions the reviewer may want to check against the code:**
  DR-02 shutdown boundary (mission NOTES §2 09:30Z) and the limiter's clock
  policy, under which a backward wall-clock step fails closed (§2 12:40Z).

## Owed after `wave_review` (not wave-review findings)

- `release_prep`:
  - run AC-21…AC-28 against the container and the jar;
  - drive the specified 100 redirects/s and 20 creates/s before judging NFR-L1/L2 p95, or record the shortfall. QA reached about 82/16 with closed loops;
  - judge NFR-L3 (redirect p95, `02-analytics`);
  - run the secret scan (`01-create-redirect` item 14);
  - state the DR-02 boundary-loss count and load rate in `RELEASE.md`.
- Proof items still open:
  - `01-create-redirect` item 14 (`qa-agent`, after the release secret scan);
  - `02-analytics` item 13 (`qa-agent`, release bench);
  - `03-operate` item 11 (`qa2-agent`, from the re-review rows, `qitem-20261003135618-4e62dbf5`);
  - `03-operate` item 13 (`qa2-agent`, after `release_prep`).
- Delivery stamps for all three slices wait for the human's ship sign-off.
