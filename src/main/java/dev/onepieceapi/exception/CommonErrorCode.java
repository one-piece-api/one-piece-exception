package dev.onepieceapi.exception;

/**
 * Error codes this library itself raises, for the cross-cutting cases every service hits
 * regardless of domain (request validation, a path or method nothing is mapped to, an
 * upload over the configured limit, a concurrent modification, an unmapped/unexpected
 * failure). Domain-specific error codes belong to the consuming service, not here - see
 * {@link ErrorCode}.
 */
public enum CommonErrorCode implements ErrorCode {

	VALIDATION_FAILED, NOT_FOUND, METHOD_NOT_ALLOWED, CONTENT_TOO_LARGE, CONCURRENT_MODIFICATION, INTERNAL_ERROR;

	@Override
	public String code() {
		return name();
	}

}
