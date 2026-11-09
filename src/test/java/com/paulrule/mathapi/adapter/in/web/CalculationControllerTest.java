package com.paulrule.mathapi.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paulrule.mathapi.application.PerformCalculation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(CalculationController.class)
// The real use case, imported directly: referencing config from adapter code breaks the layer rule (STANDARDS.md §2).
@Import(PerformCalculation.class)
class CalculationControllerTest {

    private static final String PATH = "/api/v1/calculations";
    private static final String INVALID_REQUEST = "/problems/invalid-request";
    private static final String UNKNOWN_OPERATION = "/problems/unknown-operation";
    private static final String DIVISION_BY_ZERO = "/problems/division-by-zero";

    @Autowired
    private MockMvc mvc;

    private ResultActions send(String body) throws Exception {
        return mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void expectResult(String body, String result) throws Exception {
        send(body).andExpect(status().isOk()).andExpect(jsonPath("$.result").value(result));
    }

    private void expectBody(String body, String response) throws Exception {
        send(body).andExpect(status().isOk()).andExpect(content().string(response));
    }

    private ResultActions expectProblem(String body, String type) throws Exception {
        return send(body)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(type));
    }

    private ResultActions expectDivisionByZero(String body) throws Exception {
        return send(body)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(DIVISION_BY_ZERO));
    }

    // BR-3: ADD is exact

    @Test
    void addsDecimalFractionsExactly() throws Exception {
        send("{\"operation\":\"ADD\",\"a\":\"0.1\",\"b\":\"0.2\"}")
                .andExpect(status().isOk())
                .andExpect(content().string("{\"operation\":\"ADD\",\"a\":\"0.1\",\"b\":\"0.2\",\"result\":\"0.3\"}"));
    }

    @Test
    void addsDecimalFractionsGivenAsNumbersExactly() throws Exception {
        expectResult("{\"operation\":\"ADD\",\"a\":0.1,\"b\":0.2}", "0.3");
    }

    @Test
    void addsNegativeOperands() throws Exception {
        expectResult("{\"operation\":\"ADD\",\"a\":\"-1.5\",\"b\":\"-2.25\"}", "-3.75");
    }

    @Test
    void addsWithLargePrecision() throws Exception {
        expectResult(
                "{\"operation\":\"ADD\",\"a\":\"12345678901234567890.123456789012345678901234567890\","
                        + "\"b\":\"0.000000000000000000000000000001\"}",
                "12345678901234567890.123456789012345678901234567891");
    }

    // BR-3: SUBTRACT is exact

    @Test
    void subtractsToZero() throws Exception {
        expectResult("{\"operation\":\"SUBTRACT\",\"a\":\"1.50\",\"b\":\"1.5\"}", "0");
    }

    @Test
    void subtractsToANegativeResult() throws Exception {
        expectResult("{\"operation\":\"SUBTRACT\",\"a\":\"2\",\"b\":\"5.5\"}", "-3.5");
    }

    // BR-3: MULTIPLY is exact

    @Test
    void multipliesByZero() throws Exception {
        expectResult("{\"operation\":\"MULTIPLY\",\"a\":\"123.45\",\"b\":\"0\"}", "0");
    }

    @Test
    void multipliesNegativeByPositive() throws Exception {
        expectResult("{\"operation\":\"MULTIPLY\",\"a\":\"-2\",\"b\":\"3\"}", "-6");
    }

    @Test
    void multipliesNegativeByNegative() throws Exception {
        expectResult("{\"operation\":\"MULTIPLY\",\"a\":\"-2\",\"b\":\"-3\"}", "6");
    }

    @Test
    void multiplyGrowsScale() throws Exception {
        expectResult("{\"operation\":\"MULTIPLY\",\"a\":\"1.25\",\"b\":\"1.25\"}", "1.5625");
    }

    // BR-4: the Result is normalised, in plain notation

    @Test
    void stripsTrailingZeros() throws Exception {
        expectResult("{\"operation\":\"ADD\",\"a\":\"1.10\",\"b\":\"2.20\"}", "3.3");
    }

    @Test
    void presentsALargeResultInPlainNotation() throws Exception {
        expectResult("{\"operation\":\"ADD\",\"a\":\"50\",\"b\":\"50\"}", "100");
    }

    @Test
    void presentsASmallResultInPlainNotation() throws Exception {
        expectResult("{\"operation\":\"MULTIPLY\",\"a\":\"0.001\",\"b\":\"0.001\"}", "0.000001");
    }

    // R-1

    @Test
    void acceptsOperandsAsNumberOrText() throws Exception {
        expectBody(
                "{\"operation\":\"ADD\",\"a\":1.5,\"b\":\"2.5\"}",
                "{\"operation\":\"ADD\",\"a\":\"1.5\",\"b\":\"2.5\",\"result\":\"4\"}");
    }

    // R-5, R-6

    @Test
    void echoesOperandsWithTheirGivenScaleInPlainNotation() throws Exception {
        expectBody(
                "{\"operation\":\"MULTIPLY\",\"a\":1e2,\"b\":\"1.50\"}",
                "{\"operation\":\"MULTIPLY\",\"a\":\"100\",\"b\":\"1.50\",\"result\":\"150\"}");
    }

    @Test
    void presentsDecimalValuesAsJsonStrings() throws Exception {
        send("{\"operation\":\"ADD\",\"a\":1,\"b\":2}")
                .andExpect(status().isOk())
                .andExpect(content().string("{\"operation\":\"ADD\",\"a\":\"1\",\"b\":\"2\",\"result\":\"3\"}"));
    }

    // R-2: invalid request

    @Test
    void rejectsMalformedJson() throws Exception {
        expectProblem("{\"operation\":\"ADD\",\"a\":1,", INVALID_REQUEST);
    }

    @Test
    void rejectsAnEmptyBody() throws Exception {
        expectProblem("", INVALID_REQUEST);
    }

    @Test
    void rejectsAMissingOperation() throws Exception {
        expectProblem("{\"a\":1,\"b\":2}", INVALID_REQUEST)
                .andExpect(jsonPath("$.detail", containsString("operation")));
    }

    @Test
    void rejectsAMissingOperand() throws Exception {
        expectProblem("{\"operation\":\"ADD\",\"b\":2}", INVALID_REQUEST)
                .andExpect(jsonPath("$.detail", containsString("'a'")));
    }

    @Test
    void rejectsANullOperand() throws Exception {
        expectProblem("{\"operation\":\"ADD\",\"a\":1,\"b\":null}", INVALID_REQUEST)
                .andExpect(jsonPath("$.detail", containsString("'b'")));
    }

    @Test
    void rejectsNonNumericText() throws Exception {
        expectProblem("{\"operation\":\"ADD\",\"a\":\"abc\",\"b\":2}", INVALID_REQUEST);
    }

    @Test
    void rejectsABooleanOperand() throws Exception {
        expectProblem("{\"operation\":\"ADD\",\"a\":true,\"b\":2}", INVALID_REQUEST);
    }

    @Test
    void rejectsEmptyText() throws Exception {
        expectProblem("{\"operation\":\"ADD\",\"a\":\"\",\"b\":2}", INVALID_REQUEST);
    }

    // R-3: unknown operation

    @Test
    void rejectsASupportedNameInTheWrongLetterCase() throws Exception {
        expectProblem("{\"operation\":\"add\",\"a\":1,\"b\":2}", UNKNOWN_OPERATION)
                .andExpect(jsonPath("$.detail", containsString("ADD, SUBTRACT, MULTIPLY, DIVIDE")));
    }

    @Test
    void rejectsAnOperationNotYetSupported() throws Exception {
        expectProblem("{\"operation\":\"POWER\",\"a\":1,\"b\":2}", UNKNOWN_OPERATION);
    }

    @Test
    void doesNotEchoTheGivenOperationName() throws Exception {
        expectProblem("{\"operation\":\"CUBE_ROOT\",\"a\":1,\"b\":2}", UNKNOWN_OPERATION)
                .andExpect(jsonPath("$.detail", not(containsString("CUBE_ROOT"))));
    }

    @Test
    void rejectsAnEmptyOperationName() throws Exception {
        expectProblem("{\"operation\":\"\",\"a\":1,\"b\":2}", UNKNOWN_OPERATION);
    }

    // R-4: only the first failure is reported

    @Test
    void reportsANonNumericOperandBeforeAnUnknownOperation() throws Exception {
        expectProblem("{\"operation\":\"add\",\"a\":\"abc\",\"b\":2}", INVALID_REQUEST);
    }

    @Test
    void reportsAMissingOperandBeforeAnUnknownOperation() throws Exception {
        expectProblem("{\"operation\":\"add\",\"b\":2}", INVALID_REQUEST);
    }

    @Test
    void reportsMalformedJsonBeforeAnUnknownOperation() throws Exception {
        expectProblem("{\"operation\":\"add\",\"a\":1,", INVALID_REQUEST);
    }

    // BR-3: DIVIDE is rounded to 34 significant digits, half even; BR-4: then normalised

    @Test
    void dividesOneByThreeToThirtyFourSignificantDigits() throws Exception {
        expectBody(
                "{\"operation\":\"DIVIDE\",\"a\":1,\"b\":\"3\"}",
                "{\"operation\":\"DIVIDE\",\"a\":\"1\",\"b\":\"3\",\"result\":\"0.3333333333333333333333333333333333\"}");
    }

    @Test
    void roundsTheLastDigitOfTwoThirdsUp() throws Exception {
        expectResult("{\"operation\":\"DIVIDE\",\"a\":\"2\",\"b\":\"3\"}", "0.6666666666666666666666666666666667");
    }

    @Test
    void roundsATieDownToTheEvenDigit() throws Exception {
        expectResult(
                "{\"operation\":\"DIVIDE\",\"a\":\"12345678901234567890123456789012345\",\"b\":\"10\"}",
                "1234567890123456789012345678901234");
    }

    @Test
    void roundsATieUpToTheEvenDigit() throws Exception {
        expectResult(
                "{\"operation\":\"DIVIDE\",\"a\":\"12345678901234567890123456789012335\",\"b\":\"10\"}",
                "1234567890123456789012345678901234");
    }

    @Test
    void dividesExactlyWithoutPadding() throws Exception {
        expectBody(
                "{\"operation\":\"DIVIDE\",\"a\":\"1\",\"b\":\"4\"}",
                "{\"operation\":\"DIVIDE\",\"a\":\"1\",\"b\":\"4\",\"result\":\"0.25\"}");
    }

    @Test
    void dividesByANegativeDivisor() throws Exception {
        expectResult("{\"operation\":\"DIVIDE\",\"a\":\"1\",\"b\":\"-4\"}", "-0.25");
    }

    @Test
    void dividesZeroToZero() throws Exception {
        expectBody(
                "{\"operation\":\"DIVIDE\",\"a\":\"0.00\",\"b\":\"-3\"}",
                "{\"operation\":\"DIVIDE\",\"a\":\"0.00\",\"b\":\"-3\",\"result\":\"0\"}");
    }

    @Test
    void normalisesAWholeQuotient() throws Exception {
        expectBody(
                "{\"operation\":\"DIVIDE\",\"a\":\"1.50\",\"b\":\"0.5\"}",
                "{\"operation\":\"DIVIDE\",\"a\":\"1.50\",\"b\":\"0.5\",\"result\":\"3\"}");
    }

    @Test
    void presentsALargeQuotientInPlainNotation() throws Exception {
        expectResult("{\"operation\":\"DIVIDE\",\"a\":\"100\",\"b\":\"0.01\"}", "10000");
    }

    // BR-2: division by zero is rejected, in every zero form, as a number or as text

    @Test
    void rejectsAZeroDivisorAsText() throws Exception {
        expectDivisionByZero("{\"operation\":\"DIVIDE\",\"a\":\"1\",\"b\":\"0\"}")
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void rejectsAZeroDivisorAsANumber() throws Exception {
        expectDivisionByZero("{\"operation\":\"DIVIDE\",\"a\":1,\"b\":0}");
    }

    @Test
    void rejectsAScaledZeroDivisorAsText() throws Exception {
        expectDivisionByZero("{\"operation\":\"DIVIDE\",\"a\":\"1\",\"b\":\"0.00\"}");
    }

    @Test
    void rejectsAScaledZeroDivisorAsANumber() throws Exception {
        expectDivisionByZero("{\"operation\":\"DIVIDE\",\"a\":1,\"b\":0.00}");
    }

    @Test
    void rejectsANegativeZeroDivisorAsText() throws Exception {
        expectDivisionByZero("{\"operation\":\"DIVIDE\",\"a\":\"1\",\"b\":\"-0\"}");
    }

    @Test
    void rejectsANegativeZeroDivisorAsANumber() throws Exception {
        expectDivisionByZero("{\"operation\":\"DIVIDE\",\"a\":1,\"b\":-0}");
    }

    @Test
    void rejectsZeroDividedByZero() throws Exception {
        expectDivisionByZero("{\"operation\":\"DIVIDE\",\"a\":\"0\",\"b\":\"0\"}");
    }

    @Test
    void divisionByZeroReasonEchoesNoOperandAndNoInternals() throws Exception {
        // UC-003 R-2, UC-002 R-7
        expectDivisionByZero("{\"operation\":\"DIVIDE\",\"a\":\"98765\",\"b\":\"0.00\"}")
                .andExpect(jsonPath("$.detail", not(containsString("98765"))))
                .andExpect(jsonPath("$.detail", not(containsString("0.00"))))
                .andExpect(jsonPath("$.detail", not(containsString("Exception"))))
                .andExpect(jsonPath("$.detail", not(containsString("java."))))
                .andExpect(jsonPath("$.detail", not(containsString("org.springframework"))))
                .andExpect(jsonPath("$.detail", not(containsString("com.paulrule"))));
    }

    // UC-003 R-1: only the first failure is reported

    @Test
    void reportsANonNumericDividendBeforeDivisionByZero() throws Exception {
        expectProblem("{\"operation\":\"DIVIDE\",\"a\":\"abc\",\"b\":0}", INVALID_REQUEST);
    }

    @Test
    void reportsAMissingDividendBeforeDivisionByZero() throws Exception {
        expectProblem("{\"operation\":\"DIVIDE\",\"b\":0}", INVALID_REQUEST);
    }

    @Test
    void reportsAnUnknownOperationBeforeDivisionByZero() throws Exception {
        expectProblem("{\"operation\":\"divide\",\"a\":1,\"b\":0}", UNKNOWN_OPERATION);
    }

    // R-7: every rejection is Problem Details without internals

    @Test
    void rejectionDetailExposesNoInternals() throws Exception {
        expectProblem("{\"operation\":\"ADD\",\"a\":\"abc\",\"b\":2}", INVALID_REQUEST)
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.detail", not(containsString("Exception"))))
                .andExpect(jsonPath("$.detail", not(containsString("java."))))
                .andExpect(jsonPath("$.detail", not(containsString("tools.jackson"))))
                .andExpect(jsonPath("$.detail", not(containsString("com.fasterxml"))))
                .andExpect(jsonPath("$.detail", not(containsString("org.springframework"))))
                .andExpect(jsonPath("$.detail", not(containsString("com.paulrule"))));
    }

    @Test
    void rejectsAnUnsupportedMediaType() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.TEXT_PLAIN).content("ADD 1 2"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void rejectsAnUnsupportedMethod() throws Exception {
        mvc.perform(get(PATH))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Method Not Allowed"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void rejectsAnUnknownPath() throws Exception {
        mvc.perform(post("/api/v1/unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").exists());
    }
}
