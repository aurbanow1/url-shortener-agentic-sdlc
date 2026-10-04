package dev.urlshort.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.util.Map;

import dev.urlshort.audit.AuditLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * AC-5's failed retire (slice 04-audit-columns): when the retire's audit write fails, the transaction
 * rolls back with its update stamp, so the link stays active with the stamps it had and no audit row is
 * added. Declares the same {@link AuditLog} spy as {@code AuditJourneyTest}, so the context is shared.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LinkAuditColumnsFailureJourneyTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private FunctionalClock clock;

	@MockitoSpyBean
	private AuditLog auditLog;

	@AfterEach
	void resetTheClock() {
		clock.reset();
	}

	@Test
	void AC05_aRetireWhoseAuditWriteFailsStampsNothing() throws Exception {
		clock.freeze();
		String code = jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/ac5-d\"}").with(r -> {
					r.setRemoteAddr("127.0.0.45");
					return r;
				})).andReturn().getResponse().getContentAsString()).get("code").asString();
		Map<String, Object> before = link(code);
		doThrow(new DataAccessResourceFailureException("audit store unavailable")).when(auditLog)
				.append(eq("link.retire"), any(), any(), any(), any());
		clock.shift(Duration.ofMinutes(1));

		int status = mockMvc.perform(delete("/api/links/" + code).with(r -> {
			r.setRemoteAddr("127.0.0.45");
			return r;
		})).andReturn().getResponse().getStatus();

		assertThat(status).isEqualTo(500);
		assertThat(link(code)).isEqualTo(before).containsEntry("RETIRED_AT", null);
		assertThat(jdbc.sql("SELECT COUNT(*) FROM audit_log WHERE entity_id = :code AND action = 'link.retire'")
				.param("code", code).query(Long.class).single()).isZero();
	}

	private Map<String, Object> link(String code) {
		return jdbc.sql("SELECT * FROM link WHERE code = :code").param("code", code).query().singleRow();
	}
}
