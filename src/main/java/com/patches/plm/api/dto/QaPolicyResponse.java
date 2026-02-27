package com.patches.plm.api.dto;

import com.patches.plm.domain.enums.QaApprovalMode;
import com.patches.plm.domain.enums.StageType;

public record QaPolicyResponse(
        Long id,
        StageType stage,
        QaApprovalMode approvalMode,
        String requiredLevels,
        String scopeType,
        String scopeValue,
        boolean enabled
) {
}
