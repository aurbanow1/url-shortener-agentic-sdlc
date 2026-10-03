package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.urlshort.web.RateLimiter.Budget;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Business rules 1 (classification and exemptions), 5 (client identity) and 4, 6, 7 (the {@code 429}
 * itself) in isolation, with a scripted limiter.
 */
class RateLimitFilterTest {

	private final RateLimiter limiter = mock(RateLimiter.class);
	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
	private final JsonMapper json = JsonMapper.builder().addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
			.build();
	private final RateLimitFilter filter = new RateLimitFilter(limiter,
			new RateLimitProperties(60, 600, Set.of("10.9.9.9")), registry, json);

	@AfterEach
	void clearMdc() {
		MDC.remove("requestId");
	}

	@ParameterizedTest
	@CsvSource({ "/api, CREATE", "/api/, CREATE", "/api/links, CREATE", "/api/links/Abc12345/stats, CREATE",
		"/api/ping, CREATE", "/%61pi/links, CREATE", "/apix, REDIRECT", "/Abc12345, REDIRECT", "/, REDIRECT",
		"/favicon.ico, REDIRECT", "/actuatorx, REDIRECT", "/swagger-uix, REDIRECT" })
	void rule1_limitedRequestsAreChargedToTheirBudget(String uri, Budget budget) throws Exception {
		when(limiter.tryTake(any(), anyString())).thenReturn(0L);
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request(uri), new MockHttpServletResponse(), chain);

		verify(limiter).tryTake(eq(budget), anyString());
		assertThat(chain.getRequest()).as("admitted requests continue").isNotNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "/actuator", "/actuator/health", "/actuator/prometheus", "/v3/api-docs",
		"/v3/api-docs/swagger-config", "/swagger-ui.html", "/swagger-ui/index.html" })
	void rule1_operatorSurfacesAreNeitherChargedNorLimited(String uri) throws Exception {
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request(uri), new MockHttpServletResponse(), chain);

		verify(limiter, never()).tryTake(any(), anyString());
		assertThat(chain.getRequest()).isNotNull();
	}

	@ParameterizedTest
	@CsvSource(nullValues = "null", value = { "10.9.9.9, 203.0.113.7, 203.0.113.7",
		"10.9.9.9, '198.51.100.1, 203.0.113.7', 203.0.113.7", "10.9.9.9, null, 10.9.9.9",
		"10.0.0.5, 203.0.113.7, 10.0.0.5", "10.9.9.9, ' 203.0.113.7 ,  ', 203.0.113.7",
		"10.9.9.9, '203.0.113.7, 10.9.9.9', 203.0.113.7", "10.9.9.9, '10.9.9.9, 10.9.9.9', 10.9.9.9",
		"10.9.9.9, ' , ', 10.9.9.9" })
	void rule5_theClientIsThePeerOrTheRightMostUntrustedForwardedHop(String remote, String forwardedFor,
			String client) {
		assertThat(RateLimitFilter.clientOf(remote, forwardedFor, Set.of("10.9.9.9"))).isEqualTo(client);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void rule5_anAbsentOrEmptyHeaderFromATrustedProxyChargesTheProxy(String forwardedFor) {
		assertThat(RateLimitFilter.clientOf("10.9.9.9", forwardedFor, Set.of("10.9.9.9"))).isEqualTo("10.9.9.9");
	}

	@Test
	void theFilterReadsOnlyXForwardedForAndOnlyFromATrustedPeer() throws Exception {
		when(limiter.tryTake(any(), anyString())).thenReturn(0L);
		MockHttpServletRequest request = request("/api/links");
		request.setRemoteAddr("10.9.9.9");
		request.addHeader("X-Forwarded-For", "203.0.113.7");
		request.addHeader("Forwarded", "for=192.0.2.1");
		request.addHeader("X-Real-IP", "192.0.2.2");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		verify(limiter).tryTake(Budget.CREATE, "203.0.113.7");
	}

	@Test
	void anEmptyBucketIsA429ProblemWithRetryAfterAndNoChain() throws Exception {
		String requestId = UUID.randomUUID().toString();
		MDC.put("requestId", requestId);
		when(limiter.tryTake(Budget.REDIRECT, "10.77.77.77")).thenReturn(7L);
		MockHttpServletRequest request = request("/Abc12345");
		request.setRemoteAddr("10.77.77.77");
		request.addHeader("User-Agent", "canary-ua");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		ListAppender<ILoggingEvent> events = new ListAppender<>();
		events.start();
		Logger logger = (Logger) LoggerFactory.getLogger(RateLimitFilter.class);
		logger.addAppender(events);
		try {
			filter.doFilter(request, response, chain);
		}
		finally {
			logger.detachAppender(events);
		}

		assertThat(chain.getRequest()).as("the request goes no further").isNull();
		assertThat(response.getStatus()).isEqualTo(429);
		assertThat(response.getHeader("Retry-After")).isEqualTo("7");
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		JsonNode body = json.readTree(response.getContentAsString());
		assertThat(body.propertyNames()).containsExactlyInAnyOrder("instance", "status", "title");
		assertThat(body.get("status").asInt()).isEqualTo(429);
		assertThat(body.get("title").asString()).isEqualTo("Too Many Requests");
		assertThat(body.get("instance").asString()).isEqualTo("urn:uuid:" + requestId);
		assertThat(response.getContentAsString()).doesNotContain("10.77.77.77").doesNotContain("canary-ua");
		assertThat(events.list).as("rule 7: the limiter itself logs nothing").isEmpty();
	}

	@Test
	void eachRejectionIsCountedOnceUnderItsBudgetOnly() throws Exception {
		when(limiter.tryTake(any(), anyString())).thenReturn(1L);

		filter.doFilter(request("/api/links"), new MockHttpServletResponse(), new MockFilterChain());
		filter.doFilter(request("/api/links"), new MockHttpServletResponse(), new MockFilterChain());
		filter.doFilter(request("/Abc12345"), new MockHttpServletResponse(), new MockFilterChain());

		assertThat(registry.get("urlshort.ratelimit.rejections").tag("budget", "create").counter().count()).isEqualTo(2);
		assertThat(registry.get("urlshort.ratelimit.rejections").tag("budget", "redirect").counter().count())
				.isEqualTo(1);
		assertThat(registry.get("urlshort.ratelimit.rejections").counters()).allSatisfy(
				counter -> assertThat(counter.getId().getTags()).extracting("key").containsExactly("budget"));
	}

	private static MockHttpServletRequest request(String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
		request.setRemoteAddr("10.0.0.1");
		return request;
	}
}
