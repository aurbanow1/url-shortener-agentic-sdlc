package dev.urlshort.click;

import static dev.urlshort.click.ClickRecordingJourneyTest.BROWSER_ACCEPT;
import static dev.urlshort.click.ClickRecordingJourneyTest.peer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.time.Instant;
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
 * The daily run with no operator and no trigger (AC-8, business rule 3): the purge's tick reads this
 * context's clock, so moving that clock past the next UTC day's 00:10Z runs the purge by itself. The
 * purge is on in this context only, and the context is closed after the class, so its tick outlives it
 * nowhere.
 */
@SpringBootTest(properties = { "urlshort.click.purge-enabled=true",
		"spring.datasource.url=jdbc:h2:mem:click-retention-schedule;MODE=PostgreSQL;DB_CLOSE_DELAY=-1" })
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext
class ClickRetentionScheduleJourneyTest {

	private static final LocalDate T = LocalDate.now(ZoneOffset.UTC);
	private static final String PEER = "10.0.43.8";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private ClickRecorder recorder;

	@Autowired
	private FunctionalClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void AC08_aPurgeRunsEveryUtcDayWithoutAnOperator(CapturedOutput output) throws Exception {
		String code = jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", "https://example.com/daily")))).andReturn()
				.getResponse().getContentAsString()).get("code").asString();
		for (LocalDate day : List.of(T.minusDays(90), T.minusDays(89))) {
			shiftTo(day.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC));
			for (int i = 0; i < 2; i++) {
				assertThat(mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT).with(peer(PEER)))
						.andReturn().getResponse().getStatus()).isEqualTo(302);
			}
		}
		recorder.settle();
		int windowStart = output.getAll().length();

		Instant scheduled = T.plusDays(1).atTime(ClickPurge.DAILY_AT).toInstant(ZoneOffset.UTC);
		shiftTo(scheduled.plusSeconds(1));
		// the suite clock runs at real speed from here, so 60 s of real time is 60 s on the suite clock
		Instant deadline = Instant.now().plus(Duration.ofSeconds(60));
		while (days(code).contains(T.minusDays(90)) && Instant.now().isBefore(deadline)) {
			Thread.sleep(100);
		}

		assertThat(days(code)).containsExactly(T.minusDays(89), T.minusDays(89));
		List<JsonNode> runs = output.getAll().substring(windowStart).lines().filter(line -> !line.isBlank())
				.map(jsonMapper::readTree).filter(event -> event.path("message").asString().equals("clicks purged"))
				.toList();
		assertThat(runs).singleElement()
				.satisfies(event -> assertThat(event.path("cutoff").asString()).isEqualTo(T.minusDays(89).toString()));
	}

	private List<LocalDate> days(String code) {
		return jdbc.sql("SELECT c.clicked_on FROM click c JOIN link l ON l.id = c.link_id WHERE l.code = :code"
				+ " ORDER BY c.clicked_on").param("code", code).query(LocalDate.class).list();
	}

	private void shiftTo(Instant instant) {
		clock.reset();
		clock.shift(Duration.between(clock.instant(), instant));
	}
}
