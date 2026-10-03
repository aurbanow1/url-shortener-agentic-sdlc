package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * NFR-R3's 10 s shutdown phase as shipped (business rule 13, A-15). The drain itself is release-level
 * (AC-25, AC-28, {@code scripts/smoke.sh --drain}); this pins the number the release checks rely on.
 */
class ShutdownPhaseDefaultTest {

	@Test
	void theShippedGracefulShutdownPhaseIsTenSeconds() throws Exception {
		Properties shipped = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));

		assertThat(shipped.getProperty("server.shutdown")).isEqualTo("graceful");
		assertThat(shipped.getProperty("spring.lifecycle.timeout-per-shutdown-phase")).isEqualTo("10s");
	}
}
