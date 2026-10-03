package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

/** The meter filter drops only the {@code path} tag key, and the gauge still reads its value. */
class MetricsConfigTest {

	@Test
	void onlyThePathTagIsDropped() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		registry.config().meterFilter(new MetricsConfig().withoutPathTag());
		AtomicLong free = new AtomicLong(42);

		Gauge.builder("disk.free", free, AtomicLong::get).tag("path", "/x").tag("other", "kept").register(registry);

		Gauge gauge = registry.get("disk.free").gauge();
		assertThat(gauge.getId().getTags()).containsExactly(Tag.of("other", "kept"));
		assertThat(gauge.value()).isEqualTo(42);
	}
}
