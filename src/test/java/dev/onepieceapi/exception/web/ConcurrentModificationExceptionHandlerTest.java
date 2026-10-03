package dev.onepieceapi.exception.web;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConcurrentModificationExceptionHandlerTest {

	// Both handlers, as a service with data access gets them: the catch-all of
	// ApplicationExceptionHandler must not win.
	private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
		.setControllerAdvice(new ApplicationExceptionHandler(), new ConcurrentModificationExceptionHandler())
		.build();

	@Test
	void mapsAnOptimisticLockingFailureToConflict() throws Exception {
		this.mockMvc.perform(post("/stub/stale"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CONCURRENT_MODIFICATION"))
			.andExpect(jsonPath("$.detail").value(containsString("changed by another request")))
			.andExpect(jsonPath("$.timestamp").value(notNullValue()));
	}

	@Test
	void leavesAnyOtherExceptionToTheCatchAll() throws Exception {
		this.mockMvc.perform(post("/stub/boom"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
	}

	@RestController
	@RequestMapping("/stub")
	static class StubController {

		@PostMapping("/stale")
		void stale() {
			throw new OptimisticLockingFailureException("row was updated by another transaction");
		}

		@PostMapping("/boom")
		void boom() {
			throw new IllegalStateException("kaboom");
		}

	}

}
