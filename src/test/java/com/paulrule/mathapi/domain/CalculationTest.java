package com.paulrule.mathapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculationTest {

    private static BigDecimal result(Operation operation, String a, String b) {
        return new Calculation(operation, new BigDecimal(a), new BigDecimal(b)).result();
    }

    // BR-3: ADD is exact

    @Test
    void addsDecimalFractionsExactly() {
        assertThat(result(Operation.ADD, "0.1", "0.2")).isEqualTo(new BigDecimal("0.3"));
    }

    @Test
    void addsNegativeOperands() {
        assertThat(result(Operation.ADD, "-1.5", "-2.25")).isEqualTo(new BigDecimal("-3.75"));
    }

    @Test
    void addsWithLargePrecision() {
        assertThat(result(
                        Operation.ADD,
                        "12345678901234567890.123456789012345678901234567890",
                        "0.000000000000000000000000000001"))
                .isEqualTo(new BigDecimal("12345678901234567890.123456789012345678901234567891"));
    }

    // BR-3: SUBTRACT is exact

    @Test
    void subtractsToZero() {
        assertThat(result(Operation.SUBTRACT, "1.50", "1.5")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void subtractsToANegativeResult() {
        assertThat(result(Operation.SUBTRACT, "2", "5.5")).isEqualTo(new BigDecimal("-3.5"));
    }

    // BR-3: MULTIPLY is exact

    @Test
    void multipliesByZero() {
        assertThat(result(Operation.MULTIPLY, "123.45", "0")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void multipliesNegativeByPositive() {
        assertThat(result(Operation.MULTIPLY, "-2", "3")).isEqualTo(new BigDecimal("-6"));
    }

    @Test
    void multipliesNegativeByNegative() {
        assertThat(result(Operation.MULTIPLY, "-2", "-3")).isEqualTo(new BigDecimal("6"));
    }

    @Test
    void multiplyGrowsScale() {
        assertThat(result(Operation.MULTIPLY, "1.25", "1.25")).isEqualTo(new BigDecimal("1.5625"));
    }

    // BR-4: the Result is normalised

    @Test
    void stripsTrailingZerosFromTheResult() {
        assertThat(result(Operation.ADD, "1.10", "2.20")).isEqualTo(new BigDecimal("3.3"));
    }

    @Test
    void normalisesAWholeResult() {
        assertThat(result(Operation.ADD, "50", "50")).isEqualTo(new BigDecimal("1E+2"));
    }

    @Test
    void rejectsMissingParts() {
        var one = new BigDecimal("1");

        assertThatThrownBy(() -> new Calculation(null, one, one)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Calculation(Operation.ADD, null, one)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Calculation(Operation.ADD, one, null)).isInstanceOf(NullPointerException.class);
    }
}
