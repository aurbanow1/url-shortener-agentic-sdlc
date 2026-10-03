package dev.urlshort.link;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/** {@code GET /{code}}, the Visitor's path: AC-12, AC-13 and business rule 7. */
@SpringBootTest
@AutoConfigureMockMvc
class RedirectJourneyTest {

	private static final String BROWSER_ACCEPT = "text/html,application/xhtml+xml,*/*;q=0.8";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	void AC12_visitorIsRedirectedWithANonCacheable302() throws Exception {
		String target = "https://example.com/some/path?q=1&r=a%20b#frag";
		String code = create(target);

		mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", target))
				.andExpect(header().string("Cache-Control", "no-store"));
	}

	@Test
	void AC13_aRetiredLinkTellsTheVisitorItIsGone() throws Exception {
		String code = create("https://example.com/gone");
		mockMvc.perform(delete("/api/links/" + code)).andExpect(status().isNoContent());

		mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT))
				.andExpect(status().isGone())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.status").value(410))
				.andExpect(header().doesNotExist("Location"));
	}

	@Test
	void rule7_queryStringOnTheShortLinkIsNotForwarded() throws Exception {
		String target = "https://example.com/landing";
		String code = create(target);

		mockMvc.perform(get("/" + code + "?utm_source=x").header("Accept", BROWSER_ACCEPT))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", target));
	}

	private String create(String url) throws Exception {
		return jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url))))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString()).get("code").asString();
	}
}
