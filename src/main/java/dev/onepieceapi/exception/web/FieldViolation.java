package dev.onepieceapi.exception.web;

/** One field-level failure within a {@code VALIDATION_FAILED} error response. */
public record FieldViolation(String field, String message) {
}
