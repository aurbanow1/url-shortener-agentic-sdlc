package dev.urlshort.click;

import static dev.urlshort.click.ClickRecordingJourneyTest.BROWSER_ACCEPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Failures the Visitor never sees: a purge that fails in the store is one WARN with the exception's
 * class only, leaves redirects and statistics working, and the next run deletes the old clicks (AC-10,
 * rule 6); a click that cannot be reduced is one correlated WARN with its own reason (AC-12, rule 7,
 * W2-05). The spies give this class its own context, on its own database.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:click-purge-failure;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ClickPurgeFailureJourneyTest {

	private static final String CANARY = "failurecanary";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private ClickRecorder recorder;

	@Autowired
	private ClickPurge purge;

	@MockitoSpyBean
	private DailySalt salt;

	@MockitoSpyBean
	private ClickStore store;

	@Test
	void AC10_aFailedRunIsReportedAndLeavesTheServiceWorking(CapturedOutput output) throws Exception {
		String code = create("https://example.com/purge-failure");
		long linkId = jdbc.sql("SELECT id FROM link WHERE code = :code").param("code", code).query(Long.class).single();
		LocalDate old = LocalDate.now(ZoneOffset.UTC).minusDays(100);
		jdbc.sql("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
				+ " SELECT :linkId, :at, :on, NULL, 'browser', :hash FROM SYSTEM_RANGE(1, 3)").param("linkId", linkId)
				.param("at", old.atStartOfDay().atOffset(ZoneOffset.UTC)).param("on", old)
				.param("hash", "e".repeat(64)).update();
		doThrow(new DataAccessResourceFailureException("store down " + CANARY)).when(store).deleteBefore(any());
		int windowStart = output.getAll().length();
		MockHttpServletResponse during;
		MockHttpServletResponse statsDuring;
		try {
			purge.runNow();
			during = mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT)).andReturn().getResponse();
			recorder.settle();
			statsDuring = mockMvc.perform(get("/api/links/" + code + "/stats")).andReturn().getResponse();
		}
		finally {
			reset(store);
		}
		String window = output.getAll().substring(windowStart);

		purge.runNow();
		MockHttpServletResponse after = mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT)).andReturn()
				.getResponse();
		recorder.settle();

		List<JsonNode> failed = window.lines().filter(line -> !line.isBlank()).map(jsonMapper::readTree)
				.filter(event -> event.path("message").asString().equals("click purge failed")).toList();
		assertThat(failed).singleElement().satisfies(event -> {
			assertThat(event.path("log").path("level").asString()).isEqualTo("WARN");
			assertThat(event.path("errorType").asString()).isEqualTo(DataAccessResourceFailureException.class.getName());
			assertThat(event.has("error")).as("no error.message or error.stack_trace").isFalse();
		});
		assertThat(window).doesNotContain(CANARY).doesNotContain("clicks purged");
		assertThat(during.getStatus()).isEqualTo(302);
		assertThat(statsDuring.getStatus()).isEqualTo(200);
		assertThat(jsonMapper.readTree(statsDuring.getContentAsString()).get("totalClicks").asLong()).isEqualTo(4);
		assertThat(after.getStatus()).isEqualTo(302);
		assertThat(jdbc.sql("SELECT clicked_on FROM click WHERE link_id = :linkId").param("linkId", linkId)
				.query(LocalDate.class).list()).hasSize(2).allSatisfy(day -> assertThat(day).isAfter(old));
	}

	@Test
	void AC12_aClickThatCannotBeReducedIsOneWarnWithItsOwnReason(CapturedOutput output) throws Exception {
		String code = create("https://example.com/reduction");
		recorder.settle();
		doThrow(new GeneralSecurityException("no HMAC " + CANARY)).when(salt).stamp(anyString());
		int windowStart = output.getAll().length();
		MockHttpServletResponse response;
		try {
			response = mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT)).andReturn().getResponse();
			recorder.settle();
		}
		finally {
			reset(salt);
		}
		String window = output.getAll().substring(windowStart);

		assertThat(response.getStatus()).isEqualTo(302);
		List<JsonNode> lost = window.lines().filter(line -> !line.isBlank()).map(jsonMapper::readTree)
				.filter(event -> event.path("message").asString().equals("click lost")).toList();
		assertThat(lost).singleElement().satisfies(event -> {
			assertThat(event.path("log").path("level").asString()).isEqualTo("WARN");
			assertThat(event.path("requestId").asString()).isEqualTo(response.getHeader("X-Request-Id"));
			assertThat(event.path("reason").asString()).isEqualTo("reduction failed");
			assertThat(event.path("errorType").asString()).isEqualTo(GeneralSecurityException.class.getName());
		});
		assertThat(window).doesNotContain(CANARY);
	}

	private String create(String url) throws Exception {
		return jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url)))).andReturn().getResponse()
				.getContentAsString()).get("code").asString();
	}
}
