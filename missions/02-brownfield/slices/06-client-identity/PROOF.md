# PROOF — OPR.99.0.3.6 Client Identity

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.3.6 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

QA checked by: qa2-agent@urlshort-factory   Date: 2026-10-04 UTC   Current verdict: QA PASS on exact e40b095; CR-01 formally resolved, independent code/security PASS, post-merge item 16 accepted in receipt 36. All 17 proof items accepted; live readiness ready, no issues.

**Historical post-handoff update02:47Z onfb63a88: acceptance held.** Review2's delayed-writer probe
reproduces a HIGH test-only polling-budget/NPE race in the new AC-5 helper.
QA withdrew proof1/11; the earlier PASS/drop/gate captures remain historical
run evidence. Read `docs/qa/06-client-identity/review-polling-race.md` for the
reviewer's positive product control and QA's own omitted helper-delay check.
The reviewer owns formal failure routing; no product defect or waiver is claimed.

The current e40b095 QA re-check below observes the correction with the actual
helper/writer hold and restores the affected candidate judgments. This preserves
the original failure and does not substitute for independent code re-review.

## What this proves

Exact candidate fb63a88 preserves the original client-resolution, audit access, rate budgets,
click grouping and public responses under all15 acceptance criteria. Independent original/candidate
HTTP/log/row comparisons, fresh suites and copied coverage support that result. This is the QA
judgment; future independent review, merge and register-owner updates are not accepted in advance.

## Artifacts (media in proof/)

Dropped via `rig proof add … --evidences … --media …` (one drop per verdict; media attached, never only hand-listed):

- proof/qa-fb63a88.md — QA drop, items1–15/17, attached coverage SUMMARY and observation/custody records.
- proof/qa-e40b095.md — current QA re-check drop; same product/jar, repaired characterization helper, own fresh gate and affected effects.
- docs/qa/06-client-identity/README.md — independent observations, comparisons and instrument qualifications.
- docs/qa/coverage/06-client-identity/SUMMARY.md — fresh gate, per-suite and merged CSV totals.

## Residue / caveats (if any)

GAPS records fixture-supplied Servlet peers/whitespace request headers, per-suite misses with
merged100/100, parameterized XML attribution, the external401 collision and scoped runtime
measurement differences. All required effects are observed. Proof16 returns after independent
review/merge and design-owner guidance updates, per lead transition1882; no future closure is claimed.

## Builder (dev2-agent@urlshort-factory)

**Candidate `fb63a88`** on `slice/06-client-identity`, from `main` at `50ad9c3`. It follows design §7
exactly: tests first, the move separate, and every commit green. `git diff --stat main...slice/06-client-identity`
lists the slice's territory and the one granted properties line. **Under `src/functionalTest`, only
the new `ClientIdentityCharacterizationJourneyTest` appears, so every existing journey is
byte-for-byte unchanged.**

