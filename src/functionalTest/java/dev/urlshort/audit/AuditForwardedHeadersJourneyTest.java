package dev.urlshort.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import dev.urlshort.UrlshortApplication;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * SPEC rule 2 and AC-14 on a real Tomcat bound to {@code 127.0.0.1} (ADR-0019, design review DR-01): the
 * shipped file pins {@code server.forward-headers-strategy=none}; with the pin, a detected cloud platform
 * cannot turn a forged {@code X-Forwarded-For} into a loopback peer; and an operator override of the pin
 * closes the endpoint instead of opening it. Also AC-11's {@code HEAD}: Tomcat sends a refused
 * {@code HEAD} without a body (MockMvc would keep one).
 */
class AuditForwardedHeadersJourneyTest {

	private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

	@Test
	void theShippedFilePinsTheForwardedHeaderStrategyOff() throws Exception {
		Properties shipped = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));

		assertThat(shipped.getProperty("server.forward-headers-strategy")).isEqualTo("none");
	}

	@Test
	void onADetectedCloudPlatformThePinKeepsAForgedLoopbackHeaderOut() throws Exception {
		try (ConfigurableApplicationContext app = start(memory("urlshort-audit-kube"), "--spring.main.cloud-platform=kubernetes")) {
			String base = base(app);

			assertThat(send(base, "GET", null, null).statusCode()).isEqualTo(200);
			assertThat(send(base, "GET", "X-Forwarded-For", "127.0.0.2").statusCode()).isEqualTo(403);
			HttpResponse<String> refusedHead = send(base, "HEAD", "X-Forwarded-For", "198.51.100.9");
			assertThat(refusedHead.statusCode()).isEqualTo(403);
			assertThat(refusedHead.body()).isEmpty();
			HttpResponse<String> head = send(base, "HEAD", null, null);
			assertThat(head.statusCode()).isEqualTo(200);
			assertThat(head.body()).isEmpty();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "native", "framework" })
	void anOverrideOfThePinClosesTheEndpoint(String strategy) throws Exception {
		try (ConfigurableApplicationContext app = start(memory("urlshort-audit-" + strategy),
				"--server.forward-headers-strategy=" + strategy)) {
			String base = base(app);

			for (String[] header : List.of(new String[] { null, null }, new String[] { "X-Forwarded-For", "127.0.0.2" },
					new String[] { "Forwarded", "for=127.0.0.2" })) {
				HttpResponse<String> response = send(base, "GET", header[0], header[1]);
				assertThat(response.statusCode()).as("%s %s", header[0], header[1]).isEqualTo(403);
				assertThat(response.body()).doesNotContain("items");
			}
		}
	}

	/**
	 * Either Tomcat remoteip header setting installs the RemoteIpValve while the strategy stays pinned to
	 * {@code none}; the read must close then too, or a forged header becomes the peer (code review on
	 * {@code 35590f0}).
	 */
	@ParameterizedTest
	@ValueSource(strings = { "server.tomcat.remoteip.remote-ip-header=x-forwarded-for",
		"server.tomcat.remoteip.protocol-header=x-forwarded-proto" })
	void aTomcatRemoteIpSettingClosesTheEndpoint(String setting) throws Exception {
		String name = setting.contains("protocol") ? "protocol" : "remote-ip";
		try (ConfigurableApplicationContext app = start(memory("urlshort-audit-" + name), "--" + setting)) {
			String base = base(app);
			String canary = "https://example.com/remoteip-canary-" + name;
			HttpResponse<String> created = CLIENT.send(HttpRequest.newBuilder(URI.create(base + "/api/links"))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString("{\"url\":\"" + canary + "\"}")).build(),
					HttpResponse.BodyHandlers.ofString());
			assertThat(created.statusCode()).isEqualTo(201);

			for (String method : List.of("GET", "HEAD")) {
				for (String[] header : List.of(new String[] { null, null }, new String[] { "X-Forwarded-For", "127.0.0.2" })) {
					HttpResponse<String> response = send(base, method, header[0], header[1]);
					assertThat(response.statusCode()).as("%s %s %s %s", setting, method, header[0], header[1]).isEqualTo(403);
					assertThat(response.body()).doesNotContain("items").doesNotContain(canary);
				}
			}
		}
	}

	static String memory(String name) {
		return "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
	}

	/** The application on a real Tomcat, on a free port of {@code 127.0.0.1}. */
	static ConfigurableApplicationContext start(String databaseUrl, String... more) {
		List<String> args = new ArrayList<>(List.of("--server.port=0", "--server.address=127.0.0.1",
				"--spring.datasource.url=" + databaseUrl));
		args.addAll(List.of(more));
		return new SpringApplicationBuilder(UrlshortApplication.class).run(args.toArray(String[]::new));
	}

	static String base(ConfigurableApplicationContext app) {
		return "http://127.0.0.1:" + app.getEnvironment().getRequiredProperty("local.server.port");
	}

	static HttpResponse<String> get(String uri) throws Exception {
		return CLIENT.send(HttpRequest.newBuilder(URI.create(uri)).build(), HttpResponse.BodyHandlers.ofString());
	}

	private static HttpResponse<String> send(String base, String method, @Nullable String header, @Nullable String value)
			throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + "/api/audit"))
				.method(method, HttpRequest.BodyPublishers.noBody());
		if (header != null) {
			request.header(header, value);
		}
		return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}
}
