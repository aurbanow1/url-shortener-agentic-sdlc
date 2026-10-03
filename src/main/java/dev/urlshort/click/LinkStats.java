package dev.urlshort.click;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dev.urlshort.click.ClickStore.DayFigures;
import dev.urlshort.click.ClickStore.DayReferrerCount;
import io.swagger.v3.oas.annotations.media.Schema;

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
	 * Folds the rows of one statement, so every figure agrees with every other (AC-11): the grouped
	 * (day, referrer) counts into the total, the per-day clicks and the top referrers, and each day's
	 * unique visitors and bot clicks beside its clicks. Clicks without a referrer count in the total and
	 * per day only. Ranking happens here, not in SQL, so the tie order never depends on a database
	 * collation.
	 */
	static LinkStats of(String code, ClickStore.Stats stats) {
		long total = 0;
		Map<LocalDate, Long> perDay = new TreeMap<>();
		Map<String, Long> perReferrer = new HashMap<>();
		for (DayReferrerCount row : stats.referrers()) {
			total += row.clicks();
			perDay.merge(row.day(), row.clicks(), Long::sum);
			if (row.referrer() != null) {
				perReferrer.merge(row.referrer(), row.clicks(), Long::sum);
			}
		}
		// both branches read one snapshot, so every day with clicks has its figures
		List<DayClicks> days = perDay.entrySet().stream().map(e -> {
			DayFigures figures = stats.days().get(e.getKey());
			return new DayClicks(e.getKey(), e.getValue(), figures.uniqueVisitors(), figures.botClicks());
		}).toList();
		List<ReferrerClicks> top = perReferrer.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
						.thenComparing(Map.Entry.comparingByKey()))
				.limit(TOP_REFERRERS).map(e -> new ReferrerClicks(e.getKey(), e.getValue())).toList();
		return new LinkStats(code, total, days, top);
	}

	/**
	 * Clicks on one UTC day (01-analytics-v2 rules 1 to 4).
	 *
	 * @param date the day, rendered {@code YYYY-MM-DD}
	 * @param clicks clicks recorded on it, bots included
	 * @param uniqueVisitors distinct visitors that day; defined per UTC day only and never combined
	 *        across days (an upper bound if the service restarted that day)
	 * @param botClicks clicks whose user agent was classified {@code bot}; also counted in the other figures
	 */
	record DayClicks(LocalDate date, long clicks,
			@Schema(description = "Distinct visitors that UTC day; never combined across days") long uniqueVisitors,
			@Schema(description = "Clicks whose user agent was classified bot") long botClicks) {
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
