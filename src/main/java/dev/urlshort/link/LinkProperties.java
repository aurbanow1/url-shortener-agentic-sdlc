package dev.urlshort.link;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The Operator's link settings (NFR-S4).
 *
 * @param publicBaseUrl the base of every {@code shortUrl}, without a trailing slash; environment
 *        variable {@code URLSHORT_PUBLIC_BASE_URL}. Never derived from {@code Host} or forwarding
 *        headers (business rule 11, AC-3)
 */
@ConfigurationProperties("urlshort")
record LinkProperties(@DefaultValue("http://localhost:8080") String publicBaseUrl) {
}
