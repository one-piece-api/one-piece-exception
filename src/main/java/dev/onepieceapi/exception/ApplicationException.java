package dev.onepieceapi.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Base of every exception this library's {@code ApplicationExceptionHandler} knows how to
 * translate into a standard error response. Never thrown directly - always through one of
 * the category-fixing subclasses ({@link ValidationException}, {@link NotFoundException},
 * {@link ConflictException}, {@link ForbiddenException}, {@link UnauthorizedException},
 * {@link DomainException}), which a service extends further with its own domain
 * exceptions (e.g. {@code EmailAlreadyRegisteredException
 * extends ConflictException}).
 */
public abstract class ApplicationException extends RuntimeException {

	private final ErrorCode errorCode;

	private final ErrorCategory category;

	private final Map<String, Object> details = new LinkedHashMap<>();

	protected ApplicationException(ErrorCode errorCode, ErrorCategory category, String message) {
		super(message);
		this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
		this.category = Objects.requireNonNull(category, "category must not be null");
	}

	protected ApplicationException(ErrorCode errorCode, ErrorCategory category, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
		this.category = Objects.requireNonNull(category, "category must not be null");
	}

	public ErrorCode getErrorCode() {
		return this.errorCode;
	}

	public ErrorCategory getCategory() {
		return this.category;
	}

	/** Extra troubleshooting context (e.g. the offending field or identifier). */
	public Map<String, Object> getDetails() {
		return this.details;
	}

	/** Fluent accumulation of a detail entry, for use at throw-site. */
	public ApplicationException withDetail(String key, Object value) {
		this.details.put(key, value);
		return this;
	}

}
