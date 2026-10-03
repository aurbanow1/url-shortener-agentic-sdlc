package dev.urlshort.ping;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/ping}: answers {@code ok} with the server's current UTC time. */
@RestController
public class PingController {

	private static final Logger log = LoggerFactory.getLogger(PingController.class);

	/** Stateless; created by Spring. */
	public PingController() {
	}

	/**
	 * Liveness answer for clients (00-hello slice 01-ping, AC-1 and AC-2): writes one INFO event
	 * {@code ping} carrying the request id from the MDC and nothing client-controlled.
	 *
	 * @return {@code status} {@code ok} and {@code time}, the current instant as ISO-8601 UTC with
	 *         {@code Z}; never {@code null}
	 */
	@GetMapping("/api/ping")
	public PingResponse ping() {
		log.info("ping");
		return new PingResponse("ok", Instant.now().toString());
	}
}
