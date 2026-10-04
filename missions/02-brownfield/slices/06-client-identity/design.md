# Design — 06-client-identity

- Slice: `06-client-identity` (mission `02-brownfield`, wave w3, human decision D21). Tier low; the
  plan-lock is delegated to the orchestration lead (D11). Workflow `urlshort-slice-delegated-b`:
  builder `dev2-agent`, judges `qa2-agent` and `review2-agent`.
- SPEC: `4f247f4` (requirements review PASS, no findings, `b68ff80`).
- Impact analysis, committed before this design: [`impact-analysis.md`](impact-analysis.md)
  (`6772b68`).
- Register row 1 (D20): `design-agent`'s verdict is CONSISTENT, with no condition
  (`docs/review/06-client-identity/architecture-consistency.md`, `6fcb134`).
- Decision records: amendments to ADR-0015 and ADR-0019 (§6), on `main` with this design.
- Author: `design2-agent@urlshort-factory`, 2026-10-04. Drafted as pre-work in `84ba733`
  (`design.draft.md`), promoted here; §5 now holds the one matrix, merged with `dev2-agent`'s
  characterization pre-work `240b230`.

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
 * The rules are pinned by ClientIdentityTest, RateLimitFilterTest, AuditControllerTest and ClickRecorderTest, which test
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

Source: [`docs/diagrams/client-identity-sequence.mmd`](../../../../docs/diagrams/client-identity-sequence.mmd).

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

## 5. Characterization matrix: one matrix, written first, green on the baseline

These tests target **today's** members:
- `RateLimitFilter.clientOf`;
- `RateLimitFilter.CLIENT_ATTRIBUTE`;
- `AuditController.fromLoopback` and its constructor.

`ClientIdentity` does not exist on the baseline. `ClientIdentityTest` therefore reaches
`AuditController.fromLoopback` reflectively until commit 3 replaces the reflection with the direct
call. Expected values are the baseline's answers, which the SPEC's ACs state; a disagreement on the
baseline is a finding against this design, not a test to bend.

**The lead's two conditions (2026-10-04T01:01Z), kept here:**
1. In `RateLimitFilterTest`, `AuditControllerTest` and `ClickRecorderTest`, every existing assertion
   stays byte for byte. Cases may be added; a changed assertion is a grant request named in
   `PROOF.md`.
2. Every row below is marked **kept** (already in `dev2-agent`'s `240b230`), **added** (by the
   builder, before the move) or **dropped** (from the pre-work draft, with what replaces it), so
   implement works from this one matrix.

`P` = `10.9.9.9`, `Q` = `10.9.9.8`, `U` = `203.0.113.7`, `V` = `203.0.113.8`; peers and entries are
exact text.

### 5.1 Unit rows

**`web/ClientIdentityTest`** (new in this slice):

