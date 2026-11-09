package com.paulrule.mathapi.domain;

/** BR-1. Names the Operand but carries no value (UC-004 R-2). */
public class OperandOutOfRangeException extends RuntimeException {

    public OperandOutOfRangeException(String operand) {
        super(
                "Operand " + operand
                        + " is out of range: an Operand may have at most 100 significant digits and a scale from -1000 to 1000.");
    }
}
