import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.sql.DataSource;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import dev.urlshort.UrlshortApplication;
import dev.urlshort.web.Problems;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;

/**
 * Design probe for 01-audit-read (design.md section 12). The read endpoint is implemented here exactly
 * as design.md section 1 specifies it and run inside the shipped application on a real Tomcat bound to
 * 127.0.0.1. No file under src/ is touched.
 */
public class AuditProbe {

	static final String[] ARGS = { "--server.port=0", "--server.address=127.0.0.1", "--logging.level.root=warn",
			"--urlshort.rate-limit.create-per-minute=1000000", "--urlshort.rate-limit.redirect-per-minute=1000000" };

	public static void main(String[] args) throws Exception {
		p1Classification();
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
		// the shipped pin, as design.md section 1 adds it to application.properties
		try (ConfigurableApplicationContext ctx = start("jdbc:h2:mem:probe-audit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
				"--server.forward-headers-strategy=none")) {
			String base = "http://127.0.0.1:" + ctx.getEnvironment().getRequiredProperty("local.server.port");
			p2Responses(client, base);
			p3HeldWrite(client, base, ctx.getBean(DataSource.class));
		}
		p4ForwardHeaders(client);
		p5PageCost();
		p6StrategyAndAccept(client);
		System.out.println("PROBE done");
		System.exit(0);
	}

	// ------------------------------------------------------------------ P1: which peer addresses are loopback

	static void p1Classification() throws Exception {
		for (String address : List.of("127.0.0.1", "127.0.0.2", "127.255.255.254", "::1", "0:0:0:0:0:0:0:1",
				"::ffff:127.0.0.1", "192.0.2.10", "10.0.0.7", "::ffff:192.0.2.10", "fe80::1", "0.0.0.0", "::")) {
			InetAddress parsed = InetAddress.getByName(address);
			System.out.println("P1 " + address + " -> " + parsed.getClass().getSimpleName() + " " + parsed.getHostAddress()
					+ " loopback=" + parsed.isLoopbackAddress());
		}
	}

	// ------------------------------------------------------------------ P2: the page, order, paging, errors

	static void p2Responses(HttpClient client, String base) throws Exception {
		show(client, "P2a empty trail", get(base + "/api/audit"));
		String a = create(client, base, "https://example.org/a?canary=Q1");
		String b = create(client, base, "https://example.org/b");
		String c = create(client, base, "https://example.org/c");
		client.send(HttpRequest.newBuilder(URI.create(base + "/api/links/" + a)).DELETE().build(), HttpResponse.BodyHandlers.ofString());
		System.out.println("P2 created " + a + ", " + b + ", " + c + " then retired " + a);
		HttpResponse<String> all = show(client, "P2b full page", get(base + "/api/audit"));
		HttpResponse<String> first = show(client, "P2c limit=2", get(base + "/api/audit?limit=2"));
		String next = JsonMapper.builder().build().readTree(first.body()).get("next").asString();
		show(client, "P2d cursor=next", get(base + "/api/audit?limit=2&cursor=" + next));
		for (String query : List.of("limit=0", "limit=101", "limit=ten", "cursor=***", "cursor=" + b64("0"), "cursor=" + b64("x1"))) {
			show(client, "P2e " + query, get(base + "/api/audit?" + query));
		}
		show(client, "P2f X-Forwarded-For from 127.0.0.1", HttpRequest.newBuilder(URI.create(base + "/api/audit"))
				.header("X-Forwarded-For", "198.51.100.9").build());
		show(client, "P2g Forwarded from 127.0.0.1", HttpRequest.newBuilder(URI.create(base + "/api/audit"))
				.header("Forwarded", "for=198.51.100.9").build());
		show(client, "P2h HEAD with X-Forwarded-For", HttpRequest.newBuilder(URI.create(base + "/api/audit"))
				.header("X-Forwarded-For", "198.51.100.9").method("HEAD", HttpRequest.BodyPublishers.noBody()).build());
		show(client, "P2i POST", HttpRequest.newBuilder(URI.create(base + "/api/audit")).POST(HttpRequest.BodyPublishers.noBody()).build());
		show(client, "P2j HEAD", HttpRequest.newBuilder(URI.create(base + "/api/audit")).method("HEAD", HttpRequest.BodyPublishers.noBody()).build());
		show(client, "P2k OPTIONS", HttpRequest.newBuilder(URI.create(base + "/api/audit")).method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build());
	}

	// ------------------------------------------------------------------ P3: a write held open across a traversal (AC-20)

