import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.Positive;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.validation.annotation.Validated;

import dev.urlshort.UrlshortApplication;
import dev.urlshort.link.FunctionalClock;

/**
 * Design probe for 02-click-retention (design.md section 12). It runs the purge exactly as design.md
 * section 1 specifies it (one daemon thread, a fixed 5 s tick that decides on the application clock,
 * batches of 10 000, a stop flag at shutdown) inside the shipped application, on H2 file databases
 * shaped like a real one. No file under src/ is touched.
 */
public class RetentionProbe {

	static final PrintStream OUT = System.out;
	static final ByteArrayOutputStream WINDOW = new ByteArrayOutputStream();
	static volatile boolean recording;
	static final Path ROOT = Path.of("build/retention-probe").toAbsolutePath();
	static final LocalDate T = LocalDate.now(ZoneOffset.UTC);
	static final String HASH = "0123456789abcdef".repeat(4);
	/** The first batch form tried; H2 2.4.240 re-runs its subquery for every candidate row (L0). */
	static final String SUBQUERY_BATCH_SQL = "DELETE FROM click WHERE id IN"
			+ " (SELECT id FROM click WHERE clicked_on < :cutoff FETCH FIRST 10000 ROWS ONLY)";
	/** The batch as designed: two plain statements, select at most 10 000 ids, then delete exactly those. */
	static final String BATCH_SELECT_SQL = "SELECT id FROM click WHERE clicked_on < :cutoff FETCH FIRST 10000 ROWS ONLY";
	static final String BATCH_DELETE_SQL = "DELETE FROM click WHERE id IN (:ids)";
	/** Command-line arguments, so they outrank the shipped application.properties. */
	static final String[] ARGS = { "--server.port=0", "--server.address=127.0.0.1", "--logging.level.root=warn",
			"--logging.level.dev.urlshort.click.ClickPurge=info", "--urlshort.rate-limit.redirect-per-minute=1000000",
			"--urlshort.rate-limit.create-per-minute=1000000" };

