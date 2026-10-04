package dev.urlshort.click;

import dev.urlshort.web.Problems;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/links/{code}/stats}: the Analyst's read of a link's clicks (FR-8, AC-7 to AC-13,
 * AC-21, AC-22). Anonymous and read-only; a retired link's statistics stay readable. {@code HEAD} and
 * {@code OPTIONS} are the framework's; every other method is a {@code 405} problem detail.
 */
@RestController
class StatsController {

	private static final String EXAMPLE = "{\"code\":\"aB3dE5fG\",\"totalClicks\":6,\"clicksPerDay\":["
			+ "{\"date\":\"2026-10-01\",\"clicks\":6,\"uniqueVisitors\":3,\"botClicks\":1}],"
			+ "\"topReferrers\":[{\"referrer\":\"https://news.example.com\",\"clicks\":4}]}";

	private final ClickStore store;

	StatsController(ClickStore store) {
		this.store = store;
	}

	@GetMapping("/api/links/{code:[A-Za-z0-9]{6,32}}/stats")
	@Operation(summary = "Read a link's click statistics", responses = {
		@ApiResponse(responseCode = "200", description = "Total clicks; per UTC day the clicks, unique visitors and bot clicks; and the top 10 referrer origins",
				content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
						schema = @Schema(implementation = LinkStats.class),
						examples = @ExampleObject(name = "stats", value = EXAMPLE))),
		@ApiResponse(responseCode = "404", description = "No link has this code", content = @Content(
				mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))) })
	LinkStats stats(@PathVariable String code) {
		long linkId = store.findLinkId(code).orElseThrow(Problems::notFound);
		return LinkStats.of(code, store.stats(linkId));
	}
}
