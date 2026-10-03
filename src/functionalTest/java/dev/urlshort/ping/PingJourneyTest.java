package dev.urlshort.ping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** HTTP journeys for {@code GET /api/ping}: one test per acceptance criterion in SPEC.md. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class PingJourneyTest {

	private static final String REQUEST_ID = "X-Request-Id";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	void AC1_pingAnswersOkAsJson() throws Exception {
		mockMvc.perform(get("/api/ping"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value("ok"))
				.andExpect(jsonPath("$.time").isString())
				.andExpect(jsonPath("$.*", hasSize(2)));
	}

	@Test
	void AC2_timeIsCurrentUtcInstant() throws Exception {
		Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		MvcResult result = mockMvc.perform(get("/api/ping")).andExpect(status().isOk()).andReturn();
		Instant after = Instant.now();

		String time = jsonMapper.readTree(result.getResponse().getContentAsString()).get("time").asString();
		assertThat(time).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?Z");
		assertThat(Instant.parse(time)).isBetween(before, after);
	}

	@Test
	void AC3_everyResponseCarriesRequestId() throws Exception {
		String id = mockMvc.perform(get("/api/ping"))
				.andExpect(header().exists(REQUEST_ID))
				.andReturn().getResponse().getHeader(REQUEST_ID);

		assertThat(id).isNotEmpty().hasSizeLessThanOrEqualTo(64).matches("[\\x21-\\x7E]+");
	}

	@Test
	void AC4_requestIdsAreUniquePerRequest() throws Exception {
		String first = mockMvc.perform(get("/api/ping")).andReturn().getResponse().getHeader(REQUEST_ID);
		String second = mockMvc.perform(get("/api/ping")).andReturn().getResponse().getHeader(REQUEST_ID);

		assertThat(first).isNotNull();
		assertThat(second).isNotNull().isNotEqualTo(first);
	}

	@Test
	void AC5_wrongMethodIsProblemDetailWithRequestId() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/ping"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(header().string(REQUEST_ID, not(emptyOrNullString())))
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).doesNotContain("Exception").doesNotContain("\tat ");
	}

	@Test
	void AC6_pingIsLoggedAsJsonWithRequestId(CapturedOutput output) throws Exception {
		String id = mockMvc.perform(get("/api/ping"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getHeader(REQUEST_ID);

		List<String> events = linesContaining(output, id);
		assertThat(events).isNotEmpty();
		for (String line : events) {
			JsonNode event = jsonMapper.readTree(line);
			assertThat(event.isObject()).as("log line is one JSON object: %s", line).isTrue();
			assertThat(event.has("requestId")).as("log line carries requestId: %s", line).isTrue();
			assertThat(event.get("requestId").asString()).isEqualTo(id);
		}
	}

	@Test
	void AC7_logEventCarriesNoClientAddressOrUserAgent(CapturedOutput output) throws Exception {
		String userAgent = "canary-ua-" + UUID.randomUUID();
		String remoteAddr = "203.0.113.77";

		String id = mockMvc.perform(get("/api/ping").header("User-Agent", userAgent).with(request -> {
			request.setRemoteAddr(remoteAddr);
			return request;
		})).andExpect(status().isOk()).andReturn().getResponse().getHeader(REQUEST_ID);

		List<String> events = linesContaining(output, id);
		assertThat(events).isNotEmpty();
		for (String line : events) {
			assertThat(jsonMapper.readTree(line).at("/process/thread/name").isMissingNode())
					.as("no thread name, which carries the bind address on a loopback-bound server: %s", line)
					.isTrue();
		}
		assertThat(output.getAll()).doesNotContain(userAgent).doesNotContain(remoteAddr);
	}

	@Test
	void AC8_clientSuppliedRequestIdIsIgnored(CapturedOutput output) throws Exception {
		String canary = "canary-rid-" + UUID.randomUUID();

		String issued = mockMvc.perform(get("/api/ping").header(REQUEST_ID, canary))
				.andExpect(status().isOk())
				.andReturn().getResponse().getHeader(REQUEST_ID);

		assertThat(issued).isNotNull().isNotEqualTo(canary);
		assertThat(output.getAll()).doesNotContain(canary);
	}

	private static List<String> linesContaining(CapturedOutput output, String needle) {
		return output.getAll().lines().filter(line -> line.contains(needle)).toList();
	}
}
