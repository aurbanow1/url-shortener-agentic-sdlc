package dev.urlshort.click;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dev.urlshort.click.ClickStore.DayReferrerCount;

/**
 * A link's click statistics, exactly business rule 7's four members (AC-7, AC-17): aggregates only,
 * no hash, class or referrer path.
 *
 * @param code the link's code
 * @param totalClicks every click recorded for the link
 * @param clicksPerDay one element per UTC day with at least one click, ascending
 * @param topReferrers at most {@value #TOP_REFERRERS} referrer origins, by clicks descending, then by
 *        origin ascending by code point
 */
record LinkStats(String code, long totalClicks, List<DayClicks> clicksPerDay, List<ReferrerClicks> topReferrers) {

	static final int TOP_REFERRERS = 10;

	/**
	 * Folds the grouped (day, referrer) counts of one statement, so the three figures agree with each
	 * other (AC-11). Clicks without a referrer count in the total and per day only. Ranking happens
	 * here, not in SQL, so the tie order never depends on a database collation.
	 */
	static LinkStats of(String code, List<DayReferrerCount> rows) {
		long total = 0;
		Map<LocalDate, Long> perDay = new TreeMap<>();
		Map<String, Long> perReferrer = new HashMap<>();
		for (DayReferrerCount row : rows) {
			total += row.clicks();
			perDay.merge(row.day(), row.clicks(), Long::sum);
			if (row.referrer() != null) {
				perReferrer.merge(row.referrer(), row.clicks(), Long::sum);
			}
		}
		List<DayClicks> days = perDay.entrySet().stream().map(e -> new DayClicks(e.getKey(), e.getValue())).toList();
		List<ReferrerClicks> top = perReferrer.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
						.thenComparing(Map.Entry.comparingByKey()))
				.limit(TOP_REFERRERS).map(e -> new ReferrerClicks(e.getKey(), e.getValue())).toList();
		return new LinkStats(code, total, days, top);
	}

	/**
	 * Clicks on one UTC day.
	 *
	 * @param date the day, rendered {@code YYYY-MM-DD}
	 * @param clicks clicks recorded on it
	 */
	record DayClicks(LocalDate date, long clicks) {
	}

	/**
	 * Clicks from one referrer origin.
	 *
	 * @param referrer the origin, {@code scheme://host[:port]}
	 * @param clicks clicks it referred
	 */
	record ReferrerClicks(String referrer, long clicks) {
	}
}
