import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.flywaydb.core.Flyway;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.urlshort.UrlshortApplication;

/**
 * Design probe for 01-analytics-v2 (design.md section 12). S1-S3 run the statistics statement of
 * design.md section 3 on H2 2.4.240 with the shipped V1 and V2 schema; S4 runs the two click counters
 * of section 1 in the shipped application and reads them from /actuator/prometheus. No file under src/
 * is touched.
 */
public class StatsProbe {

	static final String STATS = "SELECT r.clicked_on, r.referrer, r.clicks, d.unique_visitors, d.bot_clicks"
			+ " FROM (SELECT clicked_on, referrer, COUNT(*) AS clicks FROM click WHERE link_id = ? GROUP BY clicked_on, referrer) r"
			+ " JOIN (SELECT clicked_on, COUNT(DISTINCT client_hash) AS unique_visitors,"
			+ " SUM(CASE WHEN user_agent_class = 'bot' THEN 1 ELSE 0 END) AS bot_clicks"
			+ " FROM click WHERE link_id = ? GROUP BY clicked_on) d ON d.clicked_on = r.clicked_on";
	/**
	 * The form chosen after S2: one statement (one snapshot), two independent grouped scans; the fold tells
	 * the row kinds apart by which columns are NULL. The join form above recomputes the per-day subquery for
	 * every (day, referrer) row (S2's plan).
	 */
	static final String STATS_UNION = "SELECT clicked_on, referrer, COUNT(*) AS clicks, CAST(NULL AS BIGINT) AS unique_visitors,"
			+ " CAST(NULL AS BIGINT) AS bot_clicks FROM click WHERE link_id = ? GROUP BY clicked_on, referrer"
			+ " UNION ALL SELECT clicked_on, NULL, NULL, COUNT(DISTINCT client_hash),"
			+ " SUM(CASE WHEN user_agent_class = 'bot' THEN 1 ELSE 0 END) FROM click WHERE link_id = ? GROUP BY clicked_on";
	static final LocalDate D = LocalDate.of(2026, 10, 1);

	public static void main(String[] args) throws Exception {
		String url = "jdbc:h2:mem:stats-probe;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
		Flyway.configure().dataSource(url, "sa", "").target("2").load().migrate();
		try (Connection c = DriverManager.getConnection(url, "sa", "")) {
			s1Semantics(c);
			s2Plan(c);
			s3HotLink(c);
		}
		s4Counters();
		System.out.println("PROBE done");
		System.exit(0);
	}

	/** AC-2, AC-4 and AC-5 shapes on one day, AC-3 across midnight, with referrers spread over the clicks. */
	static void s1Semantics(Connection c) throws SQLException {
		long link = link(c, "probeS1x1");
		// AC-2: three clients, 3 + 2 + 1 browser clicks on D; two referrers among them
		click(c, link, D, "a", "browser", "https://x.example");
		click(c, link, D, "a", "browser", "https://y.example");
		click(c, link, D, "a", "browser", null);
		click(c, link, D, "b", "browser", "https://x.example");
		click(c, link, D, "b", "browser", null);
		click(c, link, D, "c", "browser", "https://y.example");
		// AC-5 on D+1: one client, a browser and a bot click
		click(c, link, D.plusDays(1), "z", "browser", null);
		click(c, link, D.plusDays(1), "z", "bot", "https://x.example");
		// AC-4 on D+2: six clients, classes browser, bot, bot, bot, other, unknown
		String[] classes = { "browser", "bot", "bot", "bot", "other", "unknown" };
		for (int i = 0; i < classes.length; i++) {
			click(c, link, D.plusDays(2), "k" + i, classes[i], null);
		}
		// AC-3: the same client on D+3 and D+4 (a different day salt gives a different hash; here the same hash
		// string is stored on purpose, to show the per-day grouping never compares across days)
		click(c, link, D.plusDays(3), "same", "browser", null);
		click(c, link, D.plusDays(4), "same", "browser", null);
		System.out.println("S1 rows (day | referrer | clicks | uniqueVisitors | botClicks): " + rows(c, STATS, link));
		System.out.println("S1u union form, same link (referrer rows carry clicks; day rows carry the two new figures): "
				+ unionRows(c, link));
	}

