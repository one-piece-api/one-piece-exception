package dev.onepieceapi.exception.web.autoconfigure;

import dev.onepieceapi.exception.web.ApplicationExceptionHandler;
import dev.onepieceapi.exception.web.TraceIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Wires this library's exception handling into any Spring Boot Servlet application that
 * depends on it - no manual bean registration needed, following the same
 * auto-configuration pattern Spring Boot's own starters use.
 */
@AutoConfiguration
public class ExceptionHandlingAutoConfiguration {

	@Bean
	FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
		// Highest precedence so a trace id exists even for requests rejected by the
		// security filter chain before ever reaching a controller (e.g. 401/403).
		var registration = new FilterRegistrationBean<>(new TraceIdFilter());
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

	@Bean
	ApplicationExceptionHandler applicationExceptionHandler() {
		return new ApplicationExceptionHandler();
	}

}
