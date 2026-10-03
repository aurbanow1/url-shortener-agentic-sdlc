# Remaining proof records — 03-operate

Current candidate `1c8b2cff20ad8b73a060bc817c8d0011782f876f`; QA re-check packet
`qitem-20261003130258-8d163c70`. Earlier a7c533f observations remain historical;
its item 12 was withdrawn after review. Fresh 1c8b2cf checks establish the
reordered limiter, rollback cleanup, complete-response and deadline effects.

QA can accept proof items 1–10 and 12 on independently observed evidence.
Item 11 explicitly requires code and security review records about limiter
memory bounds and client privacy. Those steps follow QA; do not accept it
before their records exist. Item 13 expressly belongs to `release_prep`:
container/jar AC-21–AC-28, inspect records, restart persistence and shutdown,
and NFR-L1/L2 bench judgment recorded in `missions/01-greenfield-core/RELEASE.md`.

Supplemental QA jar observations are not a container verdict. The 60-second
new 60-second bench achieved 82.1 redirects/s and 16.4 creates/s with zero bad responses;
the specified input rates are 100 and 20. Fixed 100 ms sleeps follow each
request in each closed client loop, so process/request time reduces achieved
rate. Release must establish the required input rate before judging latency,
or record an honest unresolved gap; small measured p95 values at lower load
do not satisfy the stated NFRs. `docs/qa/03-operate/findings.md` records this.

Continuation for the orchestration lead: retain items 11 and 13 through review
and release, route their produced records back to QA for attributed judgments
on the applicable candidate/merged subject, and leave the settled items intact
unless new evidence changes them. No scope change or coverage waiver is granted.

Durable lead obligation: `qitem-20261003120849-f4cbfa97`.

Re-check host requirement: R0 modes now need Perl `Time::HiRes` and a locale
supported by that Perl installation. This macOS Codex session's inherited
`C.UTF-8` panics; independently verified `C` works. The failed attempt and
supported-locale controls are retained in `docs/qa/03-operate/`. Release must
use a supported host locale and verify its actual environment. No product or
toolchain configuration was changed. Item 11 returns after the new review;
item 13 returns after release_prep, as the lead already accepted.
