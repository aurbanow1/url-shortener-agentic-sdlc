package dev.urlshort.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Caps every request body at {@value #MAX_BODY_BYTES} bytes (NFR-S3, AC-7): reading byte 16 385
 * throws a {@code 413} {@link ErrorResponseException}, exact for declared and chunked bodies alike.
 * The exception is raised inside the DispatcherServlet, so {@code ProblemDetailsAdvice} renders it.
 *
 * <p>Runs right after {@link RequestIdFilter}, so a {@code 413} still carries its request id. Only
 * {@code getInputStream()} is wrapped: the JSON path never calls {@code getReader()} and multipart
 * parsing is disabled.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class RequestBodyLimitFilter extends OncePerRequestFilter {

	static final int MAX_BODY_BYTES = 16_384;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		chain.doFilter(new HttpServletRequestWrapper(request) {

			private @Nullable LimitedInputStream body;

			@Override
			public ServletInputStream getInputStream() throws IOException {
				// one counting stream per request, so a second call cannot restart the count
				if (body == null) {
					body = new LimitedInputStream(super.getInputStream());
				}
				return body;
			}
		}, response);
	}

	/** Counts body bytes as they are read and fails on the first byte over the cap. */
	static final class LimitedInputStream extends ServletInputStream {

		private final ServletInputStream in;
		private long count;

		LimitedInputStream(ServletInputStream in) {
			this.in = in;
		}

		@Override
		public int read() throws IOException {
			int b = in.read();
			if (b != -1) {
				count(1);
			}
			return b;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int n = in.read(b, off, len);
			if (n > 0) {
				count(n);
			}
			return n;
		}

		private void count(int n) {
			count += n;
			if (count > MAX_BODY_BYTES) {
				throw new ErrorResponseException(HttpStatus.CONTENT_TOO_LARGE);
			}
		}

		@Override
		public boolean isFinished() {
			return in.isFinished();
		}

		@Override
		public boolean isReady() {
			return in.isReady();
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			in.setReadListener(readListener);
		}

		@Override
		public void close() throws IOException {
			in.close();
		}
	}
}
