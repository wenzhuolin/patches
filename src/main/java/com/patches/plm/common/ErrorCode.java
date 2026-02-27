package com.patches.plm.common;

public final class ErrorCode {
    private ErrorCode() {
    }

    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String INVALID_STATE_TRANSITION = "INVALID_STATE_TRANSITION";
    public static final String KPI_GATE_FAILED = "KPI_GATE_FAILED";
    public static final String QA_GATE_FAILED = "QA_GATE_FAILED";
    public static final String QA_REJECTED = "QA_REJECTED";
    public static final String DUPLICATE_REQUEST = "DUPLICATE_REQUEST";
}
