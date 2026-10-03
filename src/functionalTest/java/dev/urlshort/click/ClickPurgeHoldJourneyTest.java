package dev.urlshort.click;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import dev.urlshort.link.FunctionalClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The shared suite contexts run no purge (design review DR-01): the functional profile holds it, so
 * no startup run happens and no daily tick is armed, and no purge line can enter a shipped journey's
 * log window at any time of day. Runs in the default shared context, with no properties of its own.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ClickPurgeHoldJourneyTest {

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private FunctionalClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void theSharedContextsHoldThePurge(CapturedOutput output) throws Exception {
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		jdbc.sql("INSERT INTO link (code, url, created_at) VALUES ('HoldOld01', 'https://example.com/hold', CURRENT_TIMESTAMP)")
				.update();
		long linkId = jdbc.sql("SELECT id FROM link WHERE code = 'HoldOld01'").query(Long.class).single();
		jdbc.sql("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
				+ " VALUES (:linkId, :at, :on, NULL, 'browser', :hash)").param("linkId", linkId)
				.param("at", today.minusDays(100).atStartOfDay().atOffset(ZoneOffset.UTC))
				.param("on", today.minusDays(100)).param("hash", "d".repeat(64)).update();
		int windowStart = output.getAll().length();

		clock.shift(Duration.between(clock.instant(),
				today.plusDays(1).atTime(ClickPurge.DAILY_AT).toInstant(ZoneOffset.UTC).plusSeconds(1)));
		// proving an absence: more than one tick of real time must pass with the clock past 00:10Z
		Thread.sleep(ClickPurge.TICK.plusSeconds(1).toMillis());

		assertThat(output.getAll().substring(windowStart)).doesNotContain("clicks purged");
		assertThat(jdbc.sql("SELECT COUNT(*) FROM click WHERE link_id = :linkId").param("linkId", linkId)
				.query(Long.class).single()).isEqualTo(1);
	}
}
