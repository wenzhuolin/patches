package com.patches.plm.domain.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CompareOpTest {

    @Test
    void shouldCompareByOperator() {
        Assertions.assertTrue(CompareOp.GE.compare(90, 90.0, null));
        Assertions.assertFalse(CompareOp.GT.compare(90, 90.0, null));
        Assertions.assertTrue(CompareOp.LE.compare(90, 90.0, null));
        Assertions.assertTrue(CompareOp.BETWEEN.compare(90, 80.0, 100.0));
        Assertions.assertFalse(CompareOp.BETWEEN.compare(70, 80.0, 100.0));
    }
}
