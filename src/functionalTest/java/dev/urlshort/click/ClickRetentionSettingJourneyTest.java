package dev.urlshort.click;

import static dev.urlshort.click.ClickRecordingJourneyTest.BROWSER_ACCEPT;
import static dev.urlshort.click.ClickRecordingJourneyTest.peer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import dev.urlshort.link.FunctionalClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The period is the Operator's setting (AC-3, business rule 1): set to 7 the way the environment
 * variable sets it, the purge keeps day {@code T−7} and deletes {@code T−8}.
 */
@SpringBootTest(properties = { "urlshort.click.retention-days=7",
		"spring.datasource.url=jdbc:h2:mem:click-retention-setting;MODE=PostgreSQL;DB_CLOSE_DELAY=-1" })
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext
class ClickRetentionSettingJourneyTest {

	private static final LocalDate T = LocalDate.now(ZoneOffset.UTC);
	private static final String PEER = "10.0.42.3";

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

	@Autowired
	private FunctionalClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void AC03_thePeriodIsAnOperatorSetting(CapturedOutput output) throws Exception {
		String code = jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", "https://example.com/seven-days")))).andReturn()
				.getResponse().getContentAsString()).get("code").asString();
		for (LocalDate day : List.of(T.minusDays(8), T.minusDays(7), T)) {
			at(day);
			assertThat(mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT).with(peer(PEER))).andReturn()
					.getResponse().getStatus()).isEqualTo(302);
		}
		recorder.settle();
		int windowStart = output.getAll().length();

		purge.runNow();

		assertThat(jdbc.sql("SELECT c.clicked_on FROM click c JOIN link l ON l.id = c.link_id WHERE l.code = :code"
				+ " ORDER BY c.clicked_on").param("code", code).query(LocalDate.class).list())
				.containsExactly(T.minusDays(7), T);
		JsonNode event = jsonMapper.readTree(output.getAll().substring(windowStart).strip());
		assertThat(event.path("message").asString()).isEqualTo("clicks purged");
		assertThat(event.path("retentionDays").asInt()).isEqualTo(7);
		assertThat(event.path("cutoff").asString()).isEqualTo(T.minusDays(7).toString());
	}

	private void at(LocalDate day) {
		clock.reset();
		clock.shift(Duration.between(clock.instant(), day.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC)));
	}
}
