package com.paulrule.mathapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paulrule.mathapi.domain.Calculation;
import com.paulrule.mathapi.domain.Operation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** The published OpenAPI documentation (UC-005): every documented example is checked against the live service (R-5). */
@SpringBootTest
@AutoConfigureMockMvc
class ApiDocumentationTest {

    private static final String OPERATION = "/paths/~1api~1v1~1calculations/post";
    private static final String REQUEST_SCHEMA = "/components/schemas/CalculationRequest/properties";

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    private JsonNode docs() throws Exception {
        var body = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return mapper.readTree(body);
    }

    private ResultActions send(String body) throws Exception {
        return mvc.perform(post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private JsonNode example(String status, String mediaType, String name) throws Exception {
        return docs().at(OPERATION + "/responses/" + status + "/content/" + mediaType.replace("/", "~1") + "/examples/"
                + name + "/value");
    }

    private JsonNode problemExample(String name) throws Exception {
        return example("400", MediaType.APPLICATION_PROBLEM_JSON_VALUE, name);
    }

    private static Set<String> names(JsonNode node) {
        return new HashSet<>(node.propertyNames());
    }

    private static Set<String> texts(JsonNode array) {
        var result = new HashSet<String>();
        array.values().forEach(value -> result.add(value.asString()));
        return result;
    }

    @Test
    void publishesMachineReadableDocumentationWithoutCredentials() throws Exception {
        var body = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        var docs = mapper.readTree(body);

        assertThat(docs.at("/openapi").asString()).isEqualTo("3.1.0");
        assertThat(docs.at("/info/title").asString()).isEqualTo("Math API");
        assertThat(docs.at("/info/version").asString()).isEqualTo("v1");
    }

    @Test
    void publishesInteractivePagesWithoutCredentials() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void interactivePagesLoadTheServicesOwnDocumentation() throws Exception {
        var config = body(mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk()));

        assertThat(config.at("/url").asString()).isEqualTo("/v3/api-docs");
    }

    @Test
    void calculationsTriedFromThePagesGoToTheServiceItself() throws Exception {
        // The MockMvc request origin; task 4.1 checks a running app on a real port.
        assertThat(docs().at("/servers/0/url").asString()).isEqualTo("http://localhost");
    }

    @Test
    void documentsOnlyTheCalculationService() throws Exception {
        var paths = docs().at("/paths");

        assertThat(paths.propertyNames()).containsExactly("/api/v1/calculations");
        assertThat(paths.at("/~1api~1v1~1calculations").propertyNames()).containsExactly("post");
        assertThat(paths.propertyNames()).noneMatch(name -> name.startsWith("/actuator"));
    }

    @Test
    void documentsExactlyTheDomainOperationsInOrder() throws Exception {
        var docs = docs();
        var documented = docs.at(REQUEST_SCHEMA + "/operation/enum").values().stream()
                .map(JsonNode::asString)
                .toList();

        // R-2: generated from the domain, in its order
        assertThat(documented)
                .isEqualTo(Arrays.stream(Operation.values()).map(Enum::name).toList())
                .isEqualTo(List.of("ADD", "SUBTRACT", "MULTIPLY", "DIVIDE"));
        assertThat(texts(docs.at("/components/schemas/CalculationRequest/required")))
                .isEqualTo(Set.of("operation", "a", "b"));
    }

    @Test
    void documentsOperandsAsNumbersOrTextWithinTheirLimits() throws Exception {
        var docs = docs();
        for (var operand : List.of("a", "b")) {
            var property = docs.at(REQUEST_SCHEMA + "/" + operand);
            var description = property.at("/description").asString();

            assertThat(texts(property.at("/type"))).isEqualTo(Set.of("number", "string"));
            // BR-1, R-4
            assertThat(description)
                    .contains("at most " + Calculation.MAX_SIGNIFICANT_DIGITS + " significant digits")
                    .contains("a scale from -" + Calculation.MAX_SCALE + " to " + Calculation.MAX_SCALE)
                    .contains("1,000 characters")
                    .contains("at most 100 significant digits")
                    .contains("a scale from -1000 to 1000");
        }
    }

    @Test
    void documentedResultMatchesARealCalculation() throws Exception {
        var live = body(send("{\"operation\":\"DIVIDE\",\"a\":1,\"b\":\"3\"}").andExpect(status().isOk()));
        var docs = docs();
        var documented = example("200", MediaType.APPLICATION_JSON_VALUE, "divide");

        assertThat(live).isEqualTo(documented);
        assertThat(documented.at("/result").asString()).isEqualTo("0.3333333333333333333333333333333333");
        assertThat(docs.at(OPERATION + "/requestBody/content/application~1json/examples/divide/value"))
                .isEqualTo(mapper.readTree("{\"operation\":\"DIVIDE\",\"a\":1,\"b\":\"3\"}"));
        assertThat(docs.at(OPERATION + "/responses/200/content").propertyNames())
                .containsExactly("application/json");
        var properties = docs.at("/components/schemas/CalculationResponse/properties");
        assertThat(properties.propertyNames()).isNotEmpty();
        properties
                .values()
                .forEach(property -> assertThat(property.at("/type").asString()).isEqualTo("string"));
    }

    @Test
    void documentsTheUnreadableRequestRejection() throws Exception {
        var documented = problemExample("invalid-request-unreadable");

        for (var request : List.of(
                "{\"operation\":\"ADD\",\"a\":\"x\",\"b\":2}",
                "{\"operation\":\"ADD\",\"a\":\"1e2147483648\",\"b\":1}")) {
            assertThat(body(send(request).andExpect(status().isBadRequest()))).isEqualTo(documented);
        }
        assertThat(documented.at("/type").asString()).isEqualTo("/problems/invalid-request");
    }

    @Test
    void documentsTheMissingFieldRejection() throws Exception {
        var documented = problemExample("invalid-request-missing-field");

        assertThat(body(send("{\"operation\":\"ADD\",\"a\":1}").andExpect(status().isBadRequest())))
                .isEqualTo(documented);
        assertThat(documented.at("/type").asString()).isEqualTo("/problems/invalid-request");
        assertThat(documented.at("/detail").asString()).isEqualTo("Field 'b' is required.");
    }

    @Test
    void documentsTheTooLargeRequestRejection() throws Exception {
        var documented = problemExample("invalid-request-too-large");
        var request = "{\"operation\":\"ADD\",\"a\":\"" + "1".repeat(1001) + "\",\"b\":2}";

        assertThat(body(send(request).andExpect(status().isBadRequest()))).isEqualTo(documented);
        assertThat(documented.at("/type").asString()).isEqualTo("/problems/invalid-request");
        assertThat(documented.at("/detail").asString()).contains("16,384 bytes").contains("1,000 characters");
    }

    @Test
    void documentsTheUnknownOperationRejection() throws Exception {
        var documented = problemExample("unknown-operation");

        assertThat(body(send("{\"operation\":\"add\",\"a\":1,\"b\":2}").andExpect(status().isBadRequest())))
                .isEqualTo(documented);
        assertThat(documented.at("/type").asString()).isEqualTo("/problems/unknown-operation");
        assertThat(documented.at("/detail").asString()).contains("ADD, SUBTRACT, MULTIPLY, DIVIDE");
    }

    @Test
    void documentsTheOperandOutOfRangeRejection() throws Exception {
        var documented = problemExample("operand-out-of-range");

        // BR-1: the Operand is named, its value never echoed
        assertThat(body(send("{\"operation\":\"ADD\",\"a\":\"1\",\"b\":\"1E+1001\"}")
                        .andExpect(status().isBadRequest())))
                .isEqualTo(documented);
        assertThat(documented.at("/type").asString()).isEqualTo("/problems/operand-out-of-range");
        assertThat(documented.at("/detail").asString()).contains("Operand b").doesNotContain("1E+1001");
    }

    @Test
    void documentsTheDivisionByZeroRejection() throws Exception {
        var documented = example("422", MediaType.APPLICATION_PROBLEM_JSON_VALUE, "division-by-zero");

        // BR-2
        assertThat(body(send("{\"operation\":\"DIVIDE\",\"a\":1,\"b\":0}")
                        .andExpect(status().isUnprocessableContent())))
                .isEqualTo(documented);
        assertThat(documented.at("/type").asString()).isEqualTo("/problems/division-by-zero");
    }

    @Test
    void documentsExactlyTheRejectionStatusesAndExamples() throws Exception {
        var responses = docs().at(OPERATION + "/responses");
        var problem = "/content/application~1problem+json/examples";

        assertThat(names(responses)).isEqualTo(Set.of("200", "400", "422"));
        assertThat(names(responses.at("/400" + problem)))
                .isEqualTo(Set.of(
                        "invalid-request-unreadable",
                        "invalid-request-missing-field",
                        "invalid-request-too-large",
                        "unknown-operation",
                        "operand-out-of-range"));
        assertThat(names(responses.at("/422" + problem))).isEqualTo(Set.of("division-by-zero"));
    }

    @Test
    void everyDocumentedResponseNamesItsMediaType() throws Exception {
        var paths = docs().at("/paths");
        var checked = 0;
        for (var path : paths.values()) {
            for (var method : path.values()) {
                for (var response : method.at("/responses").properties()) {
                    var expected = response.getKey().equals("200")
                            ? MediaType.APPLICATION_JSON_VALUE
                            : MediaType.APPLICATION_PROBLEM_JSON_VALUE;
                    assertThat(response.getValue().at("/content").propertyNames())
                            .as("response %s", response.getKey())
                            .containsExactly(expected)
                            .doesNotContain("*/*");
                    checked++;
                }
            }
        }
        assertThat(checked).isEqualTo(3);
    }

    @Test
    void problemDetailSchemaMatchesTheResponses() throws Exception {
        assertThat(names(docs().at("/components/schemas/ProblemDetail/properties")))
                .isEqualTo(Set.of("type", "title", "status", "detail", "instance"));
    }

    @Test
    void apiDescriptionStatesTheLimitsAndOtherErrors() throws Exception {
        // BR-1, R-4
        assertThat(docs().at("/info/description").asString())
                .contains("at most 100 significant digits")
                .contains("a scale from -1000 to 1000")
                .contains("16,384 bytes")
                .contains("1,000 characters")
                .contains("about:blank");
    }
}
