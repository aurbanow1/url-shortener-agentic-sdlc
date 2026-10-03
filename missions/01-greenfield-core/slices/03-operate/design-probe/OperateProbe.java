import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.urlshort.UrlshortApplication;
import tools.jackson.databind.json.JsonMapper;

/**
 * Design probe for slice 03-operate. Boots the shipped application (main at the design commit,
 * functional profile, in-memory H2) on a random loopback port with the configuration the design
 * proposes, plus three probe beans: a minimal rate-limit filter that rejects every request carrying
 * the header Probe-Reject (to observe the 429's shape, its log line and its metric), a DataSource
 * proxy whose connections fail while a flag is set (readiness), and nothing else. It then checks
 * metric tags under canaries, Tomcat's pre-application rejections, the bucket arithmetic of AC-3/AC-4
 * on a fixed clock, and the graceful-shutdown drain of AC-25 with a held request, a refused probe
 * connection and a closed-loop load client over three stop cycles. No file under src/ is touched.
 */
public class OperateProbe {

	static final PrintStream STDOUT = System.out;
	static final ByteArrayOutputStream WINDOW = new ByteArrayOutputStream();
	static volatile boolean recording;
	static final AtomicBoolean DB_DOWN = new AtomicBoolean();
	/** Command-line arguments, so they outrank the shipped application.properties (builder properties would not). */
	static final String[] PROPS = { "--server.port=0", "--server.address=127.0.0.1",
			"--management.endpoints.web.exposure.include=health,info,metrics,prometheus",
			"--management.endpoint.health.group.readiness.include=readinessState,db",
			"--spring.lifecycle.timeout-per-shutdown-phase=10s" };
	static final String QUIET_PARSER = "--logging.level.org.apache.coyote.http11.Http11Processor=warn";

	public static void main(String[] args) throws Exception {
		System.setOut(new PrintStream(new OutputStream() {
			@Override
			public void write(int b) {
				STDOUT.write(b);
				if (recording) {
					synchronized (WINDOW) {
						WINDOW.write(b);
					}
				}
			}

			@Override
			public void write(byte[] b, int off, int len) {
				STDOUT.write(b, off, len);
				if (recording) {
					synchronized (WINDOW) {
						WINDOW.write(b, off, len);
					}
				}
			}
		}, true));

		bucketArithmetic();

		ConfigurableApplicationContext ctx = start();
		int port = port(ctx);
		String base = "http://127.0.0.1:" + port;
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
		try {
			out("O0 Prometheus registry on the classpath", String.valueOf(isPresent("io.micrometer.prometheusmetrics.PrometheusMeterRegistry")));

			startWindow();
			HttpResponse<String> rejected = client.send(HttpRequest.newBuilder(URI.create(base + "/api/links"))
					.header("Probe-Reject", "1").header("User-Agent", "ua-canary-31f0").header("X-Forwarded-For", "192.0.2.201")
					.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"url\":\"https://example.com/?q=url-canary-77\"}"))
					.build(), HttpResponse.BodyHandlers.ofString());
			Thread.sleep(300);
			String window = stopWindow();
			String id = rejected.headers().firstValue("x-request-id").orElse("");
			out("O1 429 written by a servlet filter with the context's JsonMapper", "status=" + rejected.statusCode() + " headers="
					+ rejected.headers().map() + "\n    body=" + rejected.body() + "\n    VERDICT instanceIsUrnOfRequestId="
					+ rejected.body().contains("urn:uuid:" + id) + " logLines=" + lines(window).size() + " linesWithoutThisRequestId="
					+ lines(window).stream().filter(l -> !l.contains("\"requestId\":\"" + id + "\"")).count() + " canaryInLogs="
					+ (window.contains("ua-canary-31f0") || window.contains("192.0.2.201") || window.contains("url-canary-77"))
					+ lines(window).stream().map(l -> "\n    log> " + l).reduce("", String::concat));
			out("O1b filter chain seen by the probe limiter (outermost first)", ProbeLimiter.CHAIN.get());

