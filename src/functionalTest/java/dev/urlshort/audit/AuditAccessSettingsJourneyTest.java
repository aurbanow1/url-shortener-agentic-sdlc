package dev.urlshort.audit;

import static dev.urlshort.audit.AuditReadJourneyTest.peer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AC-13 (trusted-proxy configuration) and AC-14: every operator setting at a non-default value, the
 * rate limiter trusting {@code 127.0.0.1} and the client {@code 192.0.2.10}, and still no request with a
 * forwarding header or from a non-loopback peer reads the trail (SPEC rule 2). The forwarded-header
 * strategy, the remaining setting, is {@code AuditForwardedHeadersJourneyTest}'s.
 */
@SpringBootTest(properties = { "spring.datasource.url=jdbc:h2:mem:urlshort-audit-settings;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
	"urlshort.rate-limit.trusted-proxies=127.0.0.1,192.0.2.10", "urlshort.rate-limit.create-per-minute=5000000",
	"urlshort.rate-limit.redirect-per-minute=5000000", "urlshort.public-base-url=https://short.example" })
@AutoConfigureMockMvc
class AuditAccessSettingsJourneyTest {

	private static final String TARGET = "https://example.com/settings-secret";

	@Autowired
	private MockMvc mockMvc;

	static Stream<Arguments> refused() {
		return Stream.of(Arguments.of("127.0.0.1", "X-Forwarded-For", "198.51.100.9"),
				Arguments.of("127.0.0.1", "X-Forwarded-For", "127.0.0.1"),
				Arguments.of("127.0.0.1", "Forwarded", "for=198.51.100.9"),
				Arguments.of("192.0.2.10", "X-Forwarded-For", "127.0.0.1"),
				Arguments.of("192.0.2.10", null, null));
	}

	@ParameterizedTest
	@MethodSource("refused")
	void AC13_AC14_noSettingOpensTheEndpointBeyondLoopback(String address, String header, String value) throws Exception {
		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"" + TARGET + "\"}"));
		var request = get("/api/audit").with(peer(address));
		if (header != null) {
			request.header(header, value);
		}

		MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		assertThat(response.getContentAsString()).doesNotContain("items").doesNotContain(TARGET).doesNotContain(address);
	}

	@Test
	void aPlainLoopbackReadStillWorksUnderTheseSettings() throws Exception {
		assertThat(mockMvc.perform(get("/api/audit")).andReturn().getResponse().getStatus()).isEqualTo(200);
	}
}
