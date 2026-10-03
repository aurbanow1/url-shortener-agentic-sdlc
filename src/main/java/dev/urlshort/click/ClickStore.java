package dev.urlshort.click;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The {@code click} table (ADR-0013): one insert per recorded click, and the one grouped read the
 * statistics are folded from. Reads {@code link} only to turn a code into its id.
 */
@Component
class ClickStore {

	private final JdbcClient jdbc;

	ClickStore(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	void insert(Click click) {
		jdbc.sql("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
				+ " VALUES (:linkId, :clickedAt, :clickedOn, :referrer, :userAgentClass, :clientHash)")
				.param("linkId", click.linkId())
				.param("clickedAt", click.clickedAt().atOffset(ZoneOffset.UTC))
				.param("clickedOn", click.clickedOn())
				.param("referrer", click.referrer())
				.param("userAgentClass", click.userAgentClass())
				.param("clientHash", click.clientHash())
				.update();
	}

	/** The link's id by its code, read only to find its clicks; empty when no link has the code. */
	Optional<Long> findLinkId(String code) {
		return jdbc.sql("SELECT id FROM link WHERE code = :code").param("code", code).query(Long.class).optional();
	}

	/** The link's clicks counted per (UTC day, referrer origin) in one statement (ADR-0013). */
	List<DayReferrerCount> countByDayAndReferrer(long linkId) {
		return jdbc.sql("SELECT clicked_on, referrer, COUNT(*) AS clicks FROM click WHERE link_id = :linkId"
				+ " GROUP BY clicked_on, referrer")
				.param("linkId", linkId)
				.query((rs, row) -> new DayReferrerCount(rs.getObject("clicked_on", LocalDate.class),
						rs.getString("referrer"), rs.getLong("clicks")))
				.list();
	}

	/**
	 * One group of the statistics query.
	 *
	 * @param day the UTC day
	 * @param referrer the referrer origin, or {@code null} for clicks without one
	 * @param clicks the clicks in the group
	 */
	record DayReferrerCount(LocalDate day, @Nullable String referrer, long clicks) {
	}
}
