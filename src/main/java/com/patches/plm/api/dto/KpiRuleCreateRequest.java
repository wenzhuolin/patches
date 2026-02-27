package com.patches.plm.api.dto;

import com.patches.plm.domain.enums.CompareOp;
import com.patches.plm.domain.enums.GateType;
import com.patches.plm.domain.enums.MissingDataPolicy;
import com.patches.plm.domain.enums.StageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record KpiRuleCreateRequest(
        @NotBlank String ruleCode,
        @NotNull StageType stage,
        @NotNull GateType gateType,
        @NotBlank String metricKey,
        @NotNull CompareOp compareOp,
        @NotNull Double thresholdValue,
        Double thresholdValue2,
        boolean required,
        @NotNull MissingDataPolicy missingDataPolicy,
        Integer priority,
        String scopeType,
        String scopeValue,
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveTo,
        Boolean enabled
) {
}
