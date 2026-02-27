package com.patches.plm.api.dto;

import com.patches.plm.domain.enums.QaApprovalMode;
import com.patches.plm.domain.enums.StageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record QaPolicyCreateRequest(
        @NotNull StageType stage,
        @NotNull QaApprovalMode approvalMode,
        @NotBlank String requiredLevels,
        String scopeType,
        String scopeValue,
        boolean enabled,
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveTo
) {
}
