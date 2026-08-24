package dev.onepieceapi.exception;

/** The request itself is malformed at the field/shape level (400). */
public abstract class ValidationException extends ApplicationException {

	protected ValidationException(ErrorCode errorCode, String message) {
		super(errorCode, ErrorCategory.VALIDATION, message);
	}

}
