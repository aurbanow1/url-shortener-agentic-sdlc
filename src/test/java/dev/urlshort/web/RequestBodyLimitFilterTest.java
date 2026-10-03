package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.ErrorResponseException;

/** The NFR-S3 body cap: exactly 16 384 bytes pass, byte 16 385 is a {@code 413} (AC-7). */
class RequestBodyLimitFilterTest {

	private final RequestBodyLimitFilter filter = new RequestBodyLimitFilter();

	@Test
	void aBodyOfExactlyTheLimitIsReadInFull() throws Exception {
		AtomicInteger read = new AtomicInteger();

		filter.doFilter(requestWithBody(16_384), new MockHttpServletResponse(),
				chain(in -> read.set(in.readAllBytes().length)));

		assertThat(read.get()).isEqualTo(16_384);
	}

	@Test
	void theFirstByteOverTheLimitIs413OnTheBulkReadPath() {
		assertThatThrownBy(() -> filter.doFilter(requestWithBody(16_385), new MockHttpServletResponse(),
				chain(InputStream::readAllBytes)))
				.isInstanceOfSatisfying(ErrorResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(413));
	}

	@Test
	void singleByteReadsCountTooAndEndOfStreamIsPassedThrough() throws Exception {
		AtomicInteger read = new AtomicInteger();
		filter.doFilter(requestWithBody(16_384), new MockHttpServletResponse(), chain(in -> {
			while (in.read() != -1) {
				read.incrementAndGet();
			}
		}));
		assertThat(read.get()).isEqualTo(16_384);

		assertThatThrownBy(() -> filter.doFilter(requestWithBody(16_385), new MockHttpServletResponse(), chain(in -> {
			while (in.read() != -1) {
				// drain
			}
		}))).isInstanceOf(ErrorResponseException.class);
	}

	@Test
	void theWrappedStreamIsCreatedOnceSoTheCountCannotBeReset() throws Exception {
		AtomicReference<ServletInputStream> first = new AtomicReference<>();
		AtomicReference<ServletInputStream> second = new AtomicReference<>();

		filter.doFilter(requestWithBody(1), new MockHttpServletResponse(), (req, res) -> {
			first.set(req.getInputStream());
			second.set(req.getInputStream());
		});

		assertThat(second.get()).isSameAs(first.get());
	}

	@Test
	void servletStreamStateAndListenerDelegateToTheContainerStream() throws IOException {
		StubStream stub = new StubStream();
		RequestBodyLimitFilter.LimitedInputStream limited = new RequestBodyLimitFilter.LimitedInputStream(stub);
		ReadListener listener = new ReadListener() {
			@Override
			public void onDataAvailable() {
			}

			@Override
			public void onAllDataRead() {
			}

			@Override
			public void onError(Throwable t) {
			}
		};

		assertThat(limited.isFinished()).isTrue();
		assertThat(limited.isReady()).isFalse();
		limited.setReadListener(listener);
		limited.close();

		assertThat(stub.listener).isSameAs(listener);
		assertThat(stub.closed).isTrue();
	}

	private static MockHttpServletRequest requestWithBody(int bytes) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/links");
		request.setContent(new byte[bytes]);
		return request;
	}

	private static FilterChain chain(StreamConsumer consumer) {
		return (req, res) -> consumer.accept(((HttpServletRequest) req).getInputStream());
	}

	@FunctionalInterface
	private interface StreamConsumer {
		void accept(InputStream in) throws IOException;
	}

	private static final class StubStream extends ServletInputStream {
		private final InputStream in = new ByteArrayInputStream(new byte[0]);
		ReadListener listener;
		boolean closed;

		@Override
		public boolean isFinished() {
			return true;
		}

		@Override
		public boolean isReady() {
			return false;
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			this.listener = readListener;
		}

		@Override
		public int read() throws IOException {
			return in.read();
		}

		@Override
		public void close() {
			closed = true;
		}
	}
}
