package com.paulrule.mathapi.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Calculation(Operation operation, BigDecimal a, BigDecimal b) {

    // BR-1
    public static final int MAX_SIGNIFICANT_DIGITS = 100;
    public static final int MAX_SCALE = 1000;

    public Calculation {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        checkWithinLimits("a", a);
        checkWithinLimits("b", b);
    }

    // Both bounds compared directly: Math.abs(Integer.MIN_VALUE) is negative.
    private static void checkWithinLimits(String name, BigDecimal value) {
        if (value.precision() > MAX_SIGNIFICANT_DIGITS || value.scale() < -MAX_SCALE || value.scale() > MAX_SCALE) {
            throw new OperandOutOfRangeException(name);
        }
    }

    /** Rounded only where BR-3 says, and normalised (BR-4); a zero Result strips to {@code BigDecimal.ZERO}. */
    public BigDecimal result() {
        return operation.apply(a, b).stripTrailingZeros();
    }
}
