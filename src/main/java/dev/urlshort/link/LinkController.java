package dev.urlshort.link;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Creator's API under {@code /api/links}: create, read and retire. HTTP in and out only; the
 * rules live in {@link LinkValidation} and {@link LinkService}. Every error is a problem detail
 * rendered by the web package's advice. {@code Host} and forwarding headers are never read (AC-3).
 */
@RestController
@RequestMapping("/api/links")
class LinkController {

	static final String PROBLEM = "application/problem+json";
	static final String LINK_EXAMPLE = "{\"code\":\"Ab3dE9fG\",\"shortUrl\":\"http://localhost:8080/Ab3dE9fG\","
			+ "\"url\":\"https://example.com/some/path?q=1\",\"state\":\"active\",\"createdAt\":\"2026-10-03T05:12:42.133Z\"}";

	private final LinkService links;
	private final LinkProperties properties;

	LinkController(LinkService links, LinkProperties properties) {
		this.links = links;
		this.properties = properties;
	}

	/** AC-1 to AC-7, AC-17 to AC-21: {@code 201} with {@code Location: /api/links/<code>}. */
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Create a short link for an http(s) URL",
			description = "Without Idempotency-Key every call creates a new link. With a key, a repeat with the same "
					+ "url within 24 h of the first 201 returns that link (current state); a different url is 422.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = CreateLinkRequest.class),
							examples = @ExampleObject(name = "create",
									value = "{\"url\":\"https://example.com/some/path?q=1\"}"))),
			responses = {
				@ApiResponse(responseCode = "201", description = "Created, or replayed for the same key and url",
						headers = @Header(name = "Location", description = "/api/links/{code}",
								schema = @Schema(type = "string")),
						content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
								schema = @Schema(implementation = LinkResponse.class),
								examples = @ExampleObject(name = "created", value = LINK_EXAMPLE))),
				@ApiResponse(responseCode = "400", description = "Invalid url or Idempotency-Key (errors[] names field "
						+ "and rule), or a body that is not a JSON object", content = @Content(mediaType = PROBLEM,
								schema = @Schema(implementation = ProblemDetail.class))),
				@ApiResponse(responseCode = "413", description = "Body larger than 16 384 bytes",
						content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
				@ApiResponse(responseCode = "415", description = "Content-Type is not application/json",
						content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
				@ApiResponse(responseCode = "422", description = "Idempotency-Key already used with a different url",
						content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
				@ApiResponse(responseCode = "500", description = "Nothing was stored",
						content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))) })
	ResponseEntity<LinkResponse> createLink(@RequestBody CreateLinkRequest request,
			@Parameter(description = "1 to 255 visible ASCII characters; makes a retried create return the first link")
			@RequestHeader(name = "Idempotency-Key", required = false) @Nullable String idempotencyKey) {
		String url = request.url();
		LinkValidation.validateUrl(url);
		LinkValidation.validateIdempotencyKey(idempotencyKey);
		Link link = links.create(url, idempotencyKey);
		return ResponseEntity.created(URI.create("/api/links/" + link.code())).body(response(link));
	}

	/** AC-8, AC-9, AC-14: the current representation, retired links included. */
	@GetMapping("/{code:[A-Za-z0-9]{6,32}}")
	@Operation(summary = "Read a link by its code", responses = {
		@ApiResponse(responseCode = "200", description = "The link; state is active or retired",
				content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
						schema = @Schema(implementation = LinkResponse.class),
						examples = @ExampleObject(name = "link", value = LINK_EXAMPLE))),
		@ApiResponse(responseCode = "404", description = "No link has this code",
				content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))) })
	LinkResponse readLink(@PathVariable String code) {
		return response(links.read(code));
	}

	/** AC-10, AC-11, AC-14: {@code 204}; {@code 410} when already retired. */
	@DeleteMapping("/{code:[A-Za-z0-9]{6,32}}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Retire a link; visitors then get 410 and the record is kept", responses = {
		@ApiResponse(responseCode = "204", description = "Retired"),
		@ApiResponse(responseCode = "404", description = "No link has this code",
				content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "410", description = "Already retired",
				content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "500", description = "Nothing was changed",
				content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))) })
	void retireLink(@PathVariable String code) {
		links.retire(code);
	}

	private LinkResponse response(Link link) {
		return LinkResponse.of(link, properties.publicBaseUrl());
	}
}
