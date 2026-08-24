package dev.onepieceapi.exception.web;

import dev.onepieceapi.exception.ConflictException;
import dev.onepieceapi.exception.DomainException;
import dev.onepieceapi.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationExceptionHandlerTest {

	// TraceIdFilter itself isn't wired into this standalone MockMvc setup (that's
	// TraceIdFilterTest's job) - simulating what it always does in production, putting a
	// trace id into MDC before each request, so the handler has one to read back.
	private static final String TRACE_ID = "trace-123";

	private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
		.setControllerAdvice(new ApplicationExceptionHandler())
		.build();

	@BeforeEach
	void putTraceIdInMdc() {
		MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, TRACE_ID);
	}

	@AfterEach
	void clearMdc() {
		MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
	}

	@Test
	void mapsAConflictExceptionToItsFixedStatusAndErrorCode() throws Exception {
		this.mockMvc.perform(post("/stub/conflict"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("ALREADY_EXISTS"))
			.andExpect(jsonPath("$.detail").value("already exists"))
			.andExpect(jsonPath("$.traceId").value(TRACE_ID))
			.andExpect(jsonPath("$.timestamp").value(notNullValue()));
	}

	@Test
	void mapsADomainExceptionToUnprocessableEntity() throws Exception {
		this.mockMvc.perform(post("/stub/domain"))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_BROKEN"));
	}

	@Test
	void carriesExceptionDetailsAsResponseProperties() throws Exception {
		this.mockMvc.perform(post("/stub/conflict")).andExpect(jsonPath("$.conflictingField").value("email"));
	}

	@Test
	void mapsBeanValidationFailuresToFieldLevelViolations() throws Exception {
		this.mockMvc.perform(post("/stub/validated").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": ""}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("name"));
	}

	@Test
	void mapsAMalformedRequestBodyToValidationFailed() throws Exception {
		var request = post("/stub/validated").contentType(MediaType.APPLICATION_JSON).content("not json");
		this.mockMvc.perform(request)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
	}

	@Test
	void mapsAnUnexpectedExceptionToAGenericInternalError() throws Exception {
		this.mockMvc.perform(post("/stub/boom"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
			.andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
	}

	private enum StubErrorCode implements ErrorCode {

		ALREADY_EXISTS, BUSINESS_RULE_BROKEN;

		@Override
		public String code() {
			return name();
		}

	}

	private static final class StubConflictException extends ConflictException {

		StubConflictException() {
			super(StubErrorCode.ALREADY_EXISTS, "already exists");
			withDetail("conflictingField", "email");
		}

	}

	private static final class StubDomainException extends DomainException {

		StubDomainException() {
			super(StubErrorCode.BUSINESS_RULE_BROKEN, "business rule broken");
		}

	}

	record ValidatedBody(@NotBlank String name) {
	}

	@RestController
	@RequestMapping("/stub")
	static class StubController {

		@PostMapping("/conflict")
		void conflict() {
			throw new StubConflictException();
		}

		@PostMapping("/domain")
		void domain() {
			throw new StubDomainException();
		}

		@PostMapping("/boom")
		void boom() {
			throw new IllegalStateException("kaboom");
		}

		@PostMapping("/validated")
		void validated(@Validated @RequestBody ValidatedBody body) {
		}

	}

}
