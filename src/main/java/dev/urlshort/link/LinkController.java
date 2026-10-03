package dev.urlshort.link;

import java.net.URI;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

	private final LinkService links;
	private final LinkProperties properties;

	LinkController(LinkService links, LinkProperties properties) {
		this.links = links;
		this.properties = properties;
	}

	/** AC-1 to AC-7, AC-17 to AC-21: {@code 201} with {@code Location: /api/links/<code>}. */
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<LinkResponse> createLink(@RequestBody CreateLinkRequest request,
			@RequestHeader(name = "Idempotency-Key", required = false) @Nullable String idempotencyKey) {
		String url = request.url();
		LinkValidation.validateUrl(url);
		LinkValidation.validateIdempotencyKey(idempotencyKey);
		Link link = links.create(url, idempotencyKey);
		return ResponseEntity.created(URI.create("/api/links/" + link.code())).body(response(link));
	}

	/** AC-8, AC-9, AC-14: the current representation, retired links included. */
	@GetMapping("/{code:[A-Za-z0-9]{6,32}}")
	LinkResponse readLink(@PathVariable String code) {
		return response(links.read(code));
	}

	/** AC-10, AC-11, AC-14: {@code 204}; {@code 410} when already retired. */
	@DeleteMapping("/{code:[A-Za-z0-9]{6,32}}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void retireLink(@PathVariable String code) {
		links.retire(code);
	}

	private LinkResponse response(Link link) {
		return LinkResponse.of(link, properties.publicBaseUrl());
	}
}
