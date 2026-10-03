package dev.urlshort.web;

import java.util.Set;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * The Operator's rate-limit settings (business rule 11, NFR-R2); environment variables
 * {@code URLSHORT_RATELIMIT_CREATEPERMINUTE}, {@code URLSHORT_RATELIMIT_REDIRECTPERMINUTE} and
 * {@code URLSHORT_RATELIMIT_TRUSTEDPROXIES} (comma-separated). Decision record: ADR-0014, ADR-0015.
 *
 * @param createPerMinute the budget for every request under {@code /api}, per client
 * @param redirectPerMinute the budget for every other limited request, per client
 * @param trustedProxies peer addresses, written exactly as the server reports them, whose
 *        {@code X-Forwarded-For} is believed; empty by default
 */
@ConfigurationProperties("urlshort.rate-limit")
@Validated
record RateLimitProperties(@DefaultValue("60") @Positive int createPerMinute,
		@DefaultValue("600") @Positive int redirectPerMinute, @DefaultValue({}) Set<String> trustedProxies) {
}