	public static void main(String[] args) throws Exception {
		if ("4child".equals(System.getProperty("probe.part"))) {
			d4Child(args[0]);
			return;
		}
		System.setOut(new PrintStream(new OutputStream() {
			@Override
			public void write(int b) {
				OUT.write(b);
				if (recording) {
					synchronized (WINDOW) {
						WINDOW.write(b);
					}
				}
			}

			@Override
			public void write(byte[] b, int off, int len) {
				OUT.write(b, off, len);
				if (recording) {
					synchronized (WINDOW) {
						WINDOW.write(b, off, len);
					}
				}
			}
		}, true));
		org.springframework.util.FileSystemUtils.deleteRecursively(ROOT);
		Files.createDirectories(ROOT);
		OUT.println("PROBE day T = " + T + ", JVM max heap " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB");

		if ("4".equals(System.getProperty("probe.part"))) {
			d4ProcessExitDuringTheDelete(populateBase());
			OUT.println("PROBE done");
			System.exit(0);
		}
		if ("5".equals(System.getProperty("probe.part"))) {
			a8DailyPurgeOff();
			OUT.println("PROBE done");
			System.exit(0);
		}
		if ("3".equals(System.getProperty("probe.part"))) {
			d3ShutdownPastTheDeadline(populateBase());
			OUT.println("PROBE done");
			System.exit(0);
		}
		if ("2".equals(System.getProperty("probe.part"))) {
			// Part 2: clicks spread over 2 000 links, so the (link_id, clicked_on) index no longer lists the old
			// rows first, as it does for part 1's single link.
			Path steady = populateSpread("steady", false);
			lockVariant("L5 steady day, 2 000 links, batches, no day index", steady, true, false);
			lockVariant("L6 steady day, 2 000 links, batches, with ix_click_day", steady, true, true);
			lockVariant("L7 steady day, 2 000 links, single DELETE, no day index", steady, false, false);
			Path catchUp = populateSpread("catchup", true);
			lockVariant("L8 catch-up, 2 000 links, batches, no day index", catchUp, true, false);
			lockVariant("L9 catch-up, 2 000 links, batches, with ix_click_day", catchUp, true, true);
			lockVariant("L10 catch-up, 2 000 links, single DELETE, no day index", catchUp, false, false);
			a7SynchronousStartupRun(catchUp, true);
			a7SynchronousStartupRun(catchUp, false);
			OUT.println("PROBE done");
			System.exit(0);
		}

		s1SpringCronDoesNotFollowShiftedClock();
		a8TickerFollowsSuiteClock();
		a4InvalidSettingStopsStartup();
		Path base = populateBase();
		l0SubqueryBatchForm(base);
		lockVariant("L1 single DELETE, no day index", base, false, false);
		lockVariant("L2 batches of 10 000, no day index", base, true, false);
		lockVariant("L3 batches of 10 000, with ix_click_day", base, true, true);
		lockVariant("L4 single DELETE, with ix_click_day", base, false, true);
		d1ShutdownDuringBatchedRunThenStartupRun(base);
		d2ShutdownDuringSingleDelete(base);
		OUT.println("PROBE done");
		System.exit(0);
	}

	// ------------------------------------------------------------------ S1: Spring's scheduler sleeps real time

	static void s1SpringCronDoesNotFollowShiftedClock() throws Exception {
		FunctionalClock clock = new FunctionalClock();
		clock.shift(Duration.between(clock.instant(), T.atTime(12, 0).toInstant(ZoneOffset.UTC)));
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setClock(clock);
		scheduler.initialize();
		AtomicInteger fired = new AtomicInteger();
		scheduler.schedule(fired::incrementAndGet, new CronTrigger("0 10 0 * * *", ZoneOffset.UTC));
		clock.shift(Duration.ofHours(12).plusMinutes(10).plusSeconds(1));
		Thread.sleep(7000);
		OUT.println("S1 Spring ThreadPoolTaskScheduler.setClock(suite clock) + CronTrigger(\"0 10 0 * * *\", UTC), armed at "
				+ T + "T12:00Z; suite clock then moved to " + clock.instant() + " (past the fire time); fired after 7 s real: "
				+ fired.get() + " -> the trigger reads the clock only when arming, then sleeps real time");
		scheduler.shutdown();
	}

	// ------------------------------------------------------------------ A8: the tick decides on the application clock

	static void a8TickerFollowsSuiteClock() throws Exception {
		ProbePurge.autoStart = true;
		ProbePurge.batched = true;
		try (ConfigurableApplicationContext ctx = start("jdbc:h2:mem:probe-a8;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")) {
			ProbePurge purge = ctx.getBean(ProbePurge.class);
			FunctionalClock clock = ctx.getBean(FunctionalClock.class);
			JdbcClient jdbc = ctx.getBean(JdbcClient.class);
			await(() -> purge.runs.size() >= 1, 10_000);
			OUT.println("A8a startup run after ApplicationReadyEvent: runs=" + purge.runs + ", ready->run done "
					+ purge.lastRunDoneMillisAfterReady + " ms; retentionDays bound from the default = " + purge.days);
			long link = jdbc.sql("INSERT INTO link (code, url, created_at) VALUES ('probeA8x1', 'https://example.org/a8', :at)")
					.param("at", clock.instant().atOffset(ZoneOffset.UTC)).update() > 0
							? jdbc.sql("SELECT id FROM link WHERE code = 'probeA8x1'").query(Long.class).single() : -1;
			insert(jdbc, link, T.minusDays(90), 2);
			insert(jdbc, link, T.minusDays(89), 2);
			Instant target = T.plusDays(1).atTime(0, 10, 1).toInstant(ZoneOffset.UTC);
			clock.shift(Duration.between(clock.instant(), target));
			long t0 = System.nanoTime();
			boolean gone = await(() -> count(jdbc, "clicked_on = DATE '" + T.minusDays(90) + "'") == 0, 60_000);
			OUT.println("A8b suite clock moved to " + target + " with no trigger: day " + T.minusDays(90) + " deleted=" + gone
					+ " after " + ms(t0) + " ms real; day " + T.minusDays(89) + " left=" + count(jdbc, "clicked_on = DATE '"
							+ T.minusDays(89) + "'") + "; runs=" + purge.runs);
			Thread.sleep(11_000);
			OUT.println("A8c same UTC day, two more ticks: runs=" + purge.runs.size() + " (no second run that day)");
			clock.shift(Duration.ofDays(-3));
			Thread.sleep(11_000);
			OUT.println("A8d suite clock stepped back 3 days to " + clock.instant() + ", two ticks: runs=" + purge.runs.size()
					+ " (a backward step runs nothing)");
			clock.shift(Duration.between(clock.instant(), T.plusDays(2).atTime(0, 9, 50).toInstant(ZoneOffset.UTC)));
			Thread.sleep(6_000);
			int before = purge.runs.size();
			OUT.println("A8e suite clock at " + clock.instant() + " (next day, before 00:10Z), one tick: runs=" + before);
			await(() -> purge.runs.size() > before, 20_000);
			OUT.println("A8f suite clock reached " + clock.instant() + ": runs=" + purge.runs);
			ProbePurge.failNext = true;
			clock.shift(Duration.between(clock.instant(), T.plusDays(3).atTime(0, 10, 1).toInstant(ZoneOffset.UTC)));
			int beforeFail = purge.failures.get();
			await(() -> purge.failures.get() > beforeFail, 20_000);
			Thread.sleep(6_000);
			OUT.println("A8g a run that fails (store throws, message carries a canary): failures=" + purge.failures.get()
					+ ", runs=" + purge.runs.size() + "; no retry within the same day after one more tick");
			int beforeNext = purge.runs.size();
			clock.shift(Duration.ofDays(1));
			await(() -> purge.runs.size() > beforeNext, 20_000);
			OUT.println("A8h the next UTC day's run after the failure: runs=" + purge.runs);
			clock.reset();
		}
	}

	/**
	 * Design-review DR-01: with urlshort.click.purge-enabled=false (a hold; the functional suite's overlay) neither the
	 * startup run nor a tick happens, so moving the clock past the next 00:10Z runs
	 * nothing and no purge line can land in a later request's log window.
	 */
	static void a8DailyPurgeOff() throws Exception {
		ProbePurge.autoStart = true;
		ProbePurge.syncStartup = true;
		ProbePurge.batched = false;
		try (ConfigurableApplicationContext ctx = start("jdbc:h2:mem:probe-a8off;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
				"--urlshort.click.purge-enabled=false")) {
			ProbePurge purge = ctx.getBean(ProbePurge.class);
			FunctionalClock clock = ctx.getBean(FunctionalClock.class);
			JdbcClient jdbc = ctx.getBean(JdbcClient.class);
			OUT.println("A8off purge-enabled=false: runs after readiness=" + purge.runs + ", purgeEnabled bound=" + purge.purgeEnabled);
			jdbc.sql("INSERT INTO link (code, url, created_at) VALUES ('probeOff1', 'https://example.org/off', :at)")
					.param("at", clock.instant().atOffset(ZoneOffset.UTC)).update();
			long link = jdbc.sql("SELECT id FROM link WHERE code = 'probeOff1'").query(Long.class).single();
			insert(jdbc, link, T.minusDays(90), 2);
			clock.shift(Duration.between(clock.instant(), T.plusDays(1).atTime(0, 10, 1).toInstant(ZoneOffset.UTC)));
			Thread.sleep(11_000);
			OUT.println("A8off suite clock at " + clock.instant() + " for 11 s (two ticks' worth), no trigger: runs=" + purge.runs
					+ ", day " + T.minusDays(90) + " left=" + count(jdbc, "clicked_on = DATE '" + T.minusDays(90) + "'")
					+ " (with the purge enabled, A8b deleted it after 4.9 s)");
			clock.reset();
		}
	}

	// ------------------------------------------------------------------ A4: invalid settings

	static void a4InvalidSettingStopsStartup() throws Exception {
		String url = url(ROOT.resolve("a4"));
		migrate(url);
		try (Connection c = DriverManager.getConnection(url, "sa", "")) {
			c.setAutoCommit(false);
			long link = insertLink(c, "probeA4x1");
			insertDays(c, link, T.minusDays(100), T.minusDays(100), 3);
		}
		for (String value : List.of("0", "-5", "ninety")) {
			ProbePurge.autoStart = true;
			ProbePurge.starts.set(0);
			synchronized (WINDOW) {
				WINDOW.reset();
			}
			recording = true;
			String outcome;
			try (ConfigurableApplicationContext ctx = start(url, "--urlshort.click.retention-days=" + value)) {
				outcome = "STARTED (unexpected)";
			}
			catch (Exception ex) {
				Throwable root = ex;
				while (root.getCause() != null) {
					root = root.getCause();
				}
				outcome = "failed: " + ex.getClass().getSimpleName() + " root " + root.getClass().getSimpleName();
			}
			recording = false;
			String captured;
			synchronized (WINDOW) {
				captured = WINDOW.toString(StandardCharsets.UTF_8);
			}
			long old;
			try (Connection c = DriverManager.getConnection(url, "sa", "")) {
				old = count(c, "clicked_on < DATE '" + T.minusDays(90) + "'");
			}
			OUT.println("A4 retention-days=" + value + ": " + outcome + "; failure text names the setting="
					+ captured.contains("urlshort.click.retention-days") + ", echoes the value=" + captured.contains("\\\"" + value + "\\\"")
					+ ", names its origin=" + captured.contains("Origin") + ", contains the datasource URL=" + captured.contains("jdbc:h2")
					+ "; purge started=" + ProbePurge.starts.get() + "; old clicks still stored=" + old);
		}
	}

	// ------------------------------------------------------------------ L: lock behaviour under load

	static Path populateBase() throws Exception {
		Path dir = ROOT.resolve("base");
		String url = url(dir);
		migrate(url);
		long t0 = System.nanoTime();
		try (Connection c = DriverManager.getConnection(url, "sa", "")) {
			c.setAutoCommit(false);
			long hot = insertLink(c, "probeHot1");
			insertDays(c, hot, T.minusDays(200), T.minusDays(91), 1_000_000);
			insertDays(c, hot, T.minusDays(90), T.minusDays(1), 300_000);
		}
		OUT.println("BASE 1 000 000 clicks on " + T.minusDays(200) + ".." + T.minusDays(91) + " and 300 000 on "
				+ T.minusDays(90) + ".." + T.minusDays(1) + " for link probeHot1, inserted in day order, in " + ms(t0)
				+ " ms; file " + Files.size(dir.resolve("urlshort.mv.db")) / (1024 * 1024) + " MB");
		return dir;
	}

	/**
	 * 1 300 000 clicks over 2 000 links chosen at random, in day order: either 91 days ending yesterday (a
	 * steady-state run on day T deletes only day T-91) or 1 000 000 beyond the period plus 300 000 within it.
	 */
	static Path populateSpread(String name, boolean catchUp) throws Exception {
		Path dir = ROOT.resolve(name);
		String url = url(dir);
		migrate(url);
		long t0 = System.nanoTime();
		try (Connection c = DriverManager.getConnection(url, "sa", "")) {
			c.setAutoCommit(false);
			long[] links = new long[2_000];
			for (int i = 0; i < links.length; i++) {
				links[i] = insertLink(c, i == 0 ? "probeHot1" : String.format("probeL%04d", i));
			}
			java.util.Random random = new java.util.Random(42);
			if (catchUp) {
				insertDays(c, links, random, T.minusDays(200), T.minusDays(91), 1_000_000);
				insertDays(c, links, random, T.minusDays(90), T.minusDays(1), 300_000);
			}
			else {
				insertDays(c, links, random, T.minusDays(91), T.minusDays(1), 1_300_000);
			}
		}
		OUT.println("BASE " + name + ": 1 300 000 clicks over 2 000 links, "
				+ (catchUp ? "1 000 000 on " + T.minusDays(200) + ".." + T.minusDays(91) + " and 300 000 on " + T.minusDays(90)
						+ ".." + T.minusDays(1) : "on " + T.minusDays(91) + ".." + T.minusDays(1) + " (day T-91 holds about 14 300)")
				+ ", inserted in day order in " + ms(t0) + " ms; file " + Files.size(dir.resolve("urlshort.mv.db")) / (1024 * 1024) + " MB");
		return dir;
	}

	/** One batch in the IN-subquery form, cancelled by a 20 s query timeout if it has not finished. */
	static void l0SubqueryBatchForm(Path base) throws Exception {
		String url = url(copy(base, "L0"));
		try (Connection c = DriverManager.getConnection(url, "sa", "");
				PreparedStatement ps = c.prepareStatement(SUBQUERY_BATCH_SQL.replace(":cutoff", "?"))) {
			ps.setObject(1, T.minusDays(90));
			ps.setQueryTimeout(20);
			long t0 = System.nanoTime();
			String outcome;
			try {
				outcome = "deleted " + ps.executeUpdate();
			}
			catch (SQLException ex) {
				outcome = "cancelled (" + ex.getClass().getSimpleName() + " " + ex.getErrorCode() + ")";
			}
			OUT.println("L0 one batch as DELETE ... WHERE id IN (SELECT id ... FETCH FIRST 10000 ROWS ONLY): " + outcome + " after "
					+ ms(t0) + " ms (H2 re-runs the subquery per candidate row: batch-subquery-jstack.txt)");
		}
	}

	static void lockVariant(String name, Path base, boolean batched, boolean index) throws Exception {
		Path dir = copy(base, name.substring(0, 2));
		String url = url(dir);
		try (Connection c = DriverManager.getConnection(url, "sa", "")) {
			if (index) {
				long t0 = System.nanoTime();
				exec(c, "CREATE INDEX ix_click_day ON click (clicked_on)");
				OUT.println(name + ": CREATE INDEX ix_click_day ON click (clicked_on) on 1 300 000 rows took " + ms(t0) + " ms");
			}
			String literal = "DATE '" + T.minusDays(90) + "'";
			OUT.println(name + ": plan " + explain(c, batched ? BATCH_SELECT_SQL.replace(":cutoff", literal)
					: "DELETE FROM click WHERE clicked_on < " + literal));
		}
		ProbePurge.autoStart = false;
		ProbePurge.batched = batched;
		try (ConfigurableApplicationContext ctx = start(url)) {
			ProbePurge purge = ctx.getBean(ProbePurge.class);
			JdbcClient jdbc = ctx.getBean(JdbcClient.class);
			HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
			URI target = URI.create("http://127.0.0.1:" + ctx.getEnvironment().getRequiredProperty("local.server.port") + "/probeHot1");
			long until = System.nanoTime() + 3_000_000_000L;
			Load baseline = load(client, target, () -> System.nanoTime() > until);
			long t0 = System.nanoTime();
			Future<?> run = purge.trigger();
			Load during = load(client, target, run::isDone);
			long runMs = ms(t0);
			run.get();
			int redirected = baseline.ok + during.ok;
			boolean settled = await(() -> count(jdbc, "clicked_on = DATE '" + T + "'") == redirected, 15_000);
			OUT.println(name + ": run " + runMs + " ms, " + purge.batches + " statement(s), longest " + purge.maxBatchMillis
					+ " ms; deleted " + purge.lastDeleted + "; old left " + count(jdbc, "clicked_on < DATE '" + T.minusDays(90) + "'")
					+ "; kept " + count(jdbc, "clicked_on >= DATE '" + T.minusDays(90) + "' AND clicked_on < DATE '" + T + "'"));
			OUT.println(name + ": redirects before  " + baseline);
			OUT.println(name + ": redirects during  " + during);
			OUT.println(name + ": every redirect's click stored after the run: " + settled + " (" + redirected + " redirects, "
					+ count(jdbc, "clicked_on = DATE '" + T + "'") + " rows for day T)");
		}
	}

	// ------------------------------------------------------------------ D: shutdown during a purge, then the startup run

	static void d1ShutdownDuringBatchedRunThenStartupRun(Path base) throws Exception {
		String url = url(copy(base, "d1"));
		ProbePurge.autoStart = true;
		ProbePurge.batched = true;
		ConfigurableApplicationContext ctx = start(url);
		ProbePurge purge = ctx.getBean(ProbePurge.class);
		await(() -> purge.batches >= 5, 30_000);
		long t0 = System.nanoTime();
		ctx.close();
		long closeMs = ms(t0);
		try (Connection c = DriverManager.getConnection(url, "sa", "")) {
			OUT.println("D1 context closed while the batched startup run was deleting (after " + purge.batches
					+ " batches): close() took " + closeMs + " ms; old left " + count(c, "clicked_on < DATE '" + T.minusDays(90)
							+ "'") + "; kept " + count(c, "clicked_on >= DATE '" + T.minusDays(90) + "'"));
		}
		try (ConfigurableApplicationContext again = start(url)) {
			ProbePurge next = again.getBean(ProbePurge.class);
			await(() -> next.runs.size() >= 1, 120_000);
			JdbcClient jdbc = again.getBean(JdbcClient.class);
			OUT.println("D1 restarted on the same file: startup run done " + next.lastRunDoneMillisAfterReady
					+ " ms after ApplicationReadyEvent, deleted " + next.lastDeleted + "; old left "
					+ count(jdbc, "clicked_on < DATE '" + T.minusDays(90) + "'") + "; kept "
					+ count(jdbc, "clicked_on >= DATE '" + T.minusDays(90) + "'"));
		}
	}

	/**
	 * A single-statement run that outlasts the purge's close deadline (2 s): what close() costs, what the
	 * pool's abort does to the running DELETE, and whether clicks recorded just before the stop are written.
	 */
	static void d3ShutdownPastTheDeadline(Path base) throws Exception {
		String url = url(copy(base, "d3"));
		ProbePurge.autoStart = false;
		ProbePurge.batched = false;
		ProbePurge.closeDeadlineMillis = 2_000;
		ConfigurableApplicationContext ctx = start(url, "--logging.level.com.zaxxer.hikari=info");
		ProbePurge purge = ctx.getBean(ProbePurge.class);
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
		URI target = URI.create("http://127.0.0.1:" + ctx.getEnvironment().getRequiredProperty("local.server.port") + "/probeHot1");
		purge.trigger();
		Thread.sleep(1_000);
		int redirected = 0;
		for (int i = 0; i < 20; i++) {
			if (client.send(HttpRequest.newBuilder(target).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 302) {
				redirected++;
			}
		}
		synchronized (WINDOW) {
			WINDOW.reset();
		}
		recording = true;
		long t0 = System.nanoTime();
		ctx.close();
		long closeMs = ms(t0);
		boolean ended = await(() -> purge.runs.size() + purge.failures.get() > 0, 60_000);
		long endedMs = ms(t0);
		Thread.sleep(500);
		recording = false;
		String captured;
		synchronized (WINDOW) {
			captured = WINDOW.toString(StandardCharsets.UTF_8);
		}
		for (String line : captured.lines().toList()) {
			if (line.contains("ClickPurge") || line.contains("click lost") || line.contains("hikari") || line.contains("Exception")) {
				OUT.println("D3 log: " + (line.length() > 600 ? line.substring(0, 600) + "…" : line));
			}
		}
		try (Connection c = DriverManager.getConnection(url, "sa", "")) {
			OUT.println("D3 single DELETE of 1 000 000 rows still running when close() began (purge deadline 2 s): close() took " + closeMs
					+ " ms; the purge thread's run ended=" + ended + " " + endedMs + " ms after close began (runs=" + purge.runs
					+ ", failures=" + purge.failures.get() + "); reopened: old left " + count(c, "clicked_on < DATE '" + T.minusDays(90) + "'")
					+ ", kept " + count(c, "clicked_on >= DATE '" + T.minusDays(90) + "' AND clicked_on < DATE '" + T + "'")
					+ ", clicks of the " + redirected + " redirects sent just before the stop: " + count(c, "clicked_on = DATE '" + T + "'"));
		}
	}

	/**
	 * The stop path of a real process: a child JVM starts the service, begins a single-statement run over
	 * 1 000 000 old rows, and after 1 s calls System.exit, so Spring's shutdown hook closes the context
	 * (the purge waits 3 s, as designed) and the JVM then ends with the DELETE still running on the daemon
	 * thread. The parent reopens the file and counts.
	 */
	static void d4ProcessExitDuringTheDelete(Path base) throws Exception {
		Path dir = copy(base, "d4");
		String java = ProcessHandle.current().info().command().orElseThrow();
		Process child = new ProcessBuilder(java, "-Xmx1g", "-Dspring.profiles.active=functional", "-Dprobe.part=4child", "-cp",
				System.getProperty("java.class.path"), System.getProperty("probe.source"), url(dir)).inheritIO().start();
		long t0 = System.nanoTime();
		int exit = child.waitFor();
		long childMs = ms(t0);
		long t1 = System.nanoTime();
		try (Connection c = DriverManager.getConnection(url(dir), "sa", "")) {
			OUT.println("D4 child exited " + exit + " after " + childMs + " ms; the file reopened in " + ms(t1) + " ms; old left "
					+ count(c, "clicked_on < DATE '" + T.minusDays(90) + "'") + ", kept " + count(c, "clicked_on >= DATE '"
							+ T.minusDays(90) + "' AND clicked_on < DATE '" + T + "'") + ", day T " + count(c, "clicked_on = DATE '" + T + "'"));
		}
	}

	static void d4Child(String url) throws Exception {
		ProbePurge.autoStart = false;
		ProbePurge.batched = false;
		ProbePurge.closeDeadlineMillis = 3_000;
		ConfigurableApplicationContext ctx = start(url);
		ctx.getBean(ProbePurge.class).trigger();
		Thread.sleep(1_000);
		OUT.println("D4 child: System.exit(0) 1 s into the DELETE; Spring's shutdown hook closes the context");
		System.exit(0);
	}

	/** The startup run completes before Boot reports readiness (ACCEPTING_TRAFFIC) on a catch-up database. */
	static void a7SynchronousStartupRun(Path base, boolean batched) throws Exception {
		String name = batched ? "A7b" : "A7s";
		String url = url(copy(base, name));
		ProbePurge.autoStart = true;
		ProbePurge.syncStartup = true;
		ProbePurge.batched = batched;
		long t0 = System.nanoTime();
		try (ConfigurableApplicationContext ctx = start(url)) {
			long startedMs = ms(t0);
			ProbePurge purge = ctx.getBean(ProbePurge.class);
			JdbcClient jdbc = ctx.getBean(JdbcClient.class);
			OUT.println(name + " startup run " + (batched ? "in batches" : "as one DELETE") + " on 1 000 000 old of 1 300 000 clicks:"
					+ " run done " + purge.lastRunDoneMillisAfterReady + " ms after ApplicationReadyEvent, readiness ACCEPTING_TRAFFIC "
					+ purge.acceptingTrafficMillisAfterReady + " ms after it (after the run: "
					+ (purge.acceptingTrafficMillisAfterReady >= purge.lastRunDoneMillisAfterReady) + "); SpringApplication.run returned after "
					+ startedMs + " ms; deleted " + purge.lastDeleted + "; old left " + count(jdbc, "clicked_on < DATE '" + T.minusDays(90)
							+ "'") + "; kept " + count(jdbc, "clicked_on >= DATE '" + T.minusDays(90) + "'"));
		}
		ProbePurge.syncStartup = false;
	}

	static void d2ShutdownDuringSingleDelete(Path base) throws Exception {
		String url = url(copy(base, "d2"));
		ProbePurge.autoStart = true;
		ProbePurge.batched = false;
		ConfigurableApplicationContext ctx = start(url);
		Thread.sleep(1_000);
		long t0 = System.nanoTime();
		ctx.close();
		long closeMs = ms(t0);
		try (Connection c = DriverManager.getConnection(url, "sa", "")) {
			OUT.println("D2 context closed 1 s into a single-statement DELETE: close() took " + closeMs
					+ " ms; database reopened; old left " + count(c, "clicked_on < DATE '" + T.minusDays(90) + "'") + "; kept "
					+ count(c, "clicked_on >= DATE '" + T.minusDays(90) + "'"));
		}
	}

	// ------------------------------------------------------------------ the purge as design.md section 1 specifies it

	@ConfigurationProperties("urlshort.click")
	@Validated
	public record RetentionProps(@DefaultValue("90") @Positive int retentionDays, @DefaultValue("true") boolean purgeEnabled) {
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(RetentionProps.class)
	public static class ProbeConfig {

		@Bean
		public ProbePurge probePurge(JdbcClient jdbc, Clock clock, RetentionProps props) {
			ProbePurge purge = new ProbePurge(jdbc, clock, props.retentionDays());
			purge.purgeEnabled = props.purgeEnabled();
			return purge;
		}
	}

	public static class ProbePurge {

		static final Logger log = LoggerFactory.getLogger("dev.urlshort.click.ClickPurge");
		static final LocalTime AT = LocalTime.of(0, 10);
		static final long TICK_MILLIS = 5_000;
		static volatile boolean autoStart = true;
		static volatile boolean batched = true;
		static volatile boolean failNext;
		static volatile boolean syncStartup;
		static volatile long closeDeadlineMillis = 10_000;
		static final AtomicInteger starts = new AtomicInteger();
		volatile long acceptingTrafficMillisAfterReady = -1;
		volatile boolean purgeEnabled = true;

		final JdbcClient jdbc;
		final Clock clock;
		final int days;
		final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
			Thread thread = new Thread(task, "click-purge");
			thread.setDaemon(true);
			return thread;
		});
		final List<String> runs = Collections.synchronizedList(new ArrayList<>());
		final AtomicInteger failures = new AtomicInteger();
		volatile boolean stopping;
		volatile LocalDate lastPurgedDay;
		volatile long readyAt;
		volatile long lastRunDoneMillisAfterReady;
		volatile int batches;
		volatile long maxBatchMillis;
		volatile long lastDeleted;

		ProbePurge(JdbcClient jdbc, Clock clock, int days) {
			this.jdbc = jdbc;
			this.clock = clock;
			this.days = days;
		}

		@EventListener(ApplicationReadyEvent.class)
		public void start() throws Exception {
			readyAt = System.nanoTime();
			starts.incrementAndGet();
			if (autoStart) {
				if (!purgeEnabled) {
					// DR-01, the lead's operational reason: a hold pauses every deletion, startup and daily
					log.atWarn().setMessage("click purge off").addKeyValue("retentionDays", days).log();
					return;
				}
				if (syncStartup) {
					// the design: the startup run completes on the purge thread before Boot reports readiness
					executor.submit(this::run).get();
				}
				else {
					executor.execute(this::run);
				}
				executor.scheduleWithFixedDelay(this::tick, TICK_MILLIS, TICK_MILLIS, TimeUnit.MILLISECONDS);
			}
		}

		@EventListener
		public void readiness(org.springframework.boot.availability.AvailabilityChangeEvent<org.springframework.boot.availability.ReadinessState> event) {
			if (event.getState() == org.springframework.boot.availability.ReadinessState.ACCEPTING_TRAFFIC) {
				acceptingTrafficMillisAfterReady = (System.nanoTime() - readyAt) / 1_000_000;
			}
		}

		Future<?> trigger() {
			return executor.submit(this::run);
		}

		void tick() {
			Instant now = clock.instant();
			if (LocalDate.ofInstant(now, ZoneOffset.UTC).isAfter(lastPurgedDay)
					&& !LocalTime.ofInstant(now, ZoneOffset.UTC).isBefore(AT)) {
				run();
			}
		}

		void run() {
			LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
			lastPurgedDay = today;
			LocalDate cutoff = today.minusDays(days);
			batches = 0;
			maxBatchMillis = 0;
			try {
				if (failNext) {
					failNext = false;
					throw new org.springframework.dao.DataAccessResourceFailureException("canary-stored-value 203.0.113.9");
				}
				long deleted = 0;
				int k;
				do {
					long t0 = System.nanoTime();
					if (batched) {
						List<Long> ids = jdbc.sql(BATCH_SELECT_SQL).param("cutoff", cutoff).query(Long.class).list();
						k = ids.isEmpty() ? 0 : jdbc.sql(BATCH_DELETE_SQL).param("ids", ids).update();
					}
					else {
						k = jdbc.sql("DELETE FROM click WHERE clicked_on < :cutoff").param("cutoff", cutoff).update();
					}
					maxBatchMillis = Math.max(maxBatchMillis, ms(t0));
					batches++;
					deleted += k;
				}
				while (batched && k == 10_000 && !stopping);
				lastDeleted = deleted;
				log.atInfo().setMessage(stopping ? "click purge stopped" : "clicks purged").addKeyValue("deleted", deleted)
						.addKeyValue("cutoff", cutoff.toString()).addKeyValue("retentionDays", days).log();
				runs.add(today + ":" + deleted);
				lastRunDoneMillisAfterReady = (System.nanoTime() - readyAt) / 1_000_000;
			}
			catch (RuntimeException ex) {
				failures.incrementAndGet();
				log.atWarn().setMessage("click purge failed").addKeyValue("cutoff", cutoff.toString())
						.addKeyValue("retentionDays", days).addKeyValue("errorType", ex.getClass().getName()).log();
			}
		}

		@PreDestroy
		public void close() throws InterruptedException {
			stopping = true;
			executor.shutdown();
			executor.awaitTermination(closeDeadlineMillis, TimeUnit.MILLISECONDS);
		}
	}

	// ------------------------------------------------------------------ helpers

	record Load(List<Long> micros, int ok, int other) {

		@Override
		public String toString() {
			List<Long> sorted = new ArrayList<>(micros);
			sorted.sort(Comparator.naturalOrder());
			if (sorted.isEmpty()) {
				return "none";
			}
			return sorted.size() + " requests (" + ok + " x 302, " + other + " other) p50 " + pct(sorted, 0.50) + " p95 "
					+ pct(sorted, 0.95) + " p99 " + pct(sorted, 0.99) + " max " + String.format("%.1f", sorted.getLast() / 1000.0) + " ms";
		}

		static String pct(List<Long> sorted, double p) {
			return String.format("%.1f", sorted.get(Math.min(sorted.size() - 1, (int) Math.ceil(p * sorted.size()) - 1)) / 1000.0);
		}
	}

	/** One sequential client paced at 100 requests per second (NFR-L1's rate) until the condition holds. */
	static Load load(HttpClient client, URI target, BooleanSupplier stop) throws Exception {
		List<Long> micros = new ArrayList<>();
		int ok = 0;
		int other = 0;
		long next = System.nanoTime();
		long cap = next + 180_000_000_000L;
		while (!stop.getAsBoolean() && System.nanoTime() < cap) {
			long t0 = System.nanoTime();
			int status = client.send(HttpRequest.newBuilder(target).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode();
			micros.add((System.nanoTime() - t0) / 1000);
			if (status == 302) {
				ok++;
			}
			else {
				other++;
			}
			next += 10_000_000L;
			long wait = next - System.nanoTime();
			if (wait > 0) {
				Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
			}
		}
		return new Load(micros, ok, other);
	}

	static ConfigurableApplicationContext start(String url, String... more) {
		List<String> args = new ArrayList<>(List.of(ARGS));
		args.add("--spring.datasource.url=" + url);
		args.addAll(List.of(more));
		return new SpringApplicationBuilder(UrlshortApplication.class, ProbeConfig.class).run(args.toArray(String[]::new));
	}

	static String url(Path dir) {
		return "jdbc:h2:file:" + dir.resolve("urlshort") + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE";
	}

	static void migrate(String url) {
		Flyway.configure().dataSource(url, "sa", "").load().migrate();
	}

	static Path copy(Path base, String name) throws Exception {
		Path dir = ROOT.resolve(name);
		Files.createDirectories(dir);
		Files.copy(base.resolve("urlshort.mv.db"), dir.resolve("urlshort.mv.db"));
		return dir;
	}

	static long insertLink(Connection c, String code) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO link (code, url, created_at) VALUES (?, ?, ?)", new String[] { "ID" })) {
			ps.setString(1, code);
			ps.setString(2, "https://example.org/" + code);
			ps.setObject(3, T.minusDays(300).atStartOfDay().atOffset(ZoneOffset.UTC));
			ps.executeUpdate();
			try (ResultSet keys = ps.getGeneratedKeys()) {
				keys.next();
				long id = keys.getLong(1);
				c.commit();
				return id;
			}
		}
	}

