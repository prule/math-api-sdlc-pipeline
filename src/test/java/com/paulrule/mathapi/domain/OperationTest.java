package com.paulrule.mathapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class OperationTest {

    @Test
    void findsEachSupportedOperationByItsExactName() {
        assertThat(Operation.named("ADD")).isEqualTo(Operation.ADD);
        assertThat(Operation.named("SUBTRACT")).isEqualTo(Operation.SUBTRACT);
        assertThat(Operation.named("MULTIPLY")).isEqualTo(Operation.MULTIPLY);
    }

    @Test
    void rejectsASupportedNameInTheWrongLetterCase() {
        // R-3: names are case-sensitive
        assertThatThrownBy(() -> Operation.named("add")).isInstanceOf(UnknownOperationException.class);
    }

    @Test
    void rejectsAnUnsupportedName() {
        assertThatThrownBy(() -> Operation.named("DIVIDE")).isInstanceOf(UnknownOperationException.class);
        assertThatThrownBy(() -> Operation.named("")).isInstanceOf(UnknownOperationException.class);
        assertThatThrownBy(() -> Operation.named(null)).isInstanceOf(UnknownOperationException.class);
    }

    @Test
    void unknownOperationReasonListsSupportedOperationsInOrder() {
        // R-3: the reason lists the supported Operations in order
        var supported = String.join(
                ", ", Arrays.stream(Operation.values()).map(Enum::name).toList());

        assertThat(new UnknownOperationException().getMessage()).contains(supported);
    }
}
