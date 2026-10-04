package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * {@link ClientIdentity}'s rules, written as characterization before 06-client-identity moved them there
 * (docs/guidance/brownfield.md section 6): the trusted-proxy rule ({@code clientOf}, ADR-0015) over the
 * SPEC's identity matrix and its remaining edges, and the audit read's loopback predicate
 * ({@code fromLoopback}, ADR-0019) over its peers and forwarding headers. Every expected answer is the
 * one the code gave before the move.
 */
class ClientIdentityTest {

	private static final String P = "10.9.9.9";
	private static final String Q = "10.9.9.8";
	private static final String U = "203.0.113.7";
	private static final String V = "203.0.113.8";

	/** SPEC AC-4/AC-5 matrix: trusted list, peer, X-Forwarded-For (null = absent), the client charged. */
	static Stream<Arguments> identityMatrix() {
		return Stream.of(
				Arguments.of("", P, U, P),
				Arguments.of(P, "10.0.0.5", U, "10.0.0.5"),
				Arguments.of(P, P, U, U),
				Arguments.of(P, P, "198.51.100.1, " + U, U),
				Arguments.of(P + "," + Q, P, V + ", " + U + ", " + Q, U),
				Arguments.of(P, P, "  " + U + " ,  ", U),
				Arguments.of(P + "," + Q, P, P + ", " + Q, P),
				Arguments.of(P, P, null, P),
				Arguments.of(P, P, "", P),
				Arguments.of(P, P, " , ", P),
				Arguments.of("", "127.0.0.1", null, "127.0.0.1"),
				Arguments.of("", "::1", U, "::1"));
	}

	/** The rule's remaining edges (design section 5.1, U4 to U10), with the same columns. */
	static Stream<Arguments> ruleEdges() {
		return Stream.of(
				Arguments.of(P, P, U + ", " + V, V),
				Arguments.of(P, P, U + ",,", U),
				Arguments.of(P, P, "\t" + U + "\t", U),
				Arguments.of(P, P, "unknown", "unknown"),
				Arguments.of(P, P, "[2001:db8::1]:443", "[2001:db8::1]:443"),
				Arguments.of(P, P, "203.0.113.7:8080", "203.0.113.7:8080"),
				// exact text: Tomcat's full form of the IPv6 loopback does not match a listed "::1"
				Arguments.of("::1", "0:0:0:0:0:0:0:1", U, "0:0:0:0:0:0:0:1"));
	}

	@ParameterizedTest
	@MethodSource({ "identityMatrix", "ruleEdges" })
	void theChargedClientIsThePeerOrTheRightMostUntrustedForwardedHop(String trusted, String peer, String forwardedFor,
			String client) {
		assertThat(ClientIdentity.clientOf(peer, forwardedFor, trustedSet(trusted))).isEqualTo(client);
	}

	@ParameterizedTest
	@ValueSource(strings = { "127.0.0.1", "127.0.0.2", "127.255.255.254", "::1", "0:0:0:0:0:0:0:1", "::ffff:127.0.0.1" })
	void aHeaderlessLoopbackPeerIsFromLoopback(String peer) {
		assertThat(ClientIdentity.fromLoopback(request(peer, null, null))).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "192.0.2.10", "10.0.0.7", "::ffff:192.0.2.10", "fe80::1", "" })
	void anyOtherPeerIsNotFromLoopback(String peer) {
		assertThat(ClientIdentity.fromLoopback(request(peer, null, null))).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "203.0.113.7", "127.0.0.1", "", "   " })
	void anyXForwardedForHeaderClosesTheLoopbackPredicate(String value) {
		assertThat(ClientIdentity.fromLoopback(request("127.0.0.1", value, null))).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "for=203.0.113.7", "for=127.0.0.1", "", "   " })
	void anyForwardedHeaderClosesTheLoopbackPredicate(String value) {
		assertThat(ClientIdentity.fromLoopback(request("127.0.0.1", null, value))).isFalse();
	}

	private static Set<String> trustedSet(String trusted) {
		return Arrays.stream(trusted.split(",")).filter(entry -> !entry.isEmpty()).collect(Collectors.toSet());
	}

	private static MockHttpServletRequest request(String peer, String forwardedFor, String forwarded) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/audit");
		request.setRemoteAddr(peer);
		if (forwardedFor != null) {
			request.addHeader("X-Forwarded-For", forwardedFor);
		}
		if (forwarded != null) {
			request.addHeader("Forwarded", forwarded);
		}
		return request;
	}
}
