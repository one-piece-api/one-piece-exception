package dev.onepieceapi.exception;

/**
 * The request is well-formed and its target exists, but performing it would violate a
 * business rule (422) - e.g. "cannot remove the last administrator." Distinct from
 * {@link ValidationException} (malformed request) and {@link ConflictException} (state
 * already conflicts, independent of any rule about the action being taken).
 */
public abstract class DomainException extends ApplicationException {

	protected DomainException(ErrorCode errorCode, String message) {
		super(errorCode, ErrorCategory.DOMAIN, message);
	}

}