| Case | Status | Pins |
|---|---|---|
| `identityMatrix` (12 rows): SPEC M1 to M10 as `clientOf` sees them, plus `127.0.0.1` with no header and `::1` with `U`, both under an empty list | kept | the trusted-proxy rule over the SPEC's matrix |
| `aHeaderlessLoopbackPeerIsFromLoopback` (six loopback forms); `anyOtherPeerIsNotFromLoopback` (five, `""` included) | kept | the guard's loopback test |
| `anyXForwardedForHeaderClosesTheLoopbackPredicate` and `anyForwardedHeaderClosesTheLoopbackPredicate` (a remote value, a loopback value, `""`, whitespace) | kept | presence refuses, whatever the value (the draft's U11 to U13) |
| U4 to U10 below, a second parameterized source with the same columns as `identityMatrix` | **added** | the rule's remaining edges |

| # | Trusted | Peer | `X-Forwarded-For` | Expected `clientOf` |
|---|---|---|---|---|
| U4 | P | P | `U, V` | V (right-most untrusted) |
| U5 | P | P | `U,,` | U (Java's `split` drops trailing empty entries) |
| U6 | P | P | `\tU\t` (tabs) | U (`trim` removes them) |
| U7 | P | P | `unknown` | `unknown` (opaque tokens verbatim) |
| U8 | P | P | `[2001:db8::1]:443` | `[2001:db8::1]:443` |
| U9 | P | P | `203.0.113.7:8080` | `203.0.113.7:8080` |
| U10 | `::1` | `0:0:0:0:0:0:0:1` | U | `0:0:0:0:0:0:0:1` (exact text: Tomcat's full form does not match `::1`) |

**`AuditControllerTest`** (existing; cases added only):

| # | Status | Case | Expected |
|---|---|---|---|
| U14 | **added** | constructor, strategy `NONE`, `remote-ip-header` and `protocol-header` both `"  "` | a loopback read is admitted (`200`): whitespace is unset |
| U15 | **added** | constructor, strategy `NONE`, both headers set | `403` |

**`ClickRecorderTest`** (existing; cases added only):

| # | Status | Case | Expected |
|---|---|---|---|
| U16 | **added** | no attribute on the request | `salt.stamp(<the peer>)` |
| U17 | **added** | the attribute present but not a `String` (an `Integer`) | `salt.stamp(<the peer>)` |

**`RateLimitFilterTest`:** nothing added. Its rule-5 rows and attribute case stay byte for byte, and
are re-pointed in commit 3.

**Dropped from the draft:**
- U1 to U3: they are `identityMatrix` rows.
- U11 to U13 in `AuditControllerTest`: `ClientIdentityTest` holds them.

### 5.2 Functional rows: `web/ClientIdentityCharacterizationJourneyTest` (new in this slice; the only functional file it adds)

**Isolation, kept from `240b230`.** Each budget context freezes `FunctionalClock` and shifts it
forward 5 minutes before every case, under `@DirtiesContext`:
- a 2-per-minute bucket refills completely within 60 s, so no budget state crosses cases while the
  SPEC's fixed addresses are reused;
- the shifted clock dies with its context and cannot leak into other classes.

**Trusted lists, kept.** `TrustedProxies` (`10.9.9.9,10.9.9.8,127.0.0.1`) and `SharedBudgets`
(`10.9.9.9,10.9.9.8`) run the SPEC's `P`-only rows under a longer list. The answers are the same:
being listed matters only for the peer and the `X-Forwarded-For` entries a row carries, and those
rows name neither `Q` nor `127.0.0.1`. The file's Javadoc states this.

| Nested context | Status | Properties | Cases (SPEC AC) |
|---|---|---|---|
| `ShippedSettings` | kept | shipped | M1's click side (AC-5: `uniqueVisitors` 2). A6: all six loopback forms `200`. A7: the four non-loopback peers, `GET` and `HEAD`, `403`. A8: `X-Forwarded-For` and `Forwarded` with a remote, a loopback, a `for=127.0.0.1`, an empty and a whitespace value, from `127.0.0.1` and from `192.0.2.10`, all `403` without trail content |
| `ShippedBudgets` | **added**: AC-3 | shipped limits (60 and 600 per minute), trusted list empty; `FunctionalClock` frozen, `@DirtiesContext`, as `SharedBudgets` | a fresh peer sends 61 creates, each with a different `X-Forwarded-For`, `Forwarded` and `X-Real-IP`: 60 are `201`, then `429`. Another fresh peer sends 601 redirects likewise: 600 are `302`, then `429` without `Location`. Both refusals carry an integer `Retry-After` of at least 1. **The clock must be frozen** (SPEC AC-3's GIVEN): on a running clock, a 60-per-minute bucket refills about once a second while the requests are sent, so the 61st could be admitted. That is why it does not run in `ShippedSettings`, which shares the suite's default context and its running clock (`RateLimitJourneyTest` freezes for the same reason) |
| `ShippedSettings` | **added**: A10's `403` half | — | peer `192.0.2.10`, `GET /api/audit?limit=0` with `Accept: text/html`: a `403` problem, neither `400` nor `406` |
| `TrustedProxies` | kept | `10.9.9.9,10.9.9.8,127.0.0.1` | M2 to M12's click side (AC-5): `totalClicks` 3, one element with `clicks` 3, `uniqueVisitors` 2, `botClicks` 0, and no raw value in the body. A trusted loopback peer reads only without a forwarding header; a trusted non-loopback peer is refused, with or without one (AC-6 to AC-8 under trust) |
| `SharedBudgets` | kept | `10.9.9.9,10.9.9.8`, both limits 2 | M2 to M12's budget side (AC-4), on the redirect and the create budget: the row's request is `429` with `Retry-After: 30`, and the unrelated `192.0.2.200` succeeds |
| `UntrustedBudgets` | **added** (the draft's N1) | trusted list empty, both limits 2; isolation as `SharedBudgets` | M1's budget side (AC-4), on both budgets. A10's `429` half: peer `192.0.2.10` spends its 2 creates, then `GET /api/audit?limit=0` with `Accept: text/html` is a `429` problem with `Retry-After`, neither `403`, `400` nor `406` |
| `AddressRewritingSettings` | kept | a real server per case (`SpringApplicationBuilder`, `127.0.0.1`, port 0) | `native`, `framework`, and each `remoteip` header alone: `GET` and `HEAD`, plain and with `X-Forwarded-For: 127.0.0.2`, all `403`. The shipped control: `GET` and `HEAD` `200`, the forged header `403` |
| `AddressRewritingSettings` | **added** (the draft's R1) | both `remoteip` headers together | the same `403`s |
| `AddressRewritingSettings` | **added** (the draft's R4) | both `remoteip` settings `"  "` (whitespace only) | `GET` and `HEAD` `200`; the forged header `403`. Boot 4.1.1 agrees: `TomcatWebServerFactoryCustomizer.customizeRemoteIpValve` installs the valve only when `StringUtils.hasText` holds for either header, or forward headers are in use (`javap -c` of `spring-boot-tomcat-4.1.1.jar`, offsets 19–41). That is the guard's own test, so whitespace installs no valve and the guard admits |

**Dropped from the draft:**
- N2 and N3: `TrustedProxies` and `SharedBudgets` replace them.
- R2 and R3 (`HEAD` under one `remoteip` setting): `AddressRewritingSettings` already asserts `HEAD`.
- The 60 s shift: 5 minutes is kept.
- The draft's A6, A7 and A8 lists: `ShippedSettings` covers them with more values.

**The SPEC's identity matrix** (AC-4 and AC-5), for reference, in the SPEC's order:

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

**The oracles, as `240b230` asserts them:**
- **AC-4 (budget sharing):** the reference peer spends the budget with two headerless requests; the
  row's request is then `429` with `Retry-After: 30` (and no `Location` on a redirect); the
  unrelated `192.0.2.200` succeeds.
- **AC-5 (grouping):** the row's request, the reference peer and `192.0.2.200` each open a new
  link once, as a browser. The settled statistics show 3 clicks and 2 unique visitors, and no raw
  address in the body. **Added:** the same cases read `SELECT * FROM click WHERE link_id = …` and
  find none of the row's raw values (`P`, `Q`, `U`, `V`, `10.0.0.5`, `198.51.100.1`). SPEC AC-5
  asks this of click records, not only of the response.

The cloud-platform case stays in `AuditForwardedHeadersJourneyTest`, unchanged.

What stays with the existing, unchanged journeys (impact analysis, *What each existing journey
pins*): AC-1, AC-2, AC-11 to AC-15 and the remaining parts of AC-6 to AC-10. QA's before/after
captures (SPEC AC-14, AC-15) are QA's.

## 6. Decisions recorded as ADRs

Both amendments are on `main` with this design, before any dependent code (NFR-M2). No new ADR.

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

**Register row 1 (`design-agent`, D20; verdict CONSISTENT, no condition, `6fcb134`).** After the
merge, `design-agent` edits the row against the merged code:
- "Inside `web/`, reuse `clientOf`" becomes "reuse `ClientIdentity`";
- "Elsewhere, read `CLIENT_ATTRIBUTE`" becomes "elsewhere, call `ClientIdentity.of(request)`";
- the audit guard's line names `ClientIdentity.peerIsConnection` and `fromLoopback`;
- the M3S-03 sentence reads "must still call `ClientIdentity.resolve`" rather than "set
  `CLIENT_ATTRIBUTE`", with the same meaning.

## 7. Commit plan (tests first, the move separate)

| # | Commit | Touches | Gate |
|---|---|---|---|
| 1 | `240b230` `test(06-client-identity): characterize today's client identity before the refactor` (`dev2-agent`'s pre-work, on `slice/06-client-identity`) | `ClientIdentityTest`, `ClientIdentityCharacterizationJourneyTest`. **No production file** | green on the baseline production code, by its author's run |
| 1b | `test(06-client-identity): complete the characterization matrix` | every **added** row of §5: U4 to U10 in `ClientIdentityTest`; U14 and U15 in `AuditControllerTest`; U16 and U17 in `ClickRecorderTest`; in the journey, `UntrustedBudgets`, `ShippedBudgets` (AC-3), A10's `403` half, both `remoteip` settings, whitespace settings, and the click-row check. **No production file**; existing assertions untouched | `scripts/gw --offline check` green on the baseline production code; SHA and log captured into `proof/` |
| 2 | `refactor(06-client-identity): one ClientIdentity for the client and the audit guard` | **production only:** `ClientIdentity` added; the three call sites call it; the old members stay as one-line delegates (`RateLimitFilter.CLIENT_ATTRIBUTE = ClientIdentity.CLIENT_ATTRIBUTE`, `RateLimitFilter.clientOf` → `ClientIdentity.clientOf`, `AuditController.fromLoopback` → `ClientIdentity.fromLoopback`). **No test file** | green with every test byte-identical to commit 1b: the behaviour-preservation proof |
| 3 | `refactor(06-client-identity): tests name ClientIdentity; the delegates go` | tests: reference renames only in `RateLimitFilterTest`, `AuditControllerTest` and `ClickRecorderTest`; in `ClientIdentityTest`, `RateLimitFilter.clientOf` becomes `ClientIdentity.clientOf` and the reflective `fromLoopback` helper gives way to `ClientIdentity.fromLoopback` (expected values unchanged). Production: the three delegate members deleted, `RateLimitFilter` back to package-private, the two Javadoc links updated. Nothing else | green |

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
| the SPEC's matrix in the new `ClientIdentityTest` (`240b230`), with the existing rule tests left in `RateLimitFilterTest` and `AuditControllerTest`, re-pointed and byte for byte | the pre-work draft's placement of every row in the three existing files | it already exists and passes on the baseline. It is this slice's own file, so its reflective reach into `audit/` can become a direct call. The existing files keep their assertions untouched, which is the before-and-after proof `slice.yaml` asks for. The lead accepted either placement under the two conditions of §5 |
| delegates for one commit | a single move-and-re-point commit | commit 2 then proves behaviour with every test untouched |
| `RateLimitFilter` back to package-private | leave it `public` | its only outside reader was `click/`, for the constant that moves |
| the setting stays `urlshort.rate-limit.trusted-proxies` in `RateLimitProperties` | move the setting to `ClientIdentity` | no new or renamed setting (D21); one reader passes it in |

## 9. Territory

Only `slice.yaml`'s list:
- `ClientIdentity.java`;
- `RateLimitFilter.java` and `RateLimitFilterTest.java`;
- `ClickRecorder.java` and `ClickRecorderTest.java`;
- `AuditController.java` and `AuditControllerTest.java`;
- `ClientIdentityTest.java` and the new `ClientIdentityCharacterizationJourneyTest.java`.

No grant request. The ADRs, `docs/DESIGN.md` and the diagram are the design step's, done in the main
checkout. Register row 1 is `design-agent`'s, after the merge.

## Status

- 2026-10-04: drafted as pre-work (`c6b5560`, `84ba733`); promoted at the design step on SPEC
  `4f247f4`. Impact analysis first (`6772b68`); §5 merged with `dev2-agent`'s `240b230`; register
  verdict CONSISTENT (`6fcb134`). Handed to `design_review`.

## Self-check

- **Every SPEC AC has a mechanism.**
  - AC-1, AC-2, AC-11 to AC-15: unchanged code paths, existing journeys.
  - AC-3, AC-4, AC-5: the matrix (§5.2).
  - AC-6 to AC-10: the audit rows, plus the existing audit journeys.
- **Rule 1** is honoured by the API (§2). Rules 2 and 3 move verbatim. Rule 4 holds because the call
  order is unchanged. Rule 5 holds because `of` is the same ternary. Rule 6 is QA's.
- **No new behaviour:** every body moves verbatim, and the only new surface is a Java class with
  static methods.
- **Characterization comes first and targets the baseline's members;** commit 2 touches no test.
- **The lead's two conditions** are stated (§5): existing assertions byte for byte, and one matrix
  with every row marked kept, added or dropped.
- **The register-row verdict** was obtained before review: CONSISTENT, no condition. The two ADR
  amendments are on `main`.
- **Measured, not assumed:** the non-web context (`UrlshortApplicationTests`) that rules out a bean;
  Boot 4.1.1's valve trigger (`hasText`, bytecode); every reference to a moved member (grep).
- **Not verified here:** running anything. Commit 1 passes on the baseline by its author's run.
  Commit 1b's additions are checked on the baseline by the builder, then by QA.

## Plan review (author's lenses; the skill was not invoked separately)

- **Engineering.** The smallest move that gives one authority: one static class and five members,
  three call sites, no bean wiring.
  - The probe of the non-web context turned a natural `@Component` into a static class before
    anyone built it.
  - The bytecode read settled the whitespace case.
- **Strategy.** Exactly D21: no new behaviour, setting or endpoint, and the audit and visitor
  questions kept apart.
- **Reader's experience.**
  - Commit 2 changes only production code and every test passes untouched, so a reviewer can see
    the behaviour preserved.
  - Commit 3's production half is three deletions and one keyword.
