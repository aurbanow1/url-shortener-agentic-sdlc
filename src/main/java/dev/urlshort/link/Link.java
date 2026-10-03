package dev.urlshort.link;

import java.time.Instant;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;

/**
 * A stored short link, the {@code link} table's aggregate. A link is {@code active} until
 * {@code retiredAt} is set, then {@code retired} forever (business rule 6).
 *
 * @param id surrogate key, {@code null} until inserted; never exposed
 * @param code the public short code
 * @param url the target, stored verbatim
 * @param createdAt when the link was created; also the start of its key's 24 h window
 * @param retiredAt when the link was retired, {@code null} while active
 * @param idempotencyKey the key bound to this link by its create, {@code null} when none or released
 */
record Link(@Id @Nullable Long id, String code, String url, Instant createdAt, @Nullable Instant retiredAt,
		@Nullable String idempotencyKey) {

	String state() {
		return retiredAt == null ? "active" : "retired";
	}

	LinkSnapshot snapshot() {
		return new LinkSnapshot(url, state());
	}
}
