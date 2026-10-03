package dev.urlshort.link;

import java.time.Instant;

/**
 * A link as the Creator sees it: exactly these five fields (A-2).
 *
 * @param code the short code
 * @param shortUrl the operator's public base URL, {@code /} and the code
 * @param url the target, byte for byte as submitted
 * @param state {@code active} or {@code retired}
 * @param createdAt the creation instant, ISO-8601 UTC with {@code Z}
 */
record LinkResponse(String code, String shortUrl, String url, String state, Instant createdAt) {

	static LinkResponse of(Link link, String publicBaseUrl) {
		return new LinkResponse(link.code(), publicBaseUrl + "/" + link.code(), link.url(), link.state(),
				link.createdAt());
	}
}
