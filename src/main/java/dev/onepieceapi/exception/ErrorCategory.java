package dev.onepieceapi.exception;

import org.springframework.http.HttpStatus;

/**
 * The fixed set of error kinds this library distinguishes, each with the HTTP status its
 * {@code ApplicationExceptionHandler} maps it to. An {@link ApplicationException}
 * subclass fixes its category (see {@link ValidationException},
 * {@link NotFoundException}, {@link ConflictException}, {@link ForbiddenException},
 * {@link UnauthorizedException}, {@link DomainException}) - services never pick an
 * {@link HttpStatus} directly, so the mapping stays centralized and consistent across
 * every service that depends on this library.
 */
public enum ErrorCategory {

	/** A request failed structural or field-level validation (e.g. Bean Validation). */
	VALIDATION(HttpStatus.BAD_REQUEST),
	/** The caller has no valid credentials at all. */
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
	/** The caller is authenticated but not allowed to perform this action. */
	FORBIDDEN(HttpStatus.FORBIDDEN),
	/** The referenced resource does not exist. */
	NOT_FOUND(HttpStatus.NOT_FOUND),
	/** The request conflicts with the current state of the resource. */
	CONFLICT(HttpStatus.CONFLICT),
	/**
	 * The request is well-formed and the resource exists, but violates a business rule
	 * (RFC 9110 §15.5.21: semantically invalid despite being syntactically correct) -
	 * distinct from {@link #VALIDATION}, which is about the shape of the request itself.
	 */
	DOMAIN(HttpStatus.UNPROCESSABLE_ENTITY),
	/** An unexpected failure; never raised deliberately by application code. */
	INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR);

	private final HttpStatus httpStatus;

	ErrorCategory(HttpStatus httpStatus) {
		this.httpStatus = httpStatus;
	}

	public HttpStatus httpStatus() {
		return this.httpStatus;
	}

}
