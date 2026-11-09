package com.paulrule.mathapi.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** {@code operation} is a String, not the enum, so an unknown name is an unknown operation, not unreadable (R-3, R-4). */
record CalculationRequest(
        @NotNull String operation,
        @NotNull BigDecimal a,
        @NotNull BigDecimal b) {}
