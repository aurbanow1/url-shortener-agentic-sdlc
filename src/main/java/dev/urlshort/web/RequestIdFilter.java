package dev.urlshort.web;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * response, error paths included. When the chain returns or throws it writes one INFO event
 * {@code request completed} carrying {@code requestId} and the response {@code status} and nothing
 * client-controlled, then removes the MDC key (business rule 10, AC-26). Decision records: ADR-0003
 * and ADR-0004.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
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
			// the one event every request writes, also on paths no controller reaches (404, 405, 413, 415);
			// the method is not logged because it is a client-chosen token (ADR-0004)
			log.atInfo().setMessage("request completed").addKeyValue("status", response.getStatus()).log();
			MDC.remove(MDC_KEY);
		}
	}
}
