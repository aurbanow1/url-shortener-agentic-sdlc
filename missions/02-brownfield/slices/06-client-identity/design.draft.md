# DRAFT — Design — 06-client-identity

> **DRAFT, pre-work.** Written by `design2-agent@urlshort-factory` on 2026-10-04 at the lead's
> request (`qitem-20261004004918-ac7ea2a4`). It is the design step's input, not the slice's
> `design.md`. It follows SPEC `4f247f4` (requirements review PASS, no findings, `b68ff80`) and the
> draft impact analysis [`impact-analysis.draft.md`](impact-analysis.draft.md) (`c6b5560`). If the
> SPEC changes, this draft is revised, never pushed through.

**In one paragraph.** A new `public final class web.ClientIdentity`, static like `web.Problems`,
answers the two client questions this service asks, each by its own methods:
- **Who is the client?** The resolved client, for rate limits and click counting (ADR-0015).
- **Is the connection's own peer a direct loopback caller?** The audit guard (ADR-0019, CR-01).

The code of each answer moves verbatim from `RateLimitFilter` and `AuditController`. The three call
sites then call `ClientIdentity`:
- the limiter calls `resolve`;
- the click recorder calls `of`;
- the audit controller calls `peerIsConnection` and `fromLoopback`.

The two questions stay separate (SPEC rule 1). The trusted-proxy list never reaches the audit
guard, so trusting a proxy can never open `/api/audit`. Nothing observable changes. That is proven
by characterization tests that pass on the baseline before the move and unchanged after it (§5).

## 1. Components

| Component | Change | Specification |
|---|---|---|
| `web.ClientIdentity` | **new**, `public final`, private constructor, no state, no bean | the API of §2; the bodies are the moved code, unchanged |
| `web.RateLimitFilter` | calls `ClientIdentity.resolve` | `String client = ClientIdentity.resolve(request, trustedProxies);` replaces the `clientOf` call and the `setAttribute` (lines 76–78). The order is unchanged: after the exempt check, before `tryTake`. `CLIENT_ATTRIBUTE` and `clientOf` leave it (commit 3), and the class returns to package-private: it was made `public` in `01-analytics-v2` only so `click/` could read the constant, and no test outside `web/` names it (grep, impact analysis) |
| `click.ClickRecorder` | calls `ClientIdentity.of` | `DailySalt.Stamp stamp = salt.stamp(ClientIdentity.of(request));` replaces lines 115–117. The `record` Javadoc links `ClientIdentity#of`. The `RateLimitFilter` import goes. `click/` then depends on a `web/` utility, like `Problems`, not on a concrete filter (closes the coupling noted as M3S-03 in mission 03's wave review) |
| `audit.AuditController` | calls the two guard methods | the constructor keeps its signature and `@ConditionalOnWebApplication(SERVLET)`: `this.peerIsConnection = ClientIdentity.peerIsConnection(server, tomcat);`. `page` keeps `if (!peerIsConnection \|\| !ClientIdentity.fromLoopback(request))`. Its own `fromLoopback` leaves (commit 3). So `AuditControllerTest`'s constructor-driven cases (strategies, `remoteip`, empty settings) compile and run unchanged |

Nothing else changes: no setting, endpoint, response, log event, meter, migration or dependency.

## 2. The `ClientIdentity` API

```java
package dev.urlshort.web;

/**
 * Who a request's client is, in this service's two senses (ADR-0015, ADR-0019; architecture.md §11
 * row 1). They are deliberately different questions with separate methods:
 * <ul>
 * <li>the resolved client, which rate limits and click counts use: the peer, or behind a listed
 * trusted proxy the right-most untrusted X-Forwarded-For entry ({@link #of});
 * <li>the direct peer, which the audit read uses: the connection's own loopback address, with no
 * forwarding header and nothing configured to rewrite it ({@link #peerIsConnection},
 * {@link #fromLoopback}). The trusted-proxy list never takes part.
 * </ul>
 * The rules are pinned by RateLimitFilterTest, AuditControllerTest and ClickRecorderTest, which test
 * them through these methods.
 */
public final class ClientIdentity {

	/** Request attribute holding the client a request was charged to; set by {@link #resolve}. */
	public static final String CLIENT_ATTRIBUTE = ClientIdentity.class.getName() + ".client";

	private ClientIdentity() {
	}

	// resolved client (ADR-0015)
	static String clientOf(String remote, @Nullable String forwardedFor, Set<String> trusted)  // moved verbatim
	static String resolve(HttpServletRequest request, Set<String> trusted)                    // clientOf(peer, XFF, trusted); setAttribute; return
	public static String of(HttpServletRequest request)                                        // CLIENT_ATTRIBUTE if a String, else getRemoteAddr()

	// direct peer (ADR-0019, CR-01)
	public static boolean peerIsConnection(ServerProperties server, TomcatServerProperties tomcat) // moved expression
	public static boolean fromLoopback(HttpServletRequest request)                                // moved verbatim
}
```

- **Bodies.** `clientOf` and `fromLoopback` move byte for byte. `peerIsConnection` is
  `AuditController`'s constructor expression, with its `ponytail:` comment (Boot's valve trigger
  list). `resolve` is `RateLimitFilter`'s two lines. `of` is `ClickRecorder`'s ternary.
