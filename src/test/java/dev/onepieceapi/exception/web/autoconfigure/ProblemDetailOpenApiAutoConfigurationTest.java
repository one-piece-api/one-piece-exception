package dev.onepieceapi.exception.web.autoconfigure;

import dev.onepieceapi.exception.web.openapi.ProblemDetailOpenApiCustomizer;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailOpenApiAutoConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ProblemDetailOpenApiAutoConfiguration.class));

	@Test
	void registersTheCustomizerWhenSpringdocIsPresent() {
		this.runner.run(context -> assertThat(context).hasSingleBean(ProblemDetailOpenApiCustomizer.class));
	}

	@Test
	void backsOffWhenTheServiceDoesNotUseSpringdoc() {
		this.runner.withClassLoader(new FilteredClassLoader(OpenApiCustomizer.class))
			.run(context -> assertThat(context).doesNotHaveBean(ProblemDetailOpenApiCustomizer.class));
	}

}
