---
slice: 01-create-redirect
mission: 01-greenfield-core
spec: SPEC.md
spec_candidate: 0acbc9d4898a04df73cf61bab20570a75484629a
status: proposed
created: 2026-10-03
---

# Design — Slice 01 Create and redirect

Reader: the Development Agent, with `SPEC.md` open beside this file. Everything
below is the smallest structure that reaches every acceptance criterion (AC-1
to AC-28) and every business rule (rules 1 to 12). Where the platform already
does the job the design says so and adds no code. Mechanisms marked **[probe]**
were exercised over real HTTP on a Tomcat started from the shipped main code
(`design-probe/`, §12); **[jar]** means read from the 4.1.1 / 7.0.9 class files
in the repo-local Gradle cache; **[docs]** means from the reference
documentation and not executed by me.

## 1. Components touched

Package by feature (`docs/guidance/architecture.md` §2). Nothing is added under
`UrlshortApplication` or `ping/` except Javadoc (§13).

| Component | Package / class | New or changed | Responsibility |
|---|---|---|---|
| Link feature package | `dev.urlshort.link` (`package-info.java`) | **new** | The Creator's resource and the Visitor's redirect. Owns the `link` table. |
| Management controller | `link.LinkController` | **new** | `POST /api/links`, `GET /api/links/{code}`, `DELETE /api/links/{code}`. HTTP in/out only: runs `LinkValidation` on the body and the `Idempotency-Key` header, calls `LinkService`, maps `Link` to `LinkResponse`, sets `Location` on `201`. Carries the springdoc annotations that give AC-28 its examples and problem responses. |
| Redirect controller | `link.RedirectController` | **new** | `GET /{code:[A-Za-z0-9]{6,32}}` → `302`, `Location` = stored `url` as a **String header**, `Cache-Control: no-store`. One method. **This is the seam for `02-analytics`:** its click hook is one call inserted after `resolve` returns and before the response is built; nothing else in `link/` is touched by that grant. |
| Request body | `link.CreateLinkRequest` | **new** | `record CreateLinkRequest(String url)`. No Bean Validation annotations: the SPEC's ordered single-token contract is enforced by `LinkValidation` (§2.1). Unknown members are ignored (Boot default `FAIL_ON_UNKNOWN_PROPERTIES=false`, A-14). |
| Representation | `link.LinkResponse` | **new** | `record LinkResponse(String code, String shortUrl, String url, String state, Instant createdAt)` — exactly the five fields (A-2); `static LinkResponse of(Link link, String publicBaseUrl)`. `Instant` renders as ISO-8601 UTC with `Z` under Boot's Jackson 3 defaults. |
| Aggregate | `link.Link` | **new** | `record Link(@Id Long id, String code, String url, Instant createdAt, @Nullable Instant retiredAt, @Nullable String idempotencyKey)`; `String state()` (`active` when `retiredAt == null`, else `retired`); `LinkSnapshot snapshot()`. Spring Data JDBC maps it to table `link`, snake_case columns. |
| Audit payload | `link.LinkSnapshot` | **new** | `record LinkSnapshot(String url, String state)` — the "before"/"after" state written to the audit row (A-18); serialised in declaration order so rows read the same way every time. |
| Repository | `link.LinkRepository extends Repository<Link, Long>` | **new** | Exposes only what the slice uses: `Link save(Link)`, `Optional<Link> findByCode(String)`, `Optional<Link> findByIdempotencyKey(String)`, `@Modifying @Query("UPDATE link SET retired_at = :at WHERE id = :id AND retired_at IS NULL") int retire(Long id, Instant at)`, `@Modifying @Query("UPDATE link SET idempotency_key = NULL WHERE id = :id") int releaseIdempotencyKey(Long id)`. The two updates are targeted so a stale aggregate `save()` can never overwrite a concurrent retire. |
| Service | `link.LinkService` | **new** | The use cases and the transaction boundary: `create(url, key)`, `read(code)`, `retire(code)`, `resolve(code)` (§2.5, §4). Writes the audit row inside the same transaction. |
| Validation | `link.LinkValidation` | **new** | Final class, two static methods: `validateUrl(String)` applies rule 3 in order and throws `Problems.validation("url", <token>, …)` on the first failure; `validateIdempotencyKey(String)` enforces rule 5's format (`[\x21-\x7E]{1,255}`) and throws `Problems.validation("Idempotency-Key", "format", …)`. Pure functions, unit-tested against AC-4's table. |
| Code generator | `link.ShortCodes` | **new** | `@Component`, constructor takes `java.util.Random` (the bean is a `SecureRandom`, §1 mechanics). `String next()`: 8 characters from `[A-Za-z0-9]`, re-drawn while the result is in the reserved set `{api, actuator, v3, swagger-ui, error}` (rule 1, A-23). ADR-0007. |
| Operator setting | `link.LinkProperties` | **new** | `@ConfigurationProperties("urlshort") record LinkProperties(@DefaultValue("http://localhost:8080") String publicBaseUrl)`; environment override `URLSHORT_PUBLIC_BASE_URL` by relaxed binding. Never derived from `Host` or forwarding headers (rule 11, A-4). |
| Feature configuration | `link.LinkConfig` | **new** | `@Configuration(proxyBeanMethods = false)`, `@EnableConfigurationProperties(LinkProperties.class)`; `@Bean Clock clock()` = `Clock.tickMillis(ZoneOffset.UTC)`; `@Bean SecureRandom secureRandom()`. Lives here, not on the application class, because `UrlshortApplication.java` is a Javadoc-only grant. |
| Audit package | `dev.urlshort.audit` (`package-info.java`) | **new** | The write side of the audit trail. Owns the `audit_log` table. The read endpoint is mission 02 (`01-audit-read`). |
| Audit writer | `audit.AuditLog` | **new** | `@Component`; constructor `(JdbcClient jdbc, JsonMapper json, Clock clock)`; one public method `void append(String action, String entity, String entityId, @Nullable Object before, Object after)` that executes **one `INSERT INTO audit_log …`** with named parameters. Actor is the constant `anonymous`; `request_id` is `MDC.get("requestId")` (ADR-0003 contract); `occurred_at` is `clock.instant()`; `before`/`after` are serialised with the context's `JsonMapper` (`null` stays SQL `NULL`). Insert-only by construction: there is no update or delete statement anywhere in the class (NFR-A2). ADR-0008. |
| Web package | `dev.urlshort.web` (`package-info.java`) | **new** | Cross-cutting HTTP concerns. |
| Problem factories | `web.Problems` | **new** | Final class: `validation(field, rule, message)` → `400`; `notFound()` → `404`; `gone()` → `410`; `idempotencyMismatch()` → `422`; nested `record FieldError(String field, String rule, String message)`. Each returns an `org.springframework.web.ErrorResponseException` carrying a `ProblemDetail`; the validation ones set the `errors` property. The platform handler renders them (§2.6) — no project exception classes, no handler methods for them. |
| Error advice | `web.ProblemDetailsAdvice extends ResponseEntityExceptionHandler` | **new** | The one project advice (ADR-0002 amendment). Boot's `ProblemDetailsExceptionHandler` backs off because it is `@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)` **[jar]**. Adds three things: (1) `@ExceptionHandler(Exception.class)` → one ERROR event `request failed` carrying only the exception class chain and the first `dev.urlshort.` stack frame (§5; the throwable itself is **never** passed to the logger), then a bare `500` `ProblemDetail` through `handleExceptionInternal`; (2) an override of `handleHttpMessageNotReadable` that walks the cause chain and, when it finds an `ErrorResponseException` (the body limit raised inside Jackson databind, §2.4), renders that instead of `400` **[probe]**; (3) an override of `createResponseEntity`, the last hook every parent-rendered response passes through **[jar]**, that clears `detail` and sets `instance` to `urn:uuid:<request id>` on every `ProblemDetail` body, domain, framework and `500` alike (§2.6) **[probe R1–R9]**. |
| Body limit | `web.RequestBodyLimitFilter extends OncePerRequestFilter` (+ nested `LimitedInputStream extends ServletInputStream`) | **new** | `@Order(Ordered.HIGHEST_PRECEDENCE + 1)` (after the request-id filter). Wraps every request in an `HttpServletRequestWrapper` whose `getInputStream()` counts bytes and throws `new ErrorResponseException(HttpStatus.CONTENT_TOO_LARGE)` on the 16 385th body byte. Exact at the byte for declared and chunked bodies **[probe]**; the exception is raised inside the DispatcherServlet, so the advice renders it. `getReader()` is not wrapped: nothing on the JSON path calls it. Constant `MAX_BODY_BYTES = 16_384` (NFR-S3). |
| Request id + request event | `web.RequestIdFilter` | **changed** | Keeps ADR-0003 behaviour and, in the same `finally` block before `MDC.remove`, emits one INFO event `request completed` with one SLF4J fluent key-value, `status` (`response.getStatus()`). This is the event that makes rule 10 / AC-26 true on paths where no controller runs (`404` without a handler, `405`, `413`, `415`). The HTTP method is deliberately not logged: Tomcat accepts any token as a method, so it is a client-controlled value (§5). |
| API document configuration | `web.OpenApiConfig` | **new** | `@Bean OpenAPI`: `info` (title `urlshort`, version from the build) and a fixed `servers: [{url: "/"}]`, so the document does not carry the request-derived server URL that differs between MockMvc and a real port **[probe]** and the committed file equals the live one. ADR-0010. |
| Problem-detail handling for framework errors | Boot's `ResponseEntityExceptionHandler` base behaviour via `ProblemDetailsAdvice` | **inherited** | `405`, `415`, `404` (no handler → `NoResourceFoundException`), `400` (unreadable body) stay platform-produced **[probe]**. |
| Schema | `src/main/resources/db/migration/V1__create_link_and_audit_log.sql` | **new** | §3. ADR-0005. |
| Shipped configuration | `src/main/resources/application.properties` | **changed** | `urlshort.public-base-url=http://localhost:8080`; `spring.mvc.servlet.load-on-startup=1` so the DispatcherServlet initialises (and logs) during startup, not inside the first request (§5, DR-04; name verified in Boot 4.1.1's metadata **[jar]**, effect **[probe R1]**); `logging.level.org.springframework.web.servlet.PageNotFound=error`, which silences the parent handler's `405` WARN that prints the client's method token (§5) **[jar]**; `spring.servlet.multipart.enabled=false` (property name verified for 4.1.1 **[jar]**): "no multipart" becomes literal and the multipart resolver never parses a body before the `consumes` check **[probe]**; `springdoc.override-with-generic-response=false` so the advice's `@ExceptionHandler` methods do not inject untyped generic error responses into every operation (AC-28 requires every error response typed `application/problem+json`); `springdoc.writer-with-order-by-keys=true` so the live document is already key-sorted (both names verified in springdoc 3.1.1's metadata **[jar]**). |
| Committed API document | `docs/api/openapi.json` | **new** | Generated by the functional suite (§7.4), key-sorted, pretty-printed, trailing newline. |
| Build | `build.gradle.kts` | **changed** | Commit 1: the dependency overrides (binding constraint). Commit 2: the Javadoc gate. No new dependency: springdoc's `io.swagger.v3.oas.annotations` are already on the compile classpath through the starter. |
| Functional-suite support | `src/functionalTest/java/dev/urlshort/link/FunctionalClock*.java` | **new** | An **offset** `Clock` the suite controls for AC-19: it follows real time (millisecond ticks) plus an adjustable `Duration`, so every other journey still sees "now" and the interval assertions of AC-1 and AC-22 hold. Declared as `@Bean @Primary FunctionalClock functionalClock()`: the bean name must differ from the production `clock` (§7.2, DR-03). |

Mechanics the builder relies on, all platform-provided:

- **`ErrorResponseException` is handled by the parent class.** `ResponseEntityExceptionHandler.handleException` lists it; `handleErrorResponseException` renders the carried `ProblemDetail` with its status. A thrown `Problems.gone()` becomes `410 application/problem+json {"instance":…,"status":410,"title":"Gone"}` under a browser `Accept` and under `Accept: text/html` alone (Spring falls back to the problem media types when nothing else is acceptable) **[probe P1, P2]**.
- **`ProblemDetail.setProperty("errors", …)` renders as a top-level member.** Boot 4.1.1 registers `ProblemDetailJacksonMixin` on the auto-configured `JsonMapper` (`JacksonAutoConfiguration$JsonProblemDetailsConfiguration`) **[jar]**; the body is `{"instance":…,"status":400,"title":"Bad Request","errors":[{"field":"url","rule":"scheme","message":"…"}]}` **[probe P3]**.
- **A bare `ProblemDetail` has `type` `about:blank` and `title` = reason phrase.** The `type` member is omitted from the JSON when it is `about:blank` **[probe]**. Spring fills `instance` with the request URI **only when it is null** (`HttpEntityMethodProcessor.handleReturnValue` checks `getInstance() == null` before `setInstance`) **[jar]**, so a value the advice sets survives **[probe R1–R9]**.
- **`createResponseEntity(body, headers, status, request)` is the last step of `handleExceptionInternal`** for every exception the parent renders (domain `ErrorResponseException`s, framework `405`/`415`/`404`/`400`, the advice's own `500` and `413` unwrap) **[jar]**. Overriding it is therefore one place that sees every problem body; headers (`Allow`, `Accept`) pass through untouched **[probe R1, R5, R6]**.
- **`ResponseEntity.status(302).header(LOCATION, url).cacheControl(CacheControl.noStore())`** writes `Location` byte for byte and `Cache-Control: no-store` **[probe P4]**. `HttpHeaders.setLocation(URI)` is **not** used: it emits `toASCIIString()`, which would re-encode the stored value.
- **Unmatched paths are `404` problem details:** `/favicon.ico` and a 40-letter segment fall outside the `{code:[A-Za-z0-9]{6,32}}` pattern, reach the static-resource handler and raise `NoResourceFoundException`, which the parent handler renders **[probe P5, P6]**. The route pattern is case-sensitive (`PathPatternParser` default), satisfying rule 1's case rule without code.
- **Wrong method and wrong content type** are `405` (`Allow: GET`) and `415` problem details from the parent handler **[probe P7, P8, P9]**.
- **Jackson 3 wraps a RuntimeException raised from the request stream** while it is inside a value into `DatabindException`, which Spring turns into `HttpMessageNotReadableException` (`400`). When the limit falls between tokens the exception propagates raw. Both forms were seen **[probe P10b before the fix, P10c]**; the advice override makes both `413` **[probe P10b, P10c after the fix]**.
- **The catch-all `500`** keeps the response inside MVC (no `/error` dispatch, which `OncePerRequestFilter` would skip and which would lose `requestId`); the body is `{"instance":"urn:uuid:<id>","status":500,"title":"Internal Server Error"}`. It must pass a **non-null** body to `handleExceptionInternal`: with a null body and status `500` the parent sets `jakarta.servlet.error.exception` on the request **[jar]**. A throwable handed to the ECS formatter renders `error.message` and `error.stack_trace`, and both carry driver text with bound values: an H2 unique violation quotes the duplicate key (`design-review` probe, `docs/review/01-create-redirect/proof/design-boundary-probe.txt:51`). So the event gets the class chain and one code frame as key-values, and the throwable is never logged **[probe R7, R8]**.
- **The parent handler logs in two places** **[jar]**: `handleHttpRequestMethodNotSupported` WARNs `ex.getMessage()` (`Request method 'X' is not supported`, with the client's method token) on the `org.springframework.web.servlet.PageNotFound` category, which the shipped configuration raises to ERROR; and `handleExceptionInternal` WARNs `"Response already committed. Ignoring: " + ex` on the advice's own category, which only fires after the response is committed. Nothing in this slice fails after commit: every body is built before it is written and is far below Tomcat's 8 KiB response buffer.
- **The DispatcherServlet initialises lazily on the first request** unless `spring.mvc.servlet.load-on-startup` is set. Its three INFO lines are written inside that request but before any filter runs, so they carry no `requestId` (`design-boundary-probe.txt:35–37`). With `load-on-startup=1` they appear before `Tomcat started`, and the first request's window holds only its own correlated event **[probe R1]**. MockMvc cannot show this: `@AutoConfigureMockMvc` initialises its `TestDispatcherServlet` when the context starts.
- **Source-file mode compiles without `-parameters`**, so `@PathVariable String code` fails with `IllegalArgumentException` there. Spring 7 has no bytecode fallback for parameter names. The Spring Boot Gradle plugin adds `-parameters` to every `JavaCompile` task **[docs]**, so production code may rely on it; the probes name their parameters explicitly **[probe R4, R8, first run]**.
- **`Clock.tickMillis(UTC)`** yields millisecond instants (`…07.898Z` where `systemUTC()` gave `…07.898134Z`) **[probe]**. Every timestamp the service stores or returns comes from this bean, so the `createdAt` in the `201` body and the one read back from H2 (`TIMESTAMP WITH TIME ZONE`, microsecond precision) are equal and AC-8's byte-identical body holds on every platform.
- **springdoc 3.1.1 serialises with Jackson 2** (`com.fasterxml` in `ObjectMapperProvider`) **[jar]** — the reason the Jackson 2 constraint is part of the override commit — and, unless the `OpenAPI` bean declares servers, emits `"servers":[{"url":"http://127.0.0.1:<port>","description":"Generated server url"}]` **[probe P12]**.
- **Spring Data JDBC 4.1.1** persists records as aggregates (`@Id` component null → `INSERT` without the identity column; non-null → `UPDATE`), maps camelCase to snake_case, derives `findByCode`/`findByIdempotencyKey`, converts `Instant` for `TIMESTAMP WITH TIME ZONE` columns and `@Query` parameters, and lets a plain `Repository` sub-interface expose selected CRUD methods by signature **[docs]**. If the driver rejects an `Instant` parameter on the `@Query`, pass `java.sql.Timestamp.from(at)`; the contract does not change.

## 2. API contract

Common to every endpoint: `X-Request-Id` on every response (ADR-0003); every
non-2xx/3xx body is an RFC 9457 `ProblemDetail`, `Content-Type:
application/problem+json`, `status` equal to the HTTP status, regardless of
`Accept` (rule 8, A-15, **[probe]**). Every problem body is built from
server-owned values only: `status`, `title` (the reason phrase), `instance`
= `urn:uuid:<X-Request-Id>`, and on validation and mismatch the `errors`
array with static messages. No problem body carries `detail`, so there is no
stack trace, exception class name, SQL text or submitted value in any of them
(§2.6, **[probe R1–R9]**).

### 2.1 `POST /api/links` — create

Request: `Content-Type: application/json`; body `{"url": "<string>"}`;
optional header `Idempotency-Key`. Unknown JSON members ignored (A-14). The
`Host`, `X-Forwarded-*` and `Forwarded` headers are never read (AC-3).

Validation order (the first failure is the only one reported; rule 3, A-12),
all in `LinkValidation` before any lookup or write:

| Step | Field | `rule` token | Condition |
|---|---|---|---|
| 1 | `url` | `required` | member absent, `null`, empty, or `isBlank()` |
| 2 | `url` | `too-long` | `length() > 2048` (characters) |
| 3 | `url` | `scheme` | does not start with `http://` or `https://`, compared case-insensitively (`regionMatches(true, …)`); a leading space fails here |
| 4 | `url` | `malformed` | any character outside visible ASCII `0x21`–`0x7E` (RFC 3986 URIs are ASCII; this also makes a trailing space, an inner space, CR and LF `malformed` and keeps the `Location` header free of injection), or `new java.net.URI(url)` throws, or `getHost()` is null or empty (`https://`, `https://[bad/`) |
| 5 | `url` | `credentials` | `getUserInfo() != null` (`https://user:secret@…`, `https://user@…`) |
| 6 | `Idempotency-Key` | `format` | header present and not matching `^[\x21-\x7E]{1,255}$` (empty, 256 long, a space, a non-ASCII byte; a header Tomcat decodes as ISO-8859-1 arrives as chars above `0x7E`) |

Stored verbatim after validation (rule 2): no trim, no normalisation, no
re-encoding, `String` in and `String` out.

Success:

| Item | Value |
|---|---|
| Status | `201 Created` |
| `Content-Type` | `application/json` |
| `Location` | `/api/links/<code>` (relative path; `ResponseEntity.created(URI.create("/api/links/" + code))`) (A-21) |
| Body | `{"code":"Ab3dE9fG","shortUrl":"http://localhost:8080/Ab3dE9fG","url":"https://example.com/some/path?q=1","state":"active","createdAt":"2026-10-03T05:12:42.133Z"}` — exactly five fields (AC-1, A-2) |
| `code` | 8 characters `[A-Za-z0-9]`, matches the SPEC's `^[A-Za-z0-9]{6,32}$` (ADR-0007) |
| `shortUrl` | `urlshort.public-base-url` + `/` + `code` (rule 11) |
| `createdAt` | ISO-8601 UTC instant with `Z`, millisecond precision (`Clock.tickMillis`), seconds-precision comparison in tests (rule 12) |

Idempotent replay (rule 5, ADR-0009): a bound key within its 24 h window with
the same `url` answers `201` with the same `Location` and the link's **current**
representation (`state` may be `retired`); no row is written. See §2.5.

Errors:

| Trigger | Status | Body (beyond `status`/`title`/`instance`) | Produced by |
|---|---|---|---|
| validation failure on `url` (AC-4) | `400` `Bad Request` | `errors: [{field: "url", rule: <token>, message: <free text, never the value>}]`, exactly one element | `LinkValidation` → `Problems.validation` → parent handler |
| malformed `Idempotency-Key` (AC-20) | `400` | `errors: [{field: "Idempotency-Key", rule: "format", message}]` | same |
| body not a JSON object: empty, truncated, array, `url` is an object (AC-5) | `400` | bare (the framework's `detail` is cleared); no `errors` | `HttpMessageNotReadableException` → parent `handleHttpMessageNotReadable` (the override finds no `ErrorResponseException` in the chain and delegates) |
| `Content-Type` `text/plain` or `multipart/form-data` (AC-6) | `415` `Unsupported Media Type` | bare; the framework's `Accept: application/json` response header is kept. The framework `detail` would quote the client's `Content-Type`, parameters included, so it is cleared **[probe R1]** | `consumes = application/json` → `HttpMediaTypeNotSupportedException`; multipart disabled by property |
| body larger than 16 384 bytes (AC-7) | `413` `Content Too Large` | bare | `RequestBodyLimitFilter` → `ErrorResponseException(CONTENT_TOO_LARGE)` → parent handler, directly or via the advice's unwrap **[probe]** |
| same key, different `url`, within 24 h (AC-18) | `422` `Unprocessable Content` | `errors: [{field: "Idempotency-Key", rule: "mismatch", message}]` | `LinkService.create` → `Problems.idempotencyMismatch()` |
| audit insert fails, code collision, key race (§2.5) | `500` `Internal Server Error` | bare | `ProblemDetailsAdvice` catch-all; the transaction has rolled back |
| wrong method `GET`/`DELETE /api/links` (AC-15) | `405` `Method Not Allowed` | bare; `Allow` header kept **[probe R5, R6]** | parent handler |

### 2.2 `GET /api/links/{code}` — read

Path pattern `/api/links/{code:[A-Za-z0-9]{6,32}}`. `200`, `application/json`,
the same five-field body as the create response built from the current row
(AC-8, AC-9). `404` problem detail when no row has the code
(`LinkService.read` → `Problems.notFound()`) and also when the segment does not
match the pattern (no handler → `NoResourceFoundException`, framework `404`,
AC-14's 40-letter row). `PUT`/`PATCH` on the path → `405` (AC-15).

### 2.3 `DELETE /api/links/{code}` — retire

`204 No Content`, empty body (`@ResponseStatus(NO_CONTENT)`, `void`). `404`
when unknown; `410 Gone` problem detail when already retired — decided by the
conditional update's row count, so a repeat `DELETE` (AC-11) and a concurrent
double retire both answer `410` and write no second audit row.

### 2.4 `GET /{code}` — redirect

Path pattern `/{code:[A-Za-z0-9]{6,32}}`, case-sensitive. Query string and
fragment on the short link are ignored (rule 7, A-16).

| Case | Status | Headers | Body |
|---|---|---|---|
| active link (AC-12) | `302 Found` | `Location: <stored url, byte for byte>`; `Cache-Control: no-store`; `X-Request-Id` | empty (not part of the contract) |
| retired link (AC-13) | `410 Gone` | no `Location` | problem detail |
| unknown code, `/favicon.ico`, any segment outside the pattern (AC-14) | `404 Not Found` | — | problem detail (domain for a pattern-matching unknown code; framework for the rest) |
| `POST`/`PUT /<code>` (AC-15) | `405` | `Allow: GET` (framework) | problem detail |

`HEAD` and `OPTIONS` keep framework defaults (`HEAD` is served by the `GET`
mapping). Why `302` and `no-store`: ADR-0006.

### 2.5 Service rules (where the behaviour lives)

`LinkService`, constructor `(LinkRepository links, ShortCodes codes, AuditLog audit, Clock clock)`:

```
@Transactional
Link create(String url, @Nullable String key):
    now = clock.instant()
    if key != null:
        bound = links.findByIdempotencyKey(key)
        if bound present:
            if now < bound.createdAt + 24h:                      // rule 5: window starts at the binding 201
                if bound.url.equals(url): return bound           // replay: no insert, no audit row
                throw Problems.idempotencyMismatch()             // 422; binding and window untouched
            links.releaseIdempotencyKey(bound.id)                // expired: the key is free again
    link = links.save(new Link(null, codes.next(), url, now, null, key))
    audit.append("link.create", "link", link.code(), null, link.snapshot())
    return link

@Transactional(readOnly = true) Link read(String code):    links.findByCode(code).orElseThrow(Problems::notFound)
@Transactional(readOnly = true) Link resolve(String code): read(code); if retired → throw Problems.gone(); return it

@Transactional
void retire(String code):
    link = links.findByCode(code).orElseThrow(Problems::notFound)
    if links.retire(link.id(), clock.instant()) == 0: throw Problems.gone()   // already retired, or lost the race
    audit.append("link.retire", "link", code, link.snapshot(), new LinkSnapshot(link.url(), "retired"))
```

Consequences the SPEC asks for, and how they follow:

- **A rejected request never changes a binding** (AC-18, AC-19, AC-21): validation runs before `create` is entered; the `422` is thrown before any write; the only writes are the expired-key release and the insert, both of which happen only on the path that ends in `201`.
- **At most one link per key under concurrency** (rule 5): `uq_link_idempotency_key`. Two creates that both see no binding both insert; the second insert fails and that request answers `500` (fail closed; nothing was written). H2's message for that failure quotes the key, which is why the `500` event logs exception classes only (§5, DR-01). The retry a real client sends then replays. A one-shot retry inside the service is deliberately not built: catching the violation inside the `@Transactional` method cannot work (the repository's own transactional proxy has already marked the shared transaction rollback-only), so a retry would need a second transaction and a `TransactionTemplate`; `// ponytail: same-key race answers 500; add an out-of-transaction retry if clients hit it`. ADR-0009.
- **Code uniqueness** (rule 1): `uq_link_code`. A collision at 8 random characters is a once-in-10¹⁴ event per insert; it answers `500` and the client retries. No pre-check loop (`// ponytail:` comment in `ShortCodes`). ADR-0007.
- **Audit in the same transaction** (rule 9, AC-24): `append` is called inside the `@Transactional` method; a `DataAccessException` from it propagates, Spring rolls the link insert or retire back, the advice answers `500`.

### 2.6 Problem-detail shape decisions (ADR-0002 amendment)

Rule 8: a problem body never contains "a value the client submitted". The
request path, the method token and every header value are submitted values,
and the framework echoes them: the `415` `detail` quotes the full
`Content-Type` with its parameters, the no-handler `404` `detail` quotes the
path, the `405` `detail` quotes the method, and Spring's default `instance` is
the request path. So the advice does not try to sort safe framework wording
from unsafe; its `createResponseEntity` override applies one rule to every
body:

- **`detail` is cleared.** Domain problems never set it (`Problems` puts its
  information in `errors[]`). Framework wording is dropped, including the
  harmless `Failed to read request`. A client tells the cases apart by
  `status`, `title` and the presence of `errors`.
- **`instance` is `urn:uuid:<X-Request-Id>`**, read from the MDC key the
  request-id filter sets (ADR-0003; the id is `UUID.randomUUID()`). RFC 9457
  defines `instance` as an identifier for "the specific occurrence of the
  problem", and the request id is exactly that. It is server-issued and is
  the same value as the response header and every log event of the request,
  so an Operator can go from a body to its logs. The filter runs before the
  DispatcherServlet on every request, so the MDC value is always present when
  the advice runs. A unit test that calls the advice without the filter sets
  the MDC key itself; there is no fallback branch.
- `type` stays `about:blank` (omitted from the JSON) and `title` is the HTTP
  reason phrase. The SPEC asserts tokens and statuses, never `type`.
- `errors[]` elements are `{field, rule, message}` (A-11); `message` is a
  static text per rule that never interpolates the submitted value.
- Headers the framework adds (`Allow` on `405`, `Accept` on `415`) are
  server-derived and kept.

Verified by effect on a real Tomcat (§12, `revision-output.txt` R1–R9): with
canaries in a `Content-Type` parameter, a 45-letter path, a `.ico` path, a
code-shaped path, a custom method and an H2 duplicate-key message, no canary
appears in any body, and every body's `instance` equals `urn:uuid:` plus that
response's `X-Request-Id`.

### 2.7 Configuration

| Property | Default | Override | Used by |
|---|---|---|---|
| `urlshort.public-base-url` | `http://localhost:8080` | `URLSHORT_PUBLIC_BASE_URL` | `LinkResponse.of` (`shortUrl`). No trailing slash; the value is used as given. |
| `spring.servlet.multipart.enabled` | `false` (shipped) | — | no multipart parsing anywhere (NFR-S3) |
| `spring.mvc.servlet.load-on-startup` | `1` (shipped; Boot default `-1` = lazy) | — | the DispatcherServlet initialises at startup, so no uncorrelated framework line is written inside the first request (rule 10, AC-26) |
| `logging.level.org.springframework.web.servlet.PageNotFound` | `error` (shipped) | — | the parent handler's `405` WARN quotes the client's method token (rule 10 / NFR-O2); the category is only ever used at WARN |
| `springdoc.api-docs.path`, `springdoc.swagger-ui.path` | as shipped (`/v3/api-docs`, `/swagger-ui.html`) | — | exposure is **on** in every profile for this slice; the production profile and any restriction are `03-operate`'s decision (architecture guide §5) |
| `springdoc.override-with-generic-response` | `false` (shipped) | — | springdoc would otherwise read the advice's `@ExceptionHandler` methods and add generic, untyped error responses to every operation; each operation declares its own problem responses instead (AC-28) |
| `springdoc.writer-with-order-by-keys` | `true` (shipped) | — | the live `/v3/api-docs` is key-sorted at the source; the committed file adds indentation (§7.4) |

## 3. Data model & migration

`V1__create_link_and_audit_log.sql` (the baseline; both tables run on H2 in
PostgreSQL mode and on PostgreSQL; no vendor syntax):

```sql
-- V1: the first stored resource (link) and the write side of the audit trail (audit_log).
-- rollback: DROP TABLE audit_log; DROP TABLE link;  -- pre-production baseline; destroys all rows

CREATE TABLE link (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            VARCHAR(32)   NOT NULL,
    url             VARCHAR(2048) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    retired_at      TIMESTAMP WITH TIME ZONE,
    idempotency_key VARCHAR(255),
    CONSTRAINT uq_link_code            UNIQUE (code),
    CONSTRAINT uq_link_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_link_code_length     CHECK (LENGTH(code) >= 6),
    CONSTRAINT ck_link_url_not_empty   CHECK (url <> '')
);

CREATE TABLE audit_log (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    actor        VARCHAR(64)   NOT NULL,
    action       VARCHAR(64)   NOT NULL,
    entity       VARCHAR(32)   NOT NULL,
    entity_id    VARCHAR(64)   NOT NULL,
    request_id   VARCHAR(64)   NOT NULL,
    before_state VARCHAR(4096),
    after_state  VARCHAR(4096) NOT NULL
);
```

Design notes (ADR-0005, ADR-0008, ADR-0009):

- **`retired_at` instead of a state column.** The link has two states and one
  one-way transition (rule 6); a nullable timestamp encodes the state, records
  when it changed, and removes the need for an enum, a `CHECK`, and an
  `updated_at` column (the only mutation has its own timestamp). `state` in
  the API is derived.
- **The idempotency binding is a column on `link`.** A key binds to exactly
  one link, only by the `201` that created it, so the binding time *is*
  `created_at` and no second table or foreign key is needed. `UNIQUE` on a
  nullable column admits any number of `NULL`s on both engines. After 24 h the
  service nulls the old row's key (`releaseIdempotencyKey`) in the same
  transaction as the new insert, so the uniqueness constraint keeps guarding
  the live binding. Alternative (own table) in §11.
- **Column names avoid H2 reserved words** (`KEY`, `VALUE` are reserved;
  `AT`/`BEFORE`/`AFTER` are keywords in one engine or the other), hence
  `idempotency_key`, `occurred_at`, `before_state`, `after_state`.
- **No JSON column type**: `before_state`/`after_state` are opaque JSON text
  (`{"url":…,"state":…}`, at most ~2 100 characters) in `VARCHAR(4096)`;
  portable, no CLOB handling in the driver, never queried by field in this
  slice. Mission 02's audit-read slice reads them as text.
- **No foreign key from `audit_log` to `link`**: `entity`/`entity_id` identify
  the subject by its public code so an audit row outlives any future hard
  delete and so the writer has no dependency on the link table.
- **Time**: all `TIMESTAMP WITH TIME ZONE`, all values UTC instants from the
  application `Clock` bean (millisecond ticks). No column defaults from the
  database clock: the 24 h rule compares `created_at` with the application
  clock, and `databases.md` §4 forbids mixing clocks inside one rule.
- **Identity**: surrogate `BIGINT` identity, never exposed; `code` is the
  public identifier.

Queries this slice runs and the index that serves each:

| Query | Where | Index |
|---|---|---|
| `SELECT … FROM link WHERE code = ?` | read, resolve, retire | `uq_link_code` |
| `SELECT … FROM link WHERE idempotency_key = ?` | create with a key | `uq_link_idempotency_key` |
| `INSERT INTO link …` | create | — |
| `UPDATE link SET retired_at = ? WHERE id = ? AND retired_at IS NULL` | retire | primary key |
| `UPDATE link SET idempotency_key = NULL WHERE id = ?` | create, expired key | primary key |
| `INSERT INTO audit_log …` | create, retire | — |

`audit_log` gets no secondary index: nothing in the application reads it in
this slice; the test suite's `(entity, entity_id)` lookups run on tables of a
few rows; the audit-read slice adds the index its pagination needs.

Rollback: the two `DROP TABLE` statements above, in that order. This is a
baseline on an empty database with no production data; no `db/rollback/`
script is warranted (`databases.md` §1). A later destructive change follows
expand → migrate → contract.

Flyway runs at startup against the file database (shipped) and against the
in-memory databases of both suites (`application-functional.properties`, the
unit suite's properties file); the functional suite therefore tests the
migration on every run.

## 4. Sequences

Create, including the idempotency branches (`docs/diagrams/create-link-sequence.mmd`):

```mermaid
sequenceDiagram
    autonumber
    participant C as Creator
    participant F as RequestIdFilter + RequestBodyLimitFilter (web)
    participant LC as LinkController (link)
    participant V as LinkValidation
    participant S as LinkService (@Transactional)
    participant R as LinkRepository → H2 link
    participant A as AuditLog → H2 audit_log
    participant E as ProblemDetailsAdvice

    C->>F: POST /api/links {"url":U} [Idempotency-Key: K]
    F->>F: X-Request-Id R, MDC requestId=R; wrap body stream (limit 16 384 B)
    F->>LC: chain.doFilter
    LC->>V: validateUrl(U), validateIdempotencyKey(K)
    alt rule fails
        V-->>E: ErrorResponseException 400 {errors:[{field,rule}]}
        E-->>C: 400 application/problem+json
    end
    LC->>S: create(U, K)
    opt K present
        S->>R: findByIdempotencyKey(K)
        alt bound and now < createdAt + 24h
            alt same url
                S-->>LC: existing link (replay; no write)
                LC-->>C: 201 Location /api/links/C, current representation
            else different url
                S-->>E: ErrorResponseException 422 {errors:[{Idempotency-Key, mismatch}]}
                E-->>C: 422 problem detail (binding untouched)
            end
        else bound and expired
            S->>R: releaseIdempotencyKey(id)
        end
    end
    S->>R: save(Link(code=ShortCodes.next(), url=U, createdAt=now, key=K))
    S->>A: append("link.create","link",C,null,{url,state:active})
    A->>A: INSERT audit_log (actor anonymous, request_id = MDC requestId, occurred_at = clock)
    alt audit insert fails (or the insert loses a same-key race)
        A-->>S: DataAccessException → rollback
        S-->>E: exception
        E->>E: ERROR "request failed" {errorChain, errorOrigin} (no message, no stack trace)
        E-->>C: 500 problem detail, instance urn:uuid:R (nothing stored)
    end
    S-->>LC: link
    LC-->>C: 201 Location /api/links/C {code,shortUrl,url,state,createdAt}
    F->>F: INFO "request completed" {status}; MDC.remove
```

Redirect and retire (`docs/diagrams/redirect-sequence.mmd`):

```mermaid
sequenceDiagram
    autonumber
    participant V as Visitor / Creator
    participant F as RequestIdFilter (web)
    participant RC as RedirectController (link)
    participant LC as LinkController (link)
    participant S as LinkService
    participant R as LinkRepository → H2 link
    participant A as AuditLog → H2 audit_log
    participant E as ProblemDetailsAdvice

    V->>F: GET /C  (Accept: text/html,…,*/*;q=0.8)
    F->>RC: chain.doFilter (requestId on MDC)
    RC->>S: resolve(C)
    S->>R: findByCode(C)
    alt no row
        S-->>E: ErrorResponseException 404
        E-->>V: 404 application/problem+json
    else retired_at set
        S-->>E: ErrorResponseException 410
        E-->>V: 410 application/problem+json, no Location
    else active
        S-->>RC: link
        Note over RC: 02-analytics hook goes here (one call), nothing else changes
        RC-->>V: 302 Location: <stored url> Cache-Control: no-store
    end

    V->>F: DELETE /api/links/C
    F->>LC: chain.doFilter
    LC->>S: retire(C)
    S->>R: findByCode(C) → link (404 if absent)
    S->>R: UPDATE link SET retired_at=now WHERE id=? AND retired_at IS NULL
    alt 0 rows (already retired, or a concurrent retire won)
        S-->>E: ErrorResponseException 410
        E-->>V: 410 problem detail, no audit row
    else 1 row
        S->>A: append("link.retire","link",C,{url,active},{url,retired})
        S-->>LC: done
        LC-->>V: 204
    end
```

## 5. Logging & audit events

Log events (ECS JSON, one object per line; `requestId` arrives from the MDC;
envelope members are framework-owned and not asserted):

| Event | Logger | Level | `message` | Fields present | Must never appear |
|---|---|---|---|---|---|
| request completed | `dev.urlshort.web.RequestIdFilter` | INFO | `request completed` | `requestId`; key-value `status` (the ECS formatter renders SLF4J fluent key-value pairs as top-level members **[jar, probe R1–R9]**) | the method (a client-chosen token), path, query string, any header value, the `url`, the key, the client address, the user agent |
| request failed | `dev.urlshort.web.ProblemDetailsAdvice` | ERROR | `request failed` | `requestId`; `errorChain`: the class names of the cause chain, outermost first, joined by ` <- `, at most eight (`org.springframework.dao.DuplicateKeyException <- org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException`); `errorOrigin`: the first stack frame of the outermost exception whose class starts with `dev.urlshort.`, as `StackTraceElement.toString()`, else `none` **[probe R7, R8]** | the throwable itself: no `error.message`, no `error.stack_trace`, no message of any exception in the chain. Driver messages quote bound values (DR-01), and every value in this event is a class name or a code location, never request data |
| ping handled | `dev.urlshort.ping.PingController` | INFO | `ping` | unchanged from `01-ping` | — |

The `request failed` event is deliberately thin. An Operator triaging a `500`
has the request id (from the response header or the problem's `instance`),
the exception classes (which say what failed, for example a duplicate key
versus a lost connection) and the line of our code it went through. They do
not get the driver message or the full trace. Reproducing with the message
means a local run or a deliberately changed log configuration, which leaves
the default configuration that rule 10 governs.
`// ponytail: class chain + one frame; add a message-free full-frame renderer if triage needs more`.

Sketch, so the reviewer and the builder read the same thing:

```java
@ExceptionHandler(Exception.class)
ResponseEntity<Object> unhandled(Exception ex, WebRequest request) {
    log.atError().setMessage("request failed")
            .addKeyValue("errorChain", Stream.iterate((Throwable) ex, Objects::nonNull, Throwable::getCause).limit(8)
                    .map(t -> t.getClass().getName()).collect(Collectors.joining(" <- ")))
            .addKeyValue("errorOrigin", Arrays.stream(ex.getStackTrace()).filter(f -> f.getClassName().startsWith("dev.urlshort."))
                    .findFirst().map(StackTraceElement::toString).orElse("none"))
            .log();                                   // never log.error(..., ex)
    return handleExceptionInternal(ex, ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR),
            new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
}

@Override
protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers,
        HttpStatusCode status, WebRequest request) {
    if (body instanceof ProblemDetail problem) {      // always true on this slice's paths; the false branch is unit-tested
        problem.setDetail(null);
        problem.setInstance(URI.create("urn:uuid:" + MDC.get("requestId")));
    }
    return super.createResponseEntity(body, headers, status, request);
}
```

No controller or service logs anything else: the `url`, the `Idempotency-Key`,
the `User-Agent`, the remote address and any inbound header value are never
passed to a logger (rule 10, AC-27). The code may appear in a log only through
the audit row's `entity_id`, which is a table, not a log. One event per request
is guaranteed by the filter even on framework error paths (AC-26 rows `404`
no-handler, `405`, `413`, `415`). Framework log sites were checked too (§1
mechanics): the `405` WARN that quotes the method is silenced by the shipped
`PageNotFound` level, the committed-response WARN cannot fire on this slice,
and the DispatcherServlet initialises at startup, so the first real request
writes no line without its `requestId` (DR-04, **[probe R1]**). The H2
driver's own trace file is covered in §6.

Audit events (table `audit_log`, ADR-0008):

| `action` | When | `entity` / `entity_id` | `before_state` | `after_state` | `actor` | `request_id` | `occurred_at` |
|---|---|---|---|---|---|---|---|
| `link.create` | a create that inserted a row (never on replay) | `link` / the code | `NULL` | `{"url":"<url>","state":"active"}` | `anonymous` | the `X-Request-Id` of this request (MDC) | `Clock` instant |
| `link.retire` | the first successful retire of a code | `link` / the code | `{"url":"<url>","state":"active"}` | `{"url":"<url>","state":"retired"}` | `anonymous` | same | same |

Rules: written inside the mutating transaction (AC-24 proves the rollback);
reads, redirects, replays and every rejected request write nothing (AC-25);
the application has no statement that updates or deletes an audit row
(`AuditLog` contains one `INSERT`; the unit suite asserts the class exposes
exactly one public method and that its SQL is an insert, §7). The row holds
the `url` because the Operator needs it to reconstruct the change; it holds
no client address, no user agent, no key.

## 6. Threat model

Assets: the stored targets and their integrity (a Visitor must land where the
Creator intended); the audit trail's completeness and immutability; absence
of client PII in logs; the public base URL (short links minted on the right
host); availability.

Entry points (new in this slice): `POST /api/links` (JSON body, `Idempotency-Key`
header); `GET`/`DELETE /api/links/{code}` (path segment); `GET /{code}`
(path segment, query string, `Accept`); every other method on these paths;
the operator setting `URLSHORT_PUBLIC_BASE_URL`; the `audit_log` and `link`
tables (through the single `sa` user of the embedded database).

| STRIDE | Threat | Mitigation in this slice | Residual / owner |
|---|---|---|---|
| Spoofing | Short URLs minted on an attacker's host through `Host` / `X-Forwarded-Host` | `shortUrl` is built from configuration only; no code reads those headers; AC-3 proves it with `Host: evil.example` | none |
| Spoofing | A client supplies `X-Request-Id` to impersonate a request | inherited: inbound id ignored (ADR-0003, AC-27) | none |
| Spoofing | A client replays another client's `Idempotency-Key` and receives that client's link (or a `422` that reveals the key exists) | keys are 1–255 visible ASCII; collisions require guessing another client's key | **accepted**: one global key space because there are no clients or tenants (NFR-S6, A-9); clients should use UUIDs; per-client scopes are out of scope by the SPEC |
| Tampering | Open redirect to `javascript:`, `data:`, `file:`, `ftp:` or a scheme-less value | rule 3 allow-list (`http://`/`https://`), evaluated before storage; `Location` is only ever a stored, validated value (AC-4, AC-12) | **accepted by product**: redirecting to any `http(s)` host is the feature; link previews are out of scope (`docs/REQUIREMENTS.md` §4) |
| Tampering | Header injection through the target (`\r\n` in `Location`) | `malformed` rejects any byte outside `0x21`–`0x7E`, so CR, LF and spaces never reach the header | none |
| Tampering | Log injection through client values | no client value is logged; ECS escapes everything it writes | none |
| Tampering | SQL injection | Spring Data derived queries, `@Query` and `JdbcClient` with named parameters; no concatenated SQL | none |
| Tampering | Audit rows altered or removed by the application | insert-only writer; no update/delete statement; unit assertion + code review (NFR-A2); AC-25 | DB-level grants: the embedded H2 has one user; container and file permissions are `03-operate`'s |
| Tampering | Lost update on `link` (a stale aggregate overwriting a concurrent retire) | the two mutations are targeted `UPDATE … WHERE` statements, never `save()` of a re-read aggregate; retire is conditional on `retired_at IS NULL` | none |
| Repudiation | A mutation without a trace | one audit row per create/retire in the same transaction, with `request_id`; a failed audit write rolls back the change (AC-22 to AC-24) | the row carries no client identity by design (NFR-S6, A-17) |
| Information disclosure | Stack trace, class name or SQL in a response | every error is a `ProblemDetail` with no `detail`; the `500` body is bare (AC-24, rule 8) | none |
| Information disclosure | Submitted value echoed in an error body (path segment, method token, `Content-Type` parameter, `url`, key) | the advice's `createResponseEntity` clears `detail` and sets `instance` to `urn:uuid:<request id>` on every problem body, domain and framework; `errors[].message` is static (§2.6); header, path and method canaries in `ObservabilityJourneyTest` (§7.1) and **[probe R1–R6]** | none |
| Information disclosure | PII in logs (client address, `User-Agent`, target URL with tokens, key, method token, header values) | nothing client-controlled is passed to a logger; the `500` event carries the exception class chain and one code frame, never a throwable or message, so a driver message quoting a bound key or code never reaches the log (DR-01, **[probe R7, R8]**); the request event logs `status` only; the parent handler's `405` WARN is silenced (`PageNotFound` at ERROR); access log off; `process.thread.name` excluded (ADR-0004); AC-27 canaries including a real H2 duplicate-key failure (§7.2) | none |
| Information disclosure | Driver messages written by H2 to its own trace file (`data/urlshort.trace.db`, file database only) | H2 traces SQL errors in the `23xxx` integrity class (the duplicate key of a same-key race) at INFO, below its default file level (ERROR) **[jar: `TraceObject.logAndConvert`]**; the other driver errors that quote a value (value too long, `22001`) are excluded by validation, which caps `url` (2 048) and key (255) at their column widths and keeps the audit JSON (≈ 2 100 characters, §3) under its 4 096 limit | not a log stream of the service; `TRACE_LEVEL_FILE=0` is `03-operate`'s choice with the production profile |
| Repudiation / correlation | Requests Tomcat rejects before the servlet chain (malformed request line, headers over 8 KiB) | outside every endpoint contract and AC; no filter runs, so no `X-Request-Id` and no problem detail | **not verified here**: what Tomcat answers and logs on these paths is left to `03-operate`, which owns server limits |
| Information disclosure | Short-code enumeration | 62⁸ ≈ 2.2 × 10¹⁴ codes from `SecureRandom`; at 10⁶ links a guess hits with probability ≈ 5 × 10⁻⁹; `404` and `302` are indistinguishable in timing terms at this scale | rate limiting is `03-operate` |
| Information disclosure | `/v3/api-docs` and `/swagger-ui.html` describe the API | public contract by design; no secrets in it | production-profile exposure decided by `03-operate` |
| Denial of service | Oversized JSON body | counting stream, `413` at byte 16 385 for declared and chunked bodies; multipart disabled; headers at Tomcat defaults (8 KiB) | request flooding is `03-operate` (`429`) |
| Denial of service | Unbounded `url` after parsing | length checked before parsing (`too-long` at 2 048); the body cap bounds the parse | none |
| Denial of service | Concurrent same-key or same-code inserts | unique constraints make duplicates impossible; the loser gets a `500` and retries | none beyond the `500` |
| Elevation of privilege | — | no principals exist (NFR-S6); the service runs no server-side fetch and executes nothing from input | none |

Dependencies: none added. The override commit raises Tomcat and Jackson patch
versions (OSV capture in the proof contract); springdoc's annotations are
already on the classpath.

## 7. Test strategy hints

### 7.1 Suites and classes

Functional (`src/functionalTest/java`, `@SpringBootTest @AutoConfigureMockMvc`,
one test per AC named after it, tabled criteria as one `@ParameterizedTest`):

| Class | ACs |
|---|---|
| `link/LinkCreateJourneyTest` | AC-1, AC-2, AC-3 (default base, `Host: evil.example`), AC-4 (parameterised over the table), AC-5, AC-6, AC-7, AC-16 |
| `link/PublicBaseUrlJourneyTest` | AC-3 second row: `@SpringBootTest(properties = "urlshort.public-base-url=https://sho.rt")` (one extra context; the environment-variable spelling is Boot's relaxed binding, documented and not re-tested) |
| `link/LinkReadRetireJourneyTest` | AC-8, AC-9, AC-10, AC-11, AC-14, AC-15 |
| `link/RedirectJourneyTest` | AC-12, AC-13 (browser `Accept`; assert `Location` absent) |
| `link/IdempotencyJourneyTest` | AC-17, AC-18, AC-19 (moves the `FunctionalClock`), AC-20 (parameterised), AC-21 |
| `audit/AuditJourneyTest` | AC-22, AC-23, AC-24 (`@MockitoSpyBean AuditLog`), AC-25 |
| `web/ObservabilityJourneyTest` | AC-26 (all twelve response cases; `OutputCaptureExtension`; **every line captured while the request ran** — not only the lines containing `R` — is one JSON object with `requestId == R`, and there is at least one; this is RQ-03's quantifier and is stricter than `PingJourneyTest.AC6`), AC-27 (five canaries + `setRemoteAddr("203.0.113.77")`; plus the database-failure canary of §7.2: a real H2 duplicate-key failure whose driver message quotes the key canary, absent from the capture, with exactly one `request failed` event carrying `requestId == R` and an `errorChain` naming `DuplicateKeyException`), and rule 8 / rule 10 for submitted values the framework would echo: one `@ParameterizedTest` over `Content-Type: text/plain; note=<canary>` → `415`, `GET /api/links/<45-letter canary>` → `404`, `GET /<canary>.ico` → `404`, `GET /api/links/<code-shaped canary>` and `GET /<code-shaped canary>` → `404` (domain), and a custom method `<CANARY>` on `/api/links` → `405`. Each row asserts that the canary is absent from the body and from the capture, that the body has no `detail` member, and that `instance` equals `urn:uuid:` + `X-Request-Id` |
| `web/ColdStartJourneyTest` | AC-26 on the real server's **first** request (DR-04): `@SpringBootTest(webEnvironment = RANDOM_PORT)`, one test method, `java.net.http.HttpClient` (or `RestTestClient` bound to the port). It sends one `GET /api/ping` (the only request this context's Tomcat ever sees) and asserts that every line appended to the capture during that request is a JSON object with `requestId == R` (§7.2 for the wait) |
| `web/OpenApiDocumentTest` | AC-28 and the committed-equals-live check (§7.4) |
| existing `ping/PingJourneyTest`, `HealthJourneyTest` | unchanged (AC-16 covers the shadowing guard) |

Unit (`src/test/java`, plain JUnit 5 + AssertJ + Mockito, no Spring context):

| Class | What it proves |
|---|---|
| `link/LinkValidationTest` | every row of AC-4's table maps to its token and nothing else; the 2 048/2 049 boundary; every AC-20 key case; a valid key and a `null` key pass |
| `link/ShortCodesTest` | length 8, charset, two draws differ; with a scripted `Random` that spells `actuator` first the generator re-draws (the reserved branch) |
| `link/LinkServiceTest` | mocks for `LinkRepository`, `AuditLog`, `ShortCodes`, a fixed `Clock`: replay / mismatch / expired-release / fresh insert branches; `retire` returning `0` → `410` and no audit call; audit called with the exact snapshots; `resolve` on a retired link → `410` |
| `link/LinkTest` | `state()` for both values; `snapshot()` |
| `audit/AuditLogTest` | **the NFR-A2 persistence-level assertion:** `AuditLog` exposes exactly one public method, `append`, and its SQL constant starts with `INSERT INTO audit_log`; serialisation of a `null` before-state stays `null` (mock `JdbcClient` or capture the parameters) |
| `web/ProblemsTest` | each factory's status, `title`, `errors` content; messages never contain the value passed in |
| `web/ProblemDetailsAdviceTest` | unwrap finds an `ErrorResponseException` two levels down → its status; a plain cause chain → `400`; catch-all → `500` body without `detail`; with `MDC.put("requestId", <uuid>)` every rendered body has `instance` `urn:uuid:<uuid>` and no `detail`, even when the framework exception carried one; `createResponseEntity(null, …)` passes through (the non-`ProblemDetail` branch); the catch-all on `new IllegalStateException("<canary>", new SQLException("<canary>"))` writes one event whose captured line contains `errorChain`, `errorOrigin` and neither canary |
| `web/RequestBodyLimitFilterTest` | `MockHttpServletRequest` with content of 16 384 bytes read fully; 16 385 bytes throws on the last read; `read()` single-byte path; `isFinished`/`isReady`/`setReadListener`/`close` delegate (a stub `ServletInputStream` makes these lines reachable, which the coverage gate requires) |
| `web/RequestIdFilterTest` | existing assertions plus: the request event is emitted after the chain with `status` and without the method (a Logback `ListAppender` or `OutputCaptureExtension` on the unit suite) |
| existing `UrlshortApplicationTests`, `ping/*Test` | unchanged |

### 7.2 Mechanisms the tests depend on

- **Suite-controlled clock (AC-19, A-19).** `src/functionalTest/java/dev/urlshort/link/FunctionalClock.java` (`extends Clock`; `instant()` returns `Clock.tickMillis(UTC).instant().plus(offset)`; `shift(Duration)` adds to the offset, `reset()` zeroes it) and `FunctionalClockConfig`, a **top-level `@Configuration`** in the functional source set declaring `@Bean @Primary FunctionalClock functionalClock()`. It is picked up by the application's component scan because the functional classes sit under `dev.urlshort` on the test classpath; only `@TestConfiguration` classes are excluded from that scan. This is deliberate: one shared context for every functional class, no per-class `@Import`. The production `clock` bean still exists and `@Primary` wins by type. **The method name must not be `clock`**: Boot disables bean-definition overriding, so a second `clock` is rejected with `BeanDefinitionOverrideException` before `@Primary` is ever considered. The design review showed both outcomes (`docs/review/01-create-redirect/proof/design-boundary-probe.txt:59–62` rejected, `:66` the distinct name starts and selects the functional clock). Every functional journey starting is the suite-level check that the two configurations coexist. **It is an offset clock, not a frozen one:** every other journey compares `createdAt` and the audit time against real `Instant.now()` intervals (AC-1, AC-22), which a clock frozen at context start would break. `IdempotencyJourneyTest.AC19` shifts by 23 h, then 24 h − 1 s, then 24 h + 1 s from the recorded `t0`, and resets in `@AfterEach`. Millisecond ticks keep the round-trip equality of §1.
- **Induced audit failure (AC-24, A-22).** `@MockitoSpyBean AuditLog auditLog` in `AuditJourneyTest`; `doThrow(new DataAccessResourceFailureException("audit store unavailable")).when(auditLog).append(eq("link.retire"), any(), any(), any(), any())`. The spy delegates to the real bean otherwise and Boot resets it after each test. Expect `500 application/problem+json` without `Exception`, `\tat ` or `SQL` in the body, then `GET /C` → `302`, `GET /api/links/C` → `active`, no `link.retire` row. The spy changes the context key, so this class starts a second context.
- **Database-failure canary (DR-01, AC-27).** In `ObservabilityJourneyTest` (same `@MockitoSpyBean AuditLog`, so the same context as `AuditJourneyTest`), the spy answers `append(eq("link.create"), …)` by running one real statement through the context's `JdbcClient`: `INSERT INTO link (code, url, created_at, idempotency_key) SELECT CONCAT('Zz', code), url, created_at, idempotency_key FROM link WHERE idempotency_key = :k`. The answer runs inside the create transaction on the same connection, so it sees the row just inserted and copies it under another code. H2 rejects the copy on `uq_link_idempotency_key`, and its message quotes the key, exactly the failure of a lost same-key race (design review probe, `design-boundary-probe.txt:51`). The request sends `Idempotency-Key: <key canary>` and a `url` carrying a query canary. Expect: `500` problem detail; neither canary anywhere in the capture or the body; exactly one `request failed` line with `requestId == R` and `errorChain` containing `DuplicateKeyException`; and `SELECT COUNT(*) FROM link WHERE idempotency_key = :k` is `0` (the create rolled back, the key stays unbound). This runs the real driver message through the real advice and the shipped logging configuration; R7 shows the same on Tomcat. Fallback if the answer does not see the in-flight row (it would then wait on H2's row lock and fail differently): the spy throws `new DuplicateKeyException("… '<key canary>' …", new SQLIntegrityConstraintViolationException("… '<key canary>' …"))`. The assertions stay the same; only the message becomes synthetic.
- **Cold-start window (DR-04).** On a real server the `request completed` event is written in the filter's `finally`, which can run after the client already has the response (the probe needed a 300 ms pause). `ColdStartJourneyTest` therefore records the capture length before sending, then polls the capture (50 ms steps, at most 5 s) until a `request completed` line with `requestId == R` appears, and only then asserts on everything appended since the request began. Nothing else in the JVM logs in that window once the context has started.
- **Audit assertions** read `audit_log` with the context's `JdbcClient`. The in-memory database (`DB_CLOSE_DELAY=-1`) outlives a context and is shared by every context in the JVM, so **AC-25 and every "exactly one row" assertion must filter by code and count deltas**, never assume an empty table.
- **Browser `Accept`** is literally `text/html,application/xhtml+xml,*/*;q=0.8` on AC-12 to AC-14.
- **AC-7 bodies**: build `{"url":"https://example.com/","pad":"xxx…"}` padded to exactly 16 384 and 16 385 bytes (the probe's `jsonOfSize`); MockMvc sets `Content-Length` from the bytes.
- **Log assertions**: capture with `OutputCaptureExtension`. For AC-26 take the capture's length before the request and read everything appended by the time the response is back (MockMvc is synchronous and the suite runs serially, so that window belongs to the request); parse each line with the context's `JsonMapper` and assert it is an object whose `requestId` equals the header. A line without the id fails the test, which is what RQ-03 asked for. For AC-27 search the whole capture for the canaries. The `500` case reuses the AC-24 spy.
- **Contexts**: the base context (every MockMvc class), the spy context (`AuditJourneyTest`, `ObservabilityJourneyTest`; identical spy declarations give one cache key), the base-URL context (`PublicBaseUrlJourneyTest`) and the real-server context (`ColdStartJourneyTest`, the only `RANDOM_PORT` class, so its Tomcat sees exactly one request). Logging is configured once per JVM; all four log ECS JSON. All four share the one in-memory database, where Flyway finds the schema current after the first.

### 7.3 Coverage

The gate is 100 % line and branch on the merged data. Every branch above has a
named test: the service's idempotency branches through AC-17 to AC-21, the
`retire == 0` branch and the reserved-code branch through unit tests with
scripted collaborators, the advice's unwrap loop through AC-7 (declared length,
wrapped) and AC-5 (no match), the `createResponseEntity` non-`ProblemDetail`
branch through the advice unit test (no HTTP path reaches it), the stream's single-byte `read()` and the
delegating methods through the filter unit test. Expected `docs/qa/GAPS.md`
row: "None for this slice". If the builder finds an unreachable branch in
framework-shaped code (for instance the `ReadListener` delegate), the honest
answer is a unit test that calls it, not an exclusion.

### 7.4 The committed API document (AC-28, NFR-M3, ADR-0010)

`web/OpenApiDocumentTest` (functional):

1. `GET /v3/api-docs` through MockMvc → `200`.
2. Parse the body into `Map<String, Object>` with the context's `JsonMapper`.
3. Serialise it with a mapper built as `JsonMapper.builder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, SerializationFeature.INDENT_OUTPUT).build()` (Jackson 3), append one `\n`. Nested maps sort recursively; arrays keep their order.
4. If the environment variable `OPENAPI_EXPORT` is set, write the string to `docs/api/openapi.json` (path relative to the working directory, which is the worktree root under Gradle) and pass. Otherwise assert that the file's content equals the string byte for byte: a drift between code and the committed document fails the suite.
5. Assert AC-28's content on the parsed map: `paths` has exactly `/api/ping`, `/api/links`, `/api/links/{code}`, `/{code}`; the create operation documents `201`, `400`, `413`, `415`, `422`; read `200`, `404`; delete `204`, `404`, `410`; redirect `302` with a `Location` header, `404`, `410`; every `4xx`/`5xx` response's content is keyed `application/problem+json`; the create request body and the `201` and `200` responses carry at least one example.

Regenerate with `OPENAPI_EXPORT=1 scripts/gw functionalTest --tests '*OpenApiDocumentTest*'`
and commit the file. Determinism comes from the sort, the fixed `servers`
member (`OpenApiConfig`), springdoc's method-name-derived `operationId`s
(keep handler method names unique across controllers) and class-name-derived
schema names. QA's diff of the committed file against the candidate's live
`/v3/api-docs` (both key-sorted) is then empty by construction. Later slices
re-run the same command on their branch.

The annotations that produce the content live on the controllers:
`@Operation(summary)`, `@ApiResponses` with `@ApiResponse(responseCode, content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))` for every error, `@Header(name = "Location")` on the `302`, `@ExampleObject` on the create request body and the `201`/`200` responses. `springdoc.override-with-generic-response=false` (shipped, §2.7) keeps springdoc from deriving generic responses from `ProblemDetailsAdvice`'s handler methods; without it every operation would gain untyped `500`/`200` entries and AC-28's "every error response is `application/problem+json`" would fail. If springdoc still adds a generic `200` to the redirect or delete operation, the declared `@ApiResponses` replace it; if `/error` appears in `paths`, exclude it with `springdoc.paths-to-exclude=/error`. The builder checks the generated file once by eye before committing it.

## 8. Reachability check

| AC | Reached by | Error AC explicit? | Threat model covers the entry point? |
|---|---|---|---|
| AC-1 | `LinkController.create` → `LinkService.create` → `LinkRepository.save` → `LinkResponse.of` | — | `POST /api/links` ✔ |
| AC-2 | no dedupe by `url`; `ShortCodes.next()` per create | — | ✔ |
| AC-3 | `LinkProperties.publicBaseUrl`; `LinkResponse.of` | — | `Host` spoofing ✔ |
| AC-4 | `LinkValidation.validateUrl` → `Problems.validation` | yes, §2.1 | body input ✔ |
| AC-5 | Jackson → `HttpMessageNotReadableException` → advice override → parent `400` | yes | ✔ |
| AC-6 | `consumes = application/json`; multipart disabled | yes | ✔ |
| AC-7 | `RequestBodyLimitFilter.LimitedInputStream` → advice | yes | body size ✔ |
| AC-8, AC-9 | `LinkService.read` → `LinkResponse.of` (`state()` derived from `retired_at`) | — | path segment ✔ |
| AC-10 | `LinkService.retire` (conditional `UPDATE`) + `AuditLog.append` | — | ✔ |
| AC-11 | `retire` row count `0` → `Problems.gone()`; no audit call | yes | ✔ |
| AC-12 | `RedirectController` → `LinkService.resolve`; String `Location`; `CacheControl.noStore()` | — | open redirect ✔ |
| AC-13 | `resolve` → `Problems.gone()`; no `Location` | yes | ✔ |
| AC-14 | `Problems.notFound()` for pattern-matching unknown codes; `NoResourceFoundException` for the rest | yes | ✔ |
| AC-15 | mapping set (`POST /api/links`, `GET`/`DELETE /api/links/{code}`, `GET /{code}`) → parent `405` | yes | ✔ |
| AC-16 | the `{code:[A-Za-z0-9]{6,32}}` constraint; literal mappings win | — | reserved paths ✔ |
| AC-17 | `create`: bound + same `url` → return existing; no audit | — | key space ✔ |
| AC-18 | `create`: bound + different `url` → `422`; nothing written | yes | ✔ |
| AC-19 | `now < createdAt + 24h` against the `Clock` bean; expired → `releaseIdempotencyKey` + new insert | — | ✔ |
| AC-20 | `LinkValidation.validateIdempotencyKey` | yes | header input ✔ |
| AC-21 | validation precedes `create`; an unbound key stays unbound | — | ✔ |
| AC-22, AC-23 | `AuditLog.append` inside `create`/`retire`; MDC `requestId`; `Clock` | — | audit integrity ✔ |
| AC-24 | `@Transactional` + propagated `DataAccessException` → rollback; advice `500` | yes | ✔ |
| AC-25 | insert-only `AuditLog`; no other code touches `audit_log` | — | ✔ |
| AC-26 | `RequestIdFilter` event in `finally`; advice keeps every error inside MVC; `load-on-startup=1` keeps servlet initialisation out of the first request (`ColdStartJourneyTest`) | — | log stream ✔ |
| AC-27 | no client value reaches a logger; inbound `X-Request-Id` never read; the `500` event logs class names and one frame, never a message; `PageNotFound` at ERROR | — | inbound headers, method token, driver messages ✔ |
| rule 8 (no echo) | `ProblemDetailsAdvice.createResponseEntity`: no `detail`, `instance` = `urn:uuid:<request id>` | every error AC | path, method, `Content-Type` ✔ |
| AC-28 | springdoc + `OpenApiConfig` + controller annotations; `OpenApiDocumentTest` | — | API document exposure ✔ |

Every business rule maps to at least one row of §7.1. No AC needs a component
outside §1.

## 9. Territory (confirmed against `slice.yaml` on `main` at `ad567aa`)

| Path | In `slice.yaml` | Needed by this design |
|---|---|---|
| `src/{main,test,functionalTest}/java/dev/urlshort/link/` | yes | yes: twelve production classes, their unit tests, five journey classes, the functional clock |
| `src/{main,test,functionalTest}/java/dev/urlshort/audit/` | yes (write side) | yes: `AuditLog`, its unit test, `AuditJourneyTest` |
| `src/{main,test,functionalTest}/java/dev/urlshort/web/` | yes | yes: `Problems`, `ProblemDetailsAdvice`, `RequestBodyLimitFilter`, `OpenApiConfig`, the changed `RequestIdFilter`, their tests, `ObservabilityJourneyTest`, `ColdStartJourneyTest`, `OpenApiDocumentTest` |
| `src/main/resources/db/migration/` | yes | yes: `V1__create_link_and_audit_log.sql` |
| `docs/api/openapi.json` | yes (new) | yes |
| `build.gradle.kts` | yes | yes: overrides (commit 1), Javadoc gate (commit 2); nothing else |
| `src/main/resources/application.properties` | yes | yes: six lines (§2.7: base URL, multipart, servlet load-on-startup, `PageNotFound` level, two springdoc settings) |
| `src/test/resources/application.properties` | yes (optional conversion) | not required by this design; the unit suite loads the context only in `UrlshortApplicationTests`, which needs the in-memory URL that file already provides |
| `src/functionalTest/resources/application-functional.properties` | yes | unchanged (the in-memory database is all the suite needs) |
| `src/main/java/dev/urlshort/UrlshortApplication.java`, `src/main/java/dev/urlshort/ping/` | Javadoc-only grant (`ad567aa`) | Javadoc only (§13); no bean, no behaviour change |

No territory extension is requested. The `Clock` and `SecureRandom` beans and
`@EnableConfigurationProperties` sit in `link.LinkConfig` for that reason.

## 10. Decisions recorded as ADRs

| ADR | Decision | Status |
|---|---|---|
| [ADR-0002](../../../../docs/adr/0002-problem-details-via-platform-handler.md) (amended) | one project advice extending `ResponseEntityExceptionHandler`; domain errors as `ErrorResponseException`; `errors[{field,rule,message}]` extension; `type` stays `about:blank`; no `detail` on any problem and `instance` = `urn:uuid:<request id>` (one `createResponseEntity` override); catch-all `500` that never logs the throwable | amended 2026-10-03, revised after design review DR-01/DR-02 |
| [ADR-0004](../../../../docs/adr/0004-structured-ecs-logs-no-client-pii.md) (amended) | the `500` event carries class chain and one code frame, never a message or trace; the request event logs `status` only; `PageNotFound` at ERROR; DispatcherServlet initialised at startup | amended 2026-10-03 (DR-01, DR-04) |
| [ADR-0005](../../../../docs/adr/0005-persistence-h2-flyway-spring-data-jdbc.md) | H2 (PostgreSQL mode) + Flyway, Spring Data JDBC aggregates and `JdbcClient`, no JPA; schema conventions; application `Clock` for every stored instant | proposed with this design |
| [ADR-0006](../../../../docs/adr/0006-redirect-302-no-store.md) | redirects are `302` with `Cache-Control: no-store`; `Location` verbatim | proposed |
| [ADR-0007](../../../../docs/adr/0007-short-code-generation.md) | 8 characters `[A-Za-z0-9]` from `SecureRandom`, reserved first segments refused, uniqueness by constraint, collision answers `500` | proposed |
| [ADR-0008](../../../../docs/adr/0008-audit-record-same-transaction.md) | `audit_log` shape, same-transaction rule, insert-only writer, actor `anonymous`, `request_id` from the MDC | proposed |
| [ADR-0009](../../../../docs/adr/0009-idempotency-key-binding.md) | key bound on the `link` row, 24 h window from `created_at`, replay returns the current representation, mismatch `422`, race resolved by the unique constraint | proposed |
| [ADR-0010](../../../../docs/adr/0010-committed-openapi-document.md) | the committed document is generated by the functional suite, key-sorted, with a fixed `servers` member; the suite fails on drift | proposed |

ADRs become `accepted` at plan-lock. Not decided here: salted IP hashing
(`02-analytics`), rate limiting and the production profile (`03-operate`).

## 11. Trade-offs

| Option | Why not now | What would change the decision |
|---|---|---|
| Idempotency key in its own table (`idempotency_key` → `link`) | a second table, a foreign key and a join for a binding that is 1:1 with the row that created it; the column plus `UNIQUE` gives the same guarantee | per-client key scopes, storing the first response verbatim, or a key retention job separate from links |
| A `state` enum column (+ `CHECK`, `updated_at`) | two states and one one-way transition; `retired_at` records state and time in one nullable column | a third state or a reversible transition (reactivate) |
| Bean Validation on `CreateLinkRequest` (`@NotBlank`, `@Size`, `@Pattern`, group sequences for order) | reports every violation at once and needs `@GroupSequence` plus a custom `errors[]` mapping to meet the SPEC's one-token-in-fixed-order contract; a 30-line pure function is smaller and unit-testable against the table | a second request type with independent, unordered field rules |
| A domain exception hierarchy mapped in the advice | four outcomes map 1:1 to SPEC statuses and tokens; `ErrorResponseException` is the platform's type for exactly that and the parent handler renders it; `Problems` keeps the shape in one place (`java-spring.md` §2's intent) | a non-HTTP caller of `LinkService`, or a second feature that maps the same failure differently |
| Jackson `StreamReadConstraints.maxDocumentLength` for the 16 KiB cap | the check runs when the parser refills its buffer, so it is approximate by up to the buffer size and cannot meet AC-7's exact byte boundary | an AC that accepts an approximate limit |
| `Content-Length` check only (no counting stream) | leaves chunked bodies unbounded; the counting stream is exact for both and needs the same advice unwrap anyway | — |
| Retry the create once on a same-key unique violation | needs a second transaction (`TransactionTemplate`) because the repository proxy marks the first rollback-only; the SPEC asks only for "at most one link", which the constraint gives | evidence that clients retry concurrently in practice |
| Pre-check `existsByCode` before insert | a query per create to avoid a once-in-10¹⁴ `500`; the constraint still decides | the table approaching 10⁹ rows or a shorter code |
| `Clock.systemUTC()` plus truncation at each stamping site | one `tickMillis` bean does it once for every stamp | a need for sub-millisecond ordering |
| `@TestConfiguration` imported per functional class for the clock | every class would need the import and a forgotten one silently gets the system clock; the scanned configuration gives one context for the suite | a second functional configuration that must not see the mutable clock |
| springdoc Gradle plugin or a bootRun + curl script for the document | a new build plugin and a second process per regeneration; the suite already boots the context and must verify the document anyway | the document needing data the test context cannot produce |
| A message-free renderer of the whole stack trace for the `500` event (every frame of every cause, no messages) | more code to write, review and cover, and a 60-line event for every `500`; the class chain plus the first `dev.urlshort.` frame says what failed and where in our code, and the request id leads to the rest | triage that repeatedly needs frames below our own code |
| Keeping framework `detail` where the wording is known to be static (`Failed to read request`) and clearing only the echoing ones | a per-exception allow-list that silently goes stale when a framework upgrade changes a message; clearing every `detail` is one line and cannot drift | a client that needs prose beyond `title` and `errors` |
| `instance` = the matched route pattern (`/api/links/{code}`) | unmatched paths have no pattern (the resource handler's `/**`), so the no-handler `404` would need a second rule; the request id identifies the occurrence, which is what RFC 9457 asks of `instance`, and it links the body to the logs | — |
| Proving DR-04 only by the builder's captured run (no real-server test) | the shipped property is one line that a later change can drop without any test noticing; MockMvc cannot see the cold path; one `RANDOM_PORT` class costs one context start | the suite's runtime becoming a problem |
| Logging the request event per controller | no controller runs on `404` without a handler, `405`, `413`, `415`; the filter is the only place that sees every request | — |
| Per-operation `@ApiResponse` annotations vs an `OpenApiCustomizer` adding problem responses globally | annotations are reviewed next to the endpoint and are the standard; the customiser is more code for four operations | ten or more operations sharing the same error set |
| Splitting the slice FR-3/FR-4 by outcome (pre-authorised by the mission brief) | the four endpoints share one aggregate, one repository, one advice and one audit writer; the redirect's `410` needs retire in the same slice; the ACs are many but shallow. Buildable as one implement step | the design review judging the surface too large for one review round |

## 12. Design probe (what was verified by effect)

`design-probe/MechanismProbe.java` boots the shipped main code (functional
profile, random port) with three probe beans that use exactly the mechanisms
above — an `ErrorResponseException`-throwing controller, a
`ResponseEntityExceptionHandler` subclass with the catch-all and the unwrap,
and the counting-stream filter — and exercises them with `java.net.http`
against Tomcat. Run from the repo root:

```sh
scripts/gw --log missions/01-greenfield-core/slices/01-create-redirect/design-probe/output.txt \
    --offline -I missions/01-greenfield-core/slices/01-create-redirect/design-probe/mechanism-probe.gradle designMechanismProbe
```

`design-probe/output.txt` is the record. What it shows:

| Case | Result | Design item |
|---|---|---|
| P1, P2 | `410 application/problem+json {"instance":…,"status":410,"title":"Gone"}` under the browser `Accept` and under `text/html` alone; no `Location` | AC-13, rule 8, A-15 |
| P3 | `400` body with top-level `errors:[{field,rule,message}]` | AC-4, AC-18, AC-20 shape |
| P4 | `302`, `location=https://example.com/some/path?q=1&r=a%20b` unchanged, `cache-control=no-store` | AC-12, rule 7 |
| P5, P6 | `404` problem detail for `/favicon.ico` and a 40-letter segment (framework `detail` echoes the path; superseded by R2, R3) | AC-14, §2.6 |
| P7 | `405`, `Allow: GET`, problem detail, for `POST` on the redirect route | AC-15 |
| P8, P9 | `415` problem detail for `text/plain` and for `multipart/form-data` with multipart disabled (framework `detail` quoted the media type; superseded by R1) | AC-6 |
| P10a, P10d | `200` for exactly 16 384 bytes, declared and chunked | AC-7 |
| P10b, P10c | `413` problem detail for 16 385 bytes, declared (via the unwrap) and chunked (raw) | AC-7 |
| P11 + its ECS line | `500` bare problem detail; one ERROR event with `requestId` equal to the response header, `error.type`, `error.message`, single-line `error.stack_trace` (the logging half is superseded by R7, R8: that event leaked the message, DR-01) | AC-24, AC-26, NFR-R6 |
| P12 | springdoc emits a request-derived `servers` URL unless configured | ADR-0010 |
| clock | `systemUTC()` has microsecond precision on this machine; `tickMillis` gives milliseconds | §1 round-trip rule |

**Revision probe (design review DR-01, DR-02, DR-04).** `design-probe/RevisionProbe.java`
boots the shipped main code the same way, with the two new shipped properties
(`spring.mvc.servlet.load-on-startup=1`, `PageNotFound` at ERROR) and probe
beans that use exactly the revised mechanisms: the advice of §5 (the
`createResponseEntity` override and the message-free `request failed` event)
and a filter writing `request completed` with `status` only. It tees stdout,
and for every request it checks by effect the body and **every line written
while the request ran**, then prints a `VERDICT` line. The first request
after startup is R1, so R1 is the cold request. Run from the repo root:

```sh
scripts/gw --log missions/01-greenfield-core/slices/01-create-redirect/design-probe/revision-output.txt \
    --offline -I missions/01-greenfield-core/slices/01-create-redirect/design-probe/revision-probe.gradle designRevisionProbe
```

| Case | Request (canary) | Result in `design-probe/revision-output.txt` | Finding |
|---|---|---|---|
| startup | — | `Initializing Spring DispatcherServlet` and `Completed initialization` precede `Tomcat started` | DR-04 |
| R1 | first request: `Content-Type: text/plain; note=<canary>` | `415`, `accept=application/json` kept; body `{"instance":"urn:uuid:<id>","status":415,"title":"Unsupported Media Type"}`; one log line in the window, carrying `requestId` | DR-02, DR-04 |
| R2, R3 | 45-letter path; `/<canary>.ico` | `404`, no `detail`, `instance` = `urn:uuid:<id>` | DR-02 |
| R4 | domain `ErrorResponseException(404)` on a code-shaped path | same shape | DR-02 |
| R5, R6 | custom method `PROBEMETHODCANARY`; `PUT` | `405`, `allow=POST` kept; method canary in neither body nor logs | DR-02, rule 10 |
| R7 | two inserts of the same `Idempotency-Key` canary into an H2 `UNIQUE` column | `500`; ERROR `request failed` with `errorChain` `org.springframework.dao.DuplicateKeyException <- org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException` and `errorOrigin` at the controller line; canary absent from body and logs | DR-01 |
| R8 | exception chain whose outer and inner messages carry a canary | `500`; `errorChain` `java.lang.IllegalStateException <- java.lang.IllegalArgumentException`; canary absent | DR-01 |
| R9 | `ErrorResponseException(413)` | bare `413`, `instance` replaced | §2.6 |

Every case printed `instanceIsUrnOfRequestId=true detailPresent=false
canaryInBody=false logLinesWithoutThisRequestId=0 canaryInLogs=false`. The
probe was run twice. The first run answered R4 and R8 with `500`, because
source-file mode compiles without `-parameters` and the probe's unnamed
`@PathVariable`/`@RequestParam` could not be resolved (§1 mechanics). The
recorded output is the second run, with the parameters named explicitly.
DR-03 was verified by the reviewer's own control and is not re-run here (§7.2).

Not verified by me, left to the builder's tests: Spring Data JDBC mapping of
the record aggregate and the two `@Modifying` queries; `@MockitoSpyBean` on
`AuditLog` and the spy answer's same-transaction visibility of the row just
inserted (§7.2); the scanned functional `@Configuration`; the springdoc
annotations' exact output; the `ColdStartJourneyTest` itself (the cold path
it guards is R1). Each has a named fallback in §1 or §7 that keeps the
contract unchanged. Now verified, where the first round had left it open:
the ECS rendering of fluent key-value pairs as top-level members (`status`,
`errorChain`, `errorOrigin` in R1–R9).

## 13. Documentation and build plan

**Documentation.** The Javadoc contract of `docs/guidance/java-spring.md` §8
applies (human decision 2026-10-03, operator packet
`qitem-20261003051852-2c3bd470`): every public type and every public or
protected method under `src/main/java` carries Javadoc that states the
contract (purpose, `@param` constraints, `@return` incl. the absent case,
`@throws`, invariants such as filter order and transaction boundary); records
document components with `@param`; `package-info.java` in `link/`, `audit/`
and `web/` states the outcome each package serves and its boundary; a class
that exists because of an ADR says so and an endpoint's Javadoc names the ACs
it serves; no Javadoc on test methods, one class-level comment per test class.
`scripts/gw check` runs `javadoc -Xdoclint:all -Werror`.

**Build plan for the builder** (test-first inside each feature commit;
`scripts/gw check` green on every commit):

1. **`chore(01-create-redirect): pin Tomcat 11.0.25, Jackson 3.1.7 and Jackson 2 2.21.7`** — `build.gradle.kts` only (the overrides decided on `00-hello`, advisory `qitem-20261003021640-bc2477ef`); gated green alone; fresh OSV run captured under `proof/`.
2. **`chore(01-create-redirect): enforce Javadoc with -Xdoclint:all -Werror in check`** — the `tasks.javadoc` block from the guide, `tasks.check { dependsOn(tasks.javadoc) }`, plus Javadoc on the four classes the gate flags (`UrlshortApplication`, `PingController`, `PingResponse`, `RequestIdFilter`; comments only, Javadoc-only grant); gated green alone.
3. Feature commits, in dependency order, each with its tests: migration + `Link`/`LinkRepository`/`LinkConfig`/`LinkProperties` → `Problems` + `ProblemDetailsAdvice` + `RequestBodyLimitFilter` + the `RequestIdFilter` event → `LinkValidation` + `ShortCodes` + `LinkService` + `LinkController` (create, read) → `RedirectController` + retire → idempotency branches → `AuditLog` wiring and the audit journeys → `OpenApiConfig`, annotations and `OpenApiDocumentTest` with the committed document → traceability, coverage reports, proof captures.

The first two commits are the binding constraints from the mission plan-lock
and the Javadoc routing; nothing feature-shaped may precede them on the branch.

## Status

- 2026-10-03 05:12Z — design packet `qitem-20261003051242-83fa8c2c` claimed; SPEC candidate `0acbc9d`.
- 2026-10-03 05:19Z — lead's note on the packet: Javadoc policy (§13) and the pre-feature commit order; `SPEC.md` unchanged.
- 2026-10-03 05:33Z–05:36Z — mechanism probe run three times (`design-probe/output.txt` is the last run); the declared-length `413` path exposed Jackson's wrapping and the advice unwrap was added and re-verified.
- 2026-10-03 — design written against SPEC `0acbc9d` (28 AC, 12 rules); ADR-0005 to ADR-0010 and the ADR-0002 amendment drafted; `docs/DESIGN.md` and diagrams updated; `plan-review` run on the finished draft, three important findings fixed in place (offset clock, AC-26 quantifier, springdoc generic responses). No question parked on `human@kernel`; no territory extension needed. Handed to `design_review`.
- Mission context at handoff: the human's fast plan (05:30Z) made `02-analytics` and `03-operate` low-tier and moved `04-audit-read` to mission 02; this slice's human plan-lock is unchanged, and nothing in this design depends on the tier of its dependants.
- 2026-10-03 06:03Z — design review **FAIL** on `0aaab2f` (`docs/review/01-create-redirect/design-review.md`): DR-01 HIGH, DR-02 HIGH, DR-03 and DR-04 MEDIUM. Rework packet `qitem-20261003060343-3eb84b99` claimed 06:03Z; SPEC unchanged at `0acbc9d`.
- 2026-10-03 06:10Z–06:11Z — revision probe run twice (`design-probe/revision-output.txt` is the second run; §12). Jar checks: `HttpEntityMethodProcessor` fills `instance` only when null; `ResponseEntityExceptionHandler`'s two log sites; `spring.mvc.servlet.load-on-startup` in Boot 4.1.1's metadata; H2's trace levels by error class.
- 2026-10-03 — all four findings fixed (*Review response*); ADR-0002 and ADR-0004 amended, ADR-0009 and `docs/DESIGN.md` aligned, create sequence updated. Handed back to `design_review`.
- Plan-lock: human decision D11 (06:05Z, `missions/01-greenfield-core/NOTES.md` §1) delegates every slice plan-lock to the orchestration lead, this slice's included. When the design review passes, the gate goes to `orchestration-lead@urlshort-factory` and I stamp `--on-behalf-of orchestration-lead@urlshort-factory`. `slice.yaml` keeps `tier: high` as the risk classification.

## Review response

Review `docs/review/01-create-redirect/design-review.md` on candidate
`0aaab2f`: FAIL on DR-01 and DR-02 (HIGH), with DR-03 and DR-04 (MEDIUM). Every
finding is answered below; none is disputed. The previous design's two
"accepted" reflections and its "residual" driver-message leak are withdrawn,
not reworded. No scope change, no new dependency, and no human question.

| Id | Severity | Response | Where | Evidence |
|---|---|---|---|---|
| DR-01 | HIGH | **Fixed.** The catch-all no longer hands the throwable to the logger. `request failed` carries `requestId`, `errorChain` (class names of the cause chain) and `errorOrigin` (first `dev.urlshort.` frame), and nothing else: no message, no stack trace, no cause text. The `request completed` event drops `method`, which is a client-chosen token, and the framework's `405` WARN that quotes the method is silenced (`PageNotFound` at ERROR). The deferred privacy exception is gone from §5, §6 and the self-check. The suite gains a real database-failure canary: an H2 duplicate-key failure on the create path whose driver message quotes the key canary, asserted absent from the capture, with the create rolled back. The operator's loss (no trace by default) is written down in §5 and in ADR-0002/ADR-0004 | §1, §2.5, §5, §6, §7.1, §7.2, §11; ADR-0002, ADR-0004, ADR-0009 | R7 (H2 duplicate key, key canary absent), R8 (message canaries in a two-level chain absent) |
| DR-02 | HIGH | **Fixed**, under the strict reading of rule 8. One `createResponseEntity` override in the advice clears `detail` and sets `instance` to `urn:uuid:<request id>` on every problem body, domain, framework and `500` alike. Status, media type, `Allow`/`Accept` headers and typed `errors[]` are unchanged. Header, path and method canaries are added to `ObservabilityJourneyTest` (body and logs, plus `instance` = request id and no `detail`). ADR-0002 and `docs/DESIGN.md` §3 are aligned | §1, §2, §2.1, §2.6, §6, §7.1, §8; ADR-0002 | R1 (`Content-Type` parameter), R2/R3 (no-handler paths), R4 (domain `404`), R5/R6 (method, `Allow` kept), R9 |
| DR-03 | MEDIUM | **Fixed.** The functional bean is `@Bean @Primary FunctionalClock functionalClock()`, and the reason the name must differ is written next to it | §1, §7.2 | reviewer's control, `design-boundary-probe.txt:59–62` (rejected) and `:66` (distinct name selected) |
| DR-04 | MEDIUM | **Fixed** without weakening the quantifier. The shipped `spring.mvc.servlet.load-on-startup=1` moves DispatcherServlet initialisation into startup, before `Tomcat started`. `ColdStartJourneyTest` (the only real-server class) guards the first real request with the shipped logging configuration, waiting for the completion event before it asserts | §1, §2.7, §5, §7.1, §7.2; ADR-0004 | startup lines and R1 (first request: one line, carrying its `requestId`) |

Found in passing and fixed in the same places: the `405` WARN that quotes the
method (above), and the `method` member of the request event (above). Recorded
rather than changed: the committed-response WARN, which cannot fire on this
slice (§1, §5); H2's own trace file (§6); requests that Tomcat rejects before
the servlet chain (§6, not verified, `03-operate`).

## Self-check

Recorded before the first handoff and revised before the second.
"Verified" means I read the SPEC clause and the design section side by side.
What was executed is the two probes in §12 and the jar inspections marked
**[jar]**.

| # | Item | Result | Where |
|---|---|---|---|
| 1 | Every AC reachable, component named | ✔ 28 rows, each naming the class and method | §8 |
| 2 | Every error AC is an explicit `ProblemDetail` | ✔ `400` (three producers), `404` (two), `405`, `410`, `413`, `415`, `422`, `500`, each with its producer; shape and media type proven by effect; every body built from server-owned values only (no `detail`, `instance` = request id) | §2, §2.6, §12 |
| 3 | Migration has a written rollback | ✔ two `DROP TABLE` statements, order stated, risk stated | §3 |
| 4 | Log/audit events PII-free | ✔ two new events with their fields listed; the `500` event logs class names and one code frame, never a throwable; the request event logs `status` only; framework log sites checked (method WARN silenced, committed-response WARN unreachable, servlet init moved to startup). No residual on the log stream. Audit rows carry `url` by requirement and nothing about the client | §1, §5, §6 |
| 5 | Threat model covers every new entry point | ✔ four endpoints, other methods, the header, the setting, the tables; twenty-one rows. The two remaining non-"none" rows are H2's own trace file (verified below its level, production setting to `03-operate`) and Tomcat-level rejections (outside every AC, not verified, `03-operate`) | §6 |
| 6 | Test strategy maps each AC to a suite and names the mechanisms | ✔ nine functional classes, nine unit classes; clock, spy, the database-failure canary, the cold-start wait, deltas, four contexts, document export spelled out | §7 |
| 7 | No structure beyond the SPEC | ✔ no new dependency, no domain exception hierarchy, no second table, no retry loops, no state enum, no `common/` package; every "no" is a row in §11 | §1, §11 |
| 8 | Territory respected | ✔ everything inside `slice.yaml`; the Javadoc-only grant honoured by putting beans in `link.LinkConfig` | §9 |
| 9 | ADRs for cross-cutting choices, before dependent code | ✔ six new ADRs plus the ADR-0002 and ADR-0004 amendments, indexed in `docs/DESIGN.md` §7 | §10 |
| 10 | Lead's packet note answered | ✔ Documentation line and the ordered pre-feature commits | §13 |
| 11 | Reviewer's explicit asks (browser problem detail, `no-store`, atomic audit/idempotency, full log correlation) | ✔ first two by effect (P1, P2, P4); the second two by mechanism (`@Transactional` + propagation; filter event + catch-all keeping errors inside MVC) and, for correlation, by effect on the cold path (R1) | §2.5, §5, §12 |
| 12 | Size | ✔ position taken: one slice; the split's cost and trigger recorded | §11 |
| 13 | Design review answered | ✔ DR-01 to DR-04 each fixed, with location and probe evidence; no settled check reopened; the change adds two properties, one override, one test class and no dependency | *Review response* |

### plan-review (three lenses, run on the finished draft)

**Strategy — 9/10.** The slice is the mission's foundation and every later
slice depends on what it fixes (schema, error contract, audit, API document);
scope equals the mission allocation (FR-1–6, FR-9 and their NFRs), nothing
added. Personas: Creator and Visitor are served by the four endpoints,
Operator by the audit rows, the request event and the committed document.
Opportunity cost: none inside the mission; the one pre-authorised alternative
(FR-3/FR-4 split) is weighed in §11 and declined with its trigger stated.

**Design (API surface) — 8/10; UX lenses n/a.** Information architecture:
one resource under `/api/links`, one opaque redirect path, consistent with
`/api/ping`. Interaction states: success, every error class, replay, mismatch,
expiry and the induced `500` each have a status, a body shape and a producer.
Journey: curl → `Location` → read → retire → `410` for the Creator; browser →
`302` → target for the Visitor; `request_id` from row to log line for the
Operator. Consistency: the same problem-detail shape as `01-ping`, extended
once. Accessibility and AI lenses do not apply to a JSON API.

**Engineering feasibility — 8/10.** Every AC has a named component and a
named test; the five platform mechanisms the design rests on were run on a
real Tomcat (§12). Data model: two tables, constraints carry the rules, the
rollback is written. Dependencies: none added. Performance: one indexed
lookup per request; no N+1. Scope: about fifteen production classes, two
property lines, one migration, eight journey classes — one implement step,
large but linear.

**Issues found and fixed in this draft**

- Blocking: none.
- Important: (1) the first draft described a *frozen* suite clock; AC-1 and
  AC-22 compare timestamps with real `Instant.now()` intervals, so a frozen
  clock would fail them. Replaced by an offset clock that follows real time
  and is shifted only by AC-19 (§1, §7.2). (2) The AC-26 hint repeated the
  ping pattern (lines containing `R`), which cannot detect an event of the
  request that lacks the id — exactly the gap RQ-03 closed in the SPEC. The
  hint now asserts every line captured during the request (§7.1, §7.2).
  (3) springdoc derives generic error responses from `@ExceptionHandler`
  methods on a controller advice; with `ProblemDetailsAdvice` present every
  operation would gain untyped entries and AC-28's problem-media-type clause
  would fail. `springdoc.override-with-generic-response=false` is now shipped,
  together with `springdoc.writer-with-order-by-keys=true` (§1, §2.7, §7.4,
  ADR-0010); both property names verified in the 3.1.1 metadata.
- Suggestions: keep `Problems` message texts free of any interpolated value
  (the unit test asserts it); give the `OpenApiDocumentTest` a clear failure
  message that names the regeneration command, since the first run on a
  fresh branch has no committed file to compare against.

**Revision (after design review).** I re-applied the engineering lens by hand
to the changed sections. I did not re-run the skill: scope, personas and the
API surface did not change, so the strategy and design lenses were not
re-scored. Engineering feasibility stays 8/10. The additions are one override
and one changed event in classes the design already had, two property lines,
and one real-server test class. The new risk is that the cold-start test
depends on log-write timing, which §7.2 handles with a bounded wait. Considered
and rejected: forcing the same-key race by spying on `LinkRepository` so that
it misses the bound row. That would need a fifth context and a spy on a JDK
proxy. The AC-24 spy's answer produces the same driver failure inside the
existing spy context.

**Recommended action:** approve `SPEC.md` + `design.md` at plan-lock once the
independent design review passes. Under D11 the plan-lock is the orchestration
lead's. No human question is open.
