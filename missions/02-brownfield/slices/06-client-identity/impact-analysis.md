# Impact analysis — 06-client-identity

Written before `design.md` (`docs/guidance/brownfield.md` §2) by `design2-agent@urlshort-factory`,
2026-10-04, for SPEC `4f247f4` (requirements review PASS, no findings, `b68ff80`). It was drafted as
pre-work in `c6b5560` (`impact-analysis.draft.md`, at the lead's request
`qitem-20261004004918-ac7ea2a4`) and promoted at the design step. The reviewed SPEC is the one the
draft followed, so no assumption changed.

**Inputs:**
- D21 (`PLAN.md` §10; mission 02 SPEC, third amendment);
- `slice.yaml`;
- SPEC `4f247f4` (baseline `5cfdf8a`, AC-1 to AC-15, rules 1 to 6);
- the code on `main` `50ad9c3`, whose product code differs from `5cfdf8a` only in
  `ClickRetentionScheduleJourneyTest` (the unrelated W2F-01 fix);
- `dev2-agent`'s characterization pre-work `240b230` on `slice/06-client-identity`.

## Change in one sentence

Three pieces of client identity move, unchanged, into one new class, `web/ClientIdentity`:
- the trusted-proxy rule (`RateLimitFilter.clientOf`);
- the request attribute carrying the resolved client (`RateLimitFilter.CLIENT_ATTRIBUTE`);
- the audit guard's direct-loopback predicate (`AuditController.peerIsConnection` and
  `fromLoopback`).

`RateLimitFilter`, `ClickRecorder` and `AuditController` call it instead of deriving the client
themselves. Requirements: D21, FR-13, and the inherited rows in the SPEC's table. No behaviour
changes.

## Impacted modules

Found with `grep -rn -E "CLIENT_ATTRIBUTE|clientOf|fromLoopback|peerIsConnection|RateLimitFilter[^T]" src`
on `50ad9c3`.

| Piece today | Where it lives | Who calls or reads it | Tests that reference it by name |
|---|---|---|---|
| The trusted-proxy rule `clientOf(remote, forwardedFor, trusted)`: peer unless listed; then the right-most trimmed, non-empty, unlisted `X-Forwarded-For` entry; else the peer. Exact text, no other header | `web/RateLimitFilter:105`, package-private static | `RateLimitFilter.doFilterInternal:76` only | `RateLimitFilterTest.rule5_theClientIsThePeerOrTheRightMostUntrustedForwardedHop` (8 rows), `rule5_anAbsentOrEmptyHeaderFromATrustedProxyChargesTheProxy` (null and `""`) |
| The resolved-client attribute `CLIENT_ATTRIBUTE` = `"dev.urlshort.web.RateLimitFilter.client"`. It is set to `clientOf`'s result on every non-exempt request, before `tryTake`; exempt paths carry none | `web/RateLimitFilter:48, :78`, `public` (the class became `public` in `01-analytics-v2` for this constant only) | read by `click/ClickRecorder.record:115`: the attribute if it is a `String`, else `getRemoteAddr()` | `RateLimitFilterTest.theChargedClientIsLeftOnTheRequestForTheClickRecorderAndAnExemptRequestCarriesNone`; `ClickRecorderTest.theRateLimitersClientIsHashedWhenTheRequestCarriesIt` (imports `RateLimitFilter`) |
| The audit guard's configuration half `peerIsConnection`: strategy `NONE`, and neither `server.tomcat.remoteip.remote-ip-header` nor `protocol-header` has text (CR-01) | `audit/AuditController:65–67`, computed in the constructor from `ServerProperties` and `TomcatServerProperties` | `AuditController.page:87` | indirectly: `AuditControllerTest.theShippedStrategyAdmitsALoopbackRequest`, `anyOtherStrategyClosesTheEndpoint` (`NATIVE`, `FRAMEWORK`), `anUnsetStrategyClosesTheEndpoint`, `aRemoteIpHeaderSettingClosesTheEndpoint`, `aProtocolHeaderSettingClosesTheEndpoint`, `emptyRemoteIpSettingsKeepTheEndpointOpen` (all through the constructor, which keeps its signature) |
| The audit guard's request half `fromLoopback(request)`: no `X-Forwarded-For` and no `Forwarded` header (presence, any value), a non-empty peer, and `InetAddress.getByName(peer).isLoopbackAddress()`; unparsable is refused | `audit/AuditController:96`, package-private static | `AuditController.page:87` | `AuditControllerTest.loopbackPeersAreAdmitted` (6), `otherPeersAreRefused` (8, including `""` and `1::2::3`), `aMissingPeerIsRefused`, `aForwardingHeaderRefusesEvenFromLoopback` (2) |

Also touched, by consequence:
- `RateLimitFilter` returns to package-private once nothing outside `web/` names it. Its only outside
  reader was `ClickRecorder`, for the constant.
