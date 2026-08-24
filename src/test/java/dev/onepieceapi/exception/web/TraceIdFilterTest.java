package dev.onepieceapi.exception.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

	private final TraceIdFilter filter = new TraceIdFilter();

	@Test
	void generatesATraceIdWhenTheRequestCarriesNone() throws Exception {
		var request = new MockHttpServletRequest("GET", "/");
		var response = new MockHttpServletResponse();

		this.filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isNotBlank();
	}

	@Test
	void reusesAnInboundTraceIdInstead() throws Exception {
		var request = new MockHttpServletRequest("GET", "/");
		request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "caller-supplied-id");
		var response = new MockHttpServletResponse();

		this.filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("caller-supplied-id");
	}

	@Test
	void clearsTheMdcEntryAfterTheRequestCompletes() throws Exception {
		var request = new MockHttpServletRequest("GET", "/");
		var response = new MockHttpServletResponse();

		this.filter.doFilter(request, response, new MockFilterChain());

		assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
	}

	@Test
	void makesTheTraceIdAvailableInMdcWhileTheChainRuns() throws Exception {
		var request = new MockHttpServletRequest("GET", "/");
		var response = new MockHttpServletResponse();
		var traceIdSeenDuringChain = new String[1];
		var chain = new MockFilterChain() {

			@Override
			public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
				traceIdSeenDuringChain[0] = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
			}

		};

		this.filter.doFilter(request, response, chain);

		assertThat(traceIdSeenDuringChain[0]).isEqualTo(response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
	}

}
