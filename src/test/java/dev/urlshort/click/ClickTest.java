package dev.urlshort.click;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Business rules 3 and 4: the referrer is reduced to its origin, the user agent to a class (AC-3, AC-4). */
class ClickTest {

	@ParameterizedTest
	@CsvSource({
		"https://News.Example/a/refpathcanary?t=refquerycanary#reffragcanary, https://news.example",
		"http://blog.example:8081/post, http://blog.example:8081",
		"https://refusercanary:pw@forum.example/x, https://forum.example",
		"http://x.example:80/, http://x.example",
		"https://x.example:443/, https://x.example",
		"http://x.example:443/, http://x.example:443",
		"https://x.example:80/, https://x.example:80",
		"HTTPS://X.EXAMPLE/, https://x.example",
		"http://[::1]:8080/, http://[::1]:8080" })
	void theReferrerIsReducedToItsOrigin(String header, String origin) {
		assertThat(Click.referrerOrigin(header)).isEqualTo(origin);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "android-app://com.example.app/", "not a url", "ftp://files.example/x", "/relative/path",
		"http:///no-host", "mailto:someone@example.com" })
	void aReferrerThatIsNotAnHttpOriginIsNone(String header) {
		assertThat(Click.referrerOrigin(header)).isNull();
	}

	@ParameterizedTest
	@ValueSource(ints = { 2048, 2049 })
	void theReferrerLengthCapIs2048(int length) {
		String header = "https://long.example/" + "a".repeat(length - "https://long.example/".length());

		assertThat(Click.referrerOrigin(header)).isEqualTo(length == 2048 ? "https://long.example" : null);
	}

	@ParameterizedTest
	@CsvSource({
		"Mozilla/5.0 (X11; Linux x86_64) uacanary Firefox/131.0, browser",
		"Mozilla/5.0 (compatible; Googlebot/2.1; uacanary), bot",
		"uacanary-crawler/1.0, bot",
		"uacanary-spider, bot",
		"curl/8.7.1 uacanary, other",
		"SOME-BOT, bot",
		"mozilla/5.0, other" })
	void theUserAgentIsReducedToAClass(String header, String userAgentClass) {
		assertThat(Click.userAgentClass(header)).isEqualTo(userAgentClass);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void anAbsentOrEmptyUserAgentIsUnknown(String header) {
		assertThat(Click.userAgentClass(header)).isEqualTo("unknown");
	}

	@Test
	void rule2_aClickHoldsOnlyTheReducedFacts() {
		Click click = new Click(7L, Instant.parse("2026-10-01T12:00:00Z"), LocalDate.parse("2026-10-01"),
				"https://news.example", "browser", "a".repeat(64));

		assertThat(Arrays.stream(Click.class.getRecordComponents()).map(RecordComponent::getName))
				.containsExactly("linkId", "clickedAt", "clickedOn", "referrer", "userAgentClass", "clientHash");
		assertThat(click.referrer()).isEqualTo("https://news.example");
	}
}
