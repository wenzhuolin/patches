package com.patches.plm.api.dto;

import com.patches.plm.domain.enums.Decision;
import jakarta.validation.constraints.NotNull;

public record QaDecisionRequest(
        @NotNull Decision decision,
        String comment
) {
}
