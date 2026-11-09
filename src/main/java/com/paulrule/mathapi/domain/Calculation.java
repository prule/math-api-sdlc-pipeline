package com.paulrule.mathapi.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Calculation(Operation operation, BigDecimal a, BigDecimal b) {

    public Calculation {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
    }

    /** Rounded only where BR-3 says, and normalised (BR-4); a zero Result strips to {@code BigDecimal.ZERO}. */
    public BigDecimal result() {
        return operation.apply(a, b).stripTrailingZeros();
    }
}
