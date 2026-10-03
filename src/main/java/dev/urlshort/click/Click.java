package dev.urlshort.click;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * One click as stored: rule 2's four facts, already reduced on the request thread, plus the UTC day
 * the statistics group by (derived from {@code clickedAt}, ADR-0013).
 *
 * @param linkId the redirected link's id
 * @param clickedAt the service clock's instant of the redirect
 * @param clickedOn the UTC day of {@code clickedAt}
 * @param referrer the referrer origin, or {@code null} when there is none (rule 3)
 * @param userAgentClass {@code browser}, {@code bot}, {@code other} or {@code unknown} (rule 4)
 * @param clientHash the HMAC of the client address under the day's salt, 64 hex characters
 */
record Click(long linkId, Instant clickedAt, LocalDate clickedOn, @Nullable String referrer, String userAgentClass,
		String clientHash) {

	static final int MAX_REFERER_LENGTH = 2048;

	/**
	 * Rule 3: lowercase scheme, {@code ://}, lowercase host and a non-default port, for an absolute
	 * {@code http(s)} URL of at most 2 048 characters with a host; {@code null} otherwise. Path, query,
	 * fragment and userinfo are dropped.
	 */
	static @Nullable String referrerOrigin(@Nullable String header) {
		if (header == null || header.length() > MAX_REFERER_LENGTH) {
			return null;
		}
		URI uri;
		try {
			uri = new URI(header);
		}
		catch (URISyntaxException ex) {
			return null;
		}
		String scheme = uri.getScheme();
		String host = uri.getHost();
		if (scheme == null || host == null) {
			return null;
		}
		scheme = scheme.toLowerCase(Locale.ROOT);
		boolean http = scheme.equals("http");
		if (!http && !scheme.equals("https")) {
			return null;
		}
		int port = uri.getPort();
		boolean defaultPort = port == -1 || port == (http ? 80 : 443);
		return scheme + "://" + host.toLowerCase(Locale.ROOT) + (defaultPort ? "" : ":" + port);
	}

	/** Rule 4, in order: absent or empty, then bot words (any case), then {@code Mozilla/}, then other. */
	static String userAgentClass(@Nullable String header) {
		if (header == null || header.isEmpty()) {
			return "unknown";
		}
		String lower = header.toLowerCase(Locale.ROOT);
		if (lower.contains("bot") || lower.contains("crawler") || lower.contains("spider")) {
			return "bot";
		}
		return header.startsWith("Mozilla/") ? "browser" : "other";
	}
}
