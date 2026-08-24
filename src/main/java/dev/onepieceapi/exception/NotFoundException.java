package dev.onepieceapi.exception;

/** The referenced resource does not exist (404). */
public abstract class NotFoundException extends ApplicationException {

	protected NotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, ErrorCategory.NOT_FOUND, message);
	}

}
