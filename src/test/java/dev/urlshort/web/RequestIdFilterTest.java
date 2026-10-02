package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

	private static final String HEADER = "X-Request-Id";
	private static final String MDC_KEY = "requestId";

	private final RequestIdFilter filter = new RequestIdFilter();

	@Test
	void issuesOneIdOnHeaderAndMdcBeforeTheChainRunsAndClearsMdcAfter() throws ServletException, IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> headerDuringChain = new AtomicReference<>();
		AtomicReference<String> mdcDuringChain = new AtomicReference<>();

		filter.doFilter(new MockHttpServletRequest("GET", "/api/ping"), response, (req, res) -> {
			headerDuringChain.set(((HttpServletResponse) res).getHeader(HEADER));
			mdcDuringChain.set(MDC.get(MDC_KEY));
		});

		String issued = response.getHeader(HEADER);
		assertThat(issued).isNotEmpty().hasSizeLessThanOrEqualTo(64).matches("[\\x21-\\x7E]+");
		assertThat(headerDuringChain.get()).isEqualTo(issued);
		assertThat(mdcDuringChain.get()).isEqualTo(issued);
		assertThat(MDC.get(MDC_KEY)).isNull();
	}

	@Test
	void ignoresInboundRequestIdAndIssuesADifferentIdPerRequest() throws ServletException, IOException {
		MockHttpServletRequest withInboundId = new MockHttpServletRequest("GET", "/api/ping");
		withInboundId.addHeader(HEADER, "canary-rid");
		MockHttpServletResponse first = new MockHttpServletResponse();
		MockHttpServletResponse second = new MockHttpServletResponse();

		filter.doFilter(withInboundId, first, new MockFilterChain());
		filter.doFilter(new MockHttpServletRequest("GET", "/api/ping"), second, new MockFilterChain());

		assertThat(first.getHeader(HEADER)).isNotEqualTo("canary-rid");
		assertThat(second.getHeader(HEADER)).isNotEqualTo(first.getHeader(HEADER));
	}

	@Test
	void clearsMdcWhenTheChainThrows() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
			throw new ServletException("boom");
		})).isInstanceOf(ServletException.class);

		assertThat(MDC.get(MDC_KEY)).isNull();
	}
}
