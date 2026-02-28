package com.patches.plm.domain.enums;

public enum CompareOp {
    GT,
    GE,
    LT,
    LE,
    EQ,
    BETWEEN;

    public boolean compare(double value, Double threshold1, Double threshold2) {
        return switch (this) {
            case GT -> value > threshold1;
            case GE -> value >= threshold1;
            case LT -> value < threshold1;
            case LE -> value <= threshold1;
            case EQ -> Double.compare(value, threshold1) == 0;
            case BETWEEN -> threshold1 != null && threshold2 != null && value >= threshold1 && value <= threshold2;
        };
    }
}
