import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import dev.urlshort.UrlshortApplication;

/**
 * Design probe for slice 01-create-redirect: boots the shipped application (main at the design
 * commit, functional profile, in-memory H2) on a random port with a handful of probe beans that
 * use exactly the platform mechanisms design.md relies on, then exercises them over real HTTP so the
 * reviewer can see status lines, headers and bodies produced by Tomcat, not by MockMvc. No file
 * under src/ is touched; the probe beans live only in this file.
 */
public class MechanismProbe {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(UrlshortApplication.class, ProbeConfig.class)
				.properties("server.port=0", "spring.servlet.multipart.enabled=false")
				.run(args);
		try {
			int port = ctx.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
			String base = "http://127.0.0.1:" + port;
			HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
			String browser = "text/html,application/xhtml+xml,*/*;q=0.8";

			out("clock", "Clock.systemUTC().instant() = " + Clock.systemUTC().instant()
					+ "  tickMillis = " + Clock.tickMillis(java.time.ZoneOffset.UTC).instant());

			show("P1 ErrorResponseException(410) under browser Accept", client.send(
					get(base + "/probe/gone").header("Accept", browser).build(), HttpResponse.BodyHandlers.ofString()));
			show("P2 ErrorResponseException(410) under Accept: text/html only", client.send(
					get(base + "/probe/gone").header("Accept", "text/html").build(), HttpResponse.BodyHandlers.ofString()));
			show("P3 ErrorResponseException(400) with errors[] property", client.send(
					get(base + "/probe/invalid").build(), HttpResponse.BodyHandlers.ofString()));
			show("P4 302 with String Location and Cache-Control no-store, browser Accept", client.send(
					get(base + "/abcdefgh").header("Accept", browser).build(), HttpResponse.BodyHandlers.ofString()));
			show("P5 no handler (/favicon.ico), browser Accept", client.send(
					get(base + "/favicon.ico").header("Accept", browser).build(), HttpResponse.BodyHandlers.ofString()));
			show("P6 no handler (40 letters outside the code pattern)", client.send(
					get(base + "/" + "a".repeat(40)).header("Accept", browser).build(), HttpResponse.BodyHandlers.ofString()));
			show("P7 wrong method on the redirect route", client.send(
					HttpRequest.newBuilder(URI.create(base + "/abcdefgh")).POST(HttpRequest.BodyPublishers.noBody()).build(),
					HttpResponse.BodyHandlers.ofString()));
			show("P8 text/plain body on a JSON-only endpoint", client.send(
					post(base + "/probe/body", "text/plain", "https://example.com/").build(), HttpResponse.BodyHandlers.ofString()));
			show("P9 multipart body with multipart disabled", client.send(
					post(base + "/probe/body", "multipart/form-data; boundary=xx",
							"--xx\r\nContent-Disposition: form-data; name=\"url\"\r\n\r\nhttps://example.com/\r\n--xx--\r\n").build(),
					HttpResponse.BodyHandlers.ofString()));

			String exact = jsonOfSize(16_384);
			String over = jsonOfSize(16_385);
			out("sizes", "exact=" + exact.getBytes(StandardCharsets.UTF_8).length + " over=" + over.getBytes(StandardCharsets.UTF_8).length);
			show("P10a JSON body of exactly 16384 bytes", client.send(
					post(base + "/probe/body", "application/json", exact).build(), HttpResponse.BodyHandlers.ofString()));
			show("P10b JSON body of 16385 bytes (Content-Length set)", client.send(
					post(base + "/probe/body", "application/json", over).build(), HttpResponse.BodyHandlers.ofString()));
			show("P10c JSON body of 16385 bytes, chunked (no Content-Length)", client.send(
					HttpRequest.newBuilder(URI.create(base + "/probe/body")).header("Content-Type", "application/json")
							.POST(HttpRequest.BodyPublishers.ofInputStream(
									() -> new ByteArrayInputStream(over.getBytes(StandardCharsets.UTF_8)))).build(),
					HttpResponse.BodyHandlers.ofString()));
			show("P10d JSON body of exactly 16384 bytes, chunked", client.send(
					HttpRequest.newBuilder(URI.create(base + "/probe/body")).header("Content-Type", "application/json")
							.POST(HttpRequest.BodyPublishers.ofInputStream(
									() -> new ByteArrayInputStream(exact.getBytes(StandardCharsets.UTF_8)))).build(),
					HttpResponse.BodyHandlers.ofString()));

			show("P11 unhandled exception -> catch-all 500 (find its requestId in the ECS lines above)", client.send(
					get(base + "/probe/boom").header("Accept", browser).build(), HttpResponse.BodyHandlers.ofString()));

			HttpResponse<String> docs = client.send(get(base + "/v3/api-docs").build(), HttpResponse.BodyHandlers.ofString());
			int i = docs.body().indexOf("\"servers\"");
			out("P12 springdoc servers on a real port", i < 0 ? "<no servers member>" : docs.body().substring(i, Math.min(docs.body().length(), i + 120)));
		}
		finally {
			ctx.close();
		}
	}

	private static HttpRequest.Builder get(String url) {
		return HttpRequest.newBuilder(URI.create(url)).GET();
	}

	private static HttpRequest.Builder post(String url, String contentType, String body) {
		return HttpRequest.newBuilder(URI.create(url)).header("Content-Type", contentType)
				.POST(HttpRequest.BodyPublishers.ofString(body));
	}

	private static String jsonOfSize(int bytes) {
		String head = "{\"url\":\"https://example.com/\",\"pad\":\"";
		String tail = "\"}";
		return head + "x".repeat(bytes - head.length() - tail.length()) + tail;
	}

	private static void show(String title, HttpResponse<String> response) {
		HttpHeaders ignored = null;
		StringBuilder sb = new StringBuilder();
		sb.append("status=").append(response.statusCode());
		for (String h : List.of("content-type", "location", "cache-control", "x-request-id", "allow")) {
			response.headers().firstValue(h).ifPresent(v -> sb.append("  ").append(h).append("=").append(v));
		}
		String body = response.body();
		sb.append("\n    body=").append(body.length() > 400 ? body.substring(0, 400) + "…" : body);
		out(title, sb.toString());
	}

	private static void out(String title, String text) {
		System.out.println("PROBE " + title + "\n    " + text);
	}

	/** Probe beans: one controller, one advice, one filter; registered explicitly, not scanned. */
	@Configuration(proxyBeanMethods = false)
	public static class ProbeConfig {
		@Bean
		ProbeController probeController() {
			return new ProbeController();
		}

		@Bean
		ProbeAdvice probeAdvice() {
			return new ProbeAdvice();
		}

		@Bean
		ProbeBodyLimitFilter probeBodyLimitFilter() {
			return new ProbeBodyLimitFilter();
		}
	}

	/** Validation error element exactly as design.md specifies it. */
	public record FieldError(String field, String rule, String message) {
	}

	@RestController
	public static class ProbeController {

		@GetMapping("/probe/gone")
		public void gone() {
			throw new ErrorResponseException(HttpStatus.GONE);
		}

		@GetMapping("/probe/invalid")
		public void invalid() {
			ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
			problem.setProperty("errors", List.of(new FieldError("url", "scheme", "must start with http:// or https://")));
			throw new ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null);
		}

		@GetMapping("/probe/boom")
		public void boom() {
			throw new IllegalStateException("induced failure canary-boom-7f3a");
		}

		@GetMapping("/{code:[A-Za-z0-9]{6,32}}")
		public ResponseEntity<Void> redirect() {
			return ResponseEntity.status(HttpStatus.FOUND)
					.header(HttpHeaders.LOCATION, "https://example.com/some/path?q=1&r=a%20b")
					.cacheControl(CacheControl.noStore())
					.build();
		}

		@PostMapping(path = "/probe/body", consumes = "application/json")
		public Map<String, Object> body(@RequestBody Map<String, Object> body) {
			return Map.of("keys", body.size());
		}
	}

	/** The one project advice: extends the platform handler (Boot's backs off) and adds the catch-all. */
	@RestControllerAdvice
	public static class ProbeAdvice extends ResponseEntityExceptionHandler {

		private static final Logger log = LoggerFactory.getLogger(ProbeAdvice.class);

		@ExceptionHandler(Exception.class)
		ResponseEntity<Object> unhandled(Exception ex, WebRequest request) {
			log.error("request failed", ex);
			return handleExceptionInternal(ex, ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR),
					new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
		}

		@Override
		protected ResponseEntity<Object> handleHttpMessageNotReadable(
				org.springframework.http.converter.HttpMessageNotReadableException ex, HttpHeaders headers,
				org.springframework.http.HttpStatusCode status, WebRequest request) {
			StringBuilder chain = new StringBuilder();
			for (Throwable t = ex; t != null; t = t.getCause()) {
				chain.append(t.getClass().getName()).append(": ").append(t.getMessage()).append(" <- ");
			}
			System.out.println("PROBE not-readable cause chain\n    " + chain);
			// design.md section 1: a limit raised inside Jackson's databind arrives wrapped; unwrap it.
			for (Throwable t = ex.getCause(); t != null; t = t.getCause()) {
				if (t instanceof ErrorResponseException limit) {
					return handleExceptionInternal(limit, limit.getBody(), limit.getHeaders(), limit.getStatusCode(), request);
				}
			}
			return super.handleHttpMessageNotReadable(ex, headers, status, request);
		}
	}

	/** Counting input stream: the 16 385th body byte raises a 413 inside the DispatcherServlet. */
	@Order(Ordered.HIGHEST_PRECEDENCE + 1)
	public static class ProbeBodyLimitFilter extends OncePerRequestFilter {

		static final long MAX_BODY_BYTES = 16_384;

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException {
			chain.doFilter(new HttpServletRequestWrapper(request) {
				@Override
				public ServletInputStream getInputStream() throws IOException {
					return new LimitedInputStream(super.getInputStream(), MAX_BODY_BYTES);
				}
			}, response);
		}
	}

	static final class LimitedInputStream extends ServletInputStream {
		private final ServletInputStream in;
		private final long max;
		private long count;

		LimitedInputStream(ServletInputStream in, long max) {
			this.in = in;
			this.max = max;
		}

		@Override
		public int read() throws IOException {
			int b = in.read();
			if (b >= 0) {
				bump(1);
			}
			return b;
		}

		@Override
		public int read(byte[] buf, int off, int len) throws IOException {
			int n = in.read(buf, off, len);
			if (n > 0) {
				bump(n);
			}
			return n;
		}

		private void bump(long n) {
			count += n;
			if (count > max) {
				throw new ErrorResponseException(HttpStatus.CONTENT_TOO_LARGE);
			}
		}

		@Override
		public boolean isFinished() {
			return in.isFinished();
		}

		@Override
		public boolean isReady() {
			return in.isReady();
		}

		@Override
		public void setReadListener(ReadListener listener) {
			in.setReadListener(listener);
		}

		@Override
		public void close() throws IOException {
			in.close();
		}
	}
}
