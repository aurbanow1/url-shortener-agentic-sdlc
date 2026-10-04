# PROOF — OPR.99.0.3.6 Client Identity

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.3.6 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

QA checked by: qa2-agent@urlshort-factory   Date: 2026-10-04 UTC   Verdict: QA PASS on fb63a88; independent review and proof16 downstream closure pending.

## What this proves

Exact candidate fb63a88 preserves the original client-resolution, audit access, rate budgets,
click grouping and public responses under all15 acceptance criteria. Independent original/candidate
HTTP/log/row comparisons, fresh suites and copied coverage support that result. This is the QA
judgment; future independent review, merge and register-owner updates are not accepted in advance.

## Artifacts (media in proof/)

Dropped via `rig proof add … --evidences … --media …` (one drop per verdict; media attached, never only hand-listed):

- proof/qa-fb63a88.md — QA drop, items1–15/17, attached coverage SUMMARY and observation/custody records.
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
