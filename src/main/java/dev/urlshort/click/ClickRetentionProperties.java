package dev.urlshort.click;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * The Operator's click-retention settings (NFR-P2, business rules 1 and 3); environment variables
 * {@code URLSHORT_CLICK_RETENTIONDAYS} and {@code URLSHORT_CLICK_PURGEENABLED}. An invalid period stops
 * the service at startup, before any purge runs. Decision record: ADR-0018.
 *
 * @param retentionDays the period in whole UTC days, positive; on day {@code T} every click stored on a
 *        day before {@code T − retentionDays} is deleted, and that day itself is kept
 * @param purgeEnabled {@code false} is an operator hold (an incident investigation, a legal hold): no
 *        click is deleted, neither at startup nor daily, and every start logs one WARN saying so
 */
@ConfigurationProperties("urlshort.click")
@Validated
record ClickRetentionProperties(@DefaultValue("90") @Positive int retentionDays,
		@DefaultValue("true") boolean purgeEnabled) {
}
