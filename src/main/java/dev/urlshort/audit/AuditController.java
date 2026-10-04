package dev.urlshort.audit;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import dev.urlshort.web.ClientIdentity;
import dev.urlshort.web.Problems;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/audit}: the Operator's read of the audit trail (FR-17), newest first, one keyset page
 * at a time. Anonymous, read-only and loopback only (NFR-S6, ADR-0019): a request is served only when
 * its peer is a loopback address, it carries no {@code X-Forwarded-For} or {@code Forwarded} header, and
 * nothing rewrites the peer from those headers (the effective forwarded-header strategy is
 * {@code NONE} and no {@code server.tomcat.remoteip} header is set). Any other request is a {@code 403} problem, decided before validation and before
 * content negotiation, so no setting and no {@code Accept} can open the trail.
 */
@RestController
// ServerProperties and TomcatServerProperties, which decide the guard, exist only in a servlet application
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class AuditController {

	static final int DEFAULT_LIMIT = 50;
	static final int MAX_LIMIT = 100;

	private static final String EXAMPLE = "{\"items\":[{\"occurredAt\":\"2026-10-03T17:29:41.033Z\",\"actor\":\"anonymous\","
			+ "\"action\":\"link.retire\",\"entity\":\"link\",\"entityId\":\"jHkOIMpK\","
			+ "\"requestId\":\"140fc9de-5b8e-4c43-9d55-2f0b1e1c7a10\","
			+ "\"before\":{\"url\":\"https://example.com/\",\"state\":\"active\"},"
			+ "\"after\":{\"url\":\"https://example.com/\",\"state\":\"retired\"}},"
			+ "{\"occurredAt\":\"2026-10-03T17:28:02.512Z\",\"actor\":\"anonymous\",\"action\":\"link.create\","
			+ "\"entity\":\"link\",\"entityId\":\"jHkOIMpK\",\"requestId\":\"0d6a1f3e-9c27-4b1a-8e5f-6a4c3b2d1e0f\","
			+ "\"before\":null,\"after\":{\"url\":\"https://example.com/\",\"state\":\"active\"}}],\"next\":\"MTI\"}";
	private static final String PROBLEM_JSON = "application/problem+json";

	private final AuditTrail trail;
	private final boolean peerIsConnection;

	AuditController(AuditTrail trail, ServerProperties server, TomcatServerProperties tomcat) {
		this.trail = trail;
		this.peerIsConnection = ClientIdentity.peerIsConnection(server, tomcat);
	}

	@GetMapping("/api/audit")
	@Operation(operationId = "readAudit", summary = "Read the audit trail, newest first (loopback only)", responses = {
		@ApiResponse(responseCode = "200", description = "One page of audit rows, newest first, and the cursor of the next",
				content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuditPage.class),
						examples = @ExampleObject(name = "page", value = EXAMPLE))),
		@ApiResponse(responseCode = "400", description = "limit or cursor is malformed", content = @Content(
				mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "403", description = "Not a loopback client, or a forwarding header is present",
				content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "500", description = "The trail could not be read", content = @Content(
				mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))) })
	ResponseEntity<AuditPage> page(
			@Parameter(description = "Rows per page", schema = @Schema(type = "integer", minimum = "1", maximum = "100",
					defaultValue = "50")) @RequestParam(name = "limit", required = false) @Nullable String limit,
			@Parameter(description = "The next of the previous page") @RequestParam(name = "cursor", required = false)
			@Nullable String cursor,
			HttpServletRequest request) {
		if (!peerIsConnection || !ClientIdentity.fromLoopback(request)) {
			throw new ErrorResponseException(HttpStatus.FORBIDDEN);
		}
		int size = limit(limit);
		long before = cursor == null ? Long.MAX_VALUE : position(cursor);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(trail.page(size, before));
	}

	/** Delegates to {@link ClientIdentity#fromLoopback}, the predicate's one home; removed once the tests name it. */
	static boolean fromLoopback(HttpServletRequest request) {
		return ClientIdentity.fromLoopback(request);
	}

	/** {@code null} is the default; otherwise a whole number from 1 to 100. */
	static int limit(@Nullable String value) {
		if (value == null) {
			return DEFAULT_LIMIT;
		}
		int parsed;
		try {
			parsed = Integer.parseInt(value);
		}
		catch (NumberFormatException ex) {
			throw Problems.validation("limit", "format", "must be a whole number");
		}
		if (parsed < 1 || parsed > MAX_LIMIT) {
			throw Problems.validation("limit", "range", "must be from 1 to 100");
		}
		return parsed;
	}

	/** The row id a cursor names: base64url of a positive decimal number, as {@link AuditTrail#cursorOf} writes it. */
	static long position(String cursor) {
		try {
			long id = Long.parseLong(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.US_ASCII));
			if (id > 0) {
				return id;
			}
		}
		catch (IllegalArgumentException ex) {
			// not base64url, or not a number (NumberFormatException): answered below
		}
		throw Problems.validation("cursor", "format", "must be a value returned as next");
	}
}
