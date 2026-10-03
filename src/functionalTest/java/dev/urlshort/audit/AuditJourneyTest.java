package dev.urlshort.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The write side of the audit trail: AC-22 to AC-25. The {@link AuditLog} spy delegates to the real
 * writer except where AC-24 makes it fail; {@code ObservabilityJourneyTest} declares the same spy, so
 * both classes share one context.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditJourneyTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@MockitoSpyBean
	private AuditLog auditLog;

	@Test
	void AC22_createWritesExactlyOneAuditRow() throws Exception {
		String url = "https://example.com/audited?q=" + UUID.randomUUID();
		Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		MockHttpServletResponse response = create(url);
		Instant after = Instant.now();
		String code = code(response);

		List<Map<String, Object>> rows = rows(code);
		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row).containsEntry("ACTION", "link.create").containsEntry("ACTOR", "anonymous")
					.containsEntry("REQUEST_ID", response.getHeader("X-Request-Id")).containsEntry("BEFORE_STATE", null);
			JsonNode afterState = jsonMapper.readTree((String) row.get("AFTER_STATE"));
			assertThat(afterState.get("url").asString()).isEqualTo(url);
			assertThat(afterState.get("state").asString()).isEqualTo("active");
			assertThat(occurredAt(row).truncatedTo(ChronoUnit.SECONDS)).isBetween(before, after);
		});
	}

	@Test
	void AC23_retireWritesExactlyOneAuditRow() throws Exception {
		String url = "https://example.com/retire-audited";
		String code = code(create(url));
		Map<String, Object> createRow = rows(code).getFirst();

		String requestId = mockMvc.perform(delete("/api/links/" + code))
				.andExpect(status().isNoContent())
				.andReturn().getResponse().getHeader("X-Request-Id");

		List<Map<String, Object>> rows = rows(code);
		assertThat(rows).hasSize(2);
		assertThat(rows.getFirst()).isEqualTo(createRow);
		Map<String, Object> retire = rows.get(1);
		assertThat(retire).containsEntry("ACTION", "link.retire").containsEntry("ACTOR", "anonymous")
				.containsEntry("ENTITY", "link").containsEntry("REQUEST_ID", requestId);
		JsonNode beforeState = jsonMapper.readTree((String) retire.get("BEFORE_STATE"));
		JsonNode afterState = jsonMapper.readTree((String) retire.get("AFTER_STATE"));
		assertThat(beforeState.get("state").asString()).isEqualTo("active");
		assertThat(beforeState.get("url").asString()).isEqualTo(url);
		assertThat(afterState.get("state").asString()).isEqualTo("retired");
		assertThat(afterState.get("url").asString()).isEqualTo(url);
	}

	@Test
	void AC24_aFailedAuditWriteRollsTheRetireBackAndFailsClosed() throws Exception {
		String code = code(create("https://example.com/fail-closed"));
		doThrow(new DataAccessResourceFailureException("audit store unavailable")).when(auditLog)
				.append(eq("link.retire"), any(), any(), any(), any());

		String body = mockMvc.perform(delete("/api/links/" + code))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.status").value(500))
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain("Exception").doesNotContain("\tat ").doesNotContain("SQL")
				.doesNotContain("audit store unavailable");
		mockMvc.perform(get("/" + code)).andExpect(status().isFound());
		mockMvc.perform(get("/api/links/" + code)).andExpect(jsonPath("$.state").value("active"));
		assertThat(rows(code)).extracting(row -> row.get("ACTION")).containsExactly("link.create");
	}

	@Test
	void AC25_auditRowsAreAppendOnlyUnderEveryOperation() throws Exception {
		List<String> codes = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			codes.add(code(create("https://example.com/append-only/" + i)));
		}
		retire(codes.get(0));
		retire(codes.get(1));
		List<Map<String, Object>> recorded = allRows();

		String key = "key-" + UUID.randomUUID();
		create("https://example.com/append-only/new");
		String keyed = code(createWithKey(key, "https://example.com/append-only/keyed"));
		createWithKey(key, "https://example.com/append-only/keyed");
		mockMvc.perform(get("/api/links/" + codes.get(2))).andExpect(status().isOk());
		retire(codes.get(2));
		mockMvc.perform(delete("/api/links/" + codes.get(2))).andExpect(status().isGone());
		mockMvc.perform(get("/" + keyed)).andExpect(status().isFound());
		mockMvc.perform(get("/api/links/nosuchcode1")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/links")).andExpect(status().isMethodNotAllowed());
		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"ftp://x/\"}"))
				.andExpect(status().isBadRequest());

		List<Map<String, Object>> now = allRows();
		assertThat(now.subList(0, recorded.size())).isEqualTo(recorded);
		assertThat(now).hasSize(recorded.size() + 3);
	}

	private MockHttpServletResponse create(String url) throws Exception {
		return mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url))))
				.andExpect(status().isCreated())
				.andReturn().getResponse();
	}

	private MockHttpServletResponse createWithKey(String key, String url) throws Exception {
		return mockMvc.perform(post("/api/links").header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url))))
				.andExpect(status().isCreated())
				.andReturn().getResponse();
	}

	private void retire(String code) throws Exception {
		mockMvc.perform(delete("/api/links/" + code)).andExpect(status().isNoContent());
	}

	private String code(MockHttpServletResponse response) throws Exception {
		return jsonMapper.readTree(response.getContentAsString()).get("code").asString();
	}

	private List<Map<String, Object>> rows(String code) {
		return jdbc.sql("SELECT * FROM audit_log WHERE entity = 'link' AND entity_id = :code ORDER BY id")
				.param("code", code).query().listOfRows();
	}

	private List<Map<String, Object>> allRows() {
		return jdbc.sql("SELECT * FROM audit_log ORDER BY id").query().listOfRows();
	}

	private static Instant occurredAt(Map<String, Object> row) {
		return ((OffsetDateTime) row.get("OCCURRED_AT")).toInstant();
	}
}
