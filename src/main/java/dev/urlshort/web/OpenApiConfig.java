package dev.urlshort.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fixed metadata for the live {@code /v3/api-docs} document. The {@code servers} member is pinned to
 * {@code /} because springdoc would otherwise write the request's own host and port, which differs
 * between test and real runs; that keeps the committed {@code docs/api/openapi.json} equal to the live
 * document (NFR-M3, ADR-0010). Every operation also documents the rate limiter's {@code 429}, because
 * the limiter charges every non-operator request (slice 03-operate, AC-20, ADR-0014).
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

	@Bean
	OpenApiCustomizer tooManyRequests() {
		return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation ->
				operation.getResponses().addApiResponse("429", new ApiResponse()
						.description("Too many requests from this client; retry after Retry-After seconds")
						.addHeaderObject("Retry-After", new Header()
								.description("Whole seconds until this client's next request is admitted")
								.schema(new IntegerSchema().minimum(BigDecimal.ONE)))
						.content(new Content().addMediaType("application/problem+json", new MediaType()
								.schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
								.addExamples("tooManyRequests", new Example().value(Map.of(
										"instance", "urn:uuid:3b1f0e6a-9c2d-4f57-8a41-0d5e2c7b9f10",
										"status", 429, "title", "Too Many Requests"))))))));
	}
}
