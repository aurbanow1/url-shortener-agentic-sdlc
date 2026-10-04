package dev.urlshort.web;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties.ForwardHeadersStrategy;
import org.springframework.util.StringUtils;

/**
 * Who a request's client is, in this service's two senses (ADR-0015, ADR-0019; architecture.md section
 * 11, row 1). They are deliberately different questions with separate methods:
 * <ul>
 * <li>the resolved client, which rate limits and click counts use: the peer, or behind a listed trusted
 * proxy the right-most untrusted {@code X-Forwarded-For} entry ({@link #of});</li>
 * <li>the direct peer, which the audit read uses: the connection's own loopback address, with no
 * forwarding header and nothing configured to rewrite it ({@link #peerIsConnection},
 * {@link #fromLoopback}). The trusted-proxy list never takes part.</li>
 * </ul>
 * Static and stateless: the rules hold no state, and the settings they read are passed in by their one
 * user each. The rules are pinned by ClientIdentityTest, RateLimitFilterTest, AuditControllerTest and
 * ClickRecorderTest, which test them through these methods.
 */
public final class ClientIdentity {

	/** Request attribute holding the client a request was charged to; set by the rate limiter, read by {@link #of}. */
	public static final String CLIENT_ATTRIBUTE = ClientIdentity.class.getName() + ".client";

	private ClientIdentity() {
	}

	/**
	 * The client a request is charged to (business rule 5): the peer address, unless the peer is a
	 * trusted proxy; then the right-most {@code X-Forwarded-For} entry that is not itself a trusted
	 * proxy, or the peer when there is none. No other header is read.
	 */
	static String clientOf(String remote, @Nullable String forwardedFor, Set<String> trusted) {
		if (!trusted.contains(remote) || forwardedFor == null) {
			return remote;
		}
		String[] hops = forwardedFor.split(",");
		for (int i = hops.length - 1; i >= 0; i--) {
			String hop = hops[i].trim();
			if (!hop.isEmpty() && !trusted.contains(hop)) {
				return hop;
			}
		}
		return remote;
	}

	/** The request's client by {@link #clientOf}, left on the request in {@link #CLIENT_ATTRIBUTE}. */
	static String resolve(HttpServletRequest request, Set<String> trusted) {
		String client = clientOf(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"), trusted);
		// left for the click recorder, so a click's hashed client is the client charged here (ADR-0015)
		request.setAttribute(CLIENT_ATTRIBUTE, client);
		return client;
	}

	/**
	 * The client the rate limiter charged this request to, for counting it as a visitor (ADR-0015,
	 * analytics-v2 rule 6).
	 *
	 * @param request the request
	 * @return the client recorded in {@link #CLIENT_ATTRIBUTE}, or the peer address where the limiter did
	 *         not run; never a forwarded value the limiter did not believe
	 */
	public static String of(HttpServletRequest request) {
		return request.getAttribute(CLIENT_ATTRIBUTE) instanceof String client ? client : request.getRemoteAddr();
	}

	/**
	 * Whether the servlet container reports the connection's own peer: the negation of Boot 4.1.1's
	 * condition for installing Tomcat's {@code RemoteIpValve}, which rewrites the peer from a header
	 * (ADR-0019, CR-01). The audit read is closed whenever this is {@code false}.
	 *
	 * @param server the server settings, whose forward-headers strategy must be {@code NONE}
	 * @param tomcat the Tomcat settings, neither of whose {@code remoteip} headers may have text
	 * @return {@code true} only when nothing can rewrite the peer from a header
	 */
	public static boolean peerIsConnection(ServerProperties server, TomcatServerProperties tomcat) {
		// the negation of Boot 4.1.1's condition for installing Tomcat's RemoteIpValve, which rewrites the
		// peer from a header: any other strategy (or the platform default when unset), or either remoteip
		// header setting. ponytail: mirrors Boot's trigger list; a Boot upgrade that adds one adds it here
		return server.getForwardHeadersStrategy() == ForwardHeadersStrategy.NONE
				&& !StringUtils.hasText(tomcat.getRemoteip().getRemoteIpHeader())
				&& !StringUtils.hasText(tomcat.getRemoteip().getProtocolHeader());
	}

	/**
	 * A loopback peer and no forwarding header: forwarding headers can only refuse (audit-read SPEC
	 * rule 2, ADR-0019). The trusted-proxy list is never consulted.
	 *
	 * @param request the request
	 * @return {@code true} when the peer is a loopback address and neither {@code X-Forwarded-For} nor
	 *         {@code Forwarded} is present, whatever its value
	 */
	public static boolean fromLoopback(HttpServletRequest request) {
		String peer = request.getRemoteAddr();
		if (request.getHeader("X-Forwarded-For") != null || request.getHeader("Forwarded") != null || peer == null
				|| peer.isEmpty()) {
			return false;
		}
		try {
			// the servlet container gives a numeric literal, which is parsed, never resolved
			return InetAddress.getByName(peer).isLoopbackAddress();
		}
		catch (UnknownHostException ex) {
			return false;
		}
	}
}
