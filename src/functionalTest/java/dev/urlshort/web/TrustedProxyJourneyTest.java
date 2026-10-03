package dev.urlshort.web;

import static dev.urlshort.web.RateLimitJourneyTest.peer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import dev.urlshort.link.FunctionalClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** AC-8 and business rule 5: a configured trusted proxy's {@code X-Forwarded-For} names the client. */
@SpringBootTest(properties = { "urlshort.rate-limit.create-per-minute=60", "urlshort.rate-limit.redirect-per-minute=600",
		"urlshort.rate-limit.trusted-proxies=10.9.9.9" })
@AutoConfigureMockMvc
class TrustedProxyJourneyTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FunctionalClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void AC08_aTrustedProxysForwardedAddressIdentifiesTheClient() throws Exception {
		clock.reset();
		clock.freeze();
		for (int i = 0; i < 60; i++) {
			assertThat(create("10.9.9.9", "203.0.113.7")).as("create %d", i + 1).isEqualTo(201);
		}

		assertThat(create("10.9.9.9", "198.51.100.1, 203.0.113.7")).as("left entry forged, charged to 203.0.113.7")
				.isEqualTo(429);
		assertThat(create("10.9.9.9", null)).as("no header, charged to the proxy itself").isEqualTo(201);
		assertThat(create("10.0.0.5", "203.0.113.7")).as("untrusted peer, charged to the peer").isEqualTo(201);
	}

	private int create(String peer, String forwardedFor) throws Exception {
		MockHttpServletRequestBuilder request = post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/\"}").with(peer(peer));
		if (forwardedFor != null) {
			request.header("X-Forwarded-For", forwardedFor);
		}
		return mockMvc.perform(request).andReturn().getResponse().getStatus();
	}
}
