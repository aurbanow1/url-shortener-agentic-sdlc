package dev.urlshort.link;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import dev.urlshort.audit.AuditLog;
import dev.urlshort.web.Problems;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The link use cases and their transaction boundaries. Every successful mutation writes its audit row
 * inside the same transaction, so a failed audit write undoes the change (business rule 9). Inputs
 * are validated by the caller ({@link LinkValidation}) before any method here runs.
 */
@Service
class LinkService {

	static final Duration IDEMPOTENCY_WINDOW = Duration.ofHours(24);

	private final LinkRepository links;
	private final ShortCodes codes;
	private final AuditLog audit;
	private final Clock clock;

	LinkService(LinkRepository links, ShortCodes codes, AuditLog audit, Clock clock) {
		this.links = links;
		this.codes = codes;
		this.audit = audit;
		this.clock = clock;
	}

	/**
	 * Creates a link, or replays the one a live {@code key} is bound to (business rule 5, ADR-0009). A
	 * key binds only through a create that inserts; its window starts at that link's
	 * {@code createdAt}. A mismatch is refused before any write, so it neither releases the key nor
	 * extends the window.
	 *
	 * @throws org.springframework.web.ErrorResponseException {@code 422} when the key is bound, within
	 *         its window, to a different {@code url}
	 */
	@Transactional
	Link create(String url, @Nullable String key) {
		Instant now = clock.instant();
		if (key != null) {
			Link bound = links.findByIdempotencyKey(key).orElse(null);
			if (bound != null) {
				if (now.isBefore(bound.createdAt().plus(IDEMPOTENCY_WINDOW))) {
					if (bound.url().equals(url)) {
						return bound;
					}
					throw Problems.idempotencyMismatch();
				}
				links.releaseIdempotencyKey(bound.id());
			}
		}
		// ponytail: two concurrent creates with one key both pass the lookup and the loser's insert fails
		// on uq_link_idempotency_key (500, nothing stored, its retry replays); add an out-of-transaction
		// retry if clients hit it
		Link link = links.save(new Link(null, codes.next(), url, now, null, key));
		audit.append("link.create", "link", link.code(), null, link.snapshot());
		return link;
	}

	/** @throws org.springframework.web.ErrorResponseException {@code 404} when no link has the code */
	@Transactional(readOnly = true)
	Link read(String code) {
		return links.findByCode(code).orElseThrow(Problems::notFound);
	}

	/**
	 * The active link a Visitor is sent to.
	 *
	 * @throws org.springframework.web.ErrorResponseException {@code 404} when unknown, {@code 410} when
	 *         retired
	 */
	@Transactional(readOnly = true)
	Link resolve(String code) {
		Link link = read(code);
		if (link.retiredAt() != null) {
			throw Problems.gone();
		}
		return link;
	}

	/**
	 * Retires an active link; one-way (business rule 6). The conditional update decides, so a repeat or
	 * a concurrent second retire writes no second audit row.
	 *
	 * @throws org.springframework.web.ErrorResponseException {@code 404} when unknown, {@code 410} when
	 *         already retired
	 */
	@Transactional
	void retire(String code) {
		Link link = read(code);
		if (links.retire(link.id(), clock.instant()) == 0) {
			throw Problems.gone();
		}
		audit.append("link.retire", "link", code, link.snapshot(), new LinkSnapshot(link.url(), "retired"));
	}
}