- **Visibility.** `clientOf` and `resolve` are package-private: only the limiter, in `web/`, calls
  them. `of`, `peerIsConnection` and `fromLoopback` are public for `click/` and `audit/`, so each
  needs full Javadoc with `@param` and `@return` under `-Xdoclint:all -Werror`. `CLIENT_ATTRIBUTE`
  stays public, because `ClickRecorderTest` (in `click/`) sets it.
- **The attribute's key** becomes `"dev.urlshort.web.ClientIdentity.client"`. It is unobservable:
  the attribute is never logged, rendered or stored. In commit 2 the alias left in `RateLimitFilter`
  must be `= ClientIdentity.CLIENT_ATTRIBUTE`, not a literal, so the writer and the reader cannot
  disagree while both names exist.
- **Why static, not a bean.**
  - `UrlshortApplicationTests.mainBootsWithoutAWebServer` boots with `web-application-type=none`,
    where `ServerProperties` and `TomcatServerProperties` are not beans. That is why
    `AuditController` is servlet-conditional.
  - A `ClientIdentity` bean that needed them would fail the unconditional `RateLimitFilter` and
    `ClickRecorder`.
  - The rules hold no state, and the setting stays with `RateLimitProperties`, passed in by its only
    user.

## 3. Sequence

```mermaid
sequenceDiagram
    autonumber
    participant C as Client (maybe via a proxy)
    participant RL as RateLimitFilter (web)
    participant CI as ClientIdentity (web, static)
    participant RC as RedirectController → ClickRecorder (click)
    participant AC as AuditController (audit)

    Note over AC,CI: at start-up: peerIsConnection(server, tomcat) → boolean field
    C->>RL: any non-exempt request (peer, maybe X-Forwarded-For)
    RL->>CI: resolve(request, trustedProxies)
    CI->>CI: clientOf(peer, X-Forwarded-For, trusted); setAttribute(CLIENT_ATTRIBUTE)
    CI-->>RL: client → tryTake(budget, client)
    alt GET /{code} → 302
        RL->>RC: chain
        RC->>CI: of(request)
        CI-->>RC: the charged client (else the peer) → salt.stamp(...)
    else GET /api/audit
        RL->>AC: chain
        AC->>CI: fromLoopback(request): own peer loopback, no forwarding header
        Note over AC: 403 unless peerIsConnection && fromLoopback (the trusted list is never read)
    end
```

## 4. Logging, data, API, threat model

- **Logging and metrics:** none added or changed; `ClientIdentity` logs nothing.
- **Data:** no migration. The click hash input is the same string as before.
- **API:** no change; `docs/api/openapi.json` regenerates identical (SPEC AC-15).

**Threat model.** A refactor's threats are regressions of the properties it moves:

