package com.paulrule.mathapi.domain;

/** BR-2. Carries no Operand (UC-003 R-2). */
public class DivisionByZeroException extends RuntimeException {

    public DivisionByZeroException() {
        super("Division by zero is not allowed: the divisor b is zero.");
    }
}
