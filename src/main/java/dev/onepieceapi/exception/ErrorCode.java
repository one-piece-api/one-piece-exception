package dev.onepieceapi.exception;

/**
 * A stable, machine-readable identifier for one specific error condition (e.g.
 * {@code "USER_EMAIL_ALREADY_REGISTERED"}) - the value a client is meant to switch on, as
 * opposed to {@link ApplicationException#getMessage()}, which is a human-readable detail
 * that may change wording freely without breaking callers.
 *
 * <p>
 * Each consuming service defines its own domain error codes as an enum implementing this
 * interface (see {@link CommonErrorCode} for the codes this library itself raises) -
 * there is no shared registry, so a new service can introduce codes without coordinating
 * with this library or any other.
 */
public interface ErrorCode {

	/**
	 * The wire value. Convention: {@code SCREAMING_SNAKE_CASE}, unique within the
	 * emitting service, and never changed once a client may depend on it - introduce a
	 * new code instead of repurposing an old one.
	 */
	String code();

}
