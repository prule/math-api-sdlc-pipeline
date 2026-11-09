package com.paulrule.mathapi.application;

import com.paulrule.mathapi.domain.Calculation;
import com.paulrule.mathapi.domain.Operation;
import java.math.BigDecimal;

/** Performs a Calculation; later rules (BR-1, ...) are sequenced here in PRD §3.2 order. */
public class PerformCalculation {

    public Calculation perform(String operationName, BigDecimal a, BigDecimal b) {
        return new Calculation(Operation.named(operationName), a, b);
    }
}
