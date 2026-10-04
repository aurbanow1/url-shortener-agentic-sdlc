# PROOF — OPR.99.0.3.6 Client Identity

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.3.6 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

Closed by: <seat>   Date: <date>   Verdict: <pass | pass-with-residue | ...>

## What this proves

<1-3 sentences: the claim the slice made, now demonstrated>

## Artifacts (media in proof/)

Dropped via `rig proof add … --evidences … --media …` (one drop per verdict; media attached, never only hand-listed):

- proof/screenshot-01.png — <what it shows>
- proof/capture-behavior.gif — <what it shows>
- proof/command-output.txt — <what it proves>

## Residue / caveats (if any)

<documented residue: what's not covered + where it's tracked>

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
