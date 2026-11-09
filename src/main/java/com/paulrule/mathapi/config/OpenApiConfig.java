package com.paulrule.mathapi.config;

import com.paulrule.mathapi.domain.Calculation;
import com.paulrule.mathapi.domain.DivisionByZeroException;
import com.paulrule.mathapi.domain.OperandOutOfRangeException;
import com.paulrule.mathapi.domain.Operation;
import com.paulrule.mathapi.domain.UnknownOperationException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

/**
 * Adjusts springdoc's generated document (UC-005). Domain-derived values are generated from the domain, never copied
 * (R-2, STANDARDS.md §4); adapter texts are pinned by {@code ApiDocumentationTest}'s live comparisons.
 */
@Configuration
class OpenApiConfig {

    private static final String LIMITS = "at most " + Calculation.MAX_SIGNIFICANT_DIGITS
            + " significant digits and a scale from -" + Calculation.MAX_SCALE + " to " + Calculation.MAX_SCALE;

    private static final String DESCRIPTION = "Math API performs one Calculation (an Operation applied to the Operands"
            + " a and b) and returns its Result. Operands may be JSON numbers or JSON strings, and every decimal value"
            + " in a response is a JSON string in plain notation. Each Operand, as written, has " + LIMITS
            + " (BR-1). A request is at most 16,384 bytes, and any value at most 1,000 characters. A rejection is RFC"
            + " 9457 Problem Details (application/problem+json) with a stable type. Every other client error (4xx, for"
            + " example 404, 405 or 415) is Problem Details with type about:blank.";

    private static final String PATH = "/api/v1/calculations";

    @Bean
    OpenApiCustomizer calculationDocumentation() {
        return openApi -> {
            openApi.info(new Info().title("Math API").version("v1").description(DESCRIPTION));
            describeRequest(openApi);
            // springdoc documents ProblemDetail's internal map, which the JSON never contains.
            openApi.getComponents()
                    .getSchemas()
                    .get("ProblemDetail")
                    .getProperties()
                    .remove("properties");
            addExamples(openApi);
        };
    }

    @SuppressWarnings("unchecked")
    private static void describeRequest(OpenAPI openApi) {
        Map<String, Schema> properties =
                openApi.getComponents().getSchemas().get("CalculationRequest").getProperties();
        properties
                .get("operation")
                .setEnum(Arrays.stream(Operation.values()).map(Enum::name).toList());
        for (var operand : List.of("a", "b")) {
            var schema = properties.get(operand);
            schema.setType(null);
            schema.setTypes(Set.of("number", "string"));
            schema.setDescription("An Operand: a decimal number, as a JSON number or a JSON string. As written, "
                    + LIMITS + " (BR-1); at most 1,000 characters.");
        }
    }

    private static void addExamples(OpenAPI openApi) {
        var operation = openApi.getPaths().get(PATH).getPost();
        var divide = new Calculation(Operation.DIVIDE, new BigDecimal("1"), new BigDecimal("3"));

        json(operation.getRequestBody().getContent())
                .addExamples(
                        "divide",
                        new Example()
                                .summary("Divide 1 by 3")
                                .value(Map.of("operation", Operation.DIVIDE.name(), "a", 1, "b", "3")));

        ApiResponses responses = operation.getResponses();
        json(responses.get("200").getContent())
                .addExamples(
                        "divide",
                        new Example()
                                .summary("Divide 1 by 3")
                                .value(Map.of(
                                        "operation", divide.operation().name(),
                                        "a", divide.a().toPlainString(),
                                        "b", divide.b().toPlainString(),
                                        "result", divide.result().toPlainString())));

        var invalid = problem(responses.get("400").getContent());
        invalid.addExamples(
                "invalid-request-unreadable",
                problem(
                        "The body cannot be read",
                        "invalid-request",
                        "Invalid request",
                        400,
                        "The request body is not valid JSON, or an Operand is not a decimal number."));
        invalid.addExamples(
                "invalid-request-missing-field",
                problem("A field is missing", "invalid-request", "Invalid request", 400, "Field 'b' is required."));
        invalid.addExamples(
                "invalid-request-too-large",
                problem(
                        "The request is too large",
                        "invalid-request",
                        "Invalid request",
                        400,
                        "The request is too large: at most 16,384 bytes in total, and at most 1,000 characters for any"
                                + " value."));
        invalid.addExamples(
                "unknown-operation",
                problem(
                        "The operation is not supported",
                        "unknown-operation",
                        "Unknown operation",
                        400,
                        new UnknownOperationException().getMessage()));
        invalid.addExamples(
                "operand-out-of-range",
                problem(
                        "An Operand is outside BR-1",
                        "operand-out-of-range",
                        "Operand out of range",
                        400,
                        new OperandOutOfRangeException("b").getMessage()));
        problem(responses.get("422").getContent())
                .addExamples(
                        "division-by-zero",
                        problem(
                                "Division by zero (BR-2)",
                                "division-by-zero",
                                "Division by zero",
                                422,
                                new DivisionByZeroException().getMessage()));
    }

    private static io.swagger.v3.oas.models.media.MediaType json(Content content) {
        return content.get(MediaType.APPLICATION_JSON_VALUE);
    }

    private static io.swagger.v3.oas.models.media.MediaType problem(Content content) {
        return content.get(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }

    private static Example problem(String summary, String type, String title, int status, String detail) {
        return new Example()
                .summary(summary)
                .value(Map.of(
                        "type", "/problems/" + type,
                        "title", title,
                        "status", status,
                        "detail", detail,
                        "instance", PATH));
    }
}
