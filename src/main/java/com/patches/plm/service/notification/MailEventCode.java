package com.patches.plm.service.notification;

public final class MailEventCode {
    private MailEventCode() {
    }

    public static final String PATCH_CREATED = "PATCH_CREATED";
    public static final String PATCH_SUBMIT_REVIEW = "PATCH_SUBMIT_REVIEW";
    public static final String PATCH_TRANSFER_TO_TEST = "PATCH_TRANSFER_TO_TEST";
    public static final String PATCH_RELEASE = "PATCH_RELEASE";
    public static final String PATCH_ARCHIVE = "PATCH_ARCHIVE";
    public static final String PATCH_REVIEW_APPROVED = "PATCH_REVIEW_APPROVED";
    public static final String KPI_GATE_FAILED = "KPI_GATE_FAILED";
    public static final String QA_GATE_BLOCKED = "QA_GATE_BLOCKED";
}
