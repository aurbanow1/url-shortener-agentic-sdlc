package dev.urlshort.click;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * AC-15: v1's slow-store, failing-store and concurrency journeys run unchanged with a trusted proxy
 * configured, since the click's client now comes from the rate limiter's request attribute
 * (business rule 6). The empty setting is {@link ClickResilienceJourneyTest} itself.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "urlshort.rate-limit.trusted-proxies=10.9.9.9")
class ClickResilienceTrustedProxyJourneyTest extends ClickResilienceJourneyTest {
}
