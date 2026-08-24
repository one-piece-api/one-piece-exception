package dev.onepieceapi.exception.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a per-request trace id - reusing the caller's own {@value #TRACE_ID_HEADER} if
 * it already sent one (so a gateway or an end-to-end test can correlate its own id across
 * services), otherwise generating a fresh one. Published on both the response header, for
 * the client, and SLF4J's {@link MDC}, for {@code ApplicationExceptionHandler} to read
 * back onto the error response and for log lines in between to pick up automatically once
 * the logging pattern includes {@code %X{traceId}}.
 *
 * <p>
 * A plain request-scoped id via {@link MDC} is deliberately simpler than wiring
 * Micrometer Tracing/OpenTelemetry: this project has no distributed-tracing backend to
 * export spans to yet, so that dependency has no concrete need to justify it today -
 * revisit if/when one is introduced.
 */
public class TraceIdFilter extends OncePerRequestFilter {

	public static final String TRACE_ID_HEADER = "X-Trace-Id";

	public static final String TRACE_ID_MDC_KEY = "traceId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String inbound = request.getHeader(TRACE_ID_HEADER);
		String traceId = StringUtils.hasText(inbound) ? inbound : UUID.randomUUID().toString();
		MDC.put(TRACE_ID_MDC_KEY, traceId);
		response.setHeader(TRACE_ID_HEADER, traceId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			MDC.remove(TRACE_ID_MDC_KEY);
		}
	}

}
