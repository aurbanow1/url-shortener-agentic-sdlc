package dev.urlshort.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/** Business rule 6's two states, derived from {@code retiredAt}, and the audit snapshot. */
class LinkTest {

	private static final Instant T = Instant.parse("2026-10-03T12:00:00Z");

	@Test
	void stateIsActiveUntilRetiredAtIsSet() {
		Link active = new Link(1L, "Abc12345", "https://example.com/", T, null, null);
		Link retired = new Link(1L, "Abc12345", "https://example.com/", T, T, null);

		assertThat(active.state()).isEqualTo("active");
		assertThat(retired.state()).isEqualTo("retired");
		assertThat(retired.snapshot()).isEqualTo(new LinkSnapshot("https://example.com/", "retired"));
	}
}
