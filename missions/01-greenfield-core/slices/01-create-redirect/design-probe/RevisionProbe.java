import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import dev.urlshort.UrlshortApplication;

/**
 * Revision probe for slice 01-create-redirect, answering design review DR-01, DR-02 and DR-04.
 * Boots the shipped application on a random loopback port with the two shipped-property changes
 * the revised design adds (servlet load-on-startup, PageNotFound logger at ERROR) and probe beans
 * that use exactly the revised mechanisms: the advice's createResponseEntity override (no detail,
 * instance = urn:uuid:<request id>), the message-free "request failed" event, and the
 * "request completed" event with status only. Every request is sent with a canary and the probe
 * itself checks, by effect, the response body and every stdout line written while the request ran.
 * The first request is the cold one (DR-04). No file under src/ is touched.
 */
public class RevisionProbe {

	/** Stands in for dev.urlshort.: probe classes live in the unnamed package. */
	static final String APP_PREFIX = "RevisionProbe";

	static final PrintStream STDOUT = System.out;
	static final ByteArrayOutputStream WINDOW = new ByteArrayOutputStream();
	static volatile boolean recording;

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

		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(UrlshortApplication.class, ProbeConfig.class)
				.properties("server.port=0", "server.address=127.0.0.1", "spring.servlet.multipart.enabled=false",
						"spring.mvc.servlet.load-on-startup=1",
						"logging.level.org.springframework.web.servlet.PageNotFound=error")
				.run(args);
		out("startup", "context started; the DispatcherServlet initialisation lines above precede the 'Started' line");
		try {
			ctx.getBean(JdbcClient.class).sql("CREATE TABLE probe_key (idempotency_key VARCHAR(255) UNIQUE)").update();
			String base = "http://127.0.0.1:" + ctx.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
			HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
			String json = "{\"url\":\"https://example.com/\"}";

			String media = "probe-media-canary-41c7";
			check("R1 cold first request: 415, Content-Type parameter canary", client, HttpRequest.newBuilder(URI.create(base + "/probe/body"))
					.header("Content-Type", "text/plain; note=" + media).POST(HttpRequest.BodyPublishers.ofString(json)), media);

			String path = "probepathcanary" + "x".repeat(30);
			check("R2 404 no handler: 45-letter path canary", client,
					HttpRequest.newBuilder(URI.create(base + "/api/links/" + path)).GET(), path);

			String ico = "probe-ico-canary-9b2e";
			check("R3 404 no handler: /<canary>.ico", client,
					HttpRequest.newBuilder(URI.create(base + "/" + ico + ".ico")).header("Accept", "text/html,*/*;q=0.8").GET(), ico);

			String code = "probeCode77";
			check("R4 domain 404 (ErrorResponseException) on a code-shaped canary", client,
					HttpRequest.newBuilder(URI.create(base + "/probe/links/" + code)).GET(), code);

			String method = "PROBEMETHODCANARY";
			check("R5 405 on a custom method canary (PageNotFound WARN would echo it)", client,
					HttpRequest.newBuilder(URI.create(base + "/probe/body")).method(method, HttpRequest.BodyPublishers.noBody()), method);

			check("R6 405 PUT: Allow header kept", client,
					HttpRequest.newBuilder(URI.create(base + "/probe/body")).PUT(HttpRequest.BodyPublishers.ofString(json))
							.header("Content-Type", "application/json"));

			String key = "probe-key-canary-5d10";
			check("R7 500 from an H2 unique violation quoting the key canary", client,
					HttpRequest.newBuilder(URI.create(base + "/probe/collision")).header("Idempotency-Key", key)
							.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")), key);

			String boom = "probe-boom-canary-e3a1";
			check("R8 500 from an exception chain whose messages carry a canary", client,
					HttpRequest.newBuilder(URI.create(base + "/probe/boom?v=" + boom)).GET(), boom);

			check("R9 413 domain-style ErrorResponseException: bare, instance replaced", client,
					HttpRequest.newBuilder(URI.create(base + "/probe/too-large")).GET());
		}
		finally {
			ctx.close();
		}
	}

