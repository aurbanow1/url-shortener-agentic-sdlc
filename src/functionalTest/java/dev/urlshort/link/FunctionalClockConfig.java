package dev.urlshort.link;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Puts {@link FunctionalClock} in every functional context. A top-level {@code @Configuration} is
 * picked up by the application's component scan (only {@code @TestConfiguration} is excluded), so
 * every journey shares one context and none can forget an import. The bean name must not be
 * {@code clock}: Boot forbids overriding the production bean, so a second {@code clock} would fail
 * the context before {@code @Primary} is considered (design DR-03).
 */
@Configuration(proxyBeanMethods = false)
public class FunctionalClockConfig {

	@Bean
	@Primary
	FunctionalClock functionalClock() {
		return new FunctionalClock();
	}
}
