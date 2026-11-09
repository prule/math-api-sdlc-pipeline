package com.paulrule.mathapi.domain;

import java.math.BigDecimal;
import java.util.function.BinaryOperator;

public enum Operation {
    ADD(BigDecimal::add),
    SUBTRACT(BigDecimal::subtract),
    MULTIPLY(BigDecimal::multiply);

    private final BinaryOperator<BigDecimal> function;

    Operation(BinaryOperator<BigDecimal> function) {
        this.function = function;
    }

    public BigDecimal apply(BigDecimal a, BigDecimal b) {
        return function.apply(a, b);
    }

    /** Exact, case-sensitive lookup (R-3); not {@code valueOf}, whose failures leak internal messages. */
    public static Operation named(String name) {
        for (Operation operation : values()) {
            if (operation.name().equals(name)) {
                return operation;
            }
        }
        throw new UnknownOperationException();
    }
}