	static void p3HeldWrite(HttpClient client, String base, DataSource dataSource) throws Exception {
		try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
			s.executeUpdate("DELETE FROM audit_log");
			for (int i = 1; i <= 30; i++) {
				s.executeUpdate("INSERT INTO audit_log (occurred_at, actor, action, entity, entity_id, request_id, after_state)"
						+ " VALUES (CURRENT_TIMESTAMP, 'anonymous', 'link.create', 'link', 'seed" + i + "', 'req-" + i + "', '{}')");
			}
		}
		try (Connection held = dataSource.getConnection()) {
			held.setAutoCommit(false);
			try (Statement s = held.createStatement()) {
				s.executeUpdate("INSERT INTO audit_log (occurred_at, actor, action, entity, entity_id, request_id, after_state)"
						+ " VALUES (CURRENT_TIMESTAMP, 'anonymous', 'link.create', 'link', 'held', 'req-held', '{}')");
			}
			List<String> traversal = new ArrayList<>();
			String cursor = traverseOnePage(client, base, null, traversal);
			held.commit();
			while (cursor != null) {
				cursor = traverseOnePage(client, base, cursor, traversal);
			}
			List<String> fresh = new ArrayList<>();
			String again = null;
			do {
				again = traverseOnePage(client, base, again, fresh);
			}
			while (again != null);
			System.out.println("P3 held insert (uncommitted) during the first page, committed before the second: first traversal "
					+ traversal.size() + " rows, distinct " + traversal.stream().distinct().count() + ", held row included "
					+ traversal.contains("held") + "; fresh traversal " + fresh.size() + " rows, first " + fresh.getFirst());
		}
	}

	static @Nullable String traverseOnePage(HttpClient client, String base, @Nullable String cursor, List<String> into) throws Exception {
		String body = client.send(get(base + "/api/audit?limit=10" + (cursor == null ? "" : "&cursor=" + cursor)),
				HttpResponse.BodyHandlers.ofString()).body();
		JsonNode page = JsonMapper.builder().build().readTree(body);
		page.get("items").forEach(item -> into.add(item.get("entityId").asString()));
		return page.get("next").isNull() ? null : page.get("next").asString();
	}

	// ------------------------------------------------------------------ P4: forwarded headers on a cloud platform

	static void p4ForwardHeaders(HttpClient client) throws Exception {
		for (String[] variant : List.of(new String[] { "default" }, new String[] { "--spring.main.cloud-platform=kubernetes" },
				new String[] { "--spring.main.cloud-platform=kubernetes", "--server.forward-headers-strategy=none" })) {
			try (ConfigurableApplicationContext ctx = start("jdbc:h2:mem:probe-p4;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
					variant[0].equals("default") ? new String[0] : variant)) {
				String base = "http://127.0.0.1:" + ctx.getEnvironment().getRequiredProperty("local.server.port");
				String seen = client.send(HttpRequest.newBuilder(URI.create(base + "/probe/peer")).header("X-Forwarded-For", "198.51.100.9")
						.build(), HttpResponse.BodyHandlers.ofString()).body();
				System.out.println("P4 " + String.join(" ", variant) + ": from 127.0.0.1 with X-Forwarded-For 198.51.100.9 the application sees " + seen);
				int status = client.send(HttpRequest.newBuilder(URI.create(base + "/api/audit")).header("X-Forwarded-For", "127.0.0.2").build(),
						HttpResponse.BodyHandlers.discarding()).statusCode();
				System.out.println("P4b " + String.join(" ", variant) + ": GET /api/audit from 127.0.0.1 with X-Forwarded-For 127.0.0.2 answers " + status);
			}
		}
	}

	// ------------------------------------------------------------------ P6: design review DR-01 and DR-02 controls

	/**
	 * DR-01: the read admits only while Boot's effective forwarded-header strategy is NONE, so neither
	 * override (native, framework) nor an unset strategy on a detected platform can open it. DR-02: with no
	 * produces condition, a strict Accept still meets the guard and the validation first.
	 */
	static void p6StrategyAndAccept(HttpClient client) throws Exception {
		List<String[]> variants = List.of(new String[] { "none", "--server.forward-headers-strategy=none" },
				// an operator override of the shipped pin leaves this value effective (a repeated command-line
				// argument would be joined into a list, so the override is passed alone)
				new String[] { "native (operator override)", "--server.forward-headers-strategy=native" },
				new String[] { "framework (operator override)", "--server.forward-headers-strategy=framework" },
				new String[] { "unset" }, new String[] { "unset + kubernetes", "--spring.main.cloud-platform=kubernetes" });
		for (String[] variant : variants) {
			String[] more = java.util.Arrays.copyOfRange(variant, 1, variant.length);
			try (ConfigurableApplicationContext ctx = start("jdbc:h2:mem:probe-p6;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", more)) {
				String base = "http://127.0.0.1:" + ctx.getEnvironment().getRequiredProperty("local.server.port");
				List<String> results = new ArrayList<>();
				results.add("plain " + status(client, base + "/api/audit", null, null));
				results.add("XFF 127.0.0.2 " + status(client, base + "/api/audit", "X-Forwarded-For", "127.0.0.2"));
				results.add("Forwarded for=127.0.0.2 " + status(client, base + "/api/audit", "Forwarded", "for=127.0.0.2"));
				results.add("XFF 192.0.2.10 " + status(client, base + "/api/audit", "X-Forwarded-For", "192.0.2.10"));
				System.out.println("P6 strategy " + variant[0] + " (effective " + ctx.getBean(ServerProperties.class).getForwardHeadersStrategy()
						+ "): " + String.join(", ", results));
				if (variant[0].equals("none")) {
					for (String accept : List.of("text/html", "application/problem+json", "*/*")) {
						List<String> strict = new ArrayList<>();
						strict.add("refused " + statusAccept(client, base + "/api/audit", accept, "X-Forwarded-For", "198.51.100.9"));
						strict.add("limit=0 " + statusAccept(client, base + "/api/audit?limit=0", accept, null, null));
						strict.add("valid " + statusAccept(client, base + "/api/audit", accept, null, null));
						System.out.println("P6 Accept " + accept + ": " + String.join(", ", strict));
					}
				}
			}
		}
	}

	static String status(HttpClient client, String uri, @Nullable String header, @Nullable String value) throws Exception {
		return statusAccept(client, uri, null, header, value);
	}

	static String statusAccept(HttpClient client, String uri, @Nullable String accept, @Nullable String header, @Nullable String value)
			throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(uri));
		if (accept != null) {
			request.header("Accept", accept);
		}
		if (header != null) {
			request.header(header, value);
		}
		HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
		return response.statusCode() + " " + response.headers().firstValue("Content-Type").orElse("-");
	}

	// ------------------------------------------------------------------ P5: page cost on a million rows

	static void p5PageCost() throws Exception {
		try (Connection c = DriverManager.getConnection("jdbc:h2:mem:probe-p5;MODE=PostgreSQL", "sa", "");
				Statement s = c.createStatement()) {
			s.execute("CREATE TABLE audit_log (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,"
					+ " actor VARCHAR(64) NOT NULL, action VARCHAR(64) NOT NULL, entity VARCHAR(32) NOT NULL, entity_id VARCHAR(64) NOT NULL,"
					+ " request_id VARCHAR(64) NOT NULL, before_state VARCHAR(4096), after_state VARCHAR(4096) NOT NULL)");
			s.executeUpdate("INSERT INTO audit_log (occurred_at, actor, action, entity, entity_id, request_id, after_state)"
					+ " SELECT CURRENT_TIMESTAMP, 'anonymous', 'link.create', 'link', 'c' || X, 'r' || X, '{}' FROM SYSTEM_RANGE(1, 1000000)");
			for (String where : List.of("", "WHERE id < 500000 ", "WHERE id < 52 ")) {
				try (ResultSet rs = s.executeQuery("EXPLAIN ANALYZE " + PAGE_SQL_HEAD + where + PAGE_SQL_TAIL.replace(":n", "51"))) {
					rs.next();
					System.out.println("P5 " + (where.isEmpty() ? "first page" : where.trim()) + ": " + rs.getString(1).replaceAll("\\s+", " "));
				}
			}
		}
	}

	// ------------------------------------------------------------------ the read side as design.md section 1 specifies it

	static final String PAGE_SQL_HEAD = "SELECT id, occurred_at, actor, action, entity, entity_id, request_id, before_state, after_state"
			+ " FROM audit_log ";
	static final String PAGE_SQL_TAIL = "ORDER BY id DESC FETCH FIRST :n ROWS ONLY";

	public record AuditEntry(Instant occurredAt, String actor, String action, String entity, String entityId, String requestId,
			@Nullable JsonNode before, JsonNode after) {
	}

	public record AuditPage(List<AuditEntry> items, @Nullable String next) {
	}

	@Configuration(proxyBeanMethods = false)
	public static class ProbeConfig {

		@Bean
		public ProbeAuditController probeAuditController(JdbcClient jdbc, JsonMapper json, ServerProperties server) {
			return new ProbeAuditController(jdbc, json, server);
		}
	}

	@RestController
	public static class ProbeAuditController {

		final JdbcClient jdbc;
		final JsonMapper json;
		/** Design review DR-01: the peer address is the connection's only when nothing rewrites it. */
		final boolean peerIsConnection;

		ProbeAuditController(JdbcClient jdbc, JsonMapper json, ServerProperties server) {
			this.jdbc = jdbc;
			this.json = json;
			this.peerIsConnection = server.getForwardHeadersStrategy() == ServerProperties.ForwardHeadersStrategy.NONE;
		}

		// design review DR-02: no produces condition, so nothing is decided before the guard; the 200 names
		// application/json itself, so a client that accepts only something else gets 406 after validation
		@GetMapping(path = "/api/audit")
		public org.springframework.http.ResponseEntity<AuditPage> page(@RequestParam(name = "limit", required = false) @Nullable String limit,
				@RequestParam(name = "cursor", required = false) @Nullable String cursor,
				HttpServletRequest request) throws Exception {
			if (!peerIsConnection || !admitted(request)) {
				throw new ErrorResponseException(HttpStatus.FORBIDDEN);
			}
			int size = limit(limit);
			long before = cursor == null ? Long.MAX_VALUE : cursor(cursor);
			List<Long> ids = new ArrayList<>();
			List<AuditEntry> rows = jdbc.sql(PAGE_SQL_HEAD + "WHERE id < :before " + PAGE_SQL_TAIL.replace(":n", ":fetch"))
					.param("before", before).param("fetch", size + 1).query((rs, n) -> {
						ids.add(rs.getLong("id"));
						String beforeState = rs.getString("before_state");
						return new AuditEntry(rs.getObject("occurred_at", java.time.OffsetDateTime.class).toInstant(), rs.getString("actor"),
								rs.getString("action"), rs.getString("entity"), rs.getString("entity_id"), rs.getString("request_id"),
								beforeState == null ? null : json.readTree(beforeState), json.readTree(rs.getString("after_state")));
					}).list();
			AuditPage page = rows.size() <= size ? new AuditPage(rows, null)
					: new AuditPage(rows.subList(0, size), b64(Long.toString(ids.get(size - 1))));
			return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(page);
		}

		@GetMapping("/probe/peer")
		public String peer(HttpServletRequest request) {
			return "remoteAddr=" + request.getRemoteAddr() + ", X-Forwarded-For header=" + request.getHeader("X-Forwarded-For");
		}

		static boolean admitted(HttpServletRequest request) throws Exception {
			return request.getHeader("X-Forwarded-For") == null && request.getHeader("Forwarded") == null
					&& InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress();
		}

		static int limit(@Nullable String value) {
			if (value == null) {
				return 50;
			}
			int parsed;
			try {
				parsed = Integer.parseInt(value);
			}
			catch (NumberFormatException ex) {
				throw Problems.validation("limit", "format", "must be a whole number");
			}
			if (parsed < 1 || parsed > 100) {
				throw Problems.validation("limit", "range", "must be from 1 to 100");
			}
			return parsed;
		}

		static long cursor(String value) {
			try {
				long id = Long.parseLong(new String(Base64.getUrlDecoder().decode(value), StandardCharsets.US_ASCII));
				if (id > 0) {
					return id;
				}
			}
			catch (IllegalArgumentException ex) {
				// not base64url, or not a number: the same answer below
			}
			throw Problems.validation("cursor", "format", "must be a value returned as next");
		}
	}

	// ------------------------------------------------------------------ helpers

	static String b64(String text) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.US_ASCII));
	}

	static ConfigurableApplicationContext start(String url, String... more) {
		List<String> args = new ArrayList<>(List.of(ARGS));
		args.add("--spring.datasource.url=" + url);
		args.addAll(List.of(more));
		return new SpringApplicationBuilder(UrlshortApplication.class, ProbeConfig.class).run(args.toArray(String[]::new));
	}

	static HttpRequest get(String uri) {
		return HttpRequest.newBuilder(URI.create(uri)).build();
	}

	static String create(HttpClient client, String base, String url) throws Exception {
		String body = client.send(HttpRequest.newBuilder(URI.create(base + "/api/links")).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"url\":\"" + url + "\"}")).build(), HttpResponse.BodyHandlers.ofString()).body();
		int i = body.indexOf("\"code\":\"") + 8;
		return body.substring(i, body.indexOf('"', i));
	}

	static HttpResponse<String> show(HttpClient client, String label, HttpRequest request) throws Exception {
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		System.out.println(label + ": " + response.statusCode() + " " + response.headers().firstValue("Content-Type").orElse("-")
				+ " X-Request-Id=" + response.headers().firstValue("X-Request-Id").isPresent()
				+ response.headers().firstValue("Allow").map(allow -> " Allow=" + allow).orElse("") + " body=" + response.body());
		return response;
	}
}
