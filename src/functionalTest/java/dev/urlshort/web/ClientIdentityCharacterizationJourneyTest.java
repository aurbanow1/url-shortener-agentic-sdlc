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
 * 06-client-identity moves the rule into one component (docs/guidance/brownfield.md section 6). It pins,
 * over the SPEC's identity matrix (AC-4 to AC-9):
 * <ul>
 * <li>the client the click hash groups by (unique visitors per day, analytics-v2 rule 6);</li>
 * <li>the client the rate limiter charges (a shared 2-per-minute budget);</li>
 * <li>the audit read's guard: loopback peers, forwarding headers, a trusted loopback peer, and every
 * address-rewriting setting, including both {@code server.tomcat.remoteip} header settings (CR-01), on a
 * real server.</li>
 * </ul>
 * Each nested class is one configuration and its own context. Nothing here asserts a new behaviour: these
 * tests pass on the product as it stands.
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

	/**
	 * SPEC AC-4/AC-5 rows under a trusted list of {@code P, Q} (rows listing only {@code P} name no
	 * {@code Q}, so their answers are the same): peer, X-Forwarded-For (null = absent), other identity
	 * headers, the reference peer whose headerless requests are the same client.
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

	/** Statistics once {@code clicks} clicks are stored; the recorder writes asynchronously. */
	static JsonNode settledStats(MockMvc mockMvc, JsonMapper jsonMapper, String code, long clicks) throws Exception {
		Instant deadline = Instant.now().plusSeconds(10);
		JsonNode stats;
		do {
			stats = jsonMapper.readTree(mockMvc.perform(get("/api/links/" + code + "/stats")).andReturn().getResponse()
					.getContentAsString());
		}
		while (stats.get("totalClicks").asLong() < clicks && Instant.now().isBefore(deadline));
		return stats;
	}

	/** Shipped settings: no trusted proxy, strategy {@code none}, no remote-IP header. */
	@Nested
	class ShippedSettings {

		@Autowired
		private MockMvc mockMvc;

		@Autowired
		private JsonMapper jsonMapper;

		@Test
		void withoutATrustedProxyForwardingHeadersDoNotChangeTheClickClient() throws Exception {
			String code = create(mockMvc, jsonMapper, "https://example.com/identity-untrusted");

			for (MockHttpServletRequestBuilder request : List.of(
					withIdentity(get("/" + code), P, U, true, V), withIdentity(get("/" + code), P, null, false, null),
					withIdentity(get("/" + code), UNRELATED, null, false, null))) {
				assertThat(mockMvc.perform(request.header("User-Agent", BROWSER)).andReturn().getResponse().getStatus())
						.isEqualTo(302);
			}

			JsonNode day = settledStats(mockMvc, jsonMapper, code, 3).get("clicksPerDay").get(0);
			assertThat(day.get("clicks").asLong()).isEqualTo(3);
			assertThat(day.get("uniqueVisitors").asLong()).as("the forwarded request is the peer's client").isEqualTo(2);
		}

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
	}

	/** Trusted proxies {@code P, Q} and loopback: which client a click is hashed as, and the audit guard. */
	@Nested
	@TestPropertySource(properties = "urlshort.rate-limit.trusted-proxies=10.9.9.9,10.9.9.8,127.0.0.1")
	class TrustedProxies {

		@Autowired
		private MockMvc mockMvc;

		@Autowired
		private JsonMapper jsonMapper;

		@ParameterizedTest
		@MethodSource("dev.urlshort.web.ClientIdentityCharacterizationJourneyTest#trustedRows")
		void theClickClientIsTheChargedClient(String peer, String forwardedFor, boolean others, String reference)
				throws Exception {
			String code = create(mockMvc, jsonMapper, "https://example.com/identity-click");

			for (MockHttpServletRequestBuilder request : List.of(
					withIdentity(get("/" + code), peer, forwardedFor, others, V),
					withIdentity(get("/" + code), reference, null, false, null),
					withIdentity(get("/" + code), UNRELATED, null, false, null))) {
				assertThat(mockMvc.perform(request.header("User-Agent", BROWSER)).andReturn().getResponse().getStatus())
						.isEqualTo(302);
			}

			JsonNode stats = settledStats(mockMvc, jsonMapper, code, 3);
			JsonNode day = stats.get("clicksPerDay").get(0);
			assertThat(stats.get("totalClicks").asLong()).isEqualTo(3);
			assertThat(day.get("clicks").asLong()).isEqualTo(3);
			assertThat(day.get("uniqueVisitors").asLong()).isEqualTo(2);
			assertThat(day.get("botClicks").asLong()).isZero();
			String body = stats.toString();
			for (String value : List.of(P, Q, U, V, "10.0.0.5", "198.51.100.1", UNRELATED)) {
				assertThat(body).doesNotContain(value);
			}
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
			String code = create(mockMvc, jsonMapper, "https://example.com/identity-budget");
			for (int i = 0; i < 2; i++) {
				assertThat(mockMvc.perform(withIdentity(get("/" + code), reference, null, false, null)).andReturn()
						.getResponse().getStatus()).isEqualTo(302);
			}

			MockHttpServletResponse row = mockMvc.perform(withIdentity(get("/" + code), peer, forwardedFor, others, V))
					.andReturn().getResponse();
			MockHttpServletResponse unrelated = mockMvc
					.perform(withIdentity(get("/" + code), UNRELATED, null, false, null)).andReturn().getResponse();

			assertThat(row.getStatus()).isEqualTo(429);
			assertThat(row.getHeader("Retry-After")).isEqualTo("30");
			assertThat(row.getHeader("Location")).isNull();
			assertThat(unrelated.getStatus()).isEqualTo(302);
		}

		@ParameterizedTest
		@MethodSource("dev.urlshort.web.ClientIdentityCharacterizationJourneyTest#trustedRows")
		void theRowSpendsTheReferencePeersCreateBudget(String peer, String forwardedFor, boolean others,
				String reference) throws Exception {
			for (int i = 0; i < 2; i++) {
				assertThat(createFrom(withIdentity(post("/api/links"), reference, null, false, null)).getStatus())
						.isEqualTo(201);
			}

			MockHttpServletResponse row = createFrom(withIdentity(post("/api/links"), peer, forwardedFor, others, V));
			MockHttpServletResponse unrelated = createFrom(withIdentity(post("/api/links"), UNRELATED, null, false, null));

			assertThat(row.getStatus()).isEqualTo(429);
			assertThat(row.getHeader("Retry-After")).isEqualTo("30");
			assertThat(unrelated.getStatus()).isEqualTo(201);
		}

		private MockHttpServletResponse createFrom(MockHttpServletRequestBuilder request) throws Exception {
			return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)
					.content(jsonMapper.writeValueAsString(Map.of("url", "https://example.com/identity-create"))))
					.andReturn().getResponse();
		}
	}

	/**
	 * Every setting that lets the container rewrite the peer from a header closes the audit trail, on a
	 * real server (SPEC AC-9; 01-audit-read's CR-01 covers both {@code server.tomcat.remoteip} header
	 * settings). Each start is closed in its test; the shipped setting is the open control.
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
				for (String method : List.of("GET", "HEAD")) {
					assertThat(audit(app, method, null).statusCode()).as("%s %s", method, setting).isEqualTo(403);
					HttpResponse<String> forwarded = audit(app, method, "127.0.0.2");
					assertThat(forwarded.statusCode()).as("%s %s, X-Forwarded-For", method, setting).isEqualTo(403);
					assertThat(forwarded.body()).doesNotContain("items");
				}
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
