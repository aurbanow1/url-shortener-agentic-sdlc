package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * The shipped defaults are the SPEC's budgets (NFR-R2: 60 creates and 600 redirects per minute, no
 * trusted proxy), in both places they live. The functional overlay and the journeys' inline numbers
 * would hide a wrong shipped value everywhere else.
 */
class RateLimitDefaultsTest {

	@Test
	void theShippedConfigurationCarriesTheDecidedBudgets() throws Exception {
		Properties shipped = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));

		assertThat(shipped.getProperty("urlshort.rate-limit.create-per-minute")).isEqualTo("60");
		assertThat(shipped.getProperty("urlshort.rate-limit.redirect-per-minute")).isEqualTo("600");
		assertThat(shipped.getProperty("urlshort.rate-limit.trusted-proxies")).isEmpty();
	}

	@Test
	void theSettingsRecordDefaultsToTheSameBudgets() {
		RateLimitProperties defaults = new Binder(new MapConfigurationPropertySource(Map.of()))
				.bindOrCreate("urlshort.rate-limit", RateLimitProperties.class);

		assertThat(defaults.createPerMinute()).isEqualTo(60);
		assertThat(defaults.redirectPerMinute()).isEqualTo(600);
		assertThat(defaults.trustedProxies()).isEmpty();
	}
}
