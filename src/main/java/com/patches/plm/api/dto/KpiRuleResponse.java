package com.patches.plm.api.dto;

import com.patches.plm.domain.enums.CompareOp;
import com.patches.plm.domain.enums.GateType;
import com.patches.plm.domain.enums.StageType;

public record KpiRuleResponse(
        Long id,
        String ruleCode,
        StageType stage,
        GateType gateType,
        String metricKey,
        CompareOp compareOp,
        Double thresholdValue,
        Double thresholdValue2,
        boolean required,
        String missingDataPolicy,
        Integer priority,
        String scopeType,
        String scopeValue,
        boolean enabled
) {
}
