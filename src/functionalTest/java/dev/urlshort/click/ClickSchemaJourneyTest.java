package dev.urlshort.click;

import static dev.urlshort.click.ClickRecordingJourneyTest.BROWSER_ACCEPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.Map;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * End-to-end smoke for DR-04: after the context's pool retires its connections, a redirect with each of
 * the four user-agent classes is still recorded. Not the regression itself: the suite's in-memory
 * database may have been migrated by another context's pool. The regression is the unit
 * {@code ClickSchemaTest}, which owns its database and pool.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClickSchemaJourneyTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private HikariDataSource dataSource;

	@Autowired
	private ClickRecorder recorder;

	@Test
	void everyUserAgentClassIsRecordedAfterThePoolRetiresItsConnections() throws Exception {
		String code = jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", "https://example.com/schema")))).andReturn()
				.getResponse().getContentAsString()).get("code").asString();

		dataSource.getHikariPoolMXBean().softEvictConnections();
		for (String userAgent : List.of("Mozilla/5.0", "Googlebot/2.1", "curl/8.7.1", "")) {
			mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT).header("User-Agent", userAgent));
		}
		recorder.settle();

		assertThat(jdbc.sql("SELECT c.user_agent_class FROM click c JOIN link l ON l.id = c.link_id"
				+ " WHERE l.code = :code ORDER BY c.id").param("code", code).query(String.class).list())
				.containsExactly("browser", "bot", "other", "unknown");
	}
}
