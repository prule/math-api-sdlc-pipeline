package com.paulrule.mathapi.domain;

import java.util.Arrays;
import java.util.stream.Collectors;

/** The given name is deliberately not carried: it is unbounded until B-4 (R-7). */
public class UnknownOperationException extends RuntimeException {

    public UnknownOperationException() {
        super("The operation is not supported. Supported operations: "
                + Arrays.stream(Operation.values()).map(Enum::name).collect(Collectors.joining(", "))
                + ".");
    }
}