- The class Javadoc of `RateLimitFilter` (its `CLIENT_ATTRIBUTE` paragraph) and of
  `ClickRecorder.record` (its `{@link RateLimitFilter#CLIENT_ATTRIBUTE}`).

**Not touched:** `RateLimiter`, `RateLimitProperties` (the setting stays
`urlshort.rate-limit.trusted-proxies`), `AuditTrail`, `DailySalt`, `Click`, `ClickStore`, every
controller's mapping, `application.properties`, the schema, `docs/api/openapi.json`, and every
existing functional journey.

## Impacted endpoints

None by intent. Every status, header, body and the API document stay as they are (SPEC AC-1 to
AC-15). The paths whose answers depend on the moved code, and so are the refactor's proof surface:

| Path | Depends on | How |
|---|---|---|
| every non-exempt path (`/api/**`, `/{code}`, and anything else not under `/actuator`, `/v3/api-docs`, `/swagger-ui`) | the trusted-proxy rule | which bucket a request is charged to, so when `429` comes and with which `Retry-After` |
| `GET /{code}` (`302`) | the attribute | which address the click hashes, so `uniqueVisitors` in `GET /api/links/{code}/stats` |
| `GET`/`HEAD /api/audit` | the guard | `200` versus `403`, decided before validation and content negotiation |

## Impacted schema and data

None. No migration. Stored click hashes are not rewritten. The attribute lives only in the request,
so its renamed key (if the design renames it) is invisible outside the process.

## What each existing journey pins (unchanged files, the refactor's proof)

| Journey | Pins | Through which moved piece |
|---|---|---|
| `web/RateLimitJourneyTest` (real Tomcat) | AC06 clients are independent; AC07 a forged `X-Forwarded-For` without trust does not change the client; AC09 the limit is checked first; AC11 and AC12 the `429` names no client | rule (default, empty list) |
| `web/TrustedProxyJourneyTest` | AC08: behind a trusted proxy the forwarded address identifies the client | rule (listed peer) |
| `web/RateLimitDefaultsTest` | the shipped `trusted-proxies` is empty | (the setting, unchanged) |
| `click/TrustedProxyClickJourneyTest` (`trusted-proxies=10.9.9.9`) | AC07 uniques count the forwarded clients; AC09 aggregates only; AC12 no forwarded value or address in logs | rule and attribute |
| `click/StatsV2JourneyTest` | AC-2 to AC-5 uniques by peer; AC-8 `X-Forwarded-For` without trust changes nothing (peer `203.0.113.87`) | rule (empty list) and attribute |
| `click/ClickRecordingJourneyTest` | forwarding headers are neither stored nor logged (`X-Forwarded-For`, `Forwarded` canaries) | attribute (peer fallback is never hit on a redirect) |
| `click/ClickResilienceJourneyTest`, `ClickResilienceTrustedProxyJourneyTest` | slow, failing and concurrent store, without and with a trusted proxy (analytics-v2 AC-15) | attribute on the request thread |
| `audit/AuditReadJourneyTest` | AC11 `192.0.2.10`, `10.0.0.7` refused; AC12 `127.0.0.1`, `127.0.0.2`, `::1`, `::ffff:127.0.0.1` admitted; AC13 forwarding headers never grant; the guard and the validation come before content negotiation; AC15 correlation on `200`, `400`, `403`, `405` | `fromLoopback` |
| `audit/AuditAccessSettingsJourneyTest` (trusted list including `127.0.0.1` and `192.0.2.10`) | AC13, AC14: no setting opens the endpoint beyond loopback; a plain loopback read still works | `fromLoopback` with trust present |
| `audit/AuditForwardedHeadersJourneyTest` (real Tomcat for the valve cases) | the shipped pin; on a detected cloud platform the pin keeps a forged header out; `native` and `framework` close the endpoint; **CR-01**: `remote-ip-header=x-forwarded-for` or `protocol-header=x-forwarded-proto` closes it | `peerIsConnection` |
| `audit/AuditReadFailureJourneyTest`, `AuditUpgradeJourneyTest` | an admitted read's `500` and the upgrade journey's real-HTTP read | `peerIsConnection` and `fromLoopback`, admitting |
| `link/LinkCreateJourneyTest`, `LinkAuditColumnsJourneyTest` | forwarding headers do not change a create or an audit column | none directly (canaries) |

## The CR-01 cases

`01-audit-read`'s code review (CR-01) found that with the strategy pinned to `none`, setting
`server.tomcat.remoteip.remote-ip-header` or `protocol-header` still installs Tomcat's
`RemoteIpValve`. The valve rewrites the peer and consumes the header, so a forged
`X-Forwarded-For: 127.0.0.2` was admitted. The fix (`0052efb`) put both settings into
`peerIsConnection`.