| Threat | Mitigation | Residual |
|---|---|---|
| **Elevation:** the audit guard starts using the resolved client, so a trusted proxy forwarding `127.0.0.1` opens `/api/audit` | separate methods, and the guard never takes the trusted list (§2). Characterization rows with the peer listed as trusted (§5, A6 to A8). `AuditAccessSettingsJourneyTest` unchanged. The security review reads the move line by line | — |
| **Elevation:** a CR-01 condition is lost in the move | `peerIsConnection` is the moved expression. `AuditControllerTest` and `AuditForwardedHeadersJourneyTest` (real Tomcat) run unchanged; new rows add both settings together, `HEAD`, and whitespace-only values (§5, A9) | Boot adding a fourth valve trigger in an upgrade (the `ponytail:` ceiling, ADR-0019) |
| **Spoofing:** a rule edge changes (trim, empty entries, exact-text trust, chains, opaque tokens) and a forged header splits or merges clients | `clientOf` moves byte for byte; the unit rows of §5 pin every edge on the baseline and after the move | — |
| **Information disclosure / integrity of analytics:** the attribute is missing or keyed differently, and uniques behind a proxy fall back to the proxy | one key constant read and written through `ClientIdentity`; `TrustedProxyClickJourneyTest`; the AC-5 rows (§5) | a future path that skips the limiter (register row 1 rule) |
| **Availability:** context start-up breaks in a non-web context | static class, no new bean | — |

## 5. Characterization matrix (written first, green on the baseline)

These tests target **today's** members:
- `RateLimitFilter.clientOf`;
- `RateLimitFilter.CLIENT_ATTRIBUTE`;
- `AuditController.fromLoopback` and its constructor.

`ClientIdentity` does not exist on the baseline. Commit 3 re-points the references. Expected values
are the baseline's answers, which the SPEC's ACs state; a disagreement on the baseline is a finding
against this draft, not a test to bend.

### 5.1 Unit rows, added to the existing unit tests

**`RateLimitFilterTest`.** A new parameterized test with a trusted-list column, beside the existing
rule-5 rows. Peer and entries are exact text. `P` = `10.9.9.9`, `Q` = `10.9.9.8`, `U` =
`203.0.113.7`, `V` = `203.0.113.8`.

