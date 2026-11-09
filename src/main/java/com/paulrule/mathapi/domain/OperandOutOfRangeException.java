package com.paulrule.mathapi.domain;

/** BR-1. Names the Operand but carries no value (UC-004 R-2). */
public class OperandOutOfRangeException extends RuntimeException {

    public OperandOutOfRangeException(String operand) {
        super("Operand " + operand + " is out of range: an Operand may have at most "
                + Calculation.MAX_SIGNIFICANT_DIGITS + " significant digits and a scale from -" + Calculation.MAX_SCALE
                + " to " + Calculation.MAX_SCALE + ".");
    }
}
