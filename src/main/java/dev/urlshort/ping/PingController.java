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

	@GetMapping("/api/ping")
	public PingResponse ping() {
		log.info("ping");
		return new PingResponse("ok", Instant.now().toString());
	}
}
