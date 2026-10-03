package dev.urlshort.click;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import dev.urlshort.click.ClickStore.DayReferrerCount;
import org.junit.jupiter.api.Test;

/** Business rule 7's fold of the grouped rows into the three figures (AC-7, AC-9 to AC-11). */
class LinkStatsTest {

	private static final LocalDate D1 = LocalDate.parse("2026-10-01");
	private static final LocalDate D2 = LocalDate.parse("2026-10-02");

	@Test
	void noRowsIsZeroAndTwoEmptyLists() {
		assertThat(LinkStats.of("Abc12345", List.of()))
				.isEqualTo(new LinkStats("Abc12345", 0, List.of(), List.of()));
	}

	@Test
	void daysAreSummedAscendingAndClicksWithoutAReferrerCountOnlyInTheTotals() {
		LinkStats stats = LinkStats.of("Abc12345", List.of(new DayReferrerCount(D2, "https://a.example", 2),
				new DayReferrerCount(D1, null, 4), new DayReferrerCount(D1, "https://a.example", 1)));

		assertThat(stats.totalClicks()).isEqualTo(7);
		assertThat(stats.clicksPerDay()).containsExactly(new LinkStats.DayClicks(D1, 5), new LinkStats.DayClicks(D2, 2));
		assertThat(stats.topReferrers()).containsExactly(new LinkStats.ReferrerClicks("https://a.example", 3));
	}

	@Test
	void referrersAreRankedByClicksThenByCodePointAndCappedAtTen() {
		List<DayReferrerCount> rows = new ArrayList<>();
		rows.add(new DayReferrerCount(D1, "https://b.example", 3));
		rows.add(new DayReferrerCount(D1, "https://a.example", 5));
		rows.add(new DayReferrerCount(D1, "https://c.example", 3));
		for (int i = 1; i <= 11; i++) {
			rows.add(new DayReferrerCount(D1, "https://d" + i + ".example", 1));
		}

		List<String> top = LinkStats.of("Abc12345", rows).topReferrers().stream()
				.map(LinkStats.ReferrerClicks::referrer).toList();

		assertThat(top).containsExactly("https://a.example", "https://b.example", "https://c.example",
				"https://d1.example", "https://d10.example", "https://d11.example", "https://d2.example",
				"https://d3.example", "https://d4.example", "https://d5.example");
	}
}
