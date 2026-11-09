package com.paulrule.mathapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    // BR-3: DIVIDE is rounded to 34 significant digits, half even; BR-4: then normalised

    @Test
    void dividesOneByThreeToThirtyFourSignificantDigits() {
        assertThat(result(Operation.DIVIDE, "1", "3"))
                .isEqualTo(new BigDecimal("0.3333333333333333333333333333333333"));
    }

    @Test
    void roundsTheLastDigitOfTwoThirdsUp() {
        assertThat(result(Operation.DIVIDE, "2", "3"))
                .isEqualTo(new BigDecimal("0.6666666666666666666666666666666667"));
    }

    @Test
    void roundsATieDownToTheEvenDigit() {
        assertThat(result(Operation.DIVIDE, "12345678901234567890123456789012345", "10"))
                .isEqualTo(new BigDecimal("1234567890123456789012345678901234"));
    }

    @Test
    void roundsATieUpToTheEvenDigit() {
        assertThat(result(Operation.DIVIDE, "12345678901234567890123456789012335", "10"))
                .isEqualTo(new BigDecimal("1234567890123456789012345678901234"));
    }

    @Test
    void dividesExactlyWithoutPadding() {
        assertThat(result(Operation.DIVIDE, "1", "4")).isEqualTo(new BigDecimal("0.25"));
    }

    @Test
    void dividesByANegativeDivisor() {
        assertThat(result(Operation.DIVIDE, "1", "-4")).isEqualTo(new BigDecimal("-0.25"));
    }

    @Test
    void dividesZeroToZero() {
        assertThat(result(Operation.DIVIDE, "0.00", "-3")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void normalisesAWholeQuotient() {
        assertThat(result(Operation.DIVIDE, "1.50", "0.5")).isEqualTo(new BigDecimal("3"));
    }

    @Test
    void normalisesALargeQuotient() {
        assertThat(result(Operation.DIVIDE, "100", "0.01")).isEqualTo(new BigDecimal("1E+4"));
    }

    // BR-2: division by zero is rejected

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-0"})
    void rejectsDivisionByZeroInEveryZeroForm(String zero) {
        assertThatThrownBy(() -> result(Operation.DIVIDE, "1", zero)).isInstanceOf(DivisionByZeroException.class);
    }

    @Test
    void rejectsZeroDividedByZero() {
        assertThatThrownBy(() -> result(Operation.DIVIDE, "0", "0")).isExactlyInstanceOf(DivisionByZeroException.class);
    }

    @Test
    void divisionByZeroReasonEchoesNoOperand() {
        // UC-003 R-2
        assertThatThrownBy(() -> result(Operation.DIVIDE, "98765", "0.00"))
                .isInstanceOf(DivisionByZeroException.class)
                .message()
                .doesNotContain("98765")
                .doesNotContain("0.00");
    }

    private static Calculation calculation(Operation operation, String a, String b) {
        return new Calculation(operation, new BigDecimal(a), new BigDecimal(b));
    }

    // BR-1: an Operand, as written, has at most 100 significant digits and a scale from -1000 to 1000

    @Test
    void acceptsAScaleOfMinusOneThousand() {
        assertThat(calculation(Operation.ADD, "1E+1000", "0").a()).isEqualTo(new BigDecimal("1E+1000"));
    }

    @Test
    void acceptsAScaleOfOneThousand() {
        assertThat(calculation(Operation.ADD, "1E-1000", "0").a()).isEqualTo(new BigDecimal("1E-1000"));
    }

    @Test
    void acceptsOneHundredSignificantDigits() {
        var digits = "1".repeat(100);
        assertThat(calculation(Operation.MULTIPLY, digits, "1").a()).isEqualTo(new BigDecimal(digits));
    }

    @Test
    void measuresAnOperandAsWritten() {
        assertThat(calculation(Operation.MULTIPLY, "1E+100", "1").a()).isEqualTo(new BigDecimal("1E+100"));
    }

    @Test
    void rejectsAScaleOfMinusOneThousandAndOne() {
        assertThatThrownBy(() -> calculation(Operation.ADD, "1E+1001", "1"))
                .isInstanceOf(OperandOutOfRangeException.class)
                .hasMessageContaining("Operand a");
    }

    @Test
    void rejectsAScaleOfOneThousandAndOneInB() {
        assertThatThrownBy(() -> calculation(Operation.ADD, "1", "1E-1001"))
                .isInstanceOf(OperandOutOfRangeException.class)
                .hasMessageContaining("Operand b");
    }

    @Test
    void rejectsOneHundredAndOneSignificantDigitsEvenForASmallValue() {
        assertThatThrownBy(() -> calculation(Operation.ADD, "1" + "0".repeat(100), "1"))
                .isInstanceOf(OperandOutOfRangeException.class);
    }

    @Test
    void rejectsAShortButEnormousOperandAtOnce() {
        // BR-1, UC-004 R-4
        assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> assertThatThrownBy(() -> calculation(Operation.MULTIPLY, "1e999999999", "1e999999999"))
                        .isInstanceOf(OperandOutOfRangeException.class));
    }

    @Test
    void rejectsEnormousScalesInADivisionBeforeAnyArithmetic() {
        assertThatThrownBy(() -> calculation(Operation.DIVIDE, "1E-999999999", "1E+999999999"))
                .isInstanceOf(OperandOutOfRangeException.class);
    }

    @Test
    void checksOperandABeforeOperandB() {
        // UC-004 R-3
        assertThatThrownBy(() -> calculation(Operation.ADD, "1E+1001", "1E-1001"))
                .isInstanceOf(OperandOutOfRangeException.class)
                .message()
                .contains("Operand a")
                .doesNotContain("Operand b");
    }

    @Test
    void reportsOperandOutOfRangeBeforeDivisionByZero() {
        // UC-004 R-3: thrown by the constructor, so result() and BR-2 are never reached
        assertThatThrownBy(() -> calculation(Operation.DIVIDE, "1E+1001", "0"))
                .isInstanceOf(OperandOutOfRangeException.class);
    }

    @Test
    void operandOutOfRangeReasonEchoesNoValue() {
        // UC-004 R-2
        assertThatThrownBy(() -> calculation(Operation.ADD, "1", "98765E+1001"))
                .isInstanceOf(OperandOutOfRangeException.class)
                .message()
                .contains("Operand b")
                .doesNotContain("98765")
                .doesNotContain("98765E+1001")
                // the value's own rendering, 9.8765E+1005
                .doesNotContain("8765");
    }

    @Test
    void rejectsTheMostNegativeScale() {
        // BR-1: scale Integer.MIN_VALUE; the domain must not rely on the adapter to refuse it
        assertThatThrownBy(() -> calculation(Operation.ADD, "1e2147483648", "1"))
                .isInstanceOf(OperandOutOfRangeException.class)
                .hasMessageContaining("Operand a");
    }

    @Test
    void rejectsMissingParts() {
        var one = new BigDecimal("1");

        assertThatThrownBy(() -> new Calculation(null, one, one)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Calculation(Operation.ADD, null, one)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Calculation(Operation.ADD, one, null)).isInstanceOf(NullPointerException.class);
    }
}
