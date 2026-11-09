package com.paulrule.mathapi.domain;

import java.util.Arrays;
import java.util.stream.Collectors;

/** The given name is never echoed (UC-002 R-3). */
public class UnknownOperationException extends RuntimeException {

    public UnknownOperationException() {
        super("The operation is not supported. Supported operations: "
                + Arrays.stream(Operation.values()).map(Enum::name).collect(Collectors.joining(", "))
                + ".");
    }
}
