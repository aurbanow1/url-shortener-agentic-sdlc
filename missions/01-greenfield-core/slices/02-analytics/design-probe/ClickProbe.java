import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import dev.urlshort.UrlshortApplication;

/**
 * Design probe for slice 02-analytics. Boots the shipped application (main at the design commit,
 * functional profile, in-memory H2, Flyway V1) on a random loopback port, creates the V2 click
 * table with the exact DDL of design.md section 3, and runs probe beans that implement the design's
 * mechanisms in compact form: the request-thread reductions, the HMAC daily salt with its day-end
 * expiry, the bounded single-writer queue with MDC restore and fail-open WARN, the one grouped
 * statistics query and the Java-side aggregation. Every case prints what it observed; the windows
 * that matter are checked by effect. No file under src/ is touched.
 */
public class ClickProbe {

	static final PrintStream STDOUT = System.out;
	static final ByteArrayOutputStream WINDOW = new ByteArrayOutputStream();
	static volatile boolean recording;

	static final String V2 = """
			CREATE TABLE click (
			    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
			    link_id          BIGINT        NOT NULL,
			    clicked_at       TIMESTAMP WITH TIME ZONE NOT NULL,
			    clicked_on       DATE          NOT NULL,
			    referrer         VARCHAR(2048),
			    user_agent_class VARCHAR(16)   NOT NULL,
			    client_hash      VARCHAR(64)   NOT NULL,
			    CONSTRAINT fk_click_link FOREIGN KEY (link_id) REFERENCES link (id) ON DELETE CASCADE,
			    CONSTRAINT ck_click_user_agent_class CHECK (user_agent_class IN ('browser', 'bot', 'other', 'unknown')),
			    CONSTRAINT ck_click_client_hash_length CHECK (LENGTH(client_hash) = 64)
			);
			CREATE INDEX ix_click_link_day ON click (link_id, clicked_on);
			""";

