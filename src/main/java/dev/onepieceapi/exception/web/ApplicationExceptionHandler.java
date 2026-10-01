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
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

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
 * <li>Request-shape failures raised by Spring MVC itself - Bean Validation on a body or
 * on a method parameter, a parameter of the wrong type or missing, a malformed body -
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

	private static final String INVALID_VALUE = "invalid value";

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
		return validationFailed(violations, request);
	}

	/**
	 * A constraint annotation on a controller method parameter (a query parameter, a path
	 * variable, a resolved argument such as a {@code Pageable}) was violated.
	 */
	@ExceptionHandler(HandlerMethodValidationException.class)
	ProblemDetail handleParameterValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
		List<FieldViolation> violations = ex.getParameterValidationResults()
			.stream()
			.flatMap(ApplicationExceptionHandler::parameterViolations)
			.toList();
		return validationFailed(violations, request);
	}

	/**
	 * A query parameter or path variable could not be converted to its type, e.g. a
	 * malformed UUID or an unknown enum value.
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		return validationFailed(List.of(new FieldViolation(ex.getName(), INVALID_VALUE)), request);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
		return validationFailed(List.of(new FieldViolation(ex.getParameterName(), "is required")), request);
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

	/**
	 * The violations of one parameter: named after the field when the parameter is an
	 * object validated field by field, after the parameter itself otherwise.
	 */
	private static Stream<FieldViolation> parameterViolations(ParameterValidationResult result) {
		String parameterName = result.getMethodParameter().getParameterName();
		return result.getResolvableErrors().stream().map(error -> {
			if (error instanceof FieldError fieldError) {
				return new FieldViolation(fieldError.getField(), fieldErrorMessage(fieldError));
			}
			String message = error.getDefaultMessage();
			return new FieldViolation(parameterName, message != null ? message : INVALID_VALUE);
		});
	}

	/**
	 * A value that could not be converted to the type of its field is reported as just
	 * that: the message Spring builds for it names internal classes.
	 */
	private static String fieldErrorMessage(FieldError fieldError) {
		String message = fieldError.getDefaultMessage();
		return fieldError.isBindingFailure() || message == null ? INVALID_VALUE : message;
	}

	private ProblemDetail validationFailed(List<FieldViolation> violations, HttpServletRequest request) {
		ProblemDetail problem = problemDetail(HttpStatus.BAD_REQUEST, "Validation failed",
				CommonErrorCode.VALIDATION_FAILED, request);
		problem.setProperty("errors", violations);
		return problem;
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
