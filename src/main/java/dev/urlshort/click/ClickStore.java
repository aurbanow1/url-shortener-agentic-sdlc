package dev.urlshort.click;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The {@code click} table (ADR-0013): one insert per recorded click, the one statement the
 * statistics are folded from, and the retention delete. Reads {@code link} only to turn a code into
 * its id.
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

	/**
	 * Deletes every click stored on a UTC day before {@code cutoff}, in one statement and one
	 * transaction; a table scan by design (ADR-0018). Returns the number of clicks deleted.
	 */
	int deleteBefore(LocalDate cutoff) {
		return jdbc.sql("DELETE FROM click WHERE clicked_on < :cutoff").param("cutoff", cutoff).update();
	}

	/** The link's id by its code, read only to find its clicks; empty when no link has the code. */
	Optional<Long> findLinkId(String code) {
		return jdbc.sql("SELECT id FROM link WHERE code = :code").param("code", code).query(Long.class).optional();
	}

	/**
	 * The link's clicks per (UTC day, referrer origin), and per UTC day its distinct client hashes and
	 * {@code bot} clicks, in one statement and so one snapshot (ADR-0013 and its 01-analytics-v2
	 * amendment). This is the only read of {@code client_hash}: it is compared only within its own
	 * {@code clicked_on}, the day of the salt that produced it, and never leaves the statement (rule 5).
	 */
	Stats stats(long linkId) {
		List<DayReferrerCount> referrers = new ArrayList<>();
		Map<LocalDate, DayFigures> days = new HashMap<>();
		jdbc.sql("SELECT clicked_on, referrer, COUNT(*) AS clicks, CAST(NULL AS BIGINT) AS unique_visitors,"
				+ " CAST(NULL AS BIGINT) AS bot_clicks FROM click WHERE link_id = :linkId GROUP BY clicked_on, referrer"
				+ " UNION ALL"
				+ " SELECT clicked_on, NULL, NULL, COUNT(DISTINCT client_hash),"
				+ " SUM(CASE WHEN user_agent_class = 'bot' THEN 1 ELSE 0 END)"
				+ " FROM click WHERE link_id = :linkId GROUP BY clicked_on")
				.param("linkId", linkId)
				.query(rs -> {
					LocalDate day = rs.getObject("clicked_on", LocalDate.class);
					Long uniqueVisitors = rs.getObject("unique_visitors", Long.class);
					if (uniqueVisitors == null) {
						referrers.add(new DayReferrerCount(day, rs.getString("referrer"), rs.getLong("clicks")));
					}
					else {
						days.put(day, new DayFigures(day, uniqueVisitors, rs.getLong("bot_clicks")));
					}
				});
		return new Stats(referrers, days);
	}

	/**
	 * The statistics statement's rows, split by branch.
	 *
	 * @param referrers one group per (UTC day, referrer origin), as v1
	 * @param days the per-day figures, one entry for every day that has a referrer group
	 */
	record Stats(List<DayReferrerCount> referrers, Map<LocalDate, DayFigures> days) {
	}

	/**
	 * One UTC day's v2 figures.
	 *
	 * @param day the UTC day
	 * @param uniqueVisitors distinct client hashes among that day's clicks
	 * @param botClicks that day's clicks of user-agent class {@code bot}
	 */
	record DayFigures(LocalDate day, long uniqueVisitors, long botClicks) {
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
