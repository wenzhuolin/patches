package com.patches.plm.api.dto;

import com.patches.plm.domain.enums.PatchState;

public record PatchResponse(
        Long patchId,
        String patchNo,
        PatchState currentState,
        boolean kpiBlocked,
        boolean qaBlocked,
        String title,
        String severity,
        String priority
) {
}
