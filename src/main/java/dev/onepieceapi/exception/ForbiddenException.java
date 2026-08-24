package dev.onepieceapi.exception;

/** The caller is authenticated but not allowed to perform this action (403). */
public abstract class ForbiddenException extends ApplicationException {

	protected ForbiddenException(ErrorCode errorCode, String message) {
		super(errorCode, ErrorCategory.FORBIDDEN, message);
	}

}
