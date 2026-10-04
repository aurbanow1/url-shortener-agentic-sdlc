package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import dev.urlshort.UrlshortApplication;
import dev.urlshort.link.FunctionalClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Characterization of "who is this client" end to end, as the shipped code answers it today, before
 * 06-client-identity moves the rule into one component (docs/guidance/brownfield.md section 6; design
 * section 5.2). It pins, over the SPEC's identity matrix (AC-3 to AC-10):
 * <ul>
 * <li>the client the click hash groups by (unique visitors per day, analytics-v2 rule 6);</li>
 * <li>the client the rate limiter charges (a shared 2-per-minute budget, and the shipped budgets);</li>
 * <li>the audit read's guard: loopback peers, forwarding headers, peers listed as trusted proxies, and
 * every address-rewriting setting, including both {@code server.tomcat.remoteip} header settings
 * (CR-01), on a real server.</li>
 * </ul>
 * Each nested class is one configuration and its own context. {@code TrustedProxies} and
 * {@code SharedBudgets} run the SPEC's {@code P}-only rows under the longer list {@code P, Q} (and
 * {@code 127.0.0.1}): being listed matters only for the peer and the {@code X-Forwarded-For} entries a
 * row carries, and those rows name neither {@code Q} nor {@code 127.0.0.1}, so the answers are the
 * same. Nothing here asserts a new behaviour: these tests pass on the product as it stands.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClientIdentityCharacterizationJourneyTest {

	static final String P = "10.9.9.9";
	static final String Q = "10.9.9.8";
	static final String U = "203.0.113.7";
	static final String V = "203.0.113.8";
	static final String UNRELATED = "192.0.2.200";
	static final String BROWSER = "Mozilla/5.0 (X11; Linux x86_64) Firefox/131.0";
	static final Instant NOON = Instant.parse("2026-10-01T12:00:00Z");
	static final LocalDate DAY = LocalDate.parse("2026-10-01");
	static final List<String> ROW_VALUES = List.of(P, Q, U, V, "10.0.0.5", "198.51.100.1");

	/**
	 * SPEC AC-4/AC-5 rows M2 to M12 under a trusted list of {@code P, Q}: peer, X-Forwarded-For (null =
	 * absent), other identity headers, the reference peer whose headerless requests are the same client.
	 */
	static Stream<Arguments> trustedRows() {
		return Stream.of(
				Arguments.of("10.0.0.5", U, false, "10.0.0.5"),
				Arguments.of(P, U, false, U),
				Arguments.of(P, "198.51.100.1, " + U, false, U),
				Arguments.of(P, V + ", " + U + ", " + Q, false, U),
				Arguments.of(P, "  " + U + " ,  ", false, U),
				Arguments.of(P, P + ", " + Q, false, P),
				Arguments.of(P, null, false, P),
				Arguments.of(P, "", false, P),
				Arguments.of(P, " , ", false, P),
				Arguments.of(P, null, true, P),
				Arguments.of(P, U, true, U));
	}

	static RequestPostProcessor peer(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}

	/** A request from {@code peer} with the row's headers; {@code others} adds Forwarded and X-Real-IP. */
	static MockHttpServletRequestBuilder withIdentity(MockHttpServletRequestBuilder request, String peer,
			String forwardedFor, boolean others, String otherValue) {
		request.with(peer(peer));
		if (forwardedFor != null) {
			request.header("X-Forwarded-For", forwardedFor);
		}
		if (others) {
			request.header("Forwarded", "for=" + otherValue).header("X-Real-IP", otherValue);
		}
		return request;
	}

	static String create(MockMvc mockMvc, JsonMapper jsonMapper, String url) throws Exception {
		return jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url)))).andReturn().getResponse()
				.getContentAsString()).get("code").asString();
	}

	/**
	 * Statistics once {@code clicks} clicks are stored. The recorder writes asynchronously, so this waits,
	 * bounded, on the stored rows, which spends no rate-limit budget, and then reads the statistics once:
	 * polling the rate-limited endpoint itself would exhaust a frozen budget while a slow write is pending
	 * (CR-01).
	 */
	static JsonNode settledStats(MockMvc mockMvc, JsonMapper jsonMapper, JdbcClient jdbc, String code, long clicks)
			throws Exception {
		Instant deadline = Instant.now().plusSeconds(10);
		while (jdbc.sql("SELECT COUNT(*) FROM click c JOIN link l ON l.id = c.link_id WHERE l.code = :code")
				.param("code", code).query(Long.class).single() < clicks && Instant.now().isBefore(deadline)) {
			Thread.sleep(20);
		}
		MockHttpServletResponse response = mockMvc.perform(get("/api/links/" + code + "/stats")).andReturn()
				.getResponse();
		assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
		return jsonMapper.readTree(response.getContentAsString());
	}

	/**
	 * The AC-5 oracle (design section 5.2, DR-01): the row's request, its reference peer and an unrelated
	 * peer each open a new link once as a browser on {@link #DAY}; the statistics are exactly three
	 * clicks of two visitors on that one day, and neither the body nor any click row holds a raw value.
	 */
	static void assertGroupsAsTheReference(MockMvc mockMvc, JsonMapper jsonMapper, JdbcClient jdbc,
			MockHttpServletRequestBuilder row, String reference, String code) throws Exception {
		for (MockHttpServletRequestBuilder request : List.of(row, withIdentity(get("/" + code), reference, null, false, null),
				withIdentity(get("/" + code), UNRELATED, null, false, null))) {
			assertThat(mockMvc.perform(request.header("User-Agent", BROWSER)).andReturn().getResponse().getStatus())
					.isEqualTo(302);
		}

		JsonNode stats = settledStats(mockMvc, jsonMapper, jdbc, code, 3);
		assertThat(stats.get("totalClicks").asLong()).isEqualTo(3);
		assertThat(stats.get("clicksPerDay")).singleElement().satisfies(day -> {
			assertThat(day.get("date").asString()).isEqualTo(DAY.toString());
			assertThat(day.get("clicks").asLong()).isEqualTo(3);
			assertThat(day.get("uniqueVisitors").asLong()).isEqualTo(2);
			assertThat(day.get("botClicks").asLong()).isZero();
		});
		String body = stats.toString();
		String rows = jdbc.sql("SELECT c.* FROM click c JOIN link l ON l.id = c.link_id WHERE l.code = :code")
				.param("code", code).query().listOfRows().toString();
		for (String value : ROW_VALUES) {
			assertThat(body).doesNotContain(value);
			assertThat(rows).doesNotContain(value);
		}
	}

	/** Stands the clock on {@link #NOON} once, then 5 minutes later before every case: one fixed UTC day. */
	static void onTheFixedDay(FunctionalClock clock) {
		if (!LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).equals(DAY)) {
			clock.freeze();
			clock.shift(Duration.between(clock.instant(), NOON));
		}
		clock.shift(Duration.ofMinutes(5));
	}

	/** Shipped settings: no trusted proxy, strategy {@code none}, no remote-IP header; the suite's running clock. */
	@Nested
	class ShippedSettings {

		@Autowired
		private MockMvc mockMvc;

		@Autowired
		private JsonMapper jsonMapper;

		@ParameterizedTest
		@ValueSource(strings = { "127.0.0.1", "127.0.0.2", "127.255.255.254", "::1", "0:0:0:0:0:0:0:1",
			"::ffff:127.0.0.1" })
		void aHeaderlessLoopbackPeerReadsTheAuditTrail(String loopback) throws Exception {
			MockHttpServletResponse response = mockMvc.perform(get("/api/audit").with(peer(loopback))).andReturn()
					.getResponse();

			assertThat(response.getStatus()).isEqualTo(200);
			assertThat(jsonMapper.readTree(response.getContentAsString()).propertyNames()).containsExactly("items",
					"next");
		}

		@ParameterizedTest
		@ValueSource(strings = { "192.0.2.10", "10.0.0.7", "::ffff:192.0.2.10", "fe80::1" })
		void anyOtherPeerIsRefusedTheAuditTrail(String address) throws Exception {
			assertThat(mockMvc.perform(get("/api/audit").with(peer(address))).andReturn().getResponse().getStatus())
					.isEqualTo(403);
			assertThat(mockMvc.perform(head("/api/audit").with(peer(address))).andReturn().getResponse().getStatus())
					.isEqualTo(403);
		}

		@ParameterizedTest
		@ValueSource(strings = { "X-Forwarded-For", "Forwarded" })
		void anyForwardingHeaderClosesTheAuditTrail(String header) throws Exception {
			for (String value : List.of(U, "127.0.0.1", "for=127.0.0.1", "", "   ")) {
				for (String address : List.of("127.0.0.1", "192.0.2.10")) {
					MockHttpServletResponse response = mockMvc.perform(get("/api/audit").with(peer(address))
							.header(header, value)).andReturn().getResponse();
					assertThat(response.getStatus()).as("%s: '%s' from %s", header, value, address).isEqualTo(403);
					assertThat(response.getContentAsString()).doesNotContain("items");
				}
			}
		}

		@Test
		void aRefusedAuditReadIsRefusedBeforeValidationAndNegotiation() throws Exception {
			MockHttpServletResponse response = mockMvc.perform(get("/api/audit").param("limit", "0")
					.accept(MediaType.TEXT_HTML).with(peer("192.0.2.10"))).andReturn().getResponse();

			assertThat(response.getStatus()).as("a 403, neither 400 nor 406").isEqualTo(403);
			assertThat(response.getContentAsString()).doesNotContain("items");
		}
	}

	/**
	 * Shipped budgets (60 and 600 per minute, declared, which gives this class its own context), no
	 * trusted proxy, and the clock on one fixed day: on a running clock a 60-per-minute bucket refills
	 * about once a second while the requests are sent, so the 61st could be admitted.
	 */
	@Nested
	@TestPropertySource(properties = { "urlshort.rate-limit.create-per-minute=60",
		"urlshort.rate-limit.redirect-per-minute=600" })
	@DirtiesContext
	class ShippedBudgets {

		@Autowired
		private MockMvc mockMvc;

		@Autowired
		private JsonMapper jsonMapper;

		@Autowired
		private JdbcClient jdbc;

		@Autowired
		private FunctionalClock clock;

		@BeforeEach
		void fixedDay() {
			onTheFixedDay(clock);
		}

		@Test
		void untrustedForwardingHeadersCannotEvadeTheShippedCreateBudget() throws Exception {
			List<Integer> statuses = new ArrayList<>();
			MockHttpServletResponse last = null;
			for (int i = 1; i <= 61; i++) {
				last = mockMvc.perform(post("/api/links").with(peer("192.0.2.31")).header("X-Forwarded-For", "198.51.100." + i)
						.header("Forwarded", "for=198.51.100." + i).header("X-Real-IP", "198.51.100." + i)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(Map.of("url", "https://example.com/ac3-" + i)))).andReturn()
						.getResponse();
				statuses.add(last.getStatus());
			}

			assertThat(statuses.subList(0, 60)).containsOnly(201);
			assertThat(statuses.get(60)).isEqualTo(429);
			assertThat(Long.parseLong(last.getHeader("Retry-After"))).isGreaterThanOrEqualTo(1);
		}

		@Test
		void untrustedForwardingHeadersCannotEvadeTheShippedRedirectBudget() throws Exception {
			String code = create(mockMvc, jsonMapper, "https://example.com/ac3-redirect");
			List<Integer> statuses = new ArrayList<>();
			MockHttpServletResponse last = null;
			for (int i = 1; i <= 601; i++) {
				last = mockMvc.perform(get("/" + code).with(peer("192.0.2.32")).header("X-Forwarded-For", "198.51.100." + i)
						.header("Forwarded", "for=198.51.100." + i).header("X-Real-IP", "198.51.100." + i)).andReturn()
						.getResponse();
				statuses.add(last.getStatus());
			}

			assertThat(statuses.subList(0, 600)).containsOnly(302);
			assertThat(statuses.get(600)).isEqualTo(429);
			assertThat(last.getHeader("Location")).isNull();
			assertThat(Long.parseLong(last.getHeader("Retry-After"))).isGreaterThanOrEqualTo(1);
		}

		@Test
		void withoutATrustedProxyForwardingHeadersDoNotChangeTheClickClient() throws Exception {
			String code = create(mockMvc, jsonMapper, "https://example.com/identity-untrusted");

			assertGroupsAsTheReference(mockMvc, jsonMapper, jdbc, withIdentity(get("/" + code), P, U, true, V), P, code);
		}
	}

	/** Trusted proxies {@code P, Q} and loopback, on one fixed day: the click client, and the audit guard. */
	@Nested
	@TestPropertySource(properties = "urlshort.rate-limit.trusted-proxies=10.9.9.9,10.9.9.8,127.0.0.1")
	@DirtiesContext
	class TrustedProxies {

		@Autowired
		private MockMvc mockMvc;

		@Autowired
		private JsonMapper jsonMapper;

		@Autowired
		private JdbcClient jdbc;

		@Autowired
		private FunctionalClock clock;

		@BeforeEach
		void fixedDay() {
			onTheFixedDay(clock);
		}

		@ParameterizedTest
		@MethodSource("dev.urlshort.web.ClientIdentityCharacterizationJourneyTest#trustedRows")
		void theClickClientIsTheChargedClient(String peer, String forwardedFor, boolean others, String reference)
				throws Exception {
			String code = create(mockMvc, jsonMapper, "https://example.com/identity-click");

			assertGroupsAsTheReference(mockMvc, jsonMapper, jdbc, withIdentity(get("/" + code), peer, forwardedFor, others, V),
					reference, code);
		}

		@Test
		void aTrustedLoopbackPeerStillReadsOnlyWithoutForwardingHeaders() throws Exception {
			assertThat(mockMvc.perform(get("/api/audit").with(peer("127.0.0.1"))).andReturn().getResponse()
					.getStatus()).isEqualTo(200);
			assertThat(mockMvc.perform(get("/api/audit").with(peer("127.0.0.1")).header("X-Forwarded-For", "127.0.0.1"))
					.andReturn().getResponse().getStatus()).isEqualTo(403);
			assertThat(mockMvc.perform(get("/api/audit").with(peer(P))).andReturn().getResponse().getStatus())
					.as("trust for rate limiting never grants audit access").isEqualTo(403);
			assertThat(mockMvc.perform(get("/api/audit").with(peer(P)).header("X-Forwarded-For", "127.0.0.1"))
					.andReturn().getResponse().getStatus()).isEqualTo(403);
		}
	}

	/**
	 * Every audit peer listed as a trusted proxy, in the exact text the request uses (DR-01): trust for
	 * rate limiting neither opens nor closes the audit read. A running clock is fine: a few requests per
	 * client, under the suite's raised {@code /api} budget.
	 */
	@Nested
	@TestPropertySource(properties = "urlshort.rate-limit.trusted-proxies=127.0.0.1,127.0.0.2,127.255.255.254,::1,"
			+ "0:0:0:0:0:0:0:1,::ffff:127.0.0.1,192.0.2.10,10.0.0.7,::ffff:192.0.2.10,fe80::1")
	class TrustedAuditPeers {

		@Autowired
		private MockMvc mockMvc;

		@Autowired
		private JsonMapper jsonMapper;

		@ParameterizedTest
		@ValueSource(strings = { "127.0.0.1", "127.0.0.2", "127.255.255.254", "::1", "0:0:0:0:0:0:0:1",
			"::ffff:127.0.0.1" })
		void aListedLoopbackPeerStillReadsTheAuditTrail(String loopback) throws Exception {
			MockHttpServletResponse response = mockMvc.perform(get("/api/audit").with(peer(loopback))).andReturn()
					.getResponse();

			assertThat(response.getStatus()).isEqualTo(200);
			assertThat(jsonMapper.readTree(response.getContentAsString()).propertyNames()).containsExactly("items",
					"next");
		}

		@ParameterizedTest
		@ValueSource(strings = { "192.0.2.10", "10.0.0.7", "::ffff:192.0.2.10", "fe80::1" })
		void aListedNonLoopbackPeerIsStillRefused(String address) throws Exception {
			for (MockHttpServletRequestBuilder request : List.of(get("/api/audit"), head("/api/audit"))) {
				MockHttpServletResponse response = mockMvc.perform(request.with(peer(address))).andReturn().getResponse();
				assertThat(response.getStatus()).isEqualTo(403);
				assertThat(response.getContentAsString()).doesNotContain("items");
			}
		}

		@ParameterizedTest
		@ValueSource(strings = { "127.0.0.1", "192.0.2.10" })
		void anyForwardingHeaderFromAListedPeerClosesTheAuditTrail(String address) throws Exception {
			for (String[] header : List.of(new String[] { "X-Forwarded-For", U }, new String[] { "Forwarded", "for=" + U },
					new String[] { "X-Forwarded-For", "127.0.0.2" }, new String[] { "Forwarded", "for=127.0.0.2" },
					new String[] { "X-Forwarded-For", "" }, new String[] { "Forwarded", "" },
					new String[] { "X-Forwarded-For", "   " }, new String[] { "Forwarded", "   " })) {
				MockHttpServletResponse response = mockMvc.perform(get("/api/audit").with(peer(address))
						.header(header[0], header[1])).andReturn().getResponse();
				assertThat(response.getStatus()).as("%s: '%s' from %s", header[0], header[1], address).isEqualTo(403);
				assertThat(response.getContentAsString()).doesNotContain("items");
			}
		}
	}

	/**
	 * Trusted proxies {@code P, Q} with both budgets at 2 per minute on a frozen clock: the row's request
	 * spends the reference peer's budget. The clock only moves forward between rows, by enough to refill
	 * every bucket; the context is closed after the class.
	 */
	@Nested
	@TestPropertySource(properties = { "urlshort.rate-limit.trusted-proxies=10.9.9.9,10.9.9.8",
		"urlshort.rate-limit.create-per-minute=2", "urlshort.rate-limit.redirect-per-minute=2" })
	@DirtiesContext
	class SharedBudgets {

		@Autowired
		private MockMvc mockMvc;

		@Autowired
		private JsonMapper jsonMapper;

		@Autowired
		private FunctionalClock clock;

		@BeforeEach
		void refill() {
			clock.freeze();
			clock.shift(Duration.ofMinutes(5));
		}

		@ParameterizedTest
		@MethodSource("dev.urlshort.web.ClientIdentityCharacterizationJourneyTest#trustedRows")
		void theRowSpendsTheReferencePeersRedirectBudget(String peer, String forwardedFor, boolean others,
				String reference) throws Exception {
			assertRedirectBudgetShared(mockMvc, jsonMapper, peer, forwardedFor, others, reference);
		}

		@ParameterizedTest
		@MethodSource("dev.urlshort.web.ClientIdentityCharacterizationJourneyTest#trustedRows")
		void theRowSpendsTheReferencePeersCreateBudget(String peer, String forwardedFor, boolean others,
				String reference) throws Exception {
			assertCreateBudgetShared(mockMvc, jsonMapper, withIdentity(post("/api/links"), peer, forwardedFor, others, V),
					reference);
		}
	}

	/** No trusted proxy, both budgets at 2 per minute, isolated like {@code SharedBudgets}: SPEC row M1 and A10. */
	@Nested
	@TestPropertySource(properties = { "urlshort.rate-limit.create-per-minute=2",
		"urlshort.rate-limit.redirect-per-minute=2" })
	@DirtiesContext
	class UntrustedBudgets {

		@Autowired
		private MockMvc mockMvc;

		@Autowired
		private JsonMapper jsonMapper;

		@Autowired
		private FunctionalClock clock;

		@BeforeEach
		void refill() {
			clock.freeze();
			clock.shift(Duration.ofMinutes(5));
		}

		@Test
		void forwardingHeadersSpendThePeersRedirectBudget() throws Exception {
			assertRedirectBudgetShared(mockMvc, jsonMapper, P, U, true, P);
		}

		@Test
		void forwardingHeadersSpendThePeersCreateBudget() throws Exception {
			assertCreateBudgetShared(mockMvc, jsonMapper, withIdentity(post("/api/links"), P, U, true, V), P);
		}

		@Test
		void anEmptyBudgetIsAnsweredBeforeTheAuditGuardValidationAndNegotiation() throws Exception {
			for (int i = 0; i < 2; i++) {
				assertThat(createFrom(mockMvc, jsonMapper, post("/api/links").with(peer("192.0.2.10"))).getStatus())
						.isEqualTo(201);
			}

			MockHttpServletResponse response = mockMvc.perform(get("/api/audit").param("limit", "0")
					.accept(MediaType.TEXT_HTML).with(peer("192.0.2.10"))).andReturn().getResponse();

			assertThat(response.getStatus()).as("a 429, neither 403, 400 nor 406").isEqualTo(429);
			assertThat(response.getHeader("Retry-After")).isNotBlank();
		}
	}

	/**
	 * The AC-4 oracle on the redirect budget: a setup peer creates the link (outside the budget under
	 * test), the reference peer spends two redirects, then the row's request is {@code 429} with
	 * {@code Retry-After: 30} and no {@code Location}, while an unrelated peer is still admitted.
	 */
	static void assertRedirectBudgetShared(MockMvc mockMvc, JsonMapper jsonMapper, String peer, String forwardedFor,
			boolean others, String reference) throws Exception {
		String code = create(mockMvc, jsonMapper, "https://example.com/identity-budget");
		for (int i = 0; i < 2; i++) {
			assertThat(mockMvc.perform(withIdentity(get("/" + code), reference, null, false, null)).andReturn()
					.getResponse().getStatus()).isEqualTo(302);
		}

		MockHttpServletResponse refused = mockMvc.perform(withIdentity(get("/" + code), peer, forwardedFor, others, V))
				.andReturn().getResponse();
		MockHttpServletResponse unrelated = mockMvc.perform(withIdentity(get("/" + code), UNRELATED, null, false, null))
				.andReturn().getResponse();

		assertThat(refused.getStatus()).isEqualTo(429);
		assertThat(refused.getHeader("Retry-After")).isEqualTo("30");
		assertThat(refused.getHeader("Location")).isNull();
		assertThat(unrelated.getStatus()).isEqualTo(302);
	}

	/** The AC-4 oracle on the create budget, as {@link #assertRedirectBudgetShared}. */
	static void assertCreateBudgetShared(MockMvc mockMvc, JsonMapper jsonMapper, MockHttpServletRequestBuilder row,
			String reference) throws Exception {
		for (int i = 0; i < 2; i++) {
			assertThat(createFrom(mockMvc, jsonMapper, withIdentity(post("/api/links"), reference, null, false, null))
					.getStatus()).isEqualTo(201);
		}

		MockHttpServletResponse refused = createFrom(mockMvc, jsonMapper, row);
		MockHttpServletResponse unrelated = createFrom(mockMvc, jsonMapper,
				withIdentity(post("/api/links"), UNRELATED, null, false, null));

		assertThat(refused.getStatus()).isEqualTo(429);
		assertThat(refused.getHeader("Retry-After")).isEqualTo("30");
		assertThat(unrelated.getStatus()).isEqualTo(201);
	}

	static MockHttpServletResponse createFrom(MockMvc mockMvc, JsonMapper jsonMapper, MockHttpServletRequestBuilder request)
			throws Exception {
		return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", "https://example.com/identity-create")))).andReturn()
				.getResponse();
	}

	/**
	 * Every setting that lets the container rewrite the peer from a header closes the audit trail, on a
	 * real server (SPEC AC-9; 01-audit-read's CR-01 covers both {@code server.tomcat.remoteip} header
	 * settings). Whitespace-only settings are unset, as Boot's own valve trigger reads them. Each start
	 * is closed in its test; the shipped setting is the open control.
	 */
	@Nested
	class AddressRewritingSettings {

		private final HttpClient http = HttpClient.newHttpClient();

		@ParameterizedTest
		@ValueSource(strings = { "--server.forward-headers-strategy=native", "--server.forward-headers-strategy=framework",
			"--server.tomcat.remoteip.remote-ip-header=x-forwarded-for",
			"--server.tomcat.remoteip.protocol-header=x-forwarded-proto" })
		void anAddressRewritingSettingClosesTheAuditTrail(String setting) throws Exception {
			try (ConfigurableApplicationContext app = start(setting)) {
				assertClosed(app, setting);
			}
		}

		@Test
		void bothRemoteIpHeaderSettingsTogetherCloseTheAuditTrail() throws Exception {
			try (ConfigurableApplicationContext app = start("--server.tomcat.remoteip.remote-ip-header=x-forwarded-for",
					"--server.tomcat.remoteip.protocol-header=x-forwarded-proto")) {
				assertClosed(app, "both remoteip header settings");
			}
		}

		@Test
		void whitespaceOnlyRemoteIpHeaderSettingsKeepTheAuditTrailOpen() throws Exception {
			try (ConfigurableApplicationContext app = start("--server.tomcat.remoteip.remote-ip-header=  ",
					"--server.tomcat.remoteip.protocol-header=  ")) {
				assertThat(audit(app, "GET", null).statusCode()).isEqualTo(200);
				assertThat(audit(app, "HEAD", null).statusCode()).isEqualTo(200);
				assertThat(audit(app, "GET", "127.0.0.2").statusCode()).isEqualTo(403);
			}
		}

		@Test
		void theShippedSettingKeepsTheAuditTrailOpenToADirectLoopbackClient() throws Exception {
			try (ConfigurableApplicationContext app = start()) {
				assertThat(audit(app, "GET", null).statusCode()).isEqualTo(200);
				assertThat(audit(app, "HEAD", null).statusCode()).isEqualTo(200);
				assertThat(audit(app, "GET", "127.0.0.2").statusCode()).isEqualTo(403);
			}
		}

		private void assertClosed(ConfigurableApplicationContext app, String setting) throws Exception {
			for (String method : List.of("GET", "HEAD")) {
				assertThat(audit(app, method, null).statusCode()).as("%s %s", method, setting).isEqualTo(403);
				HttpResponse<String> forwarded = audit(app, method, "127.0.0.2");
				assertThat(forwarded.statusCode()).as("%s %s, X-Forwarded-For", method, setting).isEqualTo(403);
				assertThat(forwarded.body()).doesNotContain("items");
			}
		}

		private HttpResponse<String> audit(ConfigurableApplicationContext app, String method, String forwardedFor)
				throws Exception {
			HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"
					+ app.getEnvironment().getProperty("local.server.port") + "/api/audit"))
					.method(method, HttpRequest.BodyPublishers.noBody());
			if (forwardedFor != null) {
				request.header("X-Forwarded-For", forwardedFor);
			}
			return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
		}

		private ConfigurableApplicationContext start(String... settings) {
			List<String> args = new ArrayList<>(List.of("--server.port=0", "--server.address=127.0.0.1",
					"--spring.datasource.url=jdbc:h2:mem:client-identity-" + System.nanoTime()
							+ ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1"));
			args.addAll(List.of(settings));
			return new SpringApplicationBuilder(UrlshortApplication.class).registerShutdownHook(false)
					.run(args.toArray(String[]::new));
		}
	}
}
