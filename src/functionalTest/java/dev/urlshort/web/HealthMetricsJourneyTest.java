package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Health and the redirect count with a working database: AC-13, AC-15 (database up) and AC-18. */
@SpringBootTest
@AutoConfigureMockMvc
class HealthMetricsJourneyTest {

	static final String REDIRECT_ROUTE = "/{code:[A-Za-z0-9]{6,32}}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	void AC13_livenessAndReadinessAreUpWithAWorkingDatabase() throws Exception {
		for (String path : List.of("/actuator/health/liveness", "/actuator/health/readiness")) {
			MockHttpServletResponse response = mockMvc.perform(get(path)).andReturn().getResponse();

			assertThat(response.getStatus()).as(path).isEqualTo(200);
			assertThat(jsonMapper.readTree(response.getContentAsString()).get("status").asString()).isEqualTo("UP");
		}
	}

	@Test
	void AC15_healthBodiesDiscloseNothingAboutTheInstallation() throws Exception {
		for (String path : List.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness")) {
			String body = mockMvc.perform(get(path)).andReturn().getResponse().getContentAsString();

			assertNoInstallationDetails(jsonMapper, body);
		}
	}

	@Test
	void AC18_redirectsAreCountedByRouteTemplate() throws Exception {
		String code = jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/counted\"}")).andReturn().getResponse().getContentAsString())
				.get("code").asString();
		mockMvc.perform(get("/" + code));
		double before = redirectCount();

		for (int i = 0; i < 5; i++) {
			mockMvc.perform(get("/" + code));
		}

		assertThat(redirectCount()).isEqualTo(before + 5);
		JsonNode tags = jsonMapper.readTree(mockMvc.perform(get("/actuator/metrics/http.server.requests"))
				.andReturn().getResponse().getContentAsString()).get("availableTags");
		for (JsonNode tag : tags) {
			tag.get("values").forEach(value -> assertThat(value.asString()).doesNotContain(code));
		}
	}

	private double redirectCount() throws Exception {
		JsonNode metric = jsonMapper.readTree(mockMvc.perform(get("/actuator/metrics/http.server.requests")
				.param("tag", "uri:" + REDIRECT_ROUTE).param("tag", "status:302")).andReturn().getResponse()
				.getContentAsString());
		for (JsonNode measurement : metric.get("measurements")) {
			if (measurement.get("statistic").asString().equals("COUNT")) {
				return measurement.get("value").asDouble();
			}
		}
		throw new AssertionError("no COUNT measurement: " + metric);
	}

	// ------------------------------------------------------------------ slice 03-dogfood-fix, W2-03

	/** AC-6 on the metrics endpoint: the disk gauges keep their values and carry no filesystem path. */
	@Test
	void AC6_diskGaugesCarryNoInstallationPath() throws Exception {
		String workingDirectory = Path.of("").toAbsolutePath().toString();
		for (String name : List.of("disk.free", "disk.total")) {
			MockHttpServletResponse response = mockMvc.perform(get("/actuator/metrics/" + name)).andReturn().getResponse();
			assertThat(response.getStatus()).as(name).isEqualTo(200);
			String body = response.getContentAsString();
			JsonNode metric = jsonMapper.readTree(body);
			List<String> tags = new ArrayList<>();
			metric.get("availableTags").forEach(tag -> tags.add(tag.get("tag").asString()));
			assertThat(tags).as("%s carries no path tag", name).doesNotContain("path");
			assertThat(body).as("%s does not disclose the working directory", name).doesNotContain(workingDirectory);
			assertThat(metric.at("/measurements/0/value").asDouble()).as("%s still has a value", name).isPositive();
		}
	}

	/** AC-6 on the anonymous scrape, which Boot's test support serves only with metrics export on. */
	@Nested
	@AutoConfigureMetrics
	class AnonymousScrape {

		@Test
		void AC6_theScrapeCarriesNoInstallationPath() throws Exception {
			MockHttpServletResponse response = mockMvc.perform(get("/actuator/prometheus")).andReturn().getResponse();

			assertThat(response.getStatus()).isEqualTo(200);
			List<String> disk = response.getContentAsString().lines().filter(line -> line.startsWith("disk_")).toList();
			assertThat(disk).as("disk free and total gauges").anyMatch(line -> line.startsWith("disk_free_bytes"))
					.anyMatch(line -> line.startsWith("disk_total_bytes"));
			assertThat(response.getContentAsString()).as("the scrape carries no path label").doesNotContain("path=\"")
					.doesNotContain(Path.of("").toAbsolutePath().toString());
		}
	}

	static void assertNoInstallationDetails(JsonMapper jsonMapper, String body) throws Exception {
		assertThat(jsonMapper.readTree(body).has("status")).as(body).isTrue();
		String lower = body.toLowerCase(Locale.ROOT);
		assertThat(lower).doesNotContain("jdbc").doesNotContain("h2").doesNotContain("urlshort-functional")
				.doesNotContain("database").doesNotContain("details").doesNotContain("components")
				.doesNotContain(InetAddress.getLocalHost().getHostName().toLowerCase(Locale.ROOT));
	}
}
