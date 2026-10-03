package dev.onepieceapi.exception.web.autoconfigure;

import dev.onepieceapi.exception.web.ConcurrentModificationExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentModificationAutoConfigurationTest {

	private static final Class<?> HANDLER = ConcurrentModificationExceptionHandler.class;

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ConcurrentModificationAutoConfiguration.class));

	@Test
	void registersTheHandlerWhenDataAccessIsPresent() {
		this.runner.run(context -> assertThat(context).hasSingleBean(HANDLER));
	}

	@Test
	void backsOffWhenTheServiceHasNoDataAccess() {
		this.runner.withClassLoader(new FilteredClassLoader(OptimisticLockingFailureException.class))
			.run(context -> assertThat(context).doesNotHaveBean(HANDLER));
	}

}
