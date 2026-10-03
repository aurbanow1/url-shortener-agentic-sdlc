package dev.urlshort.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;

import dev.urlshort.web.Problems;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.ErrorResponseException;

/** Business rules 3 and 5: every AC-4 and AC-20 input maps to exactly one token, in order. */
class LinkValidationTest {

	private static final String BASE = "https://example.com/";

	static Stream<Arguments> rejectedUrls() {
		return Stream.of(
				Arguments.of(null, "required"),
				Arguments.of("", "required"),
				Arguments.of("   ", "required"),
				Arguments.of(BASE + "a".repeat(2049 - BASE.length()), "too-long"),
				Arguments.of("javascript:" + "a".repeat(3000), "too-long"),
				Arguments.of("javascript:alert(1)", "scheme"),
				Arguments.of("data:text/html,hi", "scheme"),
				Arguments.of("file:///etc/passwd", "scheme"),
				Arguments.of("ftp://example.com/", "scheme"),
				Arguments.of("example.com/path", "scheme"),
				Arguments.of(" " + BASE, "scheme"),
				Arguments.of("https://", "malformed"),
				Arguments.of("https://exa mple.com/", "malformed"),
				Arguments.of("https://[bad/", "malformed"),
				Arguments.of(BASE + " ", "malformed"),
				Arguments.of("https://example.com/\r\nSet-Cookie:x", "malformed"),
				Arguments.of("https://exämple.com/", "malformed"),
				Arguments.of("https:///no-host", "malformed"),
				Arguments.of("https://user:secret@example.com/", "credentials"),
				Arguments.of("https://user@example.com/", "credentials"));
	}

	@ParameterizedTest
	@MethodSource("rejectedUrls")
	void eachRejectedUrlFailsExactlyItsRule(String url, String rule) {
		assertThatThrownBy(() -> LinkValidation.validateUrl(url))
				.isInstanceOfSatisfying(ErrorResponseException.class, problem -> {
					assertThat(problem.getStatusCode().value()).isEqualTo(400);
					assertThat(errors(problem)).singleElement().satisfies(error -> {
						assertThat(error.field()).isEqualTo("url");
						assertThat(error.rule()).isEqualTo(rule);
						if (url != null && !url.isBlank()) {
							assertThat(error.message()).doesNotContain(url.strip());
						}
					});
				});
	}

	@ParameterizedTest
	@ValueSource(strings = { "https://example.com/some/path?q=1", "http://example.com/", "HTTPS://EXAMPLE.COM/",
			"http://localhost:8080/", "https://example.com/p?q=a%20b#frag" })
	void validUrlsPass(String url) {
		assertThatCode(() -> LinkValidation.validateUrl(url)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(ints = { 2048 })
	void exactly2048CharactersIsAccepted(int length) {
		assertThatCode(() -> LinkValidation.validateUrl(BASE + "a".repeat(length - BASE.length())))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "has space", "café", "tab\there" })
	void malformedKeysFailFormat(String key) {
		assertThatThrownBy(() -> LinkValidation.validateIdempotencyKey(key))
				.isInstanceOfSatisfying(ErrorResponseException.class, problem -> assertThat(errors(problem))
						.singleElement().satisfies(error -> {
							assertThat(error.field()).isEqualTo("Idempotency-Key");
							assertThat(error.rule()).isEqualTo("format");
						}));
	}

	@ParameterizedTest
	@ValueSource(ints = { 256, 1000 })
	void keysLongerThan255FailFormat(int length) {
		assertThatThrownBy(() -> LinkValidation.validateIdempotencyKey("k".repeat(length)))
				.isInstanceOf(ErrorResponseException.class);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "k", "!~", "550e8400-e29b-41d4-a716-446655440000" })
	void absentOrVisibleAsciiKeysPass(String key) {
		assertThatCode(() -> LinkValidation.validateIdempotencyKey(key)).doesNotThrowAnyException();
		assertThatCode(() -> LinkValidation.validateIdempotencyKey("k".repeat(255))).doesNotThrowAnyException();
	}

	@SuppressWarnings("unchecked")
	private static List<Problems.FieldError> errors(ErrorResponseException problem) {
		return (List<Problems.FieldError>) problem.getBody().getProperties().get("errors");
	}
}
