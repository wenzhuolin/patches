package com.patches.plm.api.dto;

import com.patches.plm.domain.enums.GateType;
import com.patches.plm.domain.enums.StageType;
import jakarta.validation.constraints.NotNull;

public record KpiEvaluateRequest(
        @NotNull StageType stage,
        @NotNull GateType gateType,
        @NotNull String triggerAction
) {
}
