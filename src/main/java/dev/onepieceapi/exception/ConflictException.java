package dev.onepieceapi.exception;

/** The request conflicts with the current state of the resource (409). */
public abstract class ConflictException extends ApplicationException {

	protected ConflictException(ErrorCode errorCode, String message) {
		super(errorCode, ErrorCategory.CONFLICT, message);
	}

}