	static void insertDays(Connection c, long linkId, LocalDate from, LocalDate to, int total) throws SQLException {
		insertDays(c, new long[] { linkId }, null, from, to, total);
	}

	/** {@code total} clicks spread evenly over the days from {@code from} to {@code to}, in day order. */
	static void insertDays(Connection c, long[] linkIds, java.util.Random random, LocalDate from, LocalDate to, int total)
			throws SQLException {
		long days = ChronoUnit.DAYS.between(from, to) + 1;
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO click (link_id, clicked_at, clicked_on, referrer,"
				+ " user_agent_class, client_hash) VALUES (?, ?, ?, ?, 'browser', ?)")) {
			for (int i = 0; i < total; i++) {
				LocalDate day = from.plusDays(i * days / total);
				ps.setLong(1, random == null ? linkIds[0] : linkIds[random.nextInt(linkIds.length)]);
				ps.setObject(2, day.atStartOfDay().atOffset(ZoneOffset.UTC).plusSeconds(i % 86_400));
				ps.setObject(3, day);
				ps.setString(4, i % 3 == 0 ? null : "https://ref" + i % 50 + ".example");
				ps.setString(5, HASH);
				ps.addBatch();
				if ((i + 1) % 10_000 == 0) {
					ps.executeBatch();
					c.commit();
				}
			}
			ps.executeBatch();
			c.commit();
		}
	}

	static void insert(JdbcClient jdbc, long linkId, LocalDate day, int n) {
		for (int i = 0; i < n; i++) {
			jdbc.sql("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
					+ " VALUES (:link, :at, :day, NULL, 'browser', :hash)").param("link", linkId)
					.param("at", day.atTime(12, 0).atOffset(ZoneOffset.UTC)).param("day", day).param("hash", HASH).update();
		}
	}

	static long count(JdbcClient jdbc, String where) {
		return jdbc.sql("SELECT COUNT(*) FROM click WHERE " + where).query(Long.class).single();
	}

	static long count(Connection c, String where) throws SQLException {
		try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM click WHERE " + where)) {
			rs.next();
			return rs.getLong(1);
		}
	}

	static void exec(Connection c, String sql) throws SQLException {
		try (Statement s = c.createStatement()) {
			s.execute(sql);
		}
	}

	static String explain(Connection c, String sql) throws SQLException {
		try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("EXPLAIN " + sql)) {
			rs.next();
			return rs.getString(1).replaceAll("\\s+", " ");
		}
	}

	static boolean await(BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
		long deadline = System.nanoTime() + timeoutMillis * 1_000_000;
		while (!condition.getAsBoolean()) {
			if (System.nanoTime() > deadline) {
				return false;
			}
			Thread.sleep(100);
		}
		return true;
	}

	static long ms(long t0) {
		return (System.nanoTime() - t0) / 1_000_000;
	}
}
