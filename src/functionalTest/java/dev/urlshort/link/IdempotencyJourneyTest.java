package dev.urlshort.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code Idempotency-Key} on create: AC-17 to AC-21. AC-19 moves the service clock through
 * {@link FunctionalClock}; every test uses its own random key because the in-memory database is
 * shared by every context of the suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyJourneyTest {

	private static final String KEY = "Idempotency-Key";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private FunctionalClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void AC17_aReplayReturnsTheFirstLink() throws Exception {
		String key = newKey();
		long linksBefore = linkCount();
		MockHttpServletResponse first = create(key, "https://example.com/replay");
		String code = code(first);

		for (int i = 0; i < 3; i++) {
			MockHttpServletResponse replay = create(key, "https://example.com/replay");
			assertThat(replay.getStatus()).isEqualTo(201);
			assertThat(replay.getHeader("Location")).isEqualTo(first.getHeader("Location"));
			assertThat(replay.getContentAsString()).isEqualTo(first.getContentAsString());
		}

		assertThat(createRows(code)).isEqualTo(1);
		assertThat(linkCount()).isEqualTo(linksBefore + 1);
	}

	@Test
	void AC18_sameKeyWithADifferentUrlIsRefusedAndTheBindingSurvives() throws Exception {
		String key = newKey();
		long linksBefore = linkCount();
		MockHttpServletResponse first = create(key, "https://example.com/original");
		String code = code(first);

		mockMvc.perform(post("/api/links").header(KEY, key).contentType(MediaType.APPLICATION_JSON)
				.content(body("https://example.com/different")))
				.andExpect(status().is(422))
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.status").value(422))
				.andExpect(jsonPath("$.errors.length()").value(1))
				.andExpect(jsonPath("$.errors[0].field").value(KEY))
				.andExpect(jsonPath("$.errors[0].rule").value("mismatch"));
		MockHttpServletResponse again = create(key, "https://example.com/original");

		assertThat(again.getStatus()).isEqualTo(201);
		assertThat(again.getContentAsString()).isEqualTo(first.getContentAsString());
		assertThat(createRows(code)).isEqualTo(1);
		assertThat(linkCount()).isEqualTo(linksBefore + 1);
	}

	@Test
	void AC19_aKeyIsHonouredFor24HoursAndNotLongerAndARejectionDoesNotExtendIt() throws Exception {
		String key = newKey();
		String code = code(create(key, "https://example.com/window"));

		clock.shift(Duration.ofHours(23));
		assertThat(create(key, "https://example.com/other").getStatus()).isEqualTo(422);

		clock.reset();
		clock.shift(Duration.ofHours(24).minusSeconds(1));
		MockHttpServletResponse inside = create(key, "https://example.com/window");
		assertThat(inside.getStatus()).isEqualTo(201);
		assertThat(code(inside)).isEqualTo(code);

		clock.reset();
		clock.shift(Duration.ofHours(24).plusSeconds(1));
		MockHttpServletResponse after = create(key, "https://example.com/window");
		assertThat(after.getStatus()).isEqualTo(201);
		assertThat(code(after)).isNotEqualTo(code);
	}

	static Stream<String> AC20_malformedKeys() {
		return Stream.of("", "k".repeat(256), "has space", "café");
	}

	@ParameterizedTest
	@MethodSource("AC20_malformedKeys")
	void AC20_aMalformedKeyIsRefused(String key) throws Exception {
		long linksBefore = linkCount();

		mockMvc.perform(post("/api/links").header(KEY, key).contentType(MediaType.APPLICATION_JSON)
				.content(body("https://example.com/key")))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.errors.length()").value(1))
				.andExpect(jsonPath("$.errors[0].field").value(KEY))
				.andExpect(jsonPath("$.errors[0].rule").value("format"));

		assertThat(linkCount()).isEqualTo(linksBefore);
	}

	@Test
	void AC20_aKeyOf255VisibleCharactersIsAccepted() throws Exception {
		assertThat(create("k".repeat(239) + UUID.randomUUID().toString().substring(0, 16), "https://example.com/k")
				.getStatus()).isEqualTo(201);
	}

	@Test
	void AC21_aRejectedCreateDoesNotConsumeTheKey() throws Exception {
		String key = newKey();
		mockMvc.perform(post("/api/links").header(KEY, key).contentType(MediaType.APPLICATION_JSON)
				.content(body("javascript:alert(1)")))
				.andExpect(status().isBadRequest());

		MockHttpServletResponse first = create(key, "https://example.com/after-reject");
		MockHttpServletResponse replay = create(key, "https://example.com/after-reject");

		assertThat(first.getStatus()).isEqualTo(201);
		assertThat(replay.getStatus()).isEqualTo(201);
		assertThat(code(replay)).isEqualTo(code(first));
	}

	private MockHttpServletResponse create(String key, String url) throws Exception {
		return mockMvc.perform(post("/api/links").header(KEY, key).contentType(MediaType.APPLICATION_JSON)
				.content(body(url))).andReturn().getResponse();
	}

	private String body(String url) {
		return jsonMapper.writeValueAsString(Map.of("url", url));
	}

	private String code(MockHttpServletResponse response) throws Exception {
		return jsonMapper.readTree(response.getContentAsString()).get("code").asString();
	}

	private long createRows(String code) {
		return jdbc.sql("SELECT COUNT(*) FROM audit_log WHERE entity = 'link' AND entity_id = :code AND action = 'link.create'")
				.param("code", code).query(Long.class).single();
	}

	private long linkCount() {
		return jdbc.sql("SELECT COUNT(*) FROM link").query(Long.class).single();
	}

	private static String newKey() {
		return "key-" + UUID.randomUUID();
	}
}