	static void check(String label, HttpClient client, HttpRequest.Builder request, String... canaries) throws Exception {
		synchronized (WINDOW) {
			WINDOW.reset();
		}
		recording = true;
		HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
		Thread.sleep(300); // the completion event is written in the filter's finally, after the body reached the client
		recording = false;
		String logs;
		synchronized (WINDOW) {
			logs = WINDOW.toString(StandardCharsets.UTF_8);
		}
		String id = response.headers().firstValue("x-request-id").orElse("");
		List<String> lines = logs.lines().filter(l -> !l.isBlank()).toList();
		long withoutId = lines.stream().filter(l -> !l.contains("\"requestId\":\"" + id + "\"")).count();
		boolean canaryInBody = Arrays.stream(canaries).anyMatch(c -> response.body().contains(c));
		boolean canaryInLogs = Arrays.stream(canaries).anyMatch(logs::contains);
		StringBuilder sb = new StringBuilder();
		sb.append("status=").append(response.statusCode());
		for (String h : List.of("content-type", "allow", "accept", "x-request-id")) {
			response.headers().firstValue(h).ifPresent(v -> sb.append("  ").append(h).append("=").append(v));
		}
		sb.append("\n    body=").append(response.body());
		sb.append("\n    VERDICT instanceIsUrnOfRequestId=").append(response.body().contains("\"instance\":\"urn:uuid:" + id + "\""))
				.append(" detailPresent=").append(response.body().contains("\"detail\""))
				.append(" canaryInBody=").append(canaryInBody)
				.append(" logLines=").append(lines.size())
				.append(" logLinesWithoutThisRequestId=").append(withoutId)
				.append(" canaryInLogs=").append(canaryInLogs);
		for (String line : lines) {
			sb.append("\n    log> ").append(line);
		}
		out(label, sb.toString());
	}

	static void out(String title, String text) {
		STDOUT.println("PROBE " + title + "\n    " + text);
	}

	@Configuration(proxyBeanMethods = false)
	public static class ProbeConfig {
		@Bean
		ProbeController probeController(JdbcClient jdbc) {
			return new ProbeController(jdbc);
		}

		@Bean
		ProbeAdvice probeAdvice() {
			return new ProbeAdvice();
		}

		@Bean
		CompletionEventFilter completionEventFilter() {
			return new CompletionEventFilter();
		}
	}

	@RestController
	public static class ProbeController {
		private final JdbcClient jdbc;

		ProbeController(JdbcClient jdbc) {
			this.jdbc = jdbc;
		}

		@PostMapping(path = "/probe/body", consumes = "application/json")
		public String body(@RequestBody String body) {
			return "{}";
		}

		@GetMapping("/probe/links/{code}")
		public void link(@PathVariable("code") String code) { // source-file mode compiles without -parameters
			throw new ErrorResponseException(HttpStatus.NOT_FOUND);
		}

		@PostMapping(path = "/probe/collision", consumes = "application/json")
		public void collision(@RequestHeader("Idempotency-Key") String key) {
			jdbc.sql("INSERT INTO probe_key (idempotency_key) VALUES (:key)").param("key", key).update();
			jdbc.sql("INSERT INTO probe_key (idempotency_key) VALUES (:key)").param("key", key).update();
		}

		@GetMapping("/probe/boom")
		public void boom(@org.springframework.web.bind.annotation.RequestParam("v") String v) {
			throw new IllegalStateException("outer " + v, new IllegalArgumentException("inner " + v));
		}

		@GetMapping("/probe/too-large")
		public void tooLarge() {
			throw new ErrorResponseException(HttpStatus.CONTENT_TOO_LARGE);
		}
	}

	/** The revised advice: no detail, request-id instance, message-free failure event. */
	@RestControllerAdvice
	public static class ProbeAdvice extends ResponseEntityExceptionHandler {
		private static final Logger log = LoggerFactory.getLogger(ProbeAdvice.class);

		@ExceptionHandler(Exception.class)
		ResponseEntity<Object> unhandled(Exception ex, WebRequest request) {
			log.atError().setMessage("request failed")
					.addKeyValue("errorChain", Stream.iterate((Throwable) ex, Objects::nonNull, Throwable::getCause).limit(8)
							.map(t -> t.getClass().getName()).collect(Collectors.joining(" <- ")))
					.addKeyValue("errorOrigin", Arrays.stream(ex.getStackTrace()).filter(f -> f.getClassName().startsWith(APP_PREFIX))
							.findFirst().map(StackTraceElement::toString).orElse("none"))
					.log();
			return handleExceptionInternal(ex, ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR), new HttpHeaders(),
					HttpStatus.INTERNAL_SERVER_ERROR, request);
		}

		@Override
		protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
			if (body instanceof ProblemDetail problem) {
				problem.setDetail(null);
				problem.setInstance(URI.create("urn:uuid:" + MDC.get("requestId")));
			}
			return super.createResponseEntity(body, headers, status, request);
		}
	}

	/** Stands in for the event the revised RequestIdFilter writes before MDC.remove: status only. */
	@Order(Ordered.HIGHEST_PRECEDENCE + 1)
	public static class CompletionEventFilter extends OncePerRequestFilter {
		private static final Logger log = LoggerFactory.getLogger(CompletionEventFilter.class);

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, java.io.IOException {
			try {
				chain.doFilter(request, response);
			}
			finally {
				log.atInfo().setMessage("request completed").addKeyValue("status", response.getStatus()).log();
			}
		}
	}
}
