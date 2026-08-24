package dev.onepieceapi.exception;

/** The caller has no valid credentials at all (401). */
public abstract class UnauthorizedException extends ApplicationException {

	protected UnauthorizedException(ErrorCode errorCode, String message) {
		super(errorCode, ErrorCategory.UNAUTHORIZED, message);
	}

}