			String code = createLink(client, base);
			for (int i = 0; i < 3; i++) {
				client.send(HttpRequest.newBuilder(URI.create(base + "/" + code)).GET().build(), HttpResponse.BodyHandlers.discarding());
			}
			client.send(HttpRequest.newBuilder(URI.create(base + "/zzCanary99")).GET().build(), HttpResponse.BodyHandlers.discarding());
			client.send(HttpRequest.newBuilder(URI.create(base + "/favicon-canary-55.ico")).GET().build(), HttpResponse.BodyHandlers.discarding());
			client.send(HttpRequest.newBuilder(URI.create(base + "/api/links")).method("PROBEMETHODCANARY", HttpRequest.BodyPublishers.noBody()).build(),
					HttpResponse.BodyHandlers.discarding());
			String requests = client.send(HttpRequest.newBuilder(URI.create(base + "/actuator/metrics/http.server.requests")).GET().build(),
					HttpResponse.BodyHandlers.ofString()).body();
			out("O2 http.server.requests tags after redirects, a domain 404 on /zzCanary99, a no-handler 404, a custom method, a filter 429",
					requests.substring(requests.indexOf("\"availableTags\"")));
			String names = client.send(HttpRequest.newBuilder(URI.create(base + "/actuator/metrics")).GET().build(),
					HttpResponse.BodyHandlers.ofString()).body();
			out("O2b metric names containing hikari, jdbc or urlshort", names.replaceAll(".*\"names\":\\[", "").replace("]}", "")
					.lines().flatMap(l -> List.of(l.split(",")).stream()).filter(n -> n.contains("hikari") || n.contains("jdbc")
							|| n.contains("urlshort")).toList().toString());
			HttpResponse<String> prom = client.send(HttpRequest.newBuilder(URI.create(base + "/actuator/prometheus")).GET().build(),
					HttpResponse.BodyHandlers.ofString());
			out("O3 /actuator/prometheus", "status=" + prom.statusCode() + " content-type=" + prom.headers().firstValue("content-type").orElse("")
					+ " bytes=" + prom.body().length() + "\n    VERDICT containsCode=" + prom.body().contains(code) + " containsZzCanary99="
					+ prom.body().contains("zzCanary99") + " containsMethodCanary=" + prom.body().contains("PROBEMETHODCANARY")
					+ " containsFaviconCanary=" + prom.body().contains("favicon-canary-55") + " containsTargetUrl="
					+ prom.body().contains("example.com") + "\n    families: " + prom.body().lines()
							.filter(l -> l.startsWith("# TYPE") && (l.contains("http_server_requests") || l.contains("hikaricp_connections")
									|| l.contains("urlshort")))
							.toList()
					+ "\n    redirect-route lines: " + prom.body().lines().filter(l -> l.startsWith("http_server_requests_seconds_count")
							&& l.contains("status=\"302\"")).toList()
					+ "\n    method tags: " + prom.body().lines().filter(l -> l.startsWith("http_server_requests_seconds_count"))
							.map(l -> l.replaceAll(".*method=\"([^\"]*)\".*", "$1")).distinct().toList());

			for (String path : List.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness")) {
				HttpResponse<String> r = client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
				out("O4 " + path + " with the database answering", r.statusCode() + " " + r.body());
			}
			DB_DOWN.set(true);
			startWindow();
			for (String path : List.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness")) {
				HttpResponse<String> r = client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
				out("O5 " + path + " while getConnection fails", r.statusCode() + " " + r.body());
			}
			window = stopWindow();
			DB_DOWN.set(false);
			out("O5b log lines while the database was down", lines(window).stream().filter(l -> l.startsWith("{"))
					.map(l -> "\n    log> " + (l.length() > 600 ? l.substring(0, 600) + "…" : l)).reduce("", String::concat));
			HttpResponse<String> back = client.send(HttpRequest.newBuilder(URI.create(base + "/actuator/health/readiness")).GET().build(),
					HttpResponse.BodyHandlers.ofString());
			out("O5c readiness after the database answers again", back.statusCode() + " " + back.body());

