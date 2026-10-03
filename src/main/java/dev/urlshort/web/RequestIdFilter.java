package dev.urlshort.web;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Issues a fresh server-side id for every request, exposes it as the {@code X-Request-Id}
 * response header before the chain runs and makes it available to log events through the MDC key
 * {@code requestId}. Inbound correlation headers are deliberately never read.
 *
 * <p>Runs first in the chain ({@link Ordered#HIGHEST_PRECEDENCE}) so the id exists on every
 * response, error paths included, and the MDC key is removed when the request ends. Decision
 * record: ADR-0003 ({@code docs/adr/0003-request-id-server-issued.md}).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	private static final String HEADER = "X-Request-Id";
	private static final String MDC_KEY = "requestId";

	/** Stateless; created by Spring and registered for every request. */
	public RequestIdFilter() {
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String requestId = UUID.randomUUID().toString();
		response.setHeader(HEADER, requestId);
		MDC.put(MDC_KEY, requestId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			MDC.remove(MDC_KEY);
		}
	}
}
