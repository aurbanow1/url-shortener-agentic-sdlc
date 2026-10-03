package dev.urlshort.link;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneOffset;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans the link feature needs: its settings, the application clock (millisecond UTC ticks, so an
 * instant returned in a {@code 201} equals the one read back from the database) and the random
 * source for short codes (ADR-0005, ADR-0007).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LinkProperties.class)
class LinkConfig {

	@Bean
	Clock clock() {
		return Clock.tickMillis(ZoneOffset.UTC);
	}

	@Bean
	SecureRandom secureRandom() {
		return new SecureRandom();
	}
}
