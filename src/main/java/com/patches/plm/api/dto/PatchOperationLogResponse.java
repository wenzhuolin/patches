package com.patches.plm.api.dto;

import java.time.OffsetDateTime;

public record PatchOperationLogResponse(
        Long id,
        String bizType,
        Long bizId,
        String action,
        String beforeData,
        String afterData,
        Long operatorId,
        String traceId,
        String ip,
        String userAgent,
        OffsetDateTime createdAt
) {
}