| # | Commit | What | Gate (`scripts/gw --offline check --rerun-tasks`) |
|---|---|---|---|
| 1 | `240b230` test | pre-work: `ClientIdentityTest` (31 cases) and the journey's four kept contexts. No production file | green on the baseline ([`characterization-check.txt`](proof/characterization-check.txt)) |
| 1b | `1b4e0a7` test | every **added** row of design §5, including DR-01. Unit: U4–U10 in `ClientIdentityTest`; U14/U15 in `AuditControllerTest` and U16/U17 in `ClickRecorderTest`, as added cases only (`git diff -U0` shows no removed line in those two files). Journey: `ShippedBudgets` (AC-3 on a frozen clock, plus M1's click case moved there), `TrustedAuditPeers`, `UntrustedBudgets` (M1's budget side, A10's `429` half), A10's `403` half, both `remoteip` settings together and whitespace-only on a real server, the fixed-day AC-5 oracle (`2026-10-01`, exactly one element), and the click-row `SELECT` check. No production file | green on the **baseline production code** ([`check-1b-baseline.txt`](proof/check-1b-baseline.txt)) |
| 2 | `7e23259` refactor | **production only**: `web.ClientIdentity` (bodies moved verbatim, with the guard's comment and `ponytail:` ceiling); the three call sites call it; the old members stay as one-line delegates, and the alias constant points at `ClientIdentity.CLIENT_ATTRIBUTE` | green with **every test byte-identical to 1b** (`git diff --stat 1b4e0a7 7e23259 -- src/test src/functionalTest` is empty) ([`check-2-move.txt`](proof/check-2-move.txt)) |
| 3 | `d0e74c4` refactor | tests re-pointed: `RateLimitFilterTest`, `AuditControllerTest` and `ClickRecorderTest` change **reference renames only** (`git diff -U0` shows `RateLimitFilter.clientOf`/`CLIENT_ATTRIBUTE` and `AuditController.fromLoopback` becoming `ClientIdentity.…`, plus imports; every expected value is unchanged); `ClientIdentityTest` calls `ClientIdentity` directly instead of reflectively. Production: the three delegates deleted, `RateLimitFilter` package-private again, Javadoc links and one unused import | green ([`check-3-repoint.txt`](proof/check-3-repoint.txt)) |
| G | `fb63a88` docs | the plan-lock grant: one comment sentence above `urlshort.rate-limit.trusted-proxies` (M3S-02); no key or value change | green: unit 268, functional 322, 0 failures; merged lines 584/584, branches 206/206 (100 %); javadoc green ([`check-candidate-fb63a88.txt`](proof/check-candidate-fb63a88.txt)) |

**Changed assertions in the three existing unit tests:** none. Their cases were added (1b) and
references renamed (3); no grant request was needed.

**By effect on `fb63a88`** ([`proof/builder-by-effect-fb63a88.txt`](proof/builder-by-effect-fb63a88.txt)).
The jar runs on 127.0.0.1 with 127.0.0.1 as a trusted proxy:
- a direct loopback `GET /api/audit` gets `200`;
- the same request with a forged `X-Forwarded-For: 127.0.0.1` gets a `403` problem, so trust never opens the audit read;
- two forwarded clients over three redirects give `uniqueVisitors: 2`, so the click hashes the client the limiter charged.

QA's before/after comparison against `03e0657` is QA's (SPEC AC-14, AC-15).

## Self-check

- Diff re-read against design §1–§2. `ClientIdentity` holds `clientOf`, `resolve` and `of` (resolved
  client) apart from `peerIsConnection` and `fromLoopback` (direct peer). The guard methods take no
  trusted list, and the call order in the limiter is unchanged: after the exempt check, before
  `tryTake`. `clientOf` and `fromLoopback` are the old bodies byte for byte. `peerIsConnection` is the
  constructor's expression with its three-line comment. `resolve` keeps the limiter's two lines and
  their comment.
- Ladder: a static class, as the design says (no bean, because the non-web boot test lacks the
  `ServerProperties` and `TomcatServerProperties` beans). No new dependency, setting, endpoint, log
  event or meter. `docs/api/openapi.json` is untouched, and `OpenApiDocumentTest` stays green.
- Not verified by me: the regenerated API document diff and the live before/after responses (QA,
  AC-14/15). The `server.tomcat.remoteip.*=  ` command-line arguments in the whitespace journey may
  reach Boot trimmed or untrimmed. Either way the guard admits. The untrimmed case is pinned exactly
  by the unit U14, which sets `"  "` directly.

## QA — qa2-agent@urlshort-factory (independent Codex)

**QA PASS on exact `fb63a88a9b92c1fec97ba74686af1a2f30304160`.** Own fresh offline
rerun268 unit322 functional, no failures/errors/skips, Javadoc green; merged584/584 lines and
206/206 branches. Every required AC and failure case was tried by effect on original and candidate
production, with HTTP bodies/headers, actual audit/click/link rows and correlated JSON logs inspected.
See `docs/qa/06-client-identity/README.md` for the per-AC capture table and full limitations.

| Proof items | Independently checked evidence |
|---|---|
|1,13| Fresh exact-candidate gate, every current AC mapped to functional methods;43 original functional source files equal Git blobs. QA's own original-production characterization gate also268/322 green |
|2| Baseline-origin active/retired H2 directory copied into both core apps;302 target/no-store and400/404/405/410 problems match; original link rows preserved, errors/HEAD add no click |
|3| Frozen default60/600 budgets and all12 exact trust-matrix rows on both2/min budgets; shared429Retry30 and unrelated201/302, no refused link/click |
|4| All12 rows: three clicks, exactly one UTC day, two unique hash groups, zero bots, no raw identities in private rows or public statistics |
|5| Six loopback/four nonlocal forms and forwarding refusals under both trust settings; actual unchanged jars independently verify CR-01/native/framework/cloud and six individual/combined empty/space remote-IP setting contexts per lane |
|6| Precedence403/429, invalid limit400, audit POST405 and physically failed read500; no trail change/partial page/submitted value |
|7| Address/forwarding/user-agent/referrer/audit canaries checked on logs, rows, stats and metrics; private stored hashes absent publicly. DailySalt source unchanged and inspected: per-UTC-day memory key, zeros/drops on expiry, hash only private click data. Downstream independent security review remains its own obligation |
|8| Both trust settings: actual writer held while302 completes under1s, failed physical click table retains302 and one correlated WARN/loss;20 concurrent redirects, HEAD/unsuccessful no click, recorded21/lost1; inherited files/assertions unchanged |
|9|2041 controlled responses/10380 passing effect assertions;1017 exact non-metric HTTP/log pairs per lane. Generated UUID/code/date/log-time/PID substitutions documented with equality relationships. Original overly broad final metric comparator failure retained; scoped passive comparison and all raw runtime differences preserved |
|10| Original/candidate actual-jar live OpenAPI equal; canonical generator green and committed/API worktree bytes unchanged |
|11,12| Completed characterization1b4e0a7 production tree equals original baseline; own fresh pre-move gate green. Production-only7e23259 tests equal1b; existing three unit files change only imports/API references, verified by exact replacement; characterizer repoint is separately recorded |
|14|378 copied report resources/hash rechecked; unit510/584 lines198/206 branches, functional550/584 lines172/206 branches; merged100/100. Fresh canonical test.exec+functionalTest.exec only |
|15| TRACEABILITY current15 AC/requirement rows and full326 reverse mappings; GAPS appended including instruments and all honest qualifications |
|16, available clauses only| Impact6772b68 precedes design/ADR57cb9ae, both before dependent move7e23259. Source independently read: one utility, all three consumers, direct peer kept separate from resolved visitor. **Independent review and post-merge guidance/system-description remain pending**; lead confirms durable return in qitem-20261004015644-b1493e3f/transition1882 |
|17| No schema/migration, setting value/key, dependency/build/API change. Granted properties comment only; original H2 values unchanged. Source/HTTP/log/row/coverage captures registered in the QA drop |

Actual unmodified jars: before59/336 and after59/336; blank-settings72/372. Controlled apps19,
jar apps10+10+12: all51 stopped with exit/port-refusal evidence, counting pre-work baseline apps.
No product, build file or test was edited. No Docker/PostgreSQL/NAT/capacity/new performance or
in-flight shutdown guarantee is claimed. Matrix peers and whitespace request-header values use
an explicit external Servlet fixture; separate unmodified-jar checks establish container safety.

The first candidate gate's two401s are preserved and diagnosed as another listener at50898;
the owner pgAdmin process was untouched, isolated19-test class and fresh590 gate pass unchanged.
The original comparator's full Prometheus comparison fails only on two scrapes: startup-health503
series and JVM concurrent-GC/runtime measurements differ; all business counters, required privacy
and scoped responses/events pass. Raw readiness probe responses were not retained, so startup-poll
attribution is an inference. Original/candidate springdoc INFO duration293/162ms is also retained;
its API-docs request is outside AC-14's named audit200, whose complete events match. No event
message/count/level/stable field or promised status/access/grouping/target/header field is normalized.

### Self-check

Every AC observed by effect and failure case attempted; exact candidate and original production
verified; full raw HTTP/log/row joins inspected; coverage read from CSV and378 hashes checked;
321 declarations plus five inherited mappings complete both directions,56 class-only parameterized
attributions labelled; gap entry written; proof drop names1–15/17; downstream16 explicitly pending;
all owned apps stopped; worktree remains clean atfb63a88; no product/test authorship. Evidence
and attributed judgments are committed with explicit pathspecs before the qa_check handoff.

## Builder rework: CR-01 (dev2-agent@urlshort-factory)

**New candidate `e40b095`** = `fb63a88` + one test commit. Code review on `fb63a88`
(`docs/review/06-client-identity/01-code-review.md`) failed on one HIGH, **CR-01**; security passed.

**CR-01, fixed.** `ClientIdentityCharacterizationJourneyTest.settledStats` polled
`GET /api/links/{code}/stats`. Under `ShippedBudgets`' frozen 60-per-minute budget, a delayed click
write let the polls spend the budget, and the helper threw a NullPointerException on a `429` body
after about 170 ms. Now:
- it waits, within the same 10 s bound, on the link's stored click rows via `JdbcClient`, which spends
  no rate-limit budget;
- it then reads the statistics **once** and asserts `200` before parsing.

Unchanged: the shipped 60/600 limits, the frozen fixed day, every matrix case, and every exact
day/count/privacy assertion. One file, test only, no production change.

**Proof of the fix:**
- **Review's own reproduction**, rerun unchanged against the fixed helper
  (`-I docs/review/06-client-identity/polling-probe.gradle identityPollingProbe`; log
  [`proof/cr-01-probe-after-fix.txt`](proof/cr-01-probe-after-fix.txt)):
  `PROBE elapsed_ms=3042 stats_requests=2 stats_429=0 helper_failure=none`.
  - With the real writer held for the probe's full 3 s fallback, the helper waited it out and passed
    the full oracle.
  - The two requests are the helper's single read and the probe's positive control.
  - The probe was written to expect the old failure, so its closing `AssertionError` ("expected
    helper to fail …") and `BUILD FAILED` are the signal that the race is gone. Its
  `probe writer deadline` line is its own held writer timing out, because no `429` came to release
    it early.
- **Gate on `e40b095`:** `scripts/gw --offline check --rerun-tasks` gives unit 268, functional 322,
  0 failures; merged lines 584/584, branches 206/206 (100 %). Log
  [`proof/check-cr01-fix.txt`](proof/check-cr01-fix.txt).

## QA Re-check e40b09541feb0b7555c475baa82587fdd09e4890

Independent QA2/Codex, packet qitem-20261004025925-a5886b4c: **QA PASS**, CR-01
resolution observed. Own fresh 268 unit and 322 functional tests, zero failures/errors/skips,
Javadoc/coverage green; CSV merged 584/584 lines, 206/206 branches. Fresh 378 copied
resources verified. All43 original functional files remain unchanged; new delta
from fb63a88 changes only the new helper's waiting/read order and caller, keeping
the 10-second bound, shipped60/600, every matrix/day/count/privacy oracle.

The actual candidate helper with a deliberately held real writer passes under
both no trust andP trust after 3036/3042ms. It makes one helper statistics read,
plus the probe's separate positive control; zero 429s/NPEs. The entire 3-click/
2-visitor/UTC-day/privacy oracle passes. Own external probe exits 0, not the
builder's retained negative-expectation probe result. The corrected current
characterization also passes 72 invocations when externally shadow-compiled onto
original 1b production without editing its tests/source. Baseline characterization
chronology remains the original pre-move commit; this compatibility replay shows
the corrected wait preserves those oracles.

Fresh affected AC-4/5/10 by-effect replay: all 12 exact trust-matrix rows,
159 real HTTP responses/818 assertions,3 apps stopped. Every raw status/body and
complete request-id event window joins; all 159 pairs match prior candidate under
only UUID/code/HTTP-Date/log-time/PID substitutions. Fixed time/day, headers,
stable problem/body fields and event level/message/count remain literal.

Production/unit trees and rebuilt jar bytes are identical to fb63a88 (jar 92e1b7a…);
under QA guidance §5, the other AC effects are carried from independently observed
prior QA, not all rerun. All prior qualifications persist. The old canonical
coverage and SUMMARY are preserved under archive-fb63a88, with 379 explicit aliases
verifying all 3490 original artifact hashes. Current 326 method mappings are
checked against all 74 fresh reports; 56 class-only parameterized attributions
remain honestly labelled. TRACE/GAPS append the current candidate and QA-observed
CR-01 closure; formal code re-review remains its own pending step.

Evidence: `docs/qa/06-client-identity/recheck-e40b095/README.md`, `check.txt`,
`held-writer-probe.txt`, `current-characterization-on-original/`, `validation.json`,
matrix captures and current coverage SUMMARY. `docs/qa/06-client-identity/findings.md`
records the affected finding's QA resolution. Renewed judgments 1–15/17 bind
e40b095; item 16 still returns after review/merge/design-owner updates, per lead transition 1882.

### Self-check

Exact clean candidate/diff inspected; own full gate and affected public effects;
actual helper delayed rather than settling outside it; no budget raise, retry or
weakened oracle; original-production current characterization replay; raw/log/row
joins and substitutions read; CSV totals/fresh378/historical3490 hashes checked;
both-way 326 mappings/GAPS written; valid local-media drop; all 3 HTTP apps and
both probe JVMs stopped; candidate unchanged; no product/test edit. Evidence,
renewed judgments and receipts committed before qa_check handoff. Formal review
and downstream 16 are not prematurely accepted.

## QA post-merge return — item 16

On qitem-20261004033729-f28865b7, QA2 checked the remaining chronology, independent-review, merge and register/current-system clauses. Exact e40b095 is merge b8d7fc16's second parent. Impact 6772b68 precedes design/ADR amendments 57cb9ae and dependent move 7e23259. Review2 code/security PASS 981eb8e0 verifies D21 and all three consumers. Owner update 3b2ecd0b follows the merge; architecture section 11 row 1 and DESIGN match the merged authority and call sites, and both final hashes equal the packet. Snapshot fda42757 contains the merge. No discrepancy found.

Receipt 36 accepts item 16 against exact e40b09541feb0b7555c475baa82587fdd09e4890. Live proof is ready: 17 accepted items, no issues. Evidence: docs/qa/06-client-identity/item16-followup.md and proof/qa-item16-custody.json; registered drop proof/qa-item16-e40b095.md and snapshot proof/qa-item16-readiness.json. Earlier pending-item statements in immutable captures describe their recorded time.

### Self-check

Read owner changes and authority/consumer code; checked chronology, ADR amendment bodies, review attribution, exact merge parents/tag/source equality, owner update ancestry and final document hashes. No new application or test run, no product/test mutation. The existing QA/reviewer/integrator gate and by-effect checks remain attributed to those runs. Receipt and evidence committed before closing this ordinary follow-up; the lead performs final workflow closure.
