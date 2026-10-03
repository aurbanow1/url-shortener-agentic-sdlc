package dev.urlshort.web;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The one error advice: every error response of the service is a problem detail built from
 * server-owned values only (business rule 8, NFR-R6).
 *
 * <p>The platform base class renders framework errors ({@code 400}, {@code 404}, {@code 405},
 * {@code 415}) and the domain {@link ErrorResponseException}s from {@link Problems}. This class adds
 * the catch-all {@code 500}, the {@code 413} unwrap for the body cap, and one rule for every body:
 * no {@code detail} (framework wording echoes paths, methods and headers) and {@code instance} =
 * {@code urn:uuid:<X-Request-Id>}. Defining it makes Boot's own {@code ProblemDetailsExceptionHandler}
 * back off. Decision records: ADR-0002 and ADR-0004 (amended for slice 01-create-redirect).
 */
@RestControllerAdvice
class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ProblemDetailsAdvice.class);

	/**
	 * Fails closed on anything no other handler claims: a bare {@code 500} and one ERROR event
	 * carrying only the exception class chain and the first frame of our code. The throwable is never
	 * given to the logger, because driver messages quote bound values (DR-01).
	 */
	@ExceptionHandler(Exception.class)
	ResponseEntity<Object> unhandled(Exception ex, WebRequest request) {
		log.atError().setMessage("request failed")
				.addKeyValue("errorChain", Stream.iterate((Throwable) ex, Objects::nonNull, Throwable::getCause).limit(8)
						.map(t -> t.getClass().getName()).collect(Collectors.joining(" <- ")))
				.addKeyValue("errorOrigin", Arrays.stream(ex.getStackTrace())
						.filter(frame -> frame.getClassName().startsWith("dev.urlshort."))
						.findFirst().map(StackTraceElement::toString).orElse("none"))
				.log();
		// a non-null body keeps the parent from flagging the request for the container's /error page
		return handleExceptionInternal(ex, ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR), new HttpHeaders(),
				HttpStatus.INTERNAL_SERVER_ERROR, request);
	}

	/**
	 * Renders the body cap's {@code 413} when the JSON reader wrapped it into an unreadable-body
	 * error; any other unreadable body stays the platform's {@code 400}.
	 */
	@Override
	protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
			if (cause instanceof ErrorResponseException limit) {
				return handleErrorResponseException(limit, limit.getHeaders(), limit.getStatusCode(), request);
			}
		}
		return super.handleHttpMessageNotReadable(ex, headers, status, request);
	}

	/**
	 * The last step every rendered error passes through: clears {@code detail} and sets
	 * {@code instance} to the request id, so no body can echo a submitted value. Headers the
	 * framework derived ({@code Allow}, {@code Accept}) are kept.
	 */
	@Override
	protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		if (body instanceof ProblemDetail problem) {
			problem.setDetail(null);
			problem.setInstance(URI.create("urn:uuid:" + MDC.get("requestId")));
		}
		return super.createResponseEntity(body, headers, statusCode, request);
	}
}
