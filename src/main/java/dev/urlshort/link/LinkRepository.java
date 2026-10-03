package dev.urlshort.link;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

/**
 * Access to the {@code link} table, limited to what the slice uses. The two mutations after insert
 * are targeted {@code UPDATE} statements, never a {@code save} of a re-read aggregate, so a stale
 * copy can never overwrite a concurrent retire.
 */
interface LinkRepository extends Repository<Link, Long> {

	Link save(Link link);

	Optional<Link> findByCode(String code);

	Optional<Link> findByIdempotencyKey(String idempotencyKey);

	/** Retires the link if it is still active; returns {@code 0} when it was already retired. */
	@Modifying
	@Query("UPDATE link SET retired_at = :at WHERE id = :id AND retired_at IS NULL")
	int retire(Long id, Instant at);

	/** Frees an expired key so it can bind a new link. */
	@Modifying
	@Query("UPDATE link SET idempotency_key = NULL WHERE id = :id")
	int releaseIdempotencyKey(Long id);
}
