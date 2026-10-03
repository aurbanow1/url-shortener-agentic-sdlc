package dev.urlshort.web;

import static dev.urlshort.web.HealthMetricsJourneyTest.assertNoInstallationDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * AC-14 and AC-15 with the database not answering (business rule 9): readiness follows the database,
 * liveness does not, and no body says why.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DatabaseDownJourneyTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoSpyBean
	private DataSource dataSource;

	@Test
	void AC14_AC15_readinessFollowsTheDatabaseAndLivenessDoesNot() throws Exception {
		doThrow(new SQLException("database not answering")).when(dataSource).getConnection();
		MockHttpServletResponse readinessDown;
		MockHttpServletResponse liveness;
		try {
			readinessDown = mockMvc.perform(get("/actuator/health/readiness")).andReturn().getResponse();
			liveness = mockMvc.perform(get("/actuator/health/liveness")).andReturn().getResponse();
			for (String path : List.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness")) {
				assertNoInstallationDetails(jsonMapper,
						mockMvc.perform(get(path)).andReturn().getResponse().getContentAsString());
			}
		}
		finally {
			reset(dataSource);
		}
		MockHttpServletResponse readinessUp = mockMvc.perform(get("/actuator/health/readiness")).andReturn()
				.getResponse();

		assertThat(readinessDown.getStatus()).isEqualTo(503);
		assertThat(status(readinessDown)).isEqualTo("DOWN");
		assertThat(liveness.getStatus()).isEqualTo(200);
		assertThat(status(liveness)).isEqualTo("UP");
		assertThat(readinessUp.getStatus()).isEqualTo(200);
		assertThat(status(readinessUp)).isEqualTo("UP");
	}

	private String status(MockHttpServletResponse response) throws Exception {
		return jsonMapper.readTree(response.getContentAsString()).get("status").asString();
	}
}
