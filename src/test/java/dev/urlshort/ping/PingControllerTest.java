package dev.urlshort.ping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PingControllerTest {

	@Test
	void answersOkWithTheCurrentUtcInstant() {
		Instant before = Instant.now();
		PingResponse response = new PingController().ping();
		Instant after = Instant.now();

		assertThat(response.status()).isEqualTo("ok");
		assertThat(response.time()).endsWith("Z");
		assertThat(Instant.parse(response.time())).isBetween(before, after);
	}
}
