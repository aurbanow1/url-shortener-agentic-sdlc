package dev.urlshort.link;

import dev.urlshort.click.ClickRecorder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Visitor's path {@code GET /{code}} (AC-12 to AC-14, business rule 7): {@code 302} to the stored
 * target with {@code Cache-Control: no-store}, so every click reaches the service (ADR-0006). The code
 * pattern keeps the route off {@code /api}, {@code /actuator}'s sub-paths, {@code /v3} and any path
 * with a dot; the query string of the short link is ignored. After a successful resolve, the redirect
 * hands itself to {@link ClickRecorder} (slice 02-analytics), which returns at once and never fails it.
 */
@RestController
class RedirectController {

	private final LinkService links;
	private final ClickRecorder clicks;

	RedirectController(LinkService links, ClickRecorder clicks) {
		this.links = links;
		this.clicks = clicks;
	}

	@GetMapping("/{code:[A-Za-z0-9]{6,32}}")
	@Operation(summary = "Open a short link", responses = {
		@ApiResponse(responseCode = "302", description = "Redirect to the stored url, not cacheable",
				headers = {
					@Header(name = "Location", description = "The stored url, byte for byte",
							schema = @Schema(type = "string")),
					@Header(name = "Cache-Control", description = "no-store", schema = @Schema(type = "string")) }),
		@ApiResponse(responseCode = "404", description = "No link has this code", content = @Content(
				mediaType = LinkController.PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "410", description = "The link was retired", content = @Content(
				mediaType = LinkController.PROBLEM, schema = @Schema(implementation = ProblemDetail.class))) })
	ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
		Link link = links.resolve(code);
		clicks.record(link.id(), request);
		// Location as a String: setLocation(URI) would re-encode the stored value
		return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, link.url())
				.cacheControl(CacheControl.noStore()).build();
	}
}
