package dev.urlshort.web;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * No meter carries a filesystem path (ADR-0016 amendment). Micrometer's disk gauges tag the absolute
 * data path, an installation detail on the anonymous {@code /actuator/prometheus} and
 * {@code /actuator/metrics}; the filter drops that tag key from every meter, and only {@code disk.free}
 * and {@code disk.total} carry it today. The gauges keep their values.
 */
@Configuration(proxyBeanMethods = false)
class MetricsConfig {

	// ponytail: every meter, not only disk.*; a second disk path would need its own non-path tag here
	@Bean
	MeterFilter withoutPathTag() {
		return MeterFilter.ignoreTags("path");
	}
}
