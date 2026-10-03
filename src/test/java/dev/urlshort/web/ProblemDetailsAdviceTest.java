package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.KeyValuePair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * The error advice in isolation: the body-limit unwrap, the catch-all {@code 500} and its
 * message-free log event, and the one rule applied to every problem body (no {@code detail},
 * {@code instance} = {@code urn:uuid:<request id>}).
 */
class ProblemDetailsAdviceTest {

	private static final String CANARY = "canary-" + UUID.randomUUID();

	private final ProblemDetailsAdvice advice = new ProblemDetailsAdvice();
	private final ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest(),
			new MockHttpServletResponse());
	private final String requestId = UUID.randomUUID().toString();
	private final ListAppender<ILoggingEvent> events = new ListAppender<>();
	private final Logger adviceLogger = (Logger) LoggerFactory.getLogger(ProblemDetailsAdvice.class);

	@BeforeEach
	void setUp() {
		MDC.put("requestId", requestId);
		events.start();
		adviceLogger.addAppender(events);
	}

	@AfterEach
	void tearDown() {
		adviceLogger.detachAppender(events);
		MDC.remove("requestId");
	}

	@Test
	void unwrapsABodyLimitErrorRaisedInsideTheJsonReaderTo413() throws Exception {
		HttpMessageNotReadableException wrapped = new HttpMessageNotReadableException("JSON parse error",
				new IllegalStateException(new ErrorResponseException(HttpStatus.CONTENT_TOO_LARGE)),
				new MockHttpInputMessage(new byte[0]));

		ResponseEntity<Object> response = advice.handleException(wrapped, request);

		assertThat(response.getStatusCode().value()).isEqualTo(413);
		assertServerOwnedProblem(response, 413);
	}

	@Test
	void unreadableBodyWithoutALimitErrorStays400WithoutTheFrameworkDetail() throws Exception {
		HttpMessageNotReadableException unreadable = new HttpMessageNotReadableException("JSON parse error " + CANARY,
				new IllegalArgumentException(CANARY), new MockHttpInputMessage(new byte[0]));

		ResponseEntity<Object> response = advice.handleException(unreadable, request);

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertServerOwnedProblem(response, 400);
	}

	@Test
	void frameworkDetailThatEchoesClientInputIsCleared() throws Exception {
		ResponseEntity<Object> response = advice.handleException(
				new HttpMediaTypeNotSupportedException(MediaType.parseMediaType("text/plain;note=" + CANARY),
						List.of(MediaType.APPLICATION_JSON)),
				request);

		assertThat(response.getStatusCode().value()).isEqualTo(415);
		assertThat(response.getHeaders().getAccept()).containsExactly(MediaType.APPLICATION_JSON);
		assertServerOwnedProblem(response, 415);
	}

	@Test
	void domainProblemKeepsItsErrorsAndGetsTheRequestIdAsInstance() throws Exception {
		ResponseEntity<Object> response = advice.handleException(Problems.validation("url", "scheme", "static"), request);

		ProblemDetail body = assertServerOwnedProblem(response, 400);
		assertThat(body.getProperties()).containsKey("errors");
	}

	@Test
	void unhandledExceptionIsABare500AndOneMessageFreeEvent() {
		IllegalStateException failure = new IllegalStateException(CANARY, new SQLException("duplicate '" + CANARY + "'"));

		ResponseEntity<Object> response = advice.unhandled(failure, request);

		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertServerOwnedProblem(response, 500);
		assertThat(events.list).singleElement().satisfies(event -> {
			assertThat(event.getFormattedMessage()).isEqualTo("request failed");
			assertThat(event.getThrowableProxy()).as("the throwable is never handed to the logger").isNull();
			assertThat(keyValues(event)).containsEntry("errorChain", "java.lang.IllegalStateException <- java.sql.SQLException");
			assertThat((String) keyValues(event).get("errorOrigin")).startsWith("dev.urlshort.web.ProblemDetailsAdviceTest.");
			assertThat(event.toString() + keyValues(event)).doesNotContain(CANARY);
		});
	}

	@Test
	void errorOriginIsNoneWhenNoFrameIsOurs() {
		IllegalStateException failure = new IllegalStateException("x");
		failure.setStackTrace(new StackTraceElement[] { new StackTraceElement("org.example.Lib", "run", "Lib.java", 1) });

		advice.unhandled(failure, request);

		assertThat(keyValues(events.list.getFirst())).containsEntry("errorOrigin", "none");
	}

	@Test
	void aNonProblemBodyPassesThroughUntouched() {
		ResponseEntity<Object> response = advice.createResponseEntity(null, new HttpHeaders(), HttpStatus.BAD_REQUEST,
				request);

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).isNull();
	}

	private ProblemDetail assertServerOwnedProblem(ResponseEntity<Object> response, int status) {
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail body = (ProblemDetail) response.getBody();
		assertThat(body.getStatus()).isEqualTo(status);
		assertThat(body.getDetail()).isNull();
		assertThat(body.getInstance()).isEqualTo(URI.create("urn:uuid:" + requestId));
		assertThat(body.toString()).doesNotContain(CANARY).doesNotContain("Exception");
		return body;
	}

	private static Map<String, Object> keyValues(ILoggingEvent event) {
		Map<String, Object> values = new HashMap<>();
		for (KeyValuePair pair : event.getKeyValuePairs()) {
			values.put(pair.key, pair.value);
		}
		return values;
	}
}