	static String unionRows(Connection c, long link) throws SQLException {
		List<String> out = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement(STATS_UNION)) {
			ps.setLong(1, link);
			ps.setLong(2, link);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					out.add(rs.getObject(1) + "|" + rs.getString(2) + "|" + rs.getObject(3) + "|" + rs.getObject(4) + "|" + rs.getObject(5));
				}
			}
		}
		out.sort(null);
		return String.join("; ", out);
	}

	static void s2Plan(Connection c) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("EXPLAIN " + STATS)) {
			ps.setLong(1, 1);
			ps.setLong(2, 1);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				System.out.println("S2 plan: " + rs.getString(1).replaceAll("\\s+", " "));
			}
		}
	}

	/**
	 * Hot links of the same shape, each read once cold (H2 reuses an identical query's result while the
	 * table is unchanged, so a repeated read measures nothing): 90 days x 5 000 clicks from about 2 000
	 * clients a day, with 10 referrers (S3) and with 300 referrers a day, spam-like (S5).
	 */
	static void s3HotLink(Connection c) throws SQLException {
		long[] links = { link(c, "probeS3j1"), link(c, "probeS3u1"), link(c, "probeS5j1"), link(c, "probeS5u1") };
		try (Statement s = c.createStatement()) {
			for (int i = 0; i < links.length; i++) {
				String referrers = i < 2 ? "10" : "300";
				s.executeUpdate("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
						+ " SELECT " + links[i] + ", TIMESTAMP WITH TIME ZONE '2026-07-01 00:00:00+00' + X * INTERVAL '1' SECOND,"
						+ " DATE '2026-07-01' + CAST(X / 2500 AS INT),"
						+ " CASE WHEN MOD(X, 3) = 0 THEN NULL ELSE 'https://r' || MOD(X, " + referrers + ") || '.example' END,"
						+ " CASE WHEN MOD(X, 20) = 0 THEN 'bot' ELSE 'browser' END,"
						+ " LPAD(CAST(MOD(X, 1000) AS VARCHAR), 64, '0') FROM SYSTEM_RANGE(1, 225000)");
			}
		}
		String[] labels = { "S3 join form, 10 referrers", "S3 union form, 10 referrers", "S5 join form, 300 referrers a day",
				"S5 union form, 300 referrers a day" };
		for (int i = 0; i < links.length; i++) {
			long t0 = System.nanoTime();
			int n = 0;
			try (PreparedStatement ps = c.prepareStatement(i % 2 == 0 ? STATS : STATS_UNION)) {
				ps.setLong(1, links[i]);
				ps.setLong(2, links[i]);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						n++;
					}
				}
			}
			System.out.println(labels[i] + " (225 000 clicks of one link over 90 days, about 1 000 clients a day, cold): " + (System.nanoTime() - t0) / 1_000_000
					+ " ms, " + n + " rows");
		}
	}

	static void s4Counters() throws Exception {
		try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(UrlshortApplication.class, ProbeConfig.class)
				.run("--server.port=0", "--server.address=127.0.0.1", "--logging.level.root=warn",
						"--spring.datasource.url=jdbc:h2:mem:stats-probe-app;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")) {
			MeterRegistry registry = ctx.getBean(MeterRegistry.class);
			Counter recorded = Counter.builder("urlshort.clicks.recorded").description("Clicks written to the store").register(registry);
			for (String reason : List.of("rejected", "reduction failed", "write failed", "shutdown deadline", "shutdown deadline, outcome unknown")) {
				Counter.builder("urlshort.clicks.lost").description("Clicks reported by a click lost event").tag("reason", reason).register(registry);
			}
			recorded.increment(3);
			registry.counter("urlshort.clicks.lost", "reason", "write failed").increment(2);
			String body = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"
					+ ctx.getEnvironment().getRequiredProperty("local.server.port") + "/actuator/prometheus")).build(),
					HttpResponse.BodyHandlers.ofString()).body();
			body.lines().filter(line -> line.contains("urlshort_clicks")).forEach(line -> System.out.println("S4 " + line));
		}
	}

	@Configuration(proxyBeanMethods = false)
	public static class ProbeConfig {

		@Bean
		public String probeMarker() {
			return "stats-probe";
		}
	}

	static long link(Connection c, String code) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO link (code, url, created_at) VALUES (?, ?, ?)", new String[] { "ID" })) {
			ps.setString(1, code);
			ps.setString(2, "https://example.org/" + code);
			ps.setObject(3, D.atStartOfDay().atOffset(ZoneOffset.UTC));
			ps.executeUpdate();
			try (ResultSet keys = ps.getGeneratedKeys()) {
				keys.next();
				return keys.getLong(1);
			}
		}
	}

	static void click(Connection c, long link, LocalDate day, String client, String uaClass, String referrer) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
				+ " VALUES (?, ?, ?, ?, ?, ?)")) {
			ps.setLong(1, link);
			ps.setObject(2, day.atTime(12, 0).atOffset(ZoneOffset.UTC));
			ps.setObject(3, day);
			ps.setString(4, referrer);
			ps.setString(5, uaClass);
			ps.setString(6, String.format("%64s", client).replace(' ', '0'));
			ps.executeUpdate();
		}
	}

	static String rows(Connection c, String sql, long link) throws SQLException {
		List<String> out = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement(sql + " ORDER BY r.clicked_on, r.referrer NULLS FIRST")) {
			ps.setLong(1, link);
			ps.setLong(2, link);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					out.add(rs.getObject(1) + "|" + rs.getString(2) + "|" + rs.getLong(3) + "|" + rs.getLong(4) + "|" + rs.getLong(5));
				}
			}
		}
		return String.join("; ", out);
	}
}
