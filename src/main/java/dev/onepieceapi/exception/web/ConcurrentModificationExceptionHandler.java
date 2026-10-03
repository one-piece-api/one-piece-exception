package dev.onepieceapi.exception.web;

import dev.onepieceapi.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates an optimistic locking failure - someone else changed the same row between
 * this request reading it and writing it back - into a {@code 409}
 * {@link CommonErrorCode#CONCURRENT_MODIFICATION}: the caller acted on a state that no
 * longer exists, and only has to read it again. Kept apart from
 * {@link ApplicationExceptionHandler} because it names a {@code spring-tx} type, which a
 * service without data access does not have; registered only when it does. Ordered first,
 * so it is chosen over {@link ApplicationExceptionHandler}'s catch-all.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConcurrentModificationExceptionHandler {

	private static final String DETAIL = "The resource was changed by another request in the meantime";

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException ex, HttpServletRequest request) {
		var errorCode = CommonErrorCode.CONCURRENT_MODIFICATION;
		return ApplicationExceptionHandler.problemDetail(HttpStatus.CONFLICT, DETAIL, errorCode, request);
	}

}
