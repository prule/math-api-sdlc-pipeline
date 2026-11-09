package com.paulrule.mathapi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paulrule.mathapi.domain.Operation;
import com.paulrule.mathapi.domain.UnknownOperationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PerformCalculationTest {

    private final PerformCalculation performCalculation = new PerformCalculation();

    @Test
    void performsTheNamedOperation() {
        var calculation = performCalculation.perform("ADD", new BigDecimal("0.1"), new BigDecimal("0.2"));

        assertThat(calculation.operation()).isEqualTo(Operation.ADD);
        assertThat(calculation.a()).isEqualTo(new BigDecimal("0.1"));
        assertThat(calculation.b()).isEqualTo(new BigDecimal("0.2"));
        assertThat(calculation.result()).isEqualTo(new BigDecimal("0.3"));
    }

    @Test
    void rejectsAnUnknownOperationName() {
        // R-3
        assertThatThrownBy(() -> performCalculation.perform("add", new BigDecimal("1"), new BigDecimal("2")))
                .isInstanceOf(UnknownOperationException.class);
    }
}
