package dev.urlshort.web;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fixed metadata for the live {@code /v3/api-docs} document. The {@code servers} member is pinned to
 * {@code /} because springdoc would otherwise write the request's own host and port, which differs
 * between test and real runs; that keeps the committed {@code docs/api/openapi.json} equal to the live
 * document (NFR-M3, ADR-0010).
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

	@Bean
	OpenAPI openApi() {
		return new OpenAPI()
				.info(new Info().title("urlshort").version("1")
						.description("Create, read and retire short links; visitors are redirected with 302."))
				.servers(List.of(new Server().url("/")));
	}
}
