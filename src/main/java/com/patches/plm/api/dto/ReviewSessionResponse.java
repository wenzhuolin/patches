package com.patches.plm.api.dto;

public record ReviewSessionResponse(
        Long sessionId,
        Long patchId,
        String mode,
        String status,
        String conclusion,
        Double approveRate
) {
}
