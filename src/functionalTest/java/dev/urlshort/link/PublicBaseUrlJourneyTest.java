package dev.urlshort.link;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AC-3, second configuration: the operator's public base URL is what {@code shortUrl} is built from.
 * The environment-variable spelling {@code URLSHORT_PUBLIC_BASE_URL} is Boot's relaxed binding of
 * this property.
 */
@SpringBootTest(properties = "urlshort.public-base-url=https://sho.rt")
@AutoConfigureMockMvc
class PublicBaseUrlJourneyTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void AC03_shortUrlUsesTheConfiguredBaseNeverTheHostHeader() throws Exception {
		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).header("Host", "evil.example")
				.content("{\"url\":\"https://example.com/\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.shortUrl").value(Matchers.startsWith("https://sho.rt/")))
				.andExpect(jsonPath("$.shortUrl").value(Matchers.matchesPattern("https://sho\\.rt/[A-Za-z0-9]{6,32}")));
	}
}
