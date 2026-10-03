package dev.urlshort.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * The domain failures of the service as RFC 9457 problems, in one place. Each factory returns an
 * {@link ErrorResponseException} for a feature to throw; the platform part of
 * {@code ProblemDetailsAdvice} renders it as {@code application/problem+json} with the status as
 * both HTTP status and {@code status} member.
 *
 * <p>Validation failures carry an {@code errors} array of {@link FieldError}; messages are static
 * texts chosen by the caller and must never contain the value the client submitted (business rule
 * 8). Decision record: ADR-0002 (amended for slice 01-create-redirect).
 */
public final class Problems {

	private Problems() {
	}

	/**
	 * A {@code 400 Bad Request} naming exactly one rejected input.
	 *
	 * @param field the JSON member or header name the client sent, for example {@code url}
	 * @param rule the stable rule token from the SPEC, for example {@code scheme}
	 * @param message free text for humans; never interpolates the submitted value
	 * @return the problem to throw; never {@code null}
	 */
	public static ErrorResponseException validation(String field, String rule, String message) {
		return withErrors(HttpStatus.BAD_REQUEST, new FieldError(field, rule, message));
	}

	/**
	 * A bare {@code 404 Not Found}: no resource has the identifier in the path.
	 *
	 * @return the problem to throw; never {@code null}
	 */
	public static ErrorResponseException notFound() {
		return new ErrorResponseException(HttpStatus.NOT_FOUND);
	}

	/**
	 * A bare {@code 410 Gone}: the resource existed and was retired; it will not come back.
	 *
	 * @return the problem to throw; never {@code null}
	 */
	public static ErrorResponseException gone() {
		return new ErrorResponseException(HttpStatus.GONE);
	}

	/**
	 * A {@code 422 Unprocessable Content}: the {@code Idempotency-Key} is already bound to a create
	 * with a different {@code url} (rule token {@code mismatch}).
	 *
	 * @return the problem to throw; never {@code null}
	 */
	public static ErrorResponseException idempotencyMismatch() {
		return withErrors(HttpStatus.UNPROCESSABLE_CONTENT, new FieldError("Idempotency-Key", "mismatch",
				"Idempotency-Key was already used with a different url"));
	}

	private static ErrorResponseException withErrors(HttpStatus status, FieldError error) {
		ProblemDetail body = ProblemDetail.forStatus(status);
		body.setProperty("errors", List.of(error));
		return new ErrorResponseException(status, body, null);
	}

	/**
	 * One element of a problem's {@code errors} array.
	 *
	 * @param field the JSON member or header name that was rejected
	 * @param rule the stable rule token the client can branch on
	 * @param message free text for humans, never containing the submitted value
	 */
	public record FieldError(String field, String rule, String message) {
	}
}