			startWindow();
			String malformed = raw(port, "GET /a-target-canary-9e1\u0001x HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n");
			String badMethod = raw(port, "METHOD{canary-c4} / HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n");
			String bigHeader = raw(port, "GET /api/ping HTTP/1.1\r\nHost: x\r\nX-Big: header-canary-7a2" + "y".repeat(9000)
					+ "\r\nConnection: close\r\n\r\n");
			Thread.sleep(300);
			window = stopWindow();
			out("O6 pre-application rejections", "malformed target → " + firstLine(malformed) + " | bad method → " + firstLine(badMethod)
					+ " | 9 KB header → " + firstLine(bigHeader) + "\n    VERDICT canaryInLogs=" + (window.contains("canary-9e1")
							|| window.contains("canary-c4") || window.contains("canary-7a2"))
					+ lines(window).stream().map(l -> "\n    log> " + (l.length() > 500 ? l.substring(0, 500) + "…" : l)).reduce("", String::concat));
		}
		finally {
			ctx.close();
		}

		ConfigurableApplicationContext quiet = start(QUIET_PARSER);
		try {
			startWindow();
			String malformed = raw(port(quiet), "GET /a-target-canary-9e1\u0001x HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n");
			Thread.sleep(300);
			String window = stopWindow();
			out("O6b the same malformed target in a fresh context with Http11Processor at WARN", "→ " + firstLine(malformed)
					+ "\n    VERDICT canaryInLogs=" + window.contains("canary-9e1") + " logLines=" + lines(window).size());
		}
		finally {
			quiet.close();
		}

		for (int cycle = 1; cycle <= 2; cycle++) {
			drainCycle("D" + cycle + " closed loop, 4 clients as fast as possible", 4, 0);
		}
		for (int cycle = 1; cycle <= 10; cycle++) {
			drainCycle("P" + cycle + " paced, 5 clients x 20 req/s = 100 req/s (NFR-L1's rate)", 5, 50);
		}
	}

	/** AC-3 (a), AC-3 (b) and AC-4 replayed on a fixed clock against the bucket the design specifies. */
	static void bucketArithmetic() {
		long t0 = Instant.parse("2026-10-03T12:00:00Z").toEpochMilli();
		Bucket b = new Bucket(60);
		int admitted = 0;
		for (int i = 0; i < 60; i++) {
			admitted += b.tryTake(t0) == 0 ? 1 : 0;
		}
		long s = b.tryTake(t0);
		long r1 = b.tryTake(t0 + 999);
		long r2 = b.tryTake(t0 + 1000);
		long r3 = b.tryTake(t0 + 1000);
		out("B1 AC-3(a) on a frozen clock", "admitted=" + admitted + " 61st Retry-After=" + s + " +999ms=" + verdict(r1)
				+ " +1000ms=" + verdict(r2) + " again=" + verdict(r3));
		Bucket c = new Bucket(60);
		for (int i = 0; i < 60; i++) {
			c.tryTake(t0);
		}
		long sb = c.tryTake(t0 + 250);
		out("B2 AC-3(b)", "after 250 ms Retry-After=" + sb + "; after a further " + sb + " s: " + verdict(c.tryTake(t0 + 250 + sb * 1000)));
		Bucket d = new Bucket(60);
		for (int i = 0; i < 61; i++) {
			d.tryTake(t0);
		}
		int again = 0;
		long last = 0;
		for (int i = 0; i < 61; i++) {
			last = d.tryTake(t0 + 60_000);
			again += last == 0 ? 1 : 0;
		}
		out("B3 AC-4", "after a quiet minute: admitted=" + again + " 61st=" + verdict(last) + "; full again (releasable) at t0+60s before"
				+ " the burst=" + new Bucket(60).fullAt(t0) + " -> " + (b.fullAt(t0 + 60_000)) + " ; redirect interval ms="
				+ (60_000.0 / 600));
	}

	static String verdict(long retryAfter) {
		return retryAfter == 0 ? "admit" : "429(Retry-After " + retryAfter + ")";
	}

	/**
	 * The design's bucket: GCRA form of rule 2's token bucket. One long per bucket, the theoretical
	 * arrival time, in milliseconds. Capacity N per minute, emission interval 60 000/N ms.
	 */
	static final class Bucket {
		final double interval;
		final double tolerance;
		double tat = Double.NEGATIVE_INFINITY;

		Bucket(int perMinute) {
			interval = 60_000.0 / perMinute;
			tolerance = interval * (perMinute - 1);
		}

		/** @return 0 when admitted, else the Retry-After in whole seconds (≥ 1) */
		long tryTake(long now) {
			double start = Math.max(tat, now);
			double wait = start - tolerance - now;
			if (wait > 0) {
				return (long) Math.ceil(wait / 1000.0);
			}
			tat = start + interval;
			return 0;
		}

		boolean fullAt(long now) {
			return tat <= now;
		}
	}

	static void drainCycle(String label, int clients, long periodMs) throws Exception {
		ConfigurableApplicationContext ctx = start(QUIET_PARSER);
		int port = port(ctx);
		AtomicBoolean loading = new AtomicBoolean(true);
		AtomicInteger ok = new AtomicInteger();
		AtomicInteger refused = new AtomicInteger();
		AtomicInteger failures = new AtomicInteger();
		List<String> failureKinds = java.util.Collections.synchronizedList(new ArrayList<>());
		List<Thread> load = new ArrayList<>();
		for (int t = 0; t < clients; t++) {
			Thread thread = new Thread(() -> {
				while (loading.get()) {
					if (periodMs > 0) {
						try {
							Thread.sleep(periodMs);
						}
						catch (InterruptedException e) {
							return;
						}
					}
					try (Socket socket = new Socket()) {
						try {
							socket.connect(new InetSocketAddress("127.0.0.1", port), 1000);
						}
						catch (ConnectException e) {
							refused.incrementAndGet();
							Thread.sleep(5);
							continue;
						}
						socket.setSoTimeout(15_000);
						socket.getOutputStream().write("GET /api/ping HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n"
								.getBytes(StandardCharsets.US_ASCII));
						String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
						if (response.startsWith("HTTP/1.1 2") || response.startsWith("HTTP/1.1 3")) {
							ok.incrementAndGet();
						}
						else {
							failures.incrementAndGet();
							failureKinds.add(response.isEmpty() ? "EOF before response" : firstLine(response));
						}
					}
					catch (IOException e) {
						failures.incrementAndGet();
						failureKinds.add(e.getClass().getSimpleName() + ": " + e.getMessage());
					}
					catch (InterruptedException e) {
						return;
					}
				}
			});
			thread.start();
			load.add(thread);
		}
		Thread.sleep(500);
		Socket r0 = new Socket("127.0.0.1", port);
		r0.setSoTimeout(20_000);
		String json = "{\"url\":\"https://example.com/held\"}";
		r0.getOutputStream().write(("POST /api/links HTTP/1.1\r\nHost: x\r\nContent-Type: application/json\r\nContent-Length: " + json.length()
				+ "\r\nConnection: close\r\n\r\n" + json.substring(0, 10)).getBytes(StandardCharsets.US_ASCII));
		r0.getOutputStream().flush();
		Thread.sleep(300);
		int okBeforeStop = ok.get();
		long stop = System.nanoTime();
		Thread closer = new Thread(ctx::close);
		closer.start();
		Thread.sleep(500);
		String probe;
		try (Socket s = new Socket()) {
			s.connect(new InetSocketAddress("127.0.0.1", port), 1000);
			probe = "ACCEPTED (unexpected)";
		}
		catch (ConnectException e) {
			probe = "refused (" + e.getMessage() + ")";
		}
		catch (IOException e) {
			probe = e.getClass().getSimpleName() + ": " + e.getMessage();
		}
		r0.getOutputStream().write(json.substring(10).getBytes(StandardCharsets.US_ASCII));
		r0.getOutputStream().flush();
		String r0Response = new String(r0.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
		long r0Ms = (System.nanoTime() - stop) / 1_000_000;
		r0.close();
		closer.join();
		long closeMs = (System.nanoTime() - stop) / 1_000_000;
		loading.set(false);
		for (Thread t : load) {
			t.join();
		}
		out(label + ": held POST, probe connect 500 ms after the stop",
				"R0 → " + firstLine(r0Response) + " after " + r0Ms + " ms; probe connect during drain → " + probe + "; context closed after "
						+ closeMs + " ms\n    VERDICT loadOk=" + ok.get() + " (before stop " + okBeforeStop + ") refusedBeforeAcceptance="
						+ refused.get() + " failuresAfterAcceptance=" + failures.get() + (failureKinds.isEmpty() ? "" : " " + failureKinds));
	}

	static ConfigurableApplicationContext start(String... more) {
		String[] args = java.util.Arrays.copyOf(PROPS, PROPS.length + more.length);
		System.arraycopy(more, 0, args, PROPS.length, more.length);
		return new SpringApplicationBuilder(UrlshortApplication.class, ProbeConfig.class).run(args);
	}

	static int port(ConfigurableApplicationContext ctx) {
		return ctx.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
	}

	static String createLink(HttpClient client, String base) throws Exception {
		String body = client.send(HttpRequest.newBuilder(URI.create(base + "/api/links")).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"url\":\"https://example.com/t\"}")).build(), HttpResponse.BodyHandlers.ofString()).body();
		int i = body.indexOf("\"code\":\"") + 8;
		return body.substring(i, body.indexOf('"', i));
	}

	static String raw(int port, String request) throws IOException {
		try (Socket socket = new Socket("127.0.0.1", port)) {
			socket.setSoTimeout(5000);
			socket.getOutputStream().write(request.getBytes(StandardCharsets.ISO_8859_1));
			InputStream in = socket.getInputStream();
			return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
		}
		catch (IOException e) {
			return e.getClass().getSimpleName() + ": " + e.getMessage();
		}
	}

	static String firstLine(String s) {
		int i = s.indexOf('\r');
		return i < 0 ? s : s.substring(0, i);
	}

	static boolean isPresent(String className) {
		try {
			Class.forName(className);
			return true;
		}
		catch (ClassNotFoundException e) {
			return false;
		}
	}

	static List<String> lines(String window) {
		return window.lines().filter(l -> !l.isBlank()).toList();
	}

	static void startWindow() {
		synchronized (WINDOW) {
			WINDOW.reset();
		}
		recording = true;
	}

	static String stopWindow() {
		recording = false;
		synchronized (WINDOW) {
			return WINDOW.toString(StandardCharsets.UTF_8);
		}
	}

	static void out(String title, String text) {
		STDOUT.println("PROBE " + title + "\n    " + text);
	}

	@Configuration(proxyBeanMethods = false)
	public static class ProbeConfig {
		@Bean
		ProbeLimiter probeLimiter(JsonMapper json) {
			return new ProbeLimiter(json);
		}

		@Bean
		static BeanPostProcessor failingDataSource() {
			return new BeanPostProcessor() {
				@Override
				public Object postProcessAfterInitialization(Object bean, String name) {
					if (!(bean instanceof DataSource target)) {
						return bean;
					}
					return Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class<?>[] { DataSource.class }, (p, m, a) -> {
						if (m.getName().equals("getConnection") && DB_DOWN.get()) {
							throw new SQLException("probe: database not answering");
						}
						try {
							return m.invoke(target, a);
						}
						catch (java.lang.reflect.InvocationTargetException e) {
							throw e.getCause();
						}
					});
				}
			};
		}
	}

	/** Stands in for the design's RateLimitFilter: rejects when Probe-Reject is present, writes the 429 itself. */
	@Order(Ordered.HIGHEST_PRECEDENCE + 2)
	public static class ProbeLimiter extends OncePerRequestFilter {
		static final java.util.concurrent.atomic.AtomicReference<String> CHAIN = new java.util.concurrent.atomic.AtomicReference<>("");
		private final JsonMapper json;

		ProbeLimiter(JsonMapper json) {
			this.json = json;
		}

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException {
			if (CHAIN.get().isEmpty()) {
				List<String> filters = new ArrayList<>();
				for (StackTraceElement f : new Throwable().getStackTrace()) {
					if (f.getMethodName().equals("doFilterInternal") || f.getMethodName().equals("doFilter")) {
						String name = f.getClassName().replaceAll(".*\\.", "");
						if (!name.equals("ApplicationFilterChain") && !name.equals("OncePerRequestFilter") && !filters.contains(name)) {
							filters.add(name);
						}
					}
				}
				java.util.Collections.reverse(filters);
				CHAIN.set(String.join(" → ", filters));
			}
			if (request.getHeader("Probe-Reject") == null) {
				chain.doFilter(request, response);
				return;
			}
			ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
			problem.setInstance(URI.create("urn:uuid:" + MDC.get("requestId")));
			response.setStatus(429);
			response.setHeader("Retry-After", "1");
			response.setContentType("application/problem+json");
			response.getOutputStream().write(json.writeValueAsBytes(problem));
		}
	}
}
