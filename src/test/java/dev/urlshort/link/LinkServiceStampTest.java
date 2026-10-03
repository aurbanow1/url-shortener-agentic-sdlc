package dev.urlshort.link;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import dev.urlshort.audit.AuditLog;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * Business rule 2 of slice 04-audit-columns at the use-case level: each of the three link writes stamps
 * the link's update time with the write's own instant. {@code LinkServiceTest} stays as shipped.
 */
class LinkServiceStampTest {

	private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");
	private static final String URL = "https://example.com/";

	private final LinkRepository links = mock(LinkRepository.class);
	private final ShortCodes codes = mock(ShortCodes.class);
	private final LinkService service = new LinkService(links, codes, mock(AuditLog.class), Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void aCreateStampsTheNewLinkWithItsCreationInstant() {
		when(codes.next()).thenReturn("Abc12345");
		when(links.save(any())).thenAnswer(call -> withId(call.getArgument(0), 7L));

		service.create(URL, null);

		InOrder order = inOrder(links);
		order.verify(links).save(any());
		order.verify(links).stamp(7L, NOW);
	}

	@Test
	void aKeyReleaseStampsTheReleasedLinkWithTheSameInstant() {
		when(links.findByIdempotencyKey("K")).thenReturn(
				Optional.of(new Link(3L, "Bound123", URL, NOW.minus(Duration.ofHours(25)), null, "K")));
		when(codes.next()).thenReturn("New12345");
		when(links.save(any())).thenAnswer(call -> withId(call.getArgument(0), 8L));

		service.create("https://example.com/other", "K");

		InOrder order = inOrder(links);
		order.verify(links).releaseIdempotencyKey(3L);
		order.verify(links).stamp(3L, NOW);
		order.verify(links).save(any());
		order.verify(links).stamp(8L, NOW);
	}

	@Test
	void aRetireStampsThroughItsConditionalUpdate() {
		when(links.findByCode("Active12")).thenReturn(Optional.of(new Link(1L, "Active12", URL, NOW, null, null)));
		when(links.retire(1L, NOW)).thenReturn(1);

		service.retire("Active12");

		verify(links).retire(1L, NOW);
	}

	private static Link withId(Link link, long id) {
		return new Link(id, link.code(), link.url(), link.createdAt(), link.retiredAt(), link.idempotencyKey());
	}
}
