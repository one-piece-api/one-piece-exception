package dev.onepieceapi.exception.web.autoconfigure;

import dev.onepieceapi.exception.web.ConcurrentModificationExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Maps optimistic locking failures to {@code 409} - only when the service itself has
 * Spring's data access abstraction, which this library never pulls in on its own.
 */
@AutoConfiguration
@ConditionalOnClass(OptimisticLockingFailureException.class)
public class ConcurrentModificationAutoConfiguration {

	@Bean
	ConcurrentModificationExceptionHandler concurrentModificationExceptionHandler() {
		return new ConcurrentModificationExceptionHandler();
	}

}
