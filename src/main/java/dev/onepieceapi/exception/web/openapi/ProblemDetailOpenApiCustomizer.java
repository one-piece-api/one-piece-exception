package dev.onepieceapi.exception.web.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.List;
import java.util.Objects;

/**
 * Documents this library's error contract in a consuming service's OpenAPI spec: the
 * {@code ProblemDetail} schema (RFC 9457 plus {@code errorCode}/{@code traceId}/
 * {@code timestamp}/{@code errors}), shared {@code ClientError}/{@code ServerError}
 * responses using it, and a {@code 4XX}/{@code 5XX} reference to them on every operation
 * - the shape {@code ApplicationExceptionHandler} actually returns, defined once here
 * instead of re-described by every service. Registered only when springdoc is on the
 * classpath (see {@code ProblemDetailOpenApiAutoConfiguration}).
 */
public class ProblemDetailOpenApiCustomizer implements OpenApiCustomizer {

	static final String PROBLEM_DETAIL = "ProblemDetail";

	static final String FIELD_VIOLATION = "FieldViolation";

	static final String CLIENT_ERROR = "ClientError";

	static final String SERVER_ERROR = "ServerError";

	private static final String PROBLEM_JSON = "application/problem+json";

	// 401/403 raised by the Spring Security filter chain never reach
	// ApplicationExceptionHandler (see ADR-0001), so they carry no body at all.
	private static final String CLIENT_ERROR_DESCRIPTION = "Client error - a problem detail, except a 401/403 "
			+ "rejected by the security filter chain before reaching the application, which has no body";

	@Override
	public void customise(OpenAPI openApi) {
		var components = Objects.requireNonNullElseGet(openApi.getComponents(), Components::new);
		components.addSchemas(FIELD_VIOLATION, fieldViolation())
			.addSchemas(PROBLEM_DETAIL, problemDetail())
			.addResponses(CLIENT_ERROR, problemResponse(CLIENT_ERROR_DESCRIPTION))
			.addResponses(SERVER_ERROR, problemResponse("Unexpected server error"));
		openApi.setComponents(components);
		if (openApi.getPaths() == null) {
			return;
		}
		openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
			operation.getResponses()
				.putIfAbsent("4XX", new ApiResponse().$ref("#/components/responses/" + CLIENT_ERROR));
			operation.getResponses()
				.putIfAbsent("5XX", new ApiResponse().$ref("#/components/responses/" + SERVER_ERROR));
		}));
	}

	private static ApiResponse problemResponse(String description) {
		var problemJson = new MediaType().schema(schemaRef(PROBLEM_DETAIL));
		return new ApiResponse().description(description)
			.content(new Content().addMediaType(PROBLEM_JSON, problemJson));
	}

	private static Schema<?> problemDetail() {
		var violations = new ArraySchema().items(schemaRef(FIELD_VIOLATION))
			.description("Per-field failures, present only when errorCode is VALIDATION_FAILED");
		var problem = new ObjectSchema();
		problem.description("RFC 9457 problem detail, plus this API's own properties")
			.addProperty("type", new StringSchema().format("uri"))
			.addProperty("title", new StringSchema())
			.addProperty("status", new IntegerSchema())
			.addProperty("detail", new StringSchema())
			.addProperty("instance", new StringSchema().format("uri"))
			.addProperty("errorCode", new StringSchema().description("Stable, machine-readable error code"))
			.addProperty("traceId", new StringSchema())
			.addProperty("timestamp", new DateTimeSchema())
			.addProperty("errors", violations);
		problem.setRequired(List.of("type", "title", "status", "errorCode"));
		return problem;
	}

	private static Schema<?> schemaRef(String name) {
		return new Schema<>().$ref("#/components/schemas/" + name);
	}

	private static Schema<?> fieldViolation() {
		var violation = new ObjectSchema();
		violation.addProperty("field", new StringSchema()).addProperty("message", new StringSchema());
		violation.setRequired(List.of("field", "message"));
		return violation;
	}

}
