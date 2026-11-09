package com.paulrule.mathapi.adapter.in.web;

import com.paulrule.mathapi.application.PerformCalculation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CalculationController {

    private final PerformCalculation performCalculation;

    CalculationController(PerformCalculation performCalculation) {
        this.performCalculation = performCalculation;
    }

    @PostMapping("/api/v1/calculations")
    @ApiResponse(
            responseCode = "200",
            description = "The Result of the Calculation",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CalculationResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request, unknown operation or Operand out of range",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "422",
            description = "Division by zero",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    CalculationResponse calculate(@Valid @RequestBody CalculationRequest request) {
        return CalculationResponse.from(performCalculation.perform(request.operation(), request.a(), request.b()));
    }
}
