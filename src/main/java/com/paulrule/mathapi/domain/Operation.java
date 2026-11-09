package com.paulrule.mathapi.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.BinaryOperator;

public enum Operation {
    ADD(BigDecimal::add),
    SUBTRACT(BigDecimal::subtract),
    MULTIPLY(BigDecimal::multiply),
    DIVIDE((a, b) -> {
        // BR-2, checked before divide throws a bare ArithmeticException; signum catches 0, 0.00 and -0
        if (b.signum() == 0) {
            throw new DivisionByZeroException();
        }
        return a.divide(b, MathContext.DECIMAL128); // BR-3
    });

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
