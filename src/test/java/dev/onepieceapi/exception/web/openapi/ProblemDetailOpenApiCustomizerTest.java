package dev.onepieceapi.exception.web.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailOpenApiCustomizerTest {

	private final ProblemDetailOpenApiCustomizer customizer = new ProblemDetailOpenApiCustomizer();

	@Test
	void registersTheProblemDetailAndFieldViolationSchemas() {
		var openApi = new OpenAPI();

		this.customizer.customise(openApi);

		var schemas = openApi.getComponents().getSchemas();
		assertThat(schemas).containsKeys("ProblemDetail", "FieldViolation");
		Schema<?> problem = schemas.get("ProblemDetail");
		var problemProperties = problem.getProperties();
		assertThat(problemProperties).containsKeys("type", "title", "status", "detail", "instance", "errorCode",
				"traceId", "timestamp", "errors");
	}

	@Test
	void registersSharedProblemJsonErrorResponses() {
		var openApi = new OpenAPI();

		this.customizer.customise(openApi);

		var responses = openApi.getComponents().getResponses();
		assertThat(responses).containsKeys("ClientError", "ServerError");
		var problemJson = responses.get("ClientError").getContent().get("application/problem+json");
		assertThat(problemJson.getSchema().get$ref()).isEqualTo("#/components/schemas/ProblemDetail");
	}

	@Test
	void referencesTheSharedErrorResponsesFromEveryOperation() {
		var get = new Operation().responses(new ApiResponses().addApiResponse("200", new ApiResponse()));
		var post = new Operation().responses(new ApiResponses().addApiResponse("201", new ApiResponse()));
		var openApi = new OpenAPI().paths(new Paths().addPathItem("/crew", new PathItem().get(get).post(post)));

		this.customizer.customise(openApi);

		for (Operation operation : new Operation[] { get, post }) {
			var responses = operation.getResponses();
			assertThat(responses.get("4XX").get$ref()).isEqualTo("#/components/responses/ClientError");
			assertThat(responses.get("5XX").get$ref()).isEqualTo("#/components/responses/ServerError");
		}
	}

	@Test
	void keepsAResponseTheServiceAlreadyDocumentedItself() {
		var ownDescription = new ApiResponse().description("service-specific");
		var operation = new Operation().responses(new ApiResponses().addApiResponse("4XX", ownDescription));
		var openApi = new OpenAPI().paths(new Paths().addPathItem("/crew", new PathItem().get(operation)));

		this.customizer.customise(openApi);

		assertThat(operation.getResponses().get("4XX")).isSameAs(ownDescription);
	}

}
