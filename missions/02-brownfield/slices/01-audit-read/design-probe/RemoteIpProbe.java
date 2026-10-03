import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.Valve;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.urlshort.UrlshortApplication;

/**
 * Probe for code review finding (review-agent, 20:07Z, docs/review/01-audit-read/proof/code-controls-35590f0.txt):
 * Boot 4.1.1's TomcatWebServerFactoryCustomizer.customizeRemoteIpValve installs a RemoteIpValve when
 * server.tomcat.remoteip.protocol-header has text, OR remote-ip-header has text, OR the forwarded-header
 * strategy resolves to native. The candidate's guard checks only the strategy. On a real Tomcat on 127.0.0.1
 * this compares three guard predicates under each setting; no file under src/ is touched.
 *   shipped: strategy == NONE                                  (the candidate, 35590f0)
 *   mirror:  strategy == NONE and neither remoteip header set  (Boot's own three conditions)
 *   attr:    the request carries no org.apache.tomcat.remoteAddr attribute (the valve's trace)
 * Each guard then requires a loopback peer and no X-Forwarded-For or Forwarded header, as the candidate does.
 */
public class RemoteIpProbe {

	static final String[] ARGS = { "--server.port=0", "--server.address=127.0.0.1", "--logging.level.root=warn",
			"--urlshort.rate-limit.create-per-minute=1000000", "--urlshort.rate-limit.redirect-per-minute=1000000",
			"--spring.datasource.url=jdbc:h2:mem:probe-remoteip;MODE=PostgreSQL;DB_CLOSE_DELAY=-1" };

	public static void main(String[] args) throws Exception {
		HttpClient client = HttpClient.newBuilder().build();
		List<String[]> variants = List.of(
				new String[] { "pin none", "--server.forward-headers-strategy=none" },
				new String[] { "pin none + remote-ip-header=x-forwarded-for", "--server.forward-headers-strategy=none",
						"--server.tomcat.remoteip.remote-ip-header=x-forwarded-for" },
				new String[] { "pin none + protocol-header=x-forwarded-proto", "--server.forward-headers-strategy=none",
						"--server.tomcat.remoteip.protocol-header=x-forwarded-proto" },
				new String[] { "pin none + remote-ip-header=X-Real-IP", "--server.forward-headers-strategy=none",
						"--server.tomcat.remoteip.remote-ip-header=X-Real-IP" },
				new String[] { "native", "--server.forward-headers-strategy=native" },
				new String[] { "framework", "--server.forward-headers-strategy=framework" });
		for (String[] variant : variants) {
			List<String> all = new ArrayList<>(Arrays.asList(ARGS));
			all.addAll(Arrays.asList(variant).subList(1, variant.length));
			try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(UrlshortApplication.class, ProbeConfig.class)
					.run(all.toArray(String[]::new))) {
				String base = "http://127.0.0.1:" + ctx.getEnvironment().getRequiredProperty("local.server.port");
				List<String> valves = new ArrayList<>();
				TomcatWebServer server = (TomcatWebServer) ((WebServerApplicationContext) ctx).getWebServer();
				for (Valve valve : server.getTomcat().getEngine().getPipeline().getValves()) {
					valves.add(valve.getClass().getSimpleName());
				}
				System.out.println("R " + variant[0] + ": engine valves " + valves);
				for (String[] header : List.of(new String[0], new String[] { "X-Forwarded-For", "127.0.0.2" },
						new String[] { "X-Real-IP", "127.0.0.2" }, new String[] { "X-Forwarded-For", "198.51.100.9" })) {
					String label = header.length == 0 ? "plain" : header[0] + " " + header[1];
					List<String> results = new ArrayList<>();
					for (String guard : List.of("shipped", "mirror", "attr")) {
						results.add(guard + " " + status(client, base + "/probe/guard/" + guard, header));
					}
					System.out.println("R   " + label + ": " + String.join(", ", results) + " | " + body(client, base + "/probe/seen", header));
				}
			}
		}
		System.out.println("PROBE done");
		System.exit(0);
	}

	static int status(HttpClient client, String uri, String[] header) throws Exception {
		return client.send(request(uri, header), HttpResponse.BodyHandlers.discarding()).statusCode();
	}

	static String body(HttpClient client, String uri, String[] header) throws Exception {
		return client.send(request(uri, header), HttpResponse.BodyHandlers.ofString()).body();
	}

	static HttpRequest request(String uri, String[] header) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri));
		if (header.length == 2) {
			builder.header(header[0], header[1]);
		}
		return builder.build();
	}

	@Configuration(proxyBeanMethods = false)
	public static class ProbeConfig {

		@Bean
		public GuardController guardController(ServerProperties server, TomcatServerProperties tomcat) {
			return new GuardController(server, tomcat);
		}
	}

	@RestController
	public static class GuardController {

		final boolean shipped;
		final boolean mirror;

		GuardController(ServerProperties server, TomcatServerProperties tomcat) {
			this.shipped = server.getForwardHeadersStrategy() == ServerProperties.ForwardHeadersStrategy.NONE;
			this.mirror = this.shipped && !StringUtils.hasText(tomcat.getRemoteip().getRemoteIpHeader())
					&& !StringUtils.hasText(tomcat.getRemoteip().getProtocolHeader());
		}

		@GetMapping("/probe/guard/shipped")
		public String shipped(HttpServletRequest request) throws Exception {
			return answer(shipped && fromLoopback(request));
		}

		@GetMapping("/probe/guard/mirror")
		public String mirror(HttpServletRequest request) throws Exception {
			return answer(mirror && fromLoopback(request));
		}

		@GetMapping("/probe/guard/attr")
		public String attr(HttpServletRequest request) throws Exception {
			return answer(request.getAttribute("org.apache.tomcat.remoteAddr") == null && fromLoopback(request));
		}

		@GetMapping("/probe/seen")
		public String seen(HttpServletRequest request) {
			return "remoteAddr=" + request.getRemoteAddr() + " XFF=" + request.getHeader("X-Forwarded-For") + " X-Real-IP="
					+ request.getHeader("X-Real-IP") + " valveAttr=" + request.getAttribute("org.apache.tomcat.remoteAddr");
		}

		static String answer(boolean admitted) {
			if (!admitted) {
				throw new org.springframework.web.ErrorResponseException(org.springframework.http.HttpStatus.FORBIDDEN);
			}
			return "admitted";
		}

		static boolean fromLoopback(HttpServletRequest request) throws Exception {
			return request.getHeader("X-Forwarded-For") == null && request.getHeader("Forwarded") == null
					&& InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress();
		}
	}
}
