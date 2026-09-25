package dev.onepieceapi.exception.web.autoconfigure;

import dev.onepieceapi.exception.web.openapi.ProblemDetailOpenApiCustomizer;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Documents this library's error contract in the service's OpenAPI spec - only when the
 * service itself uses springdoc, which this library never pulls in on its own.
 */
@AutoConfiguration
@ConditionalOnClass(OpenApiCustomizer.class)
public class ProblemDetailOpenApiAutoConfiguration {

	@Bean
	ProblemDetailOpenApiCustomizer problemDetailOpenApiCustomizer() {
		return new ProblemDetailOpenApiCustomizer();
	}

}
