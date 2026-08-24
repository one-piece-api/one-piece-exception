package dev.onepieceapi.exception.web;

import dev.onepieceapi.exception.ApplicationException;
import dev.onepieceapi.exception.CommonErrorCode;
import dev.onepieceapi.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Translates every exception a controller can throw into the standard
 * {@link ProblemDetail} contract (RFC 7807, extended with
 * {@code errorCode}/{@code traceId}/{@code timestamp} - ProblemDetail's own
 * {@link ProblemDetail#setProperty(String, Object) setProperty} mechanism, not a custom
 * envelope reinventing what Spring already provides). Registered automatically by this
 * library's Spring Boot auto-configuration - a consuming service gets this handler for
 * free by depending on the library, no manual wiring required.
 *
 * <p>
 * Three cases, in order of precedence:
 * <ul>
 * <li>{@link ApplicationException} (and every service-specific subclass of it) - status
 * and error code come straight from the exception.</li>
 * <li>Bean Validation / malformed request body failures raised by Spring MVC itself -
 * mapped to {@link CommonErrorCode#VALIDATION_FAILED}, with per-field detail when
 * available.</li>
 * <li>Anything else - an unanticipated failure, logged with its stack trace and returned
 * as a generic {@link CommonErrorCode#INTERNAL_ERROR} with no exception detail leaked to
 * the client.</li>
 * </ul>
 */
@RestControllerAdvice
@Slf4j
public class ApplicationExceptionHandler {

	@ExceptionHandler(ApplicationException.class)
	ProblemDetail handleApplicationException(ApplicationException ex, HttpServletRequest request) {
		ProblemDetail problem = problemDetail(ex.getCategory().httpStatus(), ex.getMessage(), ex.getErrorCode(),
				request);
		ex.getDetails().forEach(problem::setProperty);
		return problem;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<FieldViolation> violations = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(fieldError -> new FieldViolation(fieldError.getField(), fieldErrorMessage(fieldError)))
			.toList();
		ProblemDetail problem = problemDetail(HttpStatus.BAD_REQUEST, "Validation failed",
				CommonErrorCode.VALIDATION_FAILED, request);
		problem.setProperty("errors", violations);
		return problem;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleMalformedBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return problemDetail(HttpStatus.BAD_REQUEST, "The request body could not be read",
				CommonErrorCode.VALIDATION_FAILED, request);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred",
				CommonErrorCode.INTERNAL_ERROR, request);
	}

	private static String fieldErrorMessage(FieldError fieldError) {
		String message = fieldError.getDefaultMessage();
		return message != null ? message : "invalid value";
	}

	private ProblemDetail problemDetail(HttpStatus status, String detail, ErrorCode errorCode,
			HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("errorCode", errorCode.code());
		problem.setProperty("traceId", MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
		problem.setProperty("timestamp", Instant.now());
		return problem;
	}

}
