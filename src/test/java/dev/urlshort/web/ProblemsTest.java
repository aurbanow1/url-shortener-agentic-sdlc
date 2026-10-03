package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** The problem factories: status, title and the {@code errors} extension (business rule 8). */
class ProblemsTest {

	@Test
	void validationIs400WithExactlyOneFieldError() {
		ErrorResponseException problem = Problems.validation("url", "scheme", "url must start with http:// or https://");

		ProblemDetail body = problem.getBody();
		assertThat(problem.getStatusCode().value()).isEqualTo(400);
		assertThat(body.getStatus()).isEqualTo(400);
		assertThat(body.getTitle()).isEqualTo("Bad Request");
		assertThat(body.getDetail()).isNull();
		assertThat(body.getProperties()).containsEntry("errors",
				List.of(new Problems.FieldError("url", "scheme", "url must start with http:// or https://")));
	}

	@Test
	void notFoundAndGoneAreBareProblems() {
		assertThat(Problems.notFound().getBody().getStatus()).isEqualTo(404);
		assertThat(Problems.notFound().getBody().getProperties()).isNull();
		assertThat(Problems.gone().getBody().getStatus()).isEqualTo(410);
		assertThat(Problems.gone().getBody().getTitle()).isEqualTo("Gone");
		assertThat(Problems.gone().getBody().getProperties()).isNull();
	}

	@Test
	void idempotencyMismatchIs422NamingTheHeaderAndRule() {
		ProblemDetail body = Problems.idempotencyMismatch().getBody();

		assertThat(body.getStatus()).isEqualTo(422);
		@SuppressWarnings("unchecked")
		List<Problems.FieldError> errors = (List<Problems.FieldError>) body.getProperties().get("errors");
		assertThat(errors).singleElement().satisfies(error -> {
			assertThat(error.field()).isEqualTo("Idempotency-Key");
			assertThat(error.rule()).isEqualTo("mismatch");
			assertThat(error.message()).isNotBlank();
		});
	}
}