| # | Trusted | Peer | `X-Forwarded-For` | Expected `clientOf` |
|---|---|---|---|---|
| U1 | empty | P | U | P |
| U2 | P, Q | P | `V, U, Q` | U |
| U3 | P, Q | P | `P, Q` | P |
| U4 | P | P | `U, V` | V |
| U5 | P | P | `U,,` | U |
| U6 | P | P | `\tU\t` (tabs) | U |
| U7 | P | P | `unknown` | `unknown` |
| U8 | P | P | `[2001:db8::1]:443` | `[2001:db8::1]:443` |
| U9 | P | P | `203.0.113.7:8080` | `203.0.113.7:8080` |
| U10 | `::1` | `0:0:0:0:0:0:0:1` | U | `0:0:0:0:0:0:0:1` (exact text: Tomcat's full form does not match `::1`) |

The existing rows already pin a single trusted proxy, the trimmed `" U ,  "`, a trusted entry on the
right, all-trusted, blank `" , "`, `null`, `""`, an untrusted peer, and
`theFilterReadsOnlyXForwardedForAndOnlyFromATrustedPeer` (`Forwarded` and `X-Real-IP` ignored).

**`AuditControllerTest`.**

| # | Case | Expected |
|---|---|---|
| U11 | `fromLoopback`: `127.0.0.1` with `X-Forwarded-For: ""` | `false` (presence refuses) |
| U12 | the same with `X-Forwarded-For: "  "` | `false` |
| U13 | the same with `Forwarded: ""` | `false` |
| U14 | constructor, strategy `NONE`, `remote-ip-header` and `protocol-header` both `"  "` | admits a loopback read (`200`): whitespace is unset |
| U15 | constructor, strategy `NONE`, both headers set | `403` |

**`ClickRecorderTest`.**

| # | Case | Expected |
|---|---|---|
| U16 | no attribute on the request | `salt.stamp(<the peer>)` |
| U17 | attribute present but not a `String` (an `Integer`) | `salt.stamp(<the peer>)` |

### 5.2 Functional rows: `web/ClientIdentityCharacterizationJourneyTest` (new file; the only functional file this slice adds)

**One isolation mechanism**, the suite's own (`RateLimitJourneyTest`):
- each context freezes `FunctionalClock` at a fixed noon UTC (`clock.reset(); clock.freeze();`, and
  `reset()` after each case);
- before every matrix case it shifts the clock by 60 s. With 2 per minute (GCRA), a 60 s quiet
  period refills every bucket (`AC04_aFullBudgetReturnsAfterAQuietMinute` proves the refill), so no
  budget state crosses cases while the SPEC's fixed addresses are reused;
- each shift stays within the same UTC day, as AC-5 needs.

Contexts are `@Nested` classes with their own properties (the `03-dogfood-fix` precedent):

| Context | Properties | Holds |
|---|---|---|
| N0 | shipped settings | AC-3, A6, A7, A8, A10's `403` half |
| N1 | `trusted-proxies` empty, `create-per-minute=2`, `redirect-per-minute=2` | matrix row M1; A10's `429` half |
| N2 | `trusted-proxies=10.9.9.9`, both limits 2 | rows M2, M3, M4, M6, M8 to M12 |
| N3 | `trusted-proxies=10.9.9.9,10.9.9.8`, both limits 2 | rows M5, M7 |
| R1 to R3 | `RANDOM_PORT` (real Tomcat, the valve runs): R1 both `remoteip` headers set; R2 `remote-ip-header` alone and R3 `protocol-header` alone, each for `HEAD` | A9 |
| R4 | `RANDOM_PORT`, both `remoteip` settings `"  "` (whitespace) | A9: whitespace is unset. Boot 4.1.1 agrees: `TomcatWebServerFactoryCustomizer.customizeRemoteIpValve` installs the valve only when `StringUtils.hasText` holds for either header, or forward headers are used (`javap -c` of `spring-boot-tomcat-4.1.1.jar`, offsets 19–41). That is the same test as the guard's `hasText`, so whitespace installs no valve and the guard admits. R4 pins it on a real server |

**Matrix M1 to M12** is SPEC AC-4 and AC-5's table, in order, with `P`, `Q`, `U`, `V` as there.
Each row runs twice for AC-4, once on the create budget (`POST /api/links`, valid body) and once on
the redirect budget (`GET /{active code}`), and once for AC-5.

| Row | Trusted | Peer | `X-Forwarded-For` | Other headers | Reference peer |
|---|---|---|---|---|---|
| M1 | empty | P | U | `Forwarded: for=V`, `X-Real-IP: V` | P |
| M2 | P | `10.0.0.5` | U | — | `10.0.0.5` |
| M3 | P | P | U | — | U |
| M4 | P | P | `198.51.100.1, U` | — | U |
| M5 | P, Q | P | `V, U, Q` | — | U |
| M6 | P | P | `  U ,  ` | — | U |
| M7 | P, Q | P | `P, Q` | — | P |
| M8 | P | P | absent | — | P |
| M9 | P | P | `""` | — | P |
| M10 | P | P | `" , "` | — | P |
| M11 | P | P | absent | `Forwarded: for=U`, `X-Real-IP: U` | P |
| M12 | P | P | U | `Forwarded: for=V`, `X-Real-IP: V` | U |

- **AC-4 oracle (budget sharing).** Each case in N1 to N3 runs four requests:
  1. the reference peer sends two headerless requests on the budget;
  2. the row's request is answered `429` with `Retry-After: 30`;
  3. a fresh `192.0.2.200` succeeds (`201` or `302`).

  The link for the redirect budget is created by a setup peer (`192.0.2.201`) before the shift, so
  setup never spends the budget under test.
- **AC-5 oracle (uniques).** In the same contexts, per row:
  1. after the shift, a setup peer (`192.0.2.202`) creates a new link;
  2. three browser `GET`s follow: the row's request, the reference peer headerless, and
     `192.0.2.200`;
  3. `recorder.settle()`;
  4. a different setup peer (`192.0.2.203`) reads the statistics.

  Expected: `totalClicks` 3, one element `{date, clicks 3, uniqueVisitors 2, botClicks 0}`.
  `SELECT * FROM click WHERE link_id = …` contains none of `P`, `Q`, `U`, `V`, `10.0.0.5`,
  `198.51.100.1`. Every client stays within 2 requests per bucket per case: the row's request and
  the reference resolve to one client, which spends 2 of 2.
- **AC-3 (N0).** One fresh peer sends 61 creates with a changing `X-Forwarded-For`, `Forwarded` and
  `X-Real-IP` on each: 60 are `201`, then `429`. Another fresh peer sends 601 redirects the same
  way: 600 are `302`, then `429` with no `Location`. `RateLimitJourneyTest` AC01, AC02 and AC07
  cover the parts; this case adds the combination.
- **Audit rows (N0, MockMvc peers).**
  - **A6:** a headerless `GET /api/audit` from `127.255.255.254` and from `0:0:0:0:0:0:0:1` is `200`.
    The other four loopback forms are in `AuditReadJourneyTest.AC12`.
  - **A7:** `GET` and `HEAD` from `::ffff:192.0.2.10` and from `fe80::1` are `403`, with no trail
    content (`192.0.2.10` and `10.0.0.7` are in `AC11`). A non-loopback peer listed as trusted:
    `AuditAccessSettingsJourneyTest` holds `192.0.2.10`.
  - **A8:** from `127.0.0.1`, `X-Forwarded-For` or `Forwarded` with an empty value and with a
    whitespace-only value is `403`. A forged-loopback `Forwarded: for=127.0.0.2` is `403`. The same
    from `192.0.2.10` is `403`.
  - **A10:** a refused request (peer `192.0.2.10`) with `limit=0` and `Accept: text/html` is a `403`
    problem, neither `400` nor `406` (N0). With that peer's create budget spent, it is a `429`
    problem with `Retry-After`. Run that half in N1, where 2 creates spend the budget.
- **A9 (R1 to R4, real Tomcat).**
  - R1: a plain loopback `GET` and `HEAD` are `403`, and `X-Forwarded-For: 127.0.0.2` is `403`.
  - R2 and R3: `HEAD` is `403`. `GET` is already in `AuditForwardedHeadersJourneyTest`.
  - R4: a plain loopback read is `200`, and the forged header is `403`.

  The `native`, `framework`, single-setting and cloud-platform cases stay in
  `AuditForwardedHeadersJourneyTest`, unchanged.

What stays with the existing, unchanged journeys (impact analysis, *What each existing journey
pins*): AC-1, AC-2, AC-11 to AC-15 and the remaining parts of AC-6 to AC-10. QA's before/after
captures (SPEC AC-14, AC-15) are QA's.

## 6. Decisions recorded as ADRs (text for the design step)

**ADR-0015 amendment — `06-client-identity` (behaviour unchanged):**
> One code path. `web.ClientIdentity` owns this ADR's rule and its hand-off:
> - `clientOf(remote, forwardedFor, trusted)`, moved verbatim from `RateLimitFilter`;
> - `resolve(request, trusted)`, which records the result in `ClientIdentity.CLIENT_ATTRIBUTE` and
>   returns it. The limiter calls it before it charges;
> - `of(request)`, the recorded client, else the peer. The click recorder calls it.
>
> The limiter keeps the setting (`urlshort.rate-limit.trusted-proxies`) and passes it in.
> `ClientIdentity` is a static class, not a bean: the rule holds no state, and the beans the audit
> half reads (`ServerProperties`, `TomcatServerProperties`) do not exist in a non-web context. The
> audit read's question (ADR-0019) has its own methods and never uses this rule. Characterization
> tests pin every edge on the baseline and after the move.

**ADR-0019 amendment — `06-client-identity` (behaviour unchanged):**
> The guard's two halves move, unchanged, into `web.ClientIdentity`:
> - `peerIsConnection(server, tomcat)`: the effective strategy is `NONE`, and neither
>   `server.tomcat.remoteip` header setting has text (CR-01);
> - `fromLoopback(request)`: the peer is loopback, and neither `X-Forwarded-For` nor `Forwarded` is
>   present.
>
> `AuditController` keeps its servlet condition and its constructor. It computes the first once and
> calls the second per request. Both answer about the connection's own peer and never consult the
> trusted-proxy list, so trusting a proxy for rate limits cannot open the read. The `ponytail:`
> ceiling moves with the expression: Boot adding a valve trigger needs a line here.

**Register row 1 (for `design-agent`, D20):** "Inside `web/`, reuse `clientOf`" becomes "reuse
`ClientIdentity`". "Elsewhere, read `CLIENT_ATTRIBUTE`" becomes "elsewhere, call
`ClientIdentity.of(request)`". The audit guard's line names `ClientIdentity.peerIsConnection` and
`fromLoopback`.

## 7. Commit plan (tests first, the move separate)

| # | Commit | Touches | Gate |
|---|---|---|---|
| 1 | `test(06-client-identity): characterize client identity on the baseline` | the new journey (§5.2); unit rows U1 to U17 in `RateLimitFilterTest`, `AuditControllerTest`, `ClickRecorderTest`, against today's members. **No production file** | `scripts/gw --offline check` green on the baseline production code; SHA and log captured into `proof/` |
| 2 | `refactor(06-client-identity): one ClientIdentity for the client and the audit guard` | **production only:** `ClientIdentity` added; the three call sites call it; the old members stay as one-line delegates (`RateLimitFilter.CLIENT_ATTRIBUTE = ClientIdentity.CLIENT_ATTRIBUTE`, `RateLimitFilter.clientOf` → `ClientIdentity.clientOf`, `AuditController.fromLoopback` → `ClientIdentity.fromLoopback`). **No test file** | green with every test byte-identical to commit 1: the behaviour-preservation proof |
| 3 | `refactor(06-client-identity): tests name ClientIdentity; the delegates go` | tests: reference renames only, in the three unit tests. Production: the three delegate members deleted, `RateLimitFilter` back to package-private, the two Javadoc links updated. Nothing else | green |

**Why commit 3 is mixed.** A tests-only re-point would leave the delegates unreached and fail the
coverage gate, and deleting them first would not compile. So both land together. The production half
is bounded and checkable: three deleted members, one visibility keyword and two Javadoc links.

No document commit is expected: `OpenApiDocumentTest` proves committed equals live, and QA diffs the
regenerated file (AC-15).

## 8. Trade-offs

| Chosen | Over | Because |
|---|---|---|
| a static class | a `@Component` | the non-web boot test, where `ServerProperties` and `TomcatServerProperties` are absent; no state to hold |
| two named questions | one `isTrusted`/`clientOf` used by all three | SPEC rule 1. The audit read must never see the resolved client |
| the rule's tests stay in `RateLimitFilterTest` and `AuditControllerTest`, re-pointed | a new `ClientIdentityTest` (in territory) | the same assertions before and after are the refactor's proof, and `slice.yaml` allows those files "references only". Moving tests between files would make that harder to check. Coverage reaches every `ClientIdentity` branch through them |
| delegates for one commit | a single move-and-re-point commit | commit 2 then proves behaviour with every test untouched |
| `RateLimitFilter` back to package-private | leave it `public` | its only outside reader was `click/`, for the constant that moves |
| the setting stays `urlshort.rate-limit.trusted-proxies` in `RateLimitProperties` | move the setting to `ClientIdentity` | no new or renamed setting (D21); one reader passes it in |

## 9. Territory

Only `slice.yaml`'s list:
- `ClientIdentity.java`;
- `RateLimitFilter.java` and `RateLimitFilterTest.java`;
- `ClickRecorder.java` and `ClickRecorderTest.java`;
- `AuditController.java` and `AuditControllerTest.java`;
- the new `ClientIdentityCharacterizationJourneyTest.java`.

`ClientIdentityTest.java` is allowed but not created (§8). The ADRs, `docs/DESIGN.md` and the
diagram are the design step's, in the main checkout. Register row 1 is `design-agent`'s, after the
merge.

## Self-check (draft)

- **Every SPEC AC has a mechanism.**
  - AC-1, AC-2, AC-11 to AC-15: unchanged code paths, existing journeys.
  - AC-3, AC-4, AC-5: the matrix (§5.2).
  - AC-6 to AC-10: the audit rows, plus the existing audit journeys.
- **Rule 1** is honoured by the API (§2). Rules 2 and 3 move verbatim. Rule 4 holds because the call
  order is unchanged. Rule 5 holds because `of` is the same ternary. Rule 6 is QA's.
- **No new behaviour:** every body moves verbatim, and the only new surface is a Java class with
  static methods.
- **Characterization comes first and targets the baseline's members;** commit 2 touches no test.
- **Not verified here:** running anything; the matrix's expected values are the SPEC's statements of
  baseline behaviour, which commit 1 checks on the baseline.
