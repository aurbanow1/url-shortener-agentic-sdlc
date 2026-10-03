package dev.urlshort.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code GET /api/audit} (slice 01-audit-read): AC-1 to AC-13 on the shipped configuration, AC-15,
 * AC-16, AC-19 and AC-20, and business rules 1 to 7. Runs on its own in-memory database, so the trail
 * starts empty (AC-1) and row counts are exact; every test empties {@code audit_log} first. MockMvc sends
 * from {@code 127.0.0.1} unless a test sets another peer.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:urlshort-audit-read;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AuditReadJourneyTest {

	private static final String REQUEST_ID = "X-Request-Id";
	private static final String PROBLEM_JSON = "application/problem+json";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private DataSource dataSource;

	@BeforeEach
	void emptyTrail() {
		jdbc.sql("DELETE FROM audit_log").update();
	}

	// ------------------------------------------------------------------ reading the trail

	@Test
	void AC01_anEmptyTrailIsAnEmptyPage() throws Exception {
		MockHttpServletResponse response = read("");

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentType()).isEqualTo("application/json");
		assertThat(response.getContentAsString()).isEqualTo("{\"items\":[],\"next\":null}");
	}

	@Test
	void AC02_aCreateRowIsReadableAsWritten() throws Exception {
		String url = "https://example.com/audit-read?q=" + UUID.randomUUID();
		Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		MockHttpServletResponse created = create(url);
		Instant after = Instant.now();

		JsonNode row = page("").get("items").get(0);

		assertThat(row.propertyNames()).containsExactlyInAnyOrder("occurredAt", "actor", "action", "entity", "entityId",
				"requestId", "before", "after");
		assertThat(row.get("actor").asString()).isEqualTo("anonymous");
		assertThat(row.get("action").asString()).isEqualTo("link.create");
		assertThat(row.get("entity").asString()).isEqualTo("link");
		assertThat(row.get("entityId").asString()).isEqualTo(code(created));
		assertThat(row.get("before").isNull()).isTrue();
		assertThat(row.get("after").get("url").asString()).isEqualTo(url);
		assertThat(row.get("after").get("state").asString()).isEqualTo("active");
		assertThat(row.get("requestId").asString()).isEqualTo(created.getHeader(REQUEST_ID));
		String occurredAt = row.get("occurredAt").asString();
		assertThat(occurredAt).endsWith("Z");
		assertThat(Instant.parse(occurredAt).truncatedTo(ChronoUnit.SECONDS)).isBetween(before, after);
	}

	@Test
	void AC03_aRetireRowCarriesBeforeAndAfter() throws Exception {
		String url = "https://example.com/audit-retire";
		String code = code(create(url));
		String requestId = retire(code);

		JsonNode items = page("").get("items");

		JsonNode retire = items.get(0);
		assertThat(retire.get("action").asString()).isEqualTo("link.retire");
		assertThat(retire.get("entityId").asString()).isEqualTo(code);
		assertThat(retire.get("before")).isEqualTo(jsonMapper.readTree("{\"url\":\"" + url + "\",\"state\":\"active\"}"));
		assertThat(retire.get("after")).isEqualTo(jsonMapper.readTree("{\"url\":\"" + url + "\",\"state\":\"retired\"}"));
		assertThat(retire.get("requestId").asString()).isEqualTo(requestId);
		assertThat(items.get(1).get("action").asString()).isEqualTo("link.create");
		assertThat(items.get(1).get("entityId").asString()).isEqualTo(code);
	}

	@Test
	void AC04_rowsComeNewestFirstInTheOrderTheyWereWritten() throws Exception {
		String a = code(create("https://example.com/a"));
		String b = code(create("https://example.com/b"));
		String c = code(create("https://example.com/c"));
		retire(a);

		List<String> rows = new ArrayList<>();
		page("").get("items").forEach(row -> rows.add(row.get("action").asString() + " " + row.get("entityId").asString()));

		assertThat(rows).containsExactly("link.retire " + a, "link.create " + c, "link.create " + b, "link.create " + a);
	}

	@Test
	void AC05_everyFieldMatchesTheStoredRow() throws Exception {
		List<String> codes = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			codes.add(code(create("https://example.com/match/" + i + "?x=" + UUID.randomUUID())));
		}
		retire(codes.get(1));
		retire(codes.get(3));

		List<String> returned = traverse(3).stream().map(this::key).toList();

		List<String> stored = jdbc.sql("SELECT * FROM audit_log").query().listOfRows().stream().map(this::key).toList();
		assertThat(returned).hasSize(7).containsExactlyInAnyOrderElementsOf(stored);
	}

	// ------------------------------------------------------------------ pagination

	@Test
	void AC06_pagesFollowNextToTheEnd() throws Exception {
		seed(45);

		JsonNode first = page("?limit=20");
		JsonNode second = page("?limit=20&cursor=" + first.get("next").asString());
		JsonNode third = page("?limit=20&cursor=" + second.get("next").asString());

		assertThat(first.get("items").size()).isEqualTo(20);
		assertThat(second.get("items").size()).isEqualTo(20);
		assertThat(third.get("items").size()).isEqualTo(5);
		assertThat(third.get("next").isNull()).isTrue();
		List<String> ids = new ArrayList<>();
		Stream.of(first, second, third).forEach(page -> page.get("items").forEach(row -> ids.add(row.get("entityId").asString())));
		assertThat(ids).doesNotHaveDuplicates().containsExactlyElementsOf(seededNewestFirst(45));
	}

	@Test
	void AC07_theDefaultAndMaximumPageSizes() throws Exception {
		seed(150);

		JsonNode byDefault = page("");
		JsonNode maximum = page("?limit=100");

		assertThat(byDefault.get("items").size()).isEqualTo(50);
		assertThat(byDefault.get("next").isNull()).isFalse();
		assertThat(maximum.get("items").size()).isEqualTo(100);
		assertThat(maximum.get("next").isNull()).isFalse();
	}

	@Test
	void AC08_pagingIsStableWhileRowsAreWritten() throws Exception {
		seed(30);
		JsonNode first = page("?limit=10");
		List<String> fresh = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			fresh.add(code(create("https://example.com/while-paging/" + i)));
		}

		List<String> seen = new ArrayList<>();
		first.get("items").forEach(row -> seen.add(row.get("entityId").asString()));
		seen.addAll(entityIds(traverseFrom(10, first.get("next").asString())));

		assertThat(seen).containsExactlyElementsOf(seededNewestFirst(30)).doesNotContainAnyElementsOf(fresh);
		List<String> restart = entityIds(page("").get("items"));
		assertThat(restart.subList(0, 5)).containsExactlyElementsOf(fresh.reversed());
	}

	static Stream<Arguments> AC09_invalidPagingParameters() {
		return Stream.of(Arguments.of("limit=0", "limit", "range"), Arguments.of("limit=101", "limit", "range"),
				Arguments.of("limit=ten", "limit", "format"), Arguments.of("cursor=***", "cursor", "format"));
	}

	@ParameterizedTest
	@MethodSource("AC09_invalidPagingParameters")
	void AC09_invalidPagingParametersAreRefusedNamingTheField(String query, String field, String rule) throws Exception {
		MockHttpServletResponse response = read("?" + query);

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(response.getContentType()).isEqualTo(PROBLEM_JSON);
		JsonNode errors = jsonMapper.readTree(response.getContentAsString()).get("errors");
		assertThat(errors.size()).isEqualTo(1);
		assertThat(errors.get(0).get("field").asString()).isEqualTo(field);
		assertThat(errors.get(0).get("rule").asString()).isEqualTo(rule);
		String value = query.substring(query.indexOf('=') + 1);
		// a lone "0" also occurs in "400"; the unit test checks that the static message never carries it
		if (!value.equals("0")) {
			assertThat(response.getContentAsString()).doesNotContain(value);
		}
	}

	// ------------------------------------------------------------------ read-only

	@Test
	void AC10_readingChangesNothing() throws Exception {
		List<String> codes = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			codes.add(code(create("https://example.com/read-only/" + i)));
		}
		retire(codes.getFirst());
		List<Map<String, Object>> rows = allRows();
		List<Map<String, Object>> links = jdbc.sql("SELECT * FROM link ORDER BY id").query().listOfRows();

		traverse(2);
		traverse(2);
		List<Integer> writes = new ArrayList<>();
		for (MockHttpServletRequestBuilder write : List.of(post("/api/audit"), put("/api/audit"), patch("/api/audit"),
				delete("/api/audit"))) {
			MockHttpServletResponse response = mockMvc.perform(write).andReturn().getResponse();
			assertThat(response.getContentType()).isEqualTo(PROBLEM_JSON);
			writes.add(response.getStatus());
		}

		assertThat(writes).containsExactly(405, 405, 405, 405);
		assertThat(allRows()).isEqualTo(rows);
		assertThat(jdbc.sql("SELECT * FROM link ORDER BY id").query().listOfRows()).isEqualTo(links);
	}

	// ------------------------------------------------------------------ loopback only

	@ParameterizedTest
	@ValueSource(strings = { "192.0.2.10", "10.0.0.7" })
	void AC11_aNonLoopbackClientIsRefused(String address) throws Exception {
		String url = "https://example.com/secret-target";
		MockHttpServletResponse created = create(url);

		MockHttpServletResponse got = mockMvc.perform(get("/api/audit").with(peer(address))).andReturn().getResponse();
		MockHttpServletResponse headed = mockMvc.perform(head("/api/audit").with(peer(address))).andReturn().getResponse();

		assertThat(got.getStatus()).isEqualTo(403);
		assertThat(got.getContentType()).isEqualTo(PROBLEM_JSON);
		// MockMvc keeps a HEAD body; AuditForwardedHeadersJourneyTest shows Tomcat sends none
		assertThat(headed.getStatus()).isEqualTo(403);
		for (MockHttpServletResponse response : List.of(got, headed)) {
			assertThat(response.getContentAsString()).doesNotContain(code(created)).doesNotContain(url)
					.doesNotContain(created.getHeader(REQUEST_ID)).doesNotContain(address).doesNotContain("link.create")
					.doesNotContain("items");
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "127.0.0.1", "127.0.0.2", "::1", "::ffff:127.0.0.1" })
	void AC12_everyLoopbackAddressIsAdmitted(String address) throws Exception {
		assertThat(mockMvc.perform(get("/api/audit").with(peer(address))).andReturn().getResponse().getStatus()).isEqualTo(200);
	}

	static Stream<Arguments> AC13_forwardingHeaders() {
		return Stream.of(Arguments.of("127.0.0.1", "X-Forwarded-For", "198.51.100.9"),
				Arguments.of("127.0.0.1", "X-Forwarded-For", "127.0.0.1"),
				Arguments.of("127.0.0.1", "Forwarded", "for=198.51.100.9"),
				Arguments.of("192.0.2.10", "X-Forwarded-For", "127.0.0.1"));
	}

	@ParameterizedTest
	@MethodSource("AC13_forwardingHeaders")
	void AC13_forwardingHeadersNeverGrantAccess(String address, String header, String value) throws Exception {
		create("https://example.com/forwarded");

		MockHttpServletResponse response = mockMvc.perform(get("/api/audit").header(header, value).with(peer(address)))
				.andReturn().getResponse();

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentType()).isEqualTo(PROBLEM_JSON);
		assertThat(response.getContentAsString()).doesNotContain("items").doesNotContain("forwarded");
	}

	@ParameterizedTest
	@ValueSource(strings = { "text/html", PROBLEM_JSON, "*/*" })
	void theGuardAndTheValidationComeBeforeContentNegotiation(String accept) throws Exception {
		MockHttpServletResponse refused = mockMvc.perform(get("/api/audit").header("Accept", accept)
				.with(peer("192.0.2.10"))).andReturn().getResponse();
		MockHttpServletResponse invalid = mockMvc.perform(get("/api/audit?limit=0").header("Accept", accept))
				.andReturn().getResponse();
		MockHttpServletResponse valid = mockMvc.perform(get("/api/audit").header("Accept", accept)).andReturn().getResponse();

		assertThat(refused.getStatus()).isEqualTo(403);
		assertThat(refused.getContentType()).isEqualTo(PROBLEM_JSON);
		assertThat(invalid.getStatus()).isEqualTo(400);
		assertThat(invalid.getContentType()).isEqualTo(PROBLEM_JSON);
		assertThat(valid.getStatus()).isEqualTo(200);
		assertThat(valid.getContentType()).isEqualTo("application/json");
	}

	// ------------------------------------------------------------------ observability and privacy

	@ParameterizedTest
	@ValueSource(strings = { "200 page", "400 invalid limit", "403 non-loopback", "405 wrong method" })
	void AC15_requestCorrelationOnTheNewPaths(String scenario, CapturedOutput output) throws Exception {
		create("https://example.com/correlated");
		MockHttpServletRequestBuilder request = switch (scenario) {
			case "200 page" -> get("/api/audit");
			case "400 invalid limit" -> get("/api/audit?limit=ten");
			case "403 non-loopback" -> get("/api/audit").with(peer("192.0.2.10"));
			default -> post("/api/audit");
		};
		int windowStart = output.getAll().length();

		MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();

		String window = output.getAll().substring(windowStart);
		assertThat(response.getStatus()).isEqualTo(Integer.parseInt(scenario.substring(0, 3)));
		String requestId = response.getHeader(REQUEST_ID);
		assertThat(requestId).isNotBlank();
		List<String> lines = window.lines().filter(line -> !line.isBlank()).toList();
		assertThat(lines).as("at least one event for %s", scenario).isNotEmpty();
		for (String line : lines) {
			JsonNode event = jsonMapper.readTree(line);
			assertThat(event.isObject()).as("one JSON object per line: %s", line).isTrue();
			assertThat(event.path("requestId").asString()).as("event carries this request's id: %s", line).isEqualTo(requestId);
		}
	}

	@Test
	void AC16_auditContentAndClientValuesStayOutOfTheLogs(CapturedOutput output) throws Exception {
		String urlCanary = "auditurl" + UUID.randomUUID().toString().replace("-", "");
		String userAgent = "canary-ua-" + UUID.randomUUID();
		create("https://example.com/p?token=" + urlCanary);
		create("https://example.com/second");
		int windowStart = output.getAll().length();

		JsonNode first = page("?limit=1");
		String cursor = first.get("next").asString();
		JsonNode second = page("?limit=1&cursor=" + cursor);
		int refused = mockMvc.perform(get("/api/audit").header("X-Forwarded-For", "198.51.100.9").header("User-Agent", userAgent)
				.with(peer("192.0.2.10"))).andReturn().getResponse().getStatus();

		String window = output.getAll().substring(windowStart);
		assertThat(second.get("items").get(0).get("after").get("url").asString()).contains(urlCanary);
		assertThat(refused).isEqualTo(403);
		assertThat(window).isNotBlank().doesNotContain(urlCanary).doesNotContain("example.com").doesNotContain("\"state\"")
				.doesNotContain(cursor).doesNotContain("192.0.2.10").doesNotContain("198.51.100.9").doesNotContain(userAgent);
	}

	// ------------------------------------------------------------------ API document

	@Test
	void AC19_theLiveApiDocumentDescribesTheAuditRead() throws Exception {
		JsonNode document = jsonMapper.readTree(mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString());

		JsonNode operation = document.at("/paths/~1api~1audit");
		assertThat(operation.propertyNames()).containsExactly("get");
		JsonNode read = operation.get("get");
		List<String> parameters = new ArrayList<>();
		read.get("parameters").forEach(parameter -> parameters.add(parameter.get("name").asString() + " " + parameter.get("in").asString()));
		assertThat(parameters).containsExactlyInAnyOrder("limit query", "cursor query");
		JsonNode limit = read.get("parameters").get(parameters.indexOf("limit query")).get("schema");
		assertThat(limit.get("type").asString()).isEqualTo("integer");
		assertThat(limit.get("minimum").asInt()).isEqualTo(1);
		assertThat(limit.get("maximum").asInt()).isEqualTo(100);
		assertThat(limit.get("default").asInt()).isEqualTo(50);
		JsonNode ok = read.at("/responses/200/content/application~1json");
		assertThat(ok.has("schema")).isTrue();
		assertThat(ok.has("examples") || ok.has("example")).isTrue();
		for (String status : List.of("400", "403", "500")) {
			assertThat(read.at("/responses/" + status + "/content").propertyNames()).as(status).containsExactly(PROBLEM_JSON);
		}
		JsonNode row = document.at("/components/schemas/AuditEntry/properties");
		assertThat(row.propertyNames()).containsExactlyInAnyOrder("occurredAt", "actor", "action", "entity", "entityId",
				"requestId", "before", "after");
		// before and after are JSON objects (rule 3), not the stored strings
		assertThat(row.get("after").get("type").toString()).contains("object").doesNotContain("string");
		assertThat(row.get("before").get("type").toString()).contains("object").contains("null").doesNotContain("string");
		assertThat(document.at("/components/schemas/AuditPage/properties").propertyNames()).containsExactlyInAnyOrder("items", "next");
	}

	// ------------------------------------------------------------------ added after requirements review

	@Test
	void AC20_aTraversalAcrossAnInFlightWriteNeitherRepeatsNorSkipsCommittedRows() throws Exception {
		seed(30);
		List<String> traversal = new ArrayList<>();
		try (Connection held = dataSource.getConnection()) {
			held.setAutoCommit(false);
			try (Statement insert = held.createStatement()) {
				insert.executeUpdate("INSERT INTO audit_log (occurred_at, actor, action, entity, entity_id, request_id, after_state)"
						+ " VALUES (CURRENT_TIMESTAMP, 'anonymous', 'link.create', 'link', 'held', 'req-held', '{}')");
			}
			JsonNode first = page("?limit=10");
			traversal.addAll(entityIds(first.get("items")));
			held.commit();
			traversal.addAll(entityIds(traverseFrom(10, first.get("next").asString())));
		}

		List<String> fresh = entityIds(traverse(10));

		assertThat(traversal).doesNotHaveDuplicates().containsAll(seededNewestFirst(30));
		assertThat(traversal.size()).isBetween(30, 31);
		assertThat(fresh).hasSize(31).doesNotHaveDuplicates().startsWith("held");
	}

	// ------------------------------------------------------------------ helpers

	private MockHttpServletResponse read(String query) throws Exception {
		return mockMvc.perform(get("/api/audit" + query)).andReturn().getResponse();
	}

	private JsonNode page(String query) throws Exception {
		MockHttpServletResponse response = read(query);
		assertThat(response.getStatus()).as("GET /api/audit%s", query).isEqualTo(200);
		return jsonMapper.readTree(response.getContentAsString());
	}

	/** Every row of the trail, following {@code next} from the first page. */
	private List<JsonNode> traverse(int limit) throws Exception {
		return traverseFrom(limit, null);
	}

	private List<JsonNode> traverseFrom(int limit, @Nullable String cursor) throws Exception {
		List<JsonNode> rows = new ArrayList<>();
		String next = cursor;
		do {
			JsonNode page = page("?limit=" + limit + (next == null ? "" : "&cursor=" + next));
			page.get("items").forEach(rows::add);
			next = page.get("next").isNull() ? null : page.get("next").asString();
		}
		while (next != null);
		return rows;
	}

	private static List<String> entityIds(Iterable<JsonNode> rows) {
		List<String> ids = new ArrayList<>();
		rows.forEach(row -> ids.add(row.get("entityId").asString()));
		return ids;
	}

	/** {@code n} rows written directly, in order, so the volume criteria need no thousand creates. */
	private void seed(int n) {
		for (int i = 1; i <= n; i++) {
			jdbc.sql("INSERT INTO audit_log (occurred_at, actor, action, entity, entity_id, request_id, after_state)"
					+ " VALUES (CURRENT_TIMESTAMP, 'anonymous', 'link.create', 'link', :id, :rid, '{}')")
					.param("id", "seed-" + i).param("rid", "req-" + i).update();
		}
	}

	private static List<String> seededNewestFirst(int n) {
		List<String> ids = new ArrayList<>();
		for (int i = n; i >= 1; i--) {
			ids.add("seed-" + i);
		}
		return ids;
	}

	private String key(JsonNode row) {
		return String.join("|", row.get("actor").asString(), row.get("action").asString(), row.get("entity").asString(),
				row.get("entityId").asString(), row.get("requestId").asString(),
				Instant.parse(row.get("occurredAt").asString()).truncatedTo(ChronoUnit.SECONDS).toString(),
				row.get("before").toString(), row.get("after").toString());
	}

	private String key(Map<String, Object> row) {
		Object before = row.get("BEFORE_STATE");
		return String.join("|", (String) row.get("ACTOR"), (String) row.get("ACTION"), (String) row.get("ENTITY"),
				(String) row.get("ENTITY_ID"), (String) row.get("REQUEST_ID"),
				((OffsetDateTime) row.get("OCCURRED_AT")).toInstant().truncatedTo(ChronoUnit.SECONDS).toString(),
				before == null ? "null" : jsonMapper.readTree((String) before).toString(),
				jsonMapper.readTree((String) row.get("AFTER_STATE")).toString());
	}

	private List<Map<String, Object>> allRows() {
		return jdbc.sql("SELECT * FROM audit_log ORDER BY id").query().listOfRows();
	}

	private MockHttpServletResponse create(String url) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url)))).andReturn().getResponse();
		assertThat(response.getStatus()).isEqualTo(201);
		return response;
	}

	private String retire(String code) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(delete("/api/links/" + code)).andReturn().getResponse();
		assertThat(response.getStatus()).isEqualTo(204);
		return response.getHeader(REQUEST_ID);
	}

	private String code(MockHttpServletResponse response) throws Exception {
		return jsonMapper.readTree(response.getContentAsString()).get("code").asString();
	}

	static RequestPostProcessor peer(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}
}
