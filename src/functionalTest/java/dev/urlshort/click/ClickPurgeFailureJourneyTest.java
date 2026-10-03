package dev.urlshort.click;

import static dev.urlshort.click.ClickRecordingJourneyTest.BROWSER_ACCEPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Failures the Visitor never sees: a click that cannot be reduced is one correlated WARN with its own
 * reason (AC-12, rule 7, W2-05). The spies give this class its own context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ClickPurgeFailureJourneyTest {

	private static final String CANARY = "failurecanary";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private ClickRecorder recorder;

	@MockitoSpyBean
	private DailySalt salt;

	@Test
	void AC12_aClickThatCannotBeReducedIsOneWarnWithItsOwnReason(CapturedOutput output) throws Exception {
		String code = create("https://example.com/reduction");
		recorder.settle();
		doThrow(new GeneralSecurityException("no HMAC " + CANARY)).when(salt).stamp(anyString());
		int windowStart = output.getAll().length();
		MockHttpServletResponse response;
		try {
			response = mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT)).andReturn().getResponse();
			recorder.settle();
		}
		finally {
			reset(salt);
		}
		String window = output.getAll().substring(windowStart);

		assertThat(response.getStatus()).isEqualTo(302);
		List<JsonNode> lost = window.lines().filter(line -> !line.isBlank()).map(jsonMapper::readTree)
				.filter(event -> event.path("message").asString().equals("click lost")).toList();
		assertThat(lost).singleElement().satisfies(event -> {
			assertThat(event.path("log").path("level").asString()).isEqualTo("WARN");
			assertThat(event.path("requestId").asString()).isEqualTo(response.getHeader("X-Request-Id"));
			assertThat(event.path("reason").asString()).isEqualTo("reduction failed");
			assertThat(event.path("errorType").asString()).isEqualTo(GeneralSecurityException.class.getName());
		});
		assertThat(window).doesNotContain(CANARY);
	}

	private String create(String url) throws Exception {
		return jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url)))).andReturn().getResponse()
				.getContentAsString()).get("code").asString();
	}
}
