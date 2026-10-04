package dev.urlshort.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Slice 04-audit-columns: AC-2 to AC-6, AC-8 and AC-10, and business rules 1 to 4 and 7. The suite clock
 * is frozen and moved by hand, and each test sends from its own loopback peer, so a shifted clock never
 * leaves a shared rate-limit bucket ahead, and the audit read admits the peer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LinkAuditColumnsJourneyTest {

	// a fresh loopback peer per test: a test that moves the clock ahead leaves its peer's bucket ahead too
	private static final AtomicInteger PEERS = new AtomicInteger();

	private String peer;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private FunctionalClock clock;

	@BeforeEach
	void freezeTheClock() {
		peer = "127.44.0." + PEERS.incrementAndGet();
		clock.reset();
		clock.freeze();
	}

	@AfterEach
	void resetTheClock() {
		clock.reset();
	}

	@Test
	void AC02_aCreateStampsTheNewLinkAndItsAuditRow() throws Exception {
		Instant t = clock.instant();

		String code = code(create("https://example.com/ac2", null));

		Map<String, Object> link = link(code);
		assertThat(instant(link, "CREATED_AT")).isEqualTo(t);
		assertThat(instant(link, "UPDATED_AT")).isEqualTo(t);
		assertThat(link).containsEntry("CREATED_BY", "anonymous").containsEntry("UPDATED_BY", "anonymous");
		assertStampedAuditRow(code, "link.create");
	}

	@Test
	void AC03_aRetireMovesTheLinksUpdateStampOnly() throws Exception {
		Instant t0 = clock.instant();
		String code = code(create("https://example.com/ac3", null));
		clock.shift(Duration.ofHours(1));
		Instant t1 = clock.instant();

		assertThat(retire(code).getStatus()).isEqualTo(204);

		Map<String, Object> link = link(code);
		assertThat(instant(link, "CREATED_AT")).isEqualTo(t0);
		assertThat(instant(link, "UPDATED_AT")).isEqualTo(t1).isEqualTo(instant(link, "RETIRED_AT"));
		assertThat(link).containsEntry("CREATED_BY", "anonymous").containsEntry("UPDATED_BY", "anonymous");
		assertStampedAuditRow(code, "link.retire");
	}

	@Test
	void AC04_releasingAnExpiredKeyIsAWrite() throws Exception {
		String key = "key-" + UUID.randomUUID();
		Instant t0 = clock.instant();
		String a = code(create("https://example.com/ac4-a", key));
		clock.shift(Duration.ofHours(25));
		Instant t1 = clock.instant();

		String b = code(create("https://example.com/ac4-b", key));

		Map<String, Object> released = link(a);
		Map<String, Object> bound = link(b);
		assertThat(instant(released, "UPDATED_AT")).isEqualTo(t1).isEqualTo(instant(bound, "CREATED_AT"));
		assertThat(instant(released, "CREATED_AT")).isEqualTo(t0);
		assertThat(released).containsEntry("URL", "https://example.com/ac4-a").containsEntry("RETIRED_AT", null)
				.containsEntry("IDEMPOTENCY_KEY", null).containsEntry("CREATED_BY", "anonymous")
				.containsEntry("UPDATED_BY", "anonymous");
		assertThat(instant(bound, "UPDATED_AT")).isEqualTo(t1);
		assertThat(bound).containsEntry("IDEMPOTENCY_KEY", key);
	}

	@Test
	void AC05_requestsThatChangeNothingStampNothing() throws Exception {
		String key = "key-" + UUID.randomUUID();
		String url = "https://example.com/ac5";
		String code = code(create(url, key));

		assertUnchangedBy(() -> create(url, key), 201);
		assertUnchangedBy(() -> create("https://example.com/ac5-other", key), 422);
		assertUnchangedBy(() -> send(get("/api/links/" + code)), 200);
		assertUnchangedBy(() -> send(get("/" + code)), 302);

		List<Map<String, Object>> before = stamps();
		clock.shift(Duration.ofSeconds(1));
		Instant retiredAt = clock.instant();
		assertThat(retire(code).getStatus()).isEqualTo(204);
		List<Map<String, Object>> after = stamps();
		assertThat(after).hasSameSizeAs(before);
		for (int i = 0; i < before.size(); i++) {
			if (code.equals(before.get(i).get("CODE"))) {
				assertThat(instant(after.get(i), "UPDATED_AT")).isEqualTo(retiredAt);
			}
			else {
				assertThat(after.get(i)).isEqualTo(before.get(i));
			}
		}

		assertUnchangedBy(() -> retire(code), 410);
	}

	@Test
	void AC06_auditRowsAreNeverUpdated() throws Exception {
		String code = code(create("https://example.com/ac6", null));
		clock.shift(Duration.ofMinutes(1));
		retire(code);

		assertThat(jdbc.sql("SELECT COUNT(*) FROM audit_log WHERE updated_at <> created_at OR updated_by <> created_by")
				.query(Long.class).single()).isZero();
	}

	@Test
	void AC08_noResponseShowsTheNewColumns() throws Exception {
		MockHttpServletResponse created = create("https://example.com/ac8", null);
		String code = code(created);
		JsonNode read = jsonMapper.readTree(send(get("/api/links/" + code)).getContentAsString());
		MockHttpServletResponse redirect = send(get("/" + code));
		JsonNode stats = jsonMapper.readTree(send(get("/api/links/" + code + "/stats")).getContentAsString());
		clock.shift(Duration.ofMinutes(1));
		retire(code);
		JsonNode retired = jsonMapper.readTree(send(get("/api/links/" + code)).getContentAsString());
		JsonNode audit = jsonMapper.readTree(send(get("/api/audit?limit=2")).getContentAsString());

		List<String> link = List.of("code", "shortUrl", "url", "state", "createdAt");
		assertThat(jsonMapper.readTree(created.getContentAsString()).propertyNames()).containsExactlyInAnyOrderElementsOf(link);
		assertThat(read.propertyNames()).containsExactlyInAnyOrderElementsOf(link);
		assertThat(retired.propertyNames()).containsExactlyInAnyOrderElementsOf(link);
		assertThat(retired.get("createdAt")).isEqualTo(read.get("createdAt"));
		assertThat(stats.propertyNames()).containsExactlyInAnyOrder("code", "totalClicks", "clicksPerDay", "topReferrers");
		assertThat(redirect.getStatus()).isEqualTo(302);
		assertThat(redirect.getContentAsString()).isEmpty();
		for (MockHttpServletResponse response : List.of(created, redirect)) {
			assertThat(response.getHeaderNames()).noneMatch(name -> name.toLowerCase().contains("updated")
					|| name.toLowerCase().contains("created"));
		}
		for (JsonNode row : audit.get("items")) {
			assertThat(row.propertyNames()).containsExactlyInAnyOrder("occurredAt", "actor", "action", "entity", "entityId",
					"requestId", "before", "after");
		}
		assertThat(audit.get("items").get(0).get("action").asString()).isEqualTo("link.retire");
	}

	@Test
	void AC10_theAuditColumnsHoldNoClientValue() throws Exception {
		String userAgent = "canary-ua-" + UUID.randomUUID();
		String key = "canary-key-" + UUID.randomUUID();
		MockHttpServletResponse created = mockMvc.perform(withPeer(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.header("User-Agent", userAgent).header("X-Forwarded-For", "192.0.2.10").header("Idempotency-Key", key)
				.content("{\"url\":\"https://example.com/ac10?q=canary\"}"))).andReturn().getResponse();
		String code = code(created);
		clock.shift(Duration.ofMinutes(1));
		mockMvc.perform(withPeer(delete("/api/links/" + code).header("User-Agent", userAgent)
				.header("X-Forwarded-For", "192.0.2.10")));

		List<String> actors = jdbc.sql("SELECT created_by FROM link UNION SELECT updated_by FROM link"
				+ " UNION SELECT created_by FROM audit_log UNION SELECT updated_by FROM audit_log").query(String.class).list();
		assertThat(actors).containsExactly("anonymous");
		assertThat(link(code).get("UPDATED_AT")).isInstanceOf(OffsetDateTime.class);
		assertThat(created.getHeader("X-Request-Id")).isNotBlank();
	}

	/** The request answers {@code status} and leaves every link's stamps as they were. */
	private void assertUnchangedBy(Request request, int status) throws Exception {
		List<Map<String, Object>> before = stamps();
		clock.shift(Duration.ofSeconds(1));

		assertThat(request.send().getStatus()).isEqualTo(status);

		assertThat(stamps()).isEqualTo(before);
	}

	private void assertStampedAuditRow(String code, String action) {
		Map<String, Object> row = jdbc.sql("SELECT * FROM audit_log WHERE entity_id = :code AND action = :action")
				.param("code", code).param("action", action).query().singleRow();
		assertThat(row.get("CREATED_AT")).isNotNull().isEqualTo(row.get("UPDATED_AT"));
		assertThat(row).containsEntry("CREATED_BY", "anonymous").containsEntry("UPDATED_BY", "anonymous")
				.containsEntry("ACTOR", "anonymous");
	}

	private List<Map<String, Object>> stamps() {
		return jdbc.sql("SELECT id, code, created_at, updated_at, created_by, updated_by FROM link ORDER BY id")
				.query().listOfRows();
	}

	private Map<String, Object> link(String code) {
		return jdbc.sql("SELECT * FROM link WHERE code = :code").param("code", code).query().singleRow();
	}

	private static @Nullable Instant instant(Map<String, Object> row, String column) {
		Object value = row.get(column);
		return value == null ? null : ((OffsetDateTime) value).toInstant();
	}

	private MockHttpServletResponse create(String url, @Nullable String key) throws Exception {
		MockHttpServletRequestBuilder request = post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url)));
		if (key != null) {
			request.header("Idempotency-Key", key);
		}
		return send(request);
	}

	private MockHttpServletResponse retire(String code) throws Exception {
		return send(delete("/api/links/" + code));
	}

	private MockHttpServletResponse send(MockHttpServletRequestBuilder request) throws Exception {
		return mockMvc.perform(withPeer(request)).andReturn().getResponse();
	}

	private MockHttpServletRequestBuilder withPeer(MockHttpServletRequestBuilder request) {
		return request.with(r -> {
			r.setRemoteAddr(peer);
			return r;
		});
	}

	private String code(MockHttpServletResponse response) throws Exception {
		assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(201);
		return jsonMapper.readTree(response.getContentAsString()).get("code").asString();
	}

	@FunctionalInterface
	private interface Request {
		MockHttpServletResponse send() throws Exception;
	}
}
