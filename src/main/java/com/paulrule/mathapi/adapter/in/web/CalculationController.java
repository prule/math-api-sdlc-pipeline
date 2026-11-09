package com.paulrule.mathapi.adapter.in.web;

import com.paulrule.mathapi.application.PerformCalculation;
import jakarta.validation.Valid;
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
    CalculationResponse calculate(@Valid @RequestBody CalculationRequest request) {
        return CalculationResponse.from(performCalculation.perform(request.operation(), request.a(), request.b()));
    }
}
