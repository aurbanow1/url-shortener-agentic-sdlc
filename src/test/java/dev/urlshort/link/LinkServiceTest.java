package dev.urlshort.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import dev.urlshort.audit.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.web.ErrorResponseException;

/** The use cases' branches with scripted collaborators: idempotency (rule 5), retire (rule 6), audit (rule 9). */
class LinkServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");
	private static final String URL = "https://example.com/";

	private final LinkRepository links = mock(LinkRepository.class);
	private final ShortCodes codes = mock(ShortCodes.class);
	private final AuditLog audit = mock(AuditLog.class);
	private final LinkService service = new LinkService(links, codes, audit, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void createWithoutAKeyInsertsAndAuditsTheNewLink() {
		when(codes.next()).thenReturn("Abc12345");
		when(links.save(any())).thenAnswer(call -> withId(call.getArgument(0), 7L));

		Link created = service.create(URL, null);

		assertThat(created.code()).isEqualTo("Abc12345");
		assertThat(created.createdAt()).isEqualTo(NOW);
		assertThat(created.idempotencyKey()).isNull();
		verify(links, never()).findByIdempotencyKey(any());
		verify(audit).append("link.create", "link", "Abc12345", null, new LinkSnapshot(URL, "active"));
	}

	@Test
	void createWithAnUnboundKeyBindsIt() {
		when(links.findByIdempotencyKey("K")).thenReturn(Optional.empty());
		when(codes.next()).thenReturn("Abc12345");
		when(links.save(any())).thenAnswer(call -> withId(call.getArgument(0), 7L));

		assertThat(service.create(URL, "K").idempotencyKey()).isEqualTo("K");
	}

	@Test
	void replayWithinTheWindowReturnsTheBoundLinkAndWritesNothing() {
		Link bound = new Link(3L, "Bound123", URL, NOW.minus(Duration.ofHours(24)).plusMillis(1), null, "K");
		when(links.findByIdempotencyKey("K")).thenReturn(Optional.of(bound));

		assertThat(service.create(URL, "K")).isSameAs(bound);
		verify(links, never()).save(any());
		verifyNoInteractions(audit);
	}

	@Test
	void mismatchWithinTheWindowIs422AndKeepsTheBinding() {
		when(links.findByIdempotencyKey("K")).thenReturn(
				Optional.of(new Link(3L, "Bound123", URL, NOW.minus(Duration.ofHours(23)), null, "K")));

		assertThatThrownBy(() -> service.create("https://example.com/other", "K"))
				.isInstanceOfSatisfying(ErrorResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(422));
		verify(links, never()).releaseIdempotencyKey(anyLong());
		verify(links, never()).save(any());
		verifyNoInteractions(audit);
	}

	@Test
	void anExpiredKeyIsReleasedAndBindsANewLink() {
		when(links.findByIdempotencyKey("K")).thenReturn(
				Optional.of(new Link(3L, "Bound123", URL, NOW.minus(Duration.ofHours(24)), null, "K")));
		when(codes.next()).thenReturn("New12345");
		when(links.save(any())).thenAnswer(call -> withId(call.getArgument(0), 8L));

		Link created = service.create(URL, "K");

		verify(links).releaseIdempotencyKey(3L);
		assertThat(created.code()).isEqualTo("New12345");
		assertThat(created.idempotencyKey()).isEqualTo("K");
	}

	@Test
	void readOfAnUnknownCodeIs404() {
		when(links.findByCode("nosuch1")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read("nosuch1")).isInstanceOfSatisfying(ErrorResponseException.class,
				e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
	}

	@Test
	void resolveSendsVisitorsToActiveLinksOnly() {
		Link active = new Link(1L, "Active12", URL, NOW, null, null);
		when(links.findByCode("Active12")).thenReturn(Optional.of(active));
		when(links.findByCode("Retired1")).thenReturn(Optional.of(new Link(2L, "Retired1", URL, NOW, NOW, null)));

		assertThat(service.resolve("Active12")).isSameAs(active);
		assertThatThrownBy(() -> service.resolve("Retired1")).isInstanceOfSatisfying(ErrorResponseException.class,
				e -> assertThat(e.getStatusCode().value()).isEqualTo(410));
	}

	@Test
	void retireUpdatesConditionallyAndAuditsBeforeAndAfter() {
		when(links.findByCode("Active12")).thenReturn(Optional.of(new Link(1L, "Active12", URL, NOW, null, null)));
		when(links.retire(1L, NOW)).thenReturn(1);

		service.retire("Active12");

		verify(audit).append("link.retire", "link", "Active12", new LinkSnapshot(URL, "active"),
				new LinkSnapshot(URL, "retired"));
	}

	@Test
	void retireThatChangesNoRowIs410AndWritesNoAuditRow() {
		when(links.findByCode("Active12")).thenReturn(Optional.of(new Link(1L, "Active12", URL, NOW, null, null)));
		when(links.retire(1L, NOW)).thenReturn(0);

		assertThatThrownBy(() -> service.retire("Active12")).isInstanceOfSatisfying(ErrorResponseException.class,
				e -> assertThat(e.getStatusCode().value()).isEqualTo(410));
		verifyNoInteractions(audit);
	}

	private static Link withId(Link link, long id) {
		return new Link(id, link.code(), link.url(), link.createdAt(), link.retiredAt(), link.idempotencyKey());
	}
}