| Case | Pinned today by | Gap a characterization case closes |
|---|---|---|
| `remote-ip-header` set (strategy `none`) | `AuditControllerTest` (unit), `AuditForwardedHeadersJourneyTest` (real Tomcat) | `HEAD` as well as `GET` |
| `protocol-header` set | the same two | `HEAD` |
| both set together | — | the combination (SPEC AC-9) |
| both empty `""` | `AuditControllerTest.emptyRemoteIpSettingsKeepTheEndpointOpen` | — |
| whitespace-only `"  "` | — | `StringUtils.hasText` treats it as unset; pin it (SPEC rule 3) |

## Blast radius

| If the move is wrong | Worst case | Detection |
|---|---|---|
| The rule changes an edge (trim, empty entry, exact-text match, multi-proxy chain, opaque token) | a client charged to the wrong bucket; uniques wrong behind a proxy | `RateLimitFilterTest` rule-5 rows; the characterization matrix (design §5) on the baseline and on the candidate |
| The attribute is set after charging, not at all, or under another key than the reader uses | uniques behind a proxy silently fall back to the proxy's address | `TrustedProxyClickJourneyTest.AC07`; the characterization AC-5 rows |
| The two questions merge (audit uses the resolved client) | **a trusted proxy's forwarded `127.0.0.1` would open `/api/audit`** | `AuditAccessSettingsJourneyTest` (trusted list contains the forged sources); characterization AC-7 and AC-8 rows "with the peer listed as trusted"; the security review reads the move line by line (`slice.yaml`) |
| `peerIsConnection` loses a condition (CR-01) | the valve rewrites the peer and a forged loopback header is admitted | `AuditForwardedHeadersJourneyTest` on a real Tomcat; `AuditControllerTest` |
| `ClientIdentity` becomes a bean that needs `ServerProperties` | the context fails to start in `UrlshortApplicationTests.mainBootsWithoutAWebServer` (`web-application-type=none`), where those beans do not exist | that unit test (it is why the design keeps the class static) |

## Compatibility (FR-13)

- **Clients, Operators, Analysts:** nothing changes on the wire, in logs, in metrics or in the
  document (SPEC AC-14, AC-15).
- **Settings:** the same one, `urlshort.rate-limit.trusted-proxies`, with the same exact-text
  semantics.
- **Code outside the three call sites:** no caller outside them names the moved members (grep above).

## Test impact

- **Added first, green on the baseline** (design §5):
  - already committed by `dev2-agent` in `240b230`: the new unit test `web/ClientIdentityTest` and
    the new journey `web/ClientIdentityCharacterizationJourneyTest`, both against today's members
    (`fromLoopback` reached reflectively until it moves);
  - to complete the matrix before the move: added rows in `ClientIdentityTest`, added cases in
    `AuditControllerTest` and `ClickRecorderTest`, and two nested contexts and two real-server cases
    in the journey.
- **Changed with the move:** references only, in `RateLimitFilterTest`, `AuditControllerTest` and
  `ClickRecorderTest`:
  - `RateLimitFilter.clientOf` becomes `ClientIdentity.clientOf`;
  - `RateLimitFilter.CLIENT_ATTRIBUTE` becomes `ClientIdentity.CLIENT_ATTRIBUTE`;
  - `AuditController.fromLoopback` becomes `ClientIdentity.fromLoopback`.

  Every existing assertion in those three files stays byte for byte; any other change is a grant
  request named in `PROOF.md` (`slice.yaml`). In `ClientIdentityTest`, a file this slice adds, the
  reflective helper gives way to the direct call.
- **Removed:** none. **Existing functional journeys:** byte-for-byte unchanged.

## Observability impact

None. `ClientIdentity` logs nothing and registers no meter. `RateLimitFilter`'s rejection counter
and the click counters are unchanged.

## Risks, ranked

| # | Risk | Mitigation | Owner step |
|---|---|---|---|
| 1 | The audit guard and the resolved client get merged | two separately named questions in `ClientIdentity` (design §2); trust-listed characterization rows; line-by-line security review | design → QA → security review |
| 2 | A rule edge drifts in the move | characterization first, green on the baseline, rerun unchanged after; the move commit touches no test | build → QA |
| 3 | Context start-up breaks in the non-web test | a static class, no new bean | design |
| 4 | Javadoc doclint on the new public type | every public member documented, `@param` and `@return` included | build |

## Self-check

- Every caller and every test that names a moved member is listed, from a grep on `50ad9c3`; every
  journey that exercises one is listed with what it pins.
- The CR-01 cases are listed with their current pins and the gaps a characterization case closes.
  Boot 4.1.1's valve trigger was read in bytecode: it uses `StringUtils.hasText` on both headers,
  the same test as the guard (design §5).
- The reviewed SPEC `4f247f4` is the one the draft followed.
- **Not verified here:** running anything. The characterization tests in `240b230` pass on the
  baseline by their author's run; QA reruns them.
