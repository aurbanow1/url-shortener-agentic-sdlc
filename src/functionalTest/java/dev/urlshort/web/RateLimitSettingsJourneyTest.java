package dev.urlshort.web;

import static dev.urlshort.web.RateLimitJourneyTest.peer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import dev.urlshort.link.FunctionalClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * AC-10 and business rule 11: the budgets are operator settings. The environment spellings
 * ({@code URLSHORT_RATELIMIT_CREATEPERMINUTE} and friends) are Boot's relaxed binding of these
 * properties.
 */
@SpringBootTest(properties = { "urlshort.rate-limit.create-per-minute=2", "urlshort.rate-limit.redirect-per-minute=3" })
@AutoConfigureMockMvc
class RateLimitSettingsJourneyTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private FunctionalClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void AC10_theBudgetsAreOperatorSettings() throws Exception {
		clock.reset();
		clock.freeze();
		String code = jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/\"}").with(peer("10.0.10.0"))).andReturn().getResponse()
				.getContentAsString()).get("code").asString();

		int[] creates = new int[3];
		for (int i = 0; i < 3; i++) {
			creates[i] = mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
					.content("{\"url\":\"https://example.com/\"}").with(peer("10.0.10.1"))).andReturn().getResponse()
					.getStatus();
		}
		int[] redirects = new int[4];
		for (int i = 0; i < 4; i++) {
			redirects[i] = mockMvc.perform(get("/" + code).with(peer("10.0.10.1"))).andReturn().getResponse().getStatus();
		}

		assertThat(creates).containsExactly(201, 201, 429);
		assertThat(redirects).containsExactly(302, 302, 302, 429);
	}
}
