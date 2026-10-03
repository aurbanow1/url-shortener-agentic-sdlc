package dev.urlshort.link;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import dev.urlshort.web.Problems;
import org.jspecify.annotations.Nullable;

/**
 * The create request's input rules, applied before any lookup or write. Each method reports the
 * first rule that fails, so every input maps to exactly one token (business rules 3 and 5, A-12).
 * Messages are static and never contain the submitted value (business rule 8).
 */
final class LinkValidation {

	static final int MAX_URL_LENGTH = 2048;
	private static final Pattern VISIBLE_ASCII = Pattern.compile("[\\x21-\\x7E]*");
	private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[\\x21-\\x7E]{1,255}");

	private LinkValidation() {
	}

	/**
	 * Applies, in order: {@code required}, {@code too-long}, {@code scheme}, {@code malformed},
	 * {@code credentials}. Visible ASCII only (RFC 3986 URIs are ASCII) also keeps CR, LF and spaces
	 * out of the {@code Location} header.
	 */
	static void validateUrl(@Nullable String url) {
		if (url == null || url.isBlank()) {
			throw invalidUrl("required", "url is required");
		}
		if (url.length() > MAX_URL_LENGTH) {
			throw invalidUrl("too-long", "url must be at most 2048 characters");
		}
		if (!url.regionMatches(true, 0, "http://", 0, 7) && !url.regionMatches(true, 0, "https://", 0, 8)) {
			throw invalidUrl("scheme", "url must start with http:// or https://");
		}
		URI uri = VISIBLE_ASCII.matcher(url).matches() ? parse(url) : null;
		if (uri == null || uri.getHost() == null) {
			throw invalidUrl("malformed", "url must be an absolute URL with a host");
		}
		if (uri.getUserInfo() != null) {
			throw invalidUrl("credentials", "url must not contain credentials");
		}
	}

	/** An absent key is allowed; a present one must be 1 to 255 visible ASCII characters. */
	static void validateIdempotencyKey(@Nullable String key) {
		if (key != null && !IDEMPOTENCY_KEY.matcher(key).matches()) {
			throw Problems.validation("Idempotency-Key", "format",
					"Idempotency-Key must be 1 to 255 visible ASCII characters");
		}
	}

	private static @Nullable URI parse(String url) {
		try {
			return new URI(url);
		}
		catch (URISyntaxException ex) {
			return null;
		}
	}

	private static RuntimeException invalidUrl(String rule, String message) {
		return Problems.validation("url", rule, message);
	}
}
