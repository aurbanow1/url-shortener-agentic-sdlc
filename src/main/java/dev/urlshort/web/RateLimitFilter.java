package dev.urlshort.web;

import java.io.IOException;
import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import dev.urlshort.web.RateLimiter.Budget;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Charges every request except the operator surfaces to its client's bucket and answers {@code 429}
 * when the bucket is empty (FR-10, NFR-R2, business rules 1 to 7; ADR-0014, ADR-0015).
 *
 * <p>Order: after {@link RequestIdFilter} (so the {@code 429} carries its request id) and Boot's
 * observation filter (so {@code 429}s appear in {@code http.server.requests}), before
 * {@link RequestBodyLimitFilter} and everything else, so a request is charged before its body,
 * method, content type or path are examined. The {@code 429} is written here, in the same shape as
 * every other problem body; this filter writes no log event, the request's one event is the
 * request-id filter's {@code request completed} with status {@code 429}.
 *
 * <p>Every charged request carries its client in {@link #CLIENT_ATTRIBUTE}; the peer address itself
 * is never rewritten, so every other reader of {@code getRemoteAddr()} sees the real peer
 * (ADR-0015 amendment, ADR-0019).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RateLimitFilter extends OncePerRequestFilter {

	/** Request attribute holding the client this request was charged to (ADR-0015); read by the click recorder. */
	public static final String CLIENT_ATTRIBUTE = RateLimitFilter.class.getName() + ".client";

	private final RateLimiter limiter;
	private final Set<String> trustedProxies;
	private final JsonMapper json;
	private final Map<Budget, Counter> rejections = new EnumMap<>(Budget.class);

	RateLimitFilter(RateLimiter limiter, RateLimitProperties properties, MeterRegistry registry, JsonMapper json) {
		this.limiter = limiter;
		this.trustedProxies = properties.trustedProxies();
		this.json = json;
		for (Budget budget : Budget.values()) {
			rejections.put(budget, Counter.builder("urlshort.ratelimit.rejections")
					.description("Requests answered 429 because the client's bucket was empty")
					.tag("budget", budget.tag()).register(registry));
		}
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		// the decoded path MVC routes on, so percent-encoding cannot move a request to the larger budget
		String path = UrlPathHelper.defaultInstance.getLookupPathForRequest(request);
		if (exempt(path)) {
			chain.doFilter(request, response);
			return;
		}
		Budget budget = path.equals("/api") || path.startsWith("/api/") ? Budget.CREATE : Budget.REDIRECT;
		String client = clientOf(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"), trustedProxies);
		// left for the click recorder, so a click's hashed client is the client charged here (ADR-0015)
		request.setAttribute(CLIENT_ATTRIBUTE, client);
		long retryAfter = limiter.tryTake(budget, client);
		if (retryAfter == 0) {
			chain.doFilter(request, response);
			return;
		}
		rejections.get(budget).increment();
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
		problem.setInstance(URI.create("urn:uuid:" + MDC.get("requestId")));
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
		response.setContentType("application/problem+json");
		// bytes, not getWriter(): the writer makes Tomcat append ";charset=ISO-8859-1", unlike every other problem
		response.getOutputStream().write(json.writeValueAsBytes(problem));
	}

	/** Probes, scrapers and the API document are never limited (business rule 1, A-7). */
	static boolean exempt(String path) {
		return path.equals("/actuator") || path.startsWith("/actuator/") || path.startsWith("/v3/api-docs")
				|| path.equals("/swagger-ui.html") || path.startsWith("/swagger-ui/");
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
}
