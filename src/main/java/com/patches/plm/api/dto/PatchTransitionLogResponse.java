package com.patches.plm.api.dto;

import java.time.OffsetDateTime;

public record PatchTransitionLogResponse(
        Long id,
        String fromState,
        String toState,
        String action,
        String result,
        String blockType,
        String blockReason,
        Long operatorId,
        String requestId,
        OffsetDateTime createdAt
) {
}