	public static void main(String[] args) throws Exception {
		System.setOut(new PrintStream(new OutputStream() {
			@Override
			public void write(int b) {
				STDOUT.write(b);
				if (recording) {
					synchronized (WINDOW) {
						WINDOW.write(b);
					}
				}
			}

			@Override
			public void write(byte[] b, int off, int len) {
				STDOUT.write(b, off, len);
				if (recording) {
					synchronized (WINDOW) {
						WINDOW.write(b, off, len);
					}
				}
			}
		}, true));

		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(UrlshortApplication.class, ProbeConfig.class)
				.properties("server.port=0", "server.address=127.0.0.1").run(args);
		try {
			JdbcClient jdbc = ctx.getBean(JdbcClient.class);
			for (String statement : V2.split(";")) {
				if (!statement.isBlank()) {
					jdbc.sql(statement).update();
				}
			}
			out("C0 V2 DDL", "click table and index created on H2 (PostgreSQL mode) without error");
			ProbeRecorder recorder = ctx.getBean(ProbeRecorder.class);
			String base = "http://127.0.0.1:" + ctx.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
			HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

			String code = createLink(client, base);
			long linkId = jdbc.sql("SELECT id FROM link WHERE code = :c").param("c", code).query(Long.class).single();
			out("C1 link created through the shipped POST /api/links", "code=" + code + " id=" + linkId);

			int before = ProbeController.INVOCATIONS.get();
			HttpResponse<String> head = client.send(HttpRequest.newBuilder(URI.create(base + "/probe/r/" + code))
					.method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
			recorder.settle();
			out("C2 HEAD on a @GetMapping route", "status=" + head.statusCode() + " handlerInvocations="
					+ (ProbeController.INVOCATIONS.get() - before) + " clicksStored=" + count(jdbc, linkId)
					+ "  (the handler runs for HEAD, so the recorder must skip it)");

			String refCanary = "refpathcanary";
			String uaCanary = "uacanary";
			startWindow();
			HttpResponse<String> click = client.send(HttpRequest.newBuilder(URI.create(base + "/probe/r/" + code + "?utm=x"))
					.header("Referer", "https://News.Example/a/" + refCanary + "?t=refquerycanary#frag")
					.header("User-Agent", "Mozilla/5.0 (X11) " + uaCanary + " Firefox/131.0")
					.header("X-Forwarded-For", "192.0.2.99").GET().build(), HttpResponse.BodyHandlers.ofString());
			recorder.settle();
			String window = stopWindow();
			Map<String, Object> row = jdbc.sql("SELECT * FROM click WHERE link_id = :id ORDER BY id DESC FETCH FIRST 1 ROWS ONLY")
					.param("id", linkId).query().singleRow();
			out("C3 one redirect with canaries, settled", "status=" + click.statusCode() + " location="
					+ click.headers().firstValue("location").orElse("") + " cache-control="
					+ click.headers().firstValue("cache-control").orElse("") + "\n    row=" + row
					+ "\n    VERDICT rowHasCanary=" + (row.toString().contains(refCanary) || row.toString().contains(uaCanary)
							|| row.toString().contains("127.0.0.1") || row.toString().contains("192.0.2.99"))
					+ " windowLines=" + window.lines().filter(l -> !l.isBlank()).count()
					+ " canaryInLogs=" + (window.contains(refCanary) || window.contains(uaCanary) || window.contains("192.0.2.99")
							|| window.contains("news.example") || window.contains((String) row.get("CLIENT_HASH"))));

			for (int i = 0; i < 200; i++) { // warm up the JIT before measuring
				client.send(HttpRequest.newBuilder(URI.create(base + "/probe/r/" + code)).GET().build(), HttpResponse.BodyHandlers.discarding());
			}
			ProbeController.HOOK_NANOS.clear();
			for (int i = 0; i < 1000; i++) {
				client.send(HttpRequest.newBuilder(URI.create(base + "/probe/r/" + code))
						.header("Referer", "https://site" + (i % 7) + ".example/p?q=" + i)
						.header("User-Agent", "Mozilla/5.0 probe").GET().build(), HttpResponse.BodyHandlers.discarding());
			}
			recorder.settle();
			long[] nanos = ProbeController.HOOK_NANOS.stream().mapToLong(Long::longValue).sorted().toArray();
			out("C4 time spent in the hook on the request thread, 1000 redirects after 200 warm-up", String.format(
					Locale.ROOT, "p50=%.1f us  p95=%.1f us  p99=%.1f us  max=%.1f us  (indicative, laptop, not the release bench)",
					nanos[499] / 1e3, nanos[949] / 1e3, nanos[989] / 1e3, nanos[nanos.length - 1] / 1e3));

			// AC-9 and AC-10 shapes: rows inserted directly with chosen days and origins, NULL referrers included.
			String code2 = createLink(client, base);
			long link2 = jdbc.sql("SELECT id FROM link WHERE code = :c").param("c", code2).query(Long.class).single();
			insert(jdbc, link2, "2026-10-01T23:59:59Z", "https://a.example", 2);
			insert(jdbc, link2, "2026-10-02T00:00:00Z", "https://a.example", 3);
			insert(jdbc, link2, "2026-10-04T12:00:00Z", "https://b.example", 3);
			insert(jdbc, link2, "2026-10-04T12:00:00Z", "https://c.example", 3);
			for (int d = 1; d <= 11; d++) {
				insert(jdbc, link2, "2026-10-04T12:00:00Z", "https://d" + d + ".example", 1);
			}
			insert(jdbc, link2, "2026-10-04T12:00:00Z", null, 4);
			HttpResponse<String> stats = client.send(HttpRequest.newBuilder(URI.create(base + "/probe/stats/" + code2)).GET().build(),
					HttpResponse.BodyHandlers.ofString());
			out("C5 statistics from one grouped query (26 clicks over three days, 14 origins, 4 without referrer)",
					"status=" + stats.statusCode() + " content-type=" + stats.headers().firstValue("content-type").orElse("")
							+ "\n    body=" + stats.body());
			List<String> sqlOrder = jdbc.sql("SELECT DISTINCT referrer FROM click WHERE link_id = :id AND referrer LIKE 'https://d%' ORDER BY referrer")
					.param("id", link2).query(String.class).list();
			out("C6 H2's own ORDER BY on the d-origins (for comparison; the design sorts in Java)", sqlOrder.toString());

			ProbeStore.MODE = 1;
			long worst = 0;
			for (int i = 0; i < 20; i++) {
				long t0 = System.nanoTime();
				HttpResponse<String> r = client.send(HttpRequest.newBuilder(URI.create(base + "/probe/r/" + code)).GET().build(),
						HttpResponse.BodyHandlers.ofString());
				long ms = (System.nanoTime() - t0) / 1_000_000;
				worst = Math.max(worst, ms);
				if (r.statusCode() != 302) {
					out("C7 slow store", "unexpected status " + r.statusCode());
				}
			}
			ProbeStore.MODE = 0; // queued writes now run at normal speed; only the one in flight still sleeps
			out("C7 every click write sleeps 2 s: 20 sequential redirects", "all 302; slowest client-observed reply=" + worst
					+ " ms; queue depth after the burst=" + recorder.queueDepth());
			recorder.settle();

			ProbeStore.MODE = 2;
			recorder.settle();
			startWindow();
			HttpResponse<String> failed = client.send(HttpRequest.newBuilder(URI.create(base + "/probe/r/" + code))
					.header("User-Agent", "Mozilla/5.0 " + uaCanary).header("Referer", "https://fail.example/" + refCanary).GET().build(),
					HttpResponse.BodyHandlers.ofString());
			recorder.settle();
			window = stopWindow();
			ProbeStore.MODE = 0;
			String id = failed.headers().firstValue("x-request-id").orElse("");
			List<String> lines = window.lines().filter(l -> !l.isBlank()).toList();
			out("C8 failing click store", "status=" + failed.statusCode() + " location=" + failed.headers().firstValue("location").orElse("")
					+ "\n    VERDICT warnLines=" + lines.stream().filter(l -> l.contains("\"WARN\"") && l.contains("click lost")).count()
					+ " linesWithoutThisRequestId=" + lines.stream().filter(l -> !l.contains("\"requestId\":\"" + id + "\"")).count()
					+ " canaryInLogs=" + (window.contains(refCanary) || window.contains(uaCanary) || window.contains("fail.example"))
					+ lines.stream().map(l -> "\n    log> " + l).reduce("", String::concat));

			HttpResponse<String> docs = client.send(HttpRequest.newBuilder(URI.create(base + "/v3/api-docs")).GET().build(),
					HttpResponse.BodyHandlers.ofString());
			int i = docs.body().indexOf("\"/probe/r/{code}\"");
			out("C9 springdoc on a handler that takes HttpServletRequest", docs.body().substring(i, Math.min(docs.body().length(), i + 260)));

			saltChecks();
		}
		finally {
			ctx.close();
		}
	}

	static void saltChecks() throws Exception {
		Instant dayD = Instant.parse("2026-10-01T00:00:01Z");
		ProbeSalt salt = new ProbeSalt(Clock.fixed(dayD, ZoneOffset.UTC), new SecureRandom());
		String a1 = salt.hash("203.0.113.77", dayD);
		String a2 = salt.hash("203.0.113.77", Instant.parse("2026-10-01T23:59:59Z"));
		String b = salt.hash("198.51.100.23", dayD);
		String a3 = salt.hash("203.0.113.77", Instant.parse("2026-10-02T00:00:00Z"));
		byte[] sha = MessageDigest.getInstance("SHA-256").digest("203.0.113.77".getBytes(StandardCharsets.US_ASCII));
		out("S1 HMAC daily salt", "sameDayEqual=" + a1.equals(a2) + " otherAddressDiffers=" + !a1.equals(b) + " nextDayDiffers="
				+ !a1.equals(a3) + " equalsUnsaltedSha256=" + (a1.equals(HexFormat.of().formatHex(sha))
						|| a1.equals(Base64.getEncoder().encodeToString(sha))) + " length=" + a1.length());

		Clock nearMidnight = Clock.fixed(Instant.parse("2026-10-01T23:59:59.500Z"), ZoneOffset.UTC);
		ProbeSalt expiring = new ProbeSalt(nearMidnight, new SecureRandom());
		expiring.hash("203.0.113.77", nearMidnight.instant());
		boolean heldBefore = expiring.holdsSalt();
		Thread.sleep(1500);
		out("S2 salt created 0.5 s before UTC midnight", "heldRightAfterUse=" + heldBefore + " heldAfterMidnight="
				+ expiring.holdsSalt() + "  (expiry scheduled at the day's end, no click needed)");
	}

	static String createLink(HttpClient client, String base) throws Exception {
		HttpResponse<String> created = client.send(HttpRequest.newBuilder(URI.create(base + "/api/links"))
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"url\":\"https://example.com/t\"}"))
				.build(), HttpResponse.BodyHandlers.ofString());
		String body = created.body();
		int i = body.indexOf("\"code\":\"") + 8;
		return body.substring(i, body.indexOf('"', i));
	}

	static long count(JdbcClient jdbc, long linkId) {
		return jdbc.sql("SELECT COUNT(*) FROM click WHERE link_id = :id").param("id", linkId).query(Long.class).single();
	}

	static void insert(JdbcClient jdbc, long linkId, String at, String referrer, int times) {
		Instant instant = Instant.parse(at);
		for (int i = 0; i < times; i++) {
			jdbc.sql("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash) "
					+ "VALUES (:l, :at, :on, :r, 'browser', :h)").param("l", linkId).param("at", java.sql.Timestamp.from(instant))
					.param("on", LocalDate.ofInstant(instant, ZoneOffset.UTC)).param("r", referrer).param("h", "0".repeat(64)).update();
		}
	}

	static void startWindow() {
		synchronized (WINDOW) {
			WINDOW.reset();
		}
		recording = true;
	}

	static String stopWindow() {
		recording = false;
		synchronized (WINDOW) {
			return WINDOW.toString(StandardCharsets.UTF_8);
		}
	}

	static void out(String title, String text) {
		STDOUT.println("PROBE " + title + "\n    " + text);
	}

	@Configuration(proxyBeanMethods = false)
	public static class ProbeConfig {
		@Bean
		ProbeSalt probeSalt(Clock clock, SecureRandom random) {
			return new ProbeSalt(clock, random);
		}

		@Bean
		ProbeStore probeStore(JdbcClient jdbc) {
			return new ProbeStore(jdbc);
		}

		@Bean(destroyMethod = "close")
		ProbeRecorder probeRecorder(ProbeStore store, ProbeSalt salt, Clock clock) {
			return new ProbeRecorder(store, salt, clock);
		}

		@Bean
		ProbeController probeController(ProbeRecorder recorder, ProbeStore store) {
			return new ProbeController(recorder, store);
		}
	}

	/** design.md section 1: the stored facts and the two reductions, pure functions. */
	record ProbeClick(long linkId, Instant clickedAt, LocalDate clickedOn, String referrer, String userAgentClass, String clientHash) {

		static String referrerOrigin(String header) {
			if (header == null || header.length() > 2048) {
				return null;
			}
			try {
				URI uri = new URI(header);
				String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
				if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null || uri.getHost().isEmpty()) {
					return null;
				}
				int port = uri.getPort();
				boolean defaultPort = port == -1 || (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443);
				return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (defaultPort ? "" : ":" + port);
			}
			catch (URISyntaxException e) {
				return null;
			}
		}

		static String userAgentClass(String header) {
			if (header == null || header.isEmpty()) {
				return "unknown";
			}
			String lower = header.toLowerCase(Locale.ROOT);
			if (lower.contains("bot") || lower.contains("crawler") || lower.contains("spider")) {
				return "bot";
			}
			return header.startsWith("Mozilla/") ? "browser" : "other";
		}
	}

	/** design.md section 1 / ADR-0012: one random salt per UTC day, in memory only, discarded at the day's end. */
	static final class ProbeSalt {
		private final Clock clock;
		private final SecureRandom random;
		private LocalDate day;
		private byte[] salt;

		ProbeSalt(Clock clock, SecureRandom random) {
			this.clock = clock;
			this.random = random;
		}

		String hash(String address, Instant at) {
			SecretKeySpec key;
			synchronized (this) {
				LocalDate today = LocalDate.ofInstant(at, ZoneOffset.UTC);
				if (!today.equals(day)) {
					discard();
					day = today;
					salt = new byte[32];
					random.nextBytes(salt);
					long untilDayEnd = Duration.between(clock.instant(), today.plusDays(1).atStartOfDay(ZoneOffset.UTC)).toMillis();
					CompletableFuture.runAsync(() -> expire(today),
							CompletableFuture.delayedExecutor(Math.max(0, untilDayEnd), TimeUnit.MILLISECONDS));
				}
				key = new SecretKeySpec(salt, "HmacSHA256");
			}
			try {
				Mac mac = Mac.getInstance("HmacSHA256");
				mac.init(key);
				return HexFormat.of().formatHex(mac.doFinal(address.getBytes(StandardCharsets.UTF_8)));
			}
			catch (java.security.GeneralSecurityException e) {
				throw new IllegalStateException(e);
			}
		}

		synchronized void expire(LocalDate ended) {
			if (ended.equals(day)) {
				discard();
			}
		}

		synchronized boolean holdsSalt() {
			return salt != null;
		}

		private void discard() {
			if (salt != null) {
				Arrays.fill(salt, (byte) 0);
			}
			salt = null;
			day = null;
		}
	}

	/** design.md section 3: one insert, one lookup, one grouped query (JdbcClient). MODE is probe-only. */
	static final class ProbeStore {
		static volatile int MODE; // 0 normal, 1 slow (2 s per write), 2 failing
		private final JdbcClient jdbc;

		ProbeStore(JdbcClient jdbc) {
			this.jdbc = jdbc;
		}

		void insert(ProbeClick click) {
			if (MODE == 1) {
				try {
					Thread.sleep(2000);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			if (MODE == 2) {
				throw new org.springframework.dao.DataAccessResourceFailureException("store down, would quote " + click.clientHash());
			}
			jdbc.sql("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash) "
					+ "VALUES (:linkId, :clickedAt, :clickedOn, :referrer, :userAgentClass, :clientHash)")
					.param("linkId", click.linkId()).param("clickedAt", java.sql.Timestamp.from(click.clickedAt()))
					.param("clickedOn", click.clickedOn()).param("referrer", click.referrer())
					.param("userAgentClass", click.userAgentClass()).param("clientHash", click.clientHash()).update();
		}

		Long linkId(String code) {
			return jdbc.sql("SELECT id FROM link WHERE code = :code").param("code", code).query(Long.class).optional().orElse(null);
		}

		List<Map<String, Object>> counts(long linkId) {
			return jdbc.sql("SELECT clicked_on, referrer, COUNT(*) AS clicks FROM click WHERE link_id = :linkId "
					+ "GROUP BY clicked_on, referrer").param("linkId", linkId).query().listOfRows();
		}
	}

	/** design.md section 1 / ADR-0011: request-thread extraction, bounded single writer, fail open. */
	static final class ProbeRecorder {
		private static final Logger log = LoggerFactory.getLogger(ProbeRecorder.class);
		private final ProbeStore store;
		private final ProbeSalt salt;
		private final Clock clock;
		private final ThreadPoolExecutor writer = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<>(10_000), r -> {
					Thread t = new Thread(r, "click-writer");
					t.setDaemon(true);
					return t;
				}, new ThreadPoolExecutor.AbortPolicy());

		ProbeRecorder(ProbeStore store, ProbeSalt salt, Clock clock) {
			this.store = store;
			this.salt = salt;
			this.clock = clock;
		}

		void record(long linkId, HttpServletRequest request) {
			if ("HEAD".equals(request.getMethod())) {
				return;
			}
			String requestId = MDC.get("requestId");
			try {
				Instant now = clock.instant();
				ProbeClick click = new ProbeClick(linkId, now, LocalDate.ofInstant(now, ZoneOffset.UTC),
						ProbeClick.referrerOrigin(request.getHeader("Referer")),
						ProbeClick.userAgentClass(request.getHeader("User-Agent")), salt.hash(request.getRemoteAddr(), now));
				writer.execute(() -> write(click, requestId));
			}
			catch (RuntimeException e) {
				lost(e);
			}
		}

		private void write(ProbeClick click, String requestId) {
			MDC.put("requestId", requestId);
			try {
				store.insert(click);
			}
			catch (RuntimeException e) {
				lost(e);
			}
			finally {
				MDC.remove("requestId");
			}
		}

		private static void lost(RuntimeException e) {
			log.atWarn().setMessage("click lost").addKeyValue("errorType", e.getClass().getName()).log();
		}

		void settle() throws Exception {
			writer.submit(() -> { }).get(30, TimeUnit.SECONDS);
		}

		int queueDepth() {
			return writer.getQueue().size();
		}

		void close() {
			writer.close();
		}
	}

	@RestController
	public static class ProbeController {
		static final AtomicInteger INVOCATIONS = new AtomicInteger();
		static final List<Long> HOOK_NANOS = java.util.Collections.synchronizedList(new ArrayList<>());
		private final ProbeRecorder recorder;
		private final ProbeStore store;

		ProbeController(ProbeRecorder recorder, ProbeStore store) {
			this.recorder = recorder;
			this.store = store;
		}

		/** Stands in for RedirectController.redirect with the one-line hook. */
		@GetMapping("/probe/r/{code:[A-Za-z0-9]{6,32}}")
		public ResponseEntity<Void> redirect(@PathVariable("code") String code, HttpServletRequest request) {
			INVOCATIONS.incrementAndGet();
			Long id = store.linkId(code);
			long t0 = System.nanoTime();
			recorder.record(id, request);
			HOOK_NANOS.add(System.nanoTime() - t0);
			return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "https://example.com/t")
					.cacheControl(CacheControl.noStore()).build();
		}

		/** Stands in for StatsController: one grouped query, aggregation and ordering in Java. */
		@GetMapping("/probe/stats/{code:[A-Za-z0-9]{6,32}}")
		public LinkStats stats(@PathVariable("code") String code) {
			return LinkStats.of(code, store.counts(store.linkId(code)));
		}
	}

	public record DayClicks(LocalDate date, long clicks) {
	}

	public record ReferrerClicks(String referrer, long clicks) {
	}

	public record LinkStats(String code, long totalClicks, List<DayClicks> clicksPerDay, List<ReferrerClicks> topReferrers) {
		static LinkStats of(String code, List<Map<String, Object>> rows) {
			TreeMap<LocalDate, Long> perDay = new TreeMap<>();
			Map<String, Long> perReferrer = new HashMap<>();
			long total = 0;
			for (Map<String, Object> row : rows) {
				long clicks = ((Number) row.get("CLICKS")).longValue();
				total += clicks;
				perDay.merge(((java.sql.Date) row.get("CLICKED_ON")).toLocalDate(), clicks, Long::sum);
				if (row.get("REFERRER") != null) {
					perReferrer.merge((String) row.get("REFERRER"), clicks, Long::sum);
				}
			}
			List<ReferrerClicks> top = perReferrer.entrySet().stream()
					.sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
					.limit(10).map(e -> new ReferrerClicks(e.getKey(), e.getValue())).toList();
			return new LinkStats(code, total, perDay.entrySet().stream().map(e -> new DayClicks(e.getKey(), e.getValue())).toList(), top);
		}
	}
}
