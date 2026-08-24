package dev.onepieceapi.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationExceptionTest {

	private enum TestErrorCode implements ErrorCode {

		SOMETHING_WENT_WRONG;

		@Override
		public String code() {
			return name();
		}

	}

	private static final class TestException extends ApplicationException {

		TestException() {
			super(TestErrorCode.SOMETHING_WENT_WRONG, ErrorCategory.CONFLICT, "something went wrong");
		}

	}

	@Test
	void exposesTheErrorCodeCategoryAndMessageItWasConstructedWith() {
		var exception = new TestException();

		assertThat(exception.getErrorCode()).isEqualTo(TestErrorCode.SOMETHING_WENT_WRONG);
		assertThat(exception.getCategory()).isEqualTo(ErrorCategory.CONFLICT);
		assertThat(exception.getMessage()).isEqualTo("something went wrong");
	}

	@Test
	void accumulatesDetailsFluentlyAtThrowSite() {
		var exception = new TestException();
		exception.withDetail("field", "email").withDetail("value", "usopp@onepiece.local");

		assertThat(exception.getDetails()).containsExactly(java.util.Map.entry("field", "email"),
				java.util.Map.entry("value", "usopp@onepiece.local"));
	}

	@Test
	void eachCategoryFixingSubclassCarriesItsOwnHttpStatus() {
		assertThat(ErrorCategory.VALIDATION.httpStatus().value()).isEqualTo(400);
		assertThat(ErrorCategory.UNAUTHORIZED.httpStatus().value()).isEqualTo(401);
		assertThat(ErrorCategory.FORBIDDEN.httpStatus().value()).isEqualTo(403);
		assertThat(ErrorCategory.NOT_FOUND.httpStatus().value()).isEqualTo(404);
		assertThat(ErrorCategory.CONFLICT.httpStatus().value()).isEqualTo(409);
		assertThat(ErrorCategory.DOMAIN.httpStatus().value()).isEqualTo(422);
		assertThat(ErrorCategory.INTERNAL.httpStatus().value()).isEqualTo(500);
	}

}
