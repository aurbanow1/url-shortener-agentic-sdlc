package dev.urlshort.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * AC-21 and SPEC rule 6: a read that fails in the store is a bare {@code 500} problem, never an empty or
 * partial page, and neither the body nor the log carries the failure's message, the cursor or trail
 * content. Induction mechanism: a spy on {@link AuditTrail} that throws the store's exception once.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:urlshort-audit-failure;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AuditReadFailureJourneyTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoSpyBean
	private AuditTrail trail;

	@Test
	void AC21_aFailedReadIsA500ProblemNeverAnEmptyOrPartialPage(CapturedOutput output) throws Exception {
		String urlCanary = "failurl" + UUID.randomUUID().toString().replace("-", "");
		String messageCanary = "canary-store-message-" + UUID.randomUUID();
		String cursor = AuditTrail.cursorOf(987_654_321L);
		String code = jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/?q=" + urlCanary + "\"}")).andReturn().getResponse().getContentAsString())
				.get("code").asString();
		doThrow(new DataAccessResourceFailureException("SELECT FROM audit_log failed: " + messageCanary)).when(trail)
				.page(anyInt(), anyLong());
		int windowStart = output.getAll().length();

		MockHttpServletResponse failed = mockMvc.perform(get("/api/audit?cursor=" + cursor)).andReturn().getResponse();

		String window = output.getAll().substring(windowStart);
		String body = failed.getContentAsString();
		assertThat(failed.getStatus()).isEqualTo(500);
		assertThat(failed.getContentType()).isEqualTo("application/problem+json");
		JsonNode problem = jsonMapper.readTree(body);
		assertThat(problem.has("items")).isFalse();
		assertThat(problem.has("next")).isFalse();
		assertThat(body).doesNotContain("Exception").doesNotContain("\tat ").doesNotContain("SELECT").doesNotContain("audit_log")
				.doesNotContain(messageCanary).doesNotContain(cursor).doesNotContain(urlCanary).doesNotContain(code);
		String requestId = failed.getHeader("X-Request-Id");
		List<String> lines = window.lines().filter(line -> !line.isBlank()).toList();
		assertThat(lines).isNotEmpty();
		for (String line : lines) {
			assertThat(jsonMapper.readTree(line).path("requestId").asString()).as(line).isEqualTo(requestId);
		}
		assertThat(window).doesNotContain(messageCanary).doesNotContain("SELECT").doesNotContain(cursor).doesNotContain(urlCanary);

		reset(trail);
		MockHttpServletResponse recovered = mockMvc.perform(get("/api/audit")).andReturn().getResponse();

		assertThat(recovered.getStatus()).isEqualTo(200);
		assertThat(recovered.getContentAsString()).contains(code);
	}
}
