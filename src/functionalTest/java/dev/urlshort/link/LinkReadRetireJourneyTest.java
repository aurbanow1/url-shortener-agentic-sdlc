package dev.urlshort.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** {@code GET} and {@code DELETE /api/links/{code}}: AC-8 to AC-11, AC-14 and AC-15. */
@SpringBootTest
@AutoConfigureMockMvc
class LinkReadRetireJourneyTest {

	private static final String PROBLEM_JSON = "application/problem+json";
	private static final String BROWSER_ACCEPT = "text/html,application/xhtml+xml,*/*;q=0.8";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Test
	void AC08_readingALinkReturnsItsDetails() throws Exception {
		String created = create("https://example.com/read?x=1");
		String code = jsonMapper.readTree(created).get("code").asString();

		String read = mockMvc.perform(get("/api/links/" + code))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andReturn().getResponse().getContentAsString();

		assertThat(jsonMapper.readTree(read)).isEqualTo(jsonMapper.readTree(created));
	}

	@Test
	void AC09_aRetiredLinkIsStillReadableWithItsState() throws Exception {
		JsonNode created = jsonMapper.readTree(create("https://example.com/retired"));
		String code = created.get("code").asString();
		mockMvc.perform(delete("/api/links/" + code)).andExpect(status().isNoContent());

		JsonNode read = jsonMapper.readTree(mockMvc.perform(get("/api/links/" + code))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString());

		assertThat(read.get("state").asString()).isEqualTo("retired");
		for (String field : new String[] { "code", "shortUrl", "url", "createdAt" }) {
			assertThat(read.get(field)).isEqualTo(created.get(field));
		}
	}

	@Test
	void AC10_retiringALinkIs204WithAnEmptyBody() throws Exception {
		String code = jsonMapper.readTree(create("https://example.com/retire")).get("code").asString();

		mockMvc.perform(delete("/api/links/" + code))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	void AC11_retiringAnAlreadyRetiredLinkIs410AndNotASecondMutation() throws Exception {
		String code = jsonMapper.readTree(create("https://example.com/twice")).get("code").asString();
		mockMvc.perform(delete("/api/links/" + code)).andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/links/" + code))
				.andExpect(status().isGone())
				.andExpect(content().contentType(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(410));

		assertThat(jdbc.sql("SELECT COUNT(*) FROM audit_log WHERE entity = 'link' AND entity_id = :code"
				+ " AND action = 'link.retire'").param("code", code).query(Long.class).single()).isEqualTo(1);
	}

	static Stream<Arguments> AC14_unknownCodes() {
		return Stream.of(
				Arguments.of(get("/api/links/nosuchcode1")),
				Arguments.of(delete("/api/links/nosuchcode1")),
				Arguments.of(get("/nosuchcode1").header("Accept", BROWSER_ACCEPT)),
				Arguments.of(get("/api/links/" + "a".repeat(40))),
				Arguments.of(get("/favicon.ico").header("Accept", BROWSER_ACCEPT)));
	}

	@ParameterizedTest
	@MethodSource("AC14_unknownCodes")
	void AC14_unknownCodeIs404OnEveryLinkOperation(MockHttpServletRequestBuilder request) throws Exception {
		mockMvc.perform(request)
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void AC15_wrongMethodIs405() throws Exception {
		String code = jsonMapper.readTree(create("https://example.com/methods")).get("code").asString();

		for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] { get("/api/links"),
				put("/api/links/" + code), patch("/api/links/" + code), delete("/api/links"), post("/" + code),
				put("/" + code) }) {
			mockMvc.perform(request)
					.andExpect(status().isMethodNotAllowed())
					.andExpect(content().contentType(PROBLEM_JSON))
					.andExpect(jsonPath("$.status").value(405));
		}
	}

	private String create(String url) throws Exception {
		return mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"" + url + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
	}
}
