package com.paulrule.mathapi.adapter.in.web;

import com.paulrule.mathapi.domain.Calculation;

/** Decimals as plain-notation strings: Operands keep their given scale (R-5, R-6, BR-4). */
record CalculationResponse(String operation, String a, String b, String result) {

    static CalculationResponse from(Calculation calculation) {
        return new CalculationResponse(
                calculation.operation().name(),
                calculation.a().toPlainString(),
                calculation.b().toPlainString(),
                calculation.result().toPlainString());
    }
}
