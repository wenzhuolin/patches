package com.patches.plm.api.dto;

import com.patches.plm.domain.enums.PatchState;

import java.time.OffsetDateTime;

public record PatchResponse(
        Long patchId,
        String patchNo,
        PatchState currentState,
        boolean kpiBlocked,
        boolean qaBlocked,
        String title,
        Long productLineId,
        String description,
        String severity,
        String priority,
        String sourceVersion,
        String targetVersion,
        Long ownerPmId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
