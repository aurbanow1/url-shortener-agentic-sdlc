package dev.urlshort.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;

import dev.urlshort.web.Problems.FieldError;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties.ForwardHeadersStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.ErrorResponseException;

/**
 * The audit read's request checks (SPEC rules 2 and 5, ADR-0019): the loopback test, the forwarded-header
 * strategy that must be {@code NONE}, and the hand parsing of {@code limit} and {@code cursor}.
 */
class AuditControllerTest {

	@ParameterizedTest
	@ValueSource(strings = { "127.0.0.1", "127.0.0.2", "127.255.255.254", "::1", "0:0:0:0:0:0:0:1", "::ffff:127.0.0.1" })
	void loopbackPeersAreAdmitted(String peer) {
		assertThat(AuditController.fromLoopback(request(peer))).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "192.0.2.10", "10.0.0.7", "::ffff:192.0.2.10", "fe80::1", "0.0.0.0", "::", "", "1::2::3" })
	void otherPeersAreRefused(String peer) {
		assertThat(AuditController.fromLoopback(request(peer))).isFalse();
	}

	@Test
	void aMissingPeerIsRefused() {
		MockHttpServletRequest request = request("127.0.0.1");
		request.setRemoteAddr(null);

		assertThat(AuditController.fromLoopback(request)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "X-Forwarded-For", "Forwarded" })
	void aForwardingHeaderRefusesEvenFromLoopback(String header) {
		MockHttpServletRequest request = request("127.0.0.1");
		request.addHeader(header, "127.0.0.1");

		assertThat(AuditController.fromLoopback(request)).isFalse();
	}

	@Test
	void theShippedStrategyAdmitsALoopbackRequest() {
		AuditController controller = controller(ForwardHeadersStrategy.NONE);

		assertThat(controller.page(null, null, request("127.0.0.1")).getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@ParameterizedTest
	@EnumSource(value = ForwardHeadersStrategy.class, names = { "NATIVE", "FRAMEWORK" })
	void anyOtherStrategyClosesTheEndpoint(ForwardHeadersStrategy strategy) {
		assertForbidden(controller(strategy));
	}

	@Test
	void anUnsetStrategyClosesTheEndpoint() {
		assertForbidden(controller(null));
	}

	@Test
	void limitDefaultsTo50AndAcceptsItsBounds() {
		assertThat(AuditController.limit(null)).isEqualTo(50);
		assertThat(AuditController.limit("1")).isEqualTo(1);
		assertThat(AuditController.limit("100")).isEqualTo(100);
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "101", "-1" })
	void limitOutsideItsRangeIsARangeProblem(String value) {
		assertProblem(() -> AuditController.limit(value), "limit", "range", value);
	}

	@ParameterizedTest
	@ValueSource(strings = { "ten", "", "1.5", "99999999999" })
	void aLimitThatIsNotAWholeNumberIsAFormatProblem(String value) {
		assertProblem(() -> AuditController.limit(value), "limit", "format", value);
	}

	@Test
	void aCursorIsTheBase64urlOfAPositiveId() {
		assertThat(AuditController.position("Mw")).isEqualTo(3);
		assertThat(AuditController.position(AuditTrail.cursorOf(9_000_000_000L))).isEqualTo(9_000_000_000L);
	}

	@ParameterizedTest
	@ValueSource(strings = { "***", "MA", "LTE", "eDE", "" })
	void aMalformedCursorIsAFormatProblem(String value) {
		assertProblem(() -> AuditController.position(value), "cursor", "format", value);
	}

	private static AuditController controller(@Nullable ForwardHeadersStrategy strategy) {
		ServerProperties server = new ServerProperties();
		server.setForwardHeadersStrategy(strategy);
		return new AuditController(mock(AuditTrail.class), server);
	}

	private static void assertForbidden(AuditController controller) {
		assertThatThrownBy(() -> controller.page(null, null, request("127.0.0.1")))
				.isInstanceOfSatisfying(ErrorResponseException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	private static void assertProblem(Runnable parse, String field, String rule, String value) {
		assertThatThrownBy(parse::run).isInstanceOfSatisfying(ErrorResponseException.class, ex -> {
			assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			@SuppressWarnings("unchecked")
			List<FieldError> errors = (List<FieldError>) ex.getBody().getProperties().get("errors");
			assertThat(errors).singleElement().satisfies(error -> {
				assertThat(error.field()).isEqualTo(field);
				assertThat(error.rule()).isEqualTo(rule);
				// a single character ("0") can occur in the static text itself ("1 to 100")
				if (value.length() > 1) {
					assertThat(error.message()).doesNotContain(value);
				}
			});
		});
	}

	private static MockHttpServletRequest request(String peer) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/audit");
		request.setRemoteAddr(peer);
		return request;
	}
}
