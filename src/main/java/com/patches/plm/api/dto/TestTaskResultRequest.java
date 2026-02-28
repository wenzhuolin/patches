package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TestTaskResultRequest(
        @NotBlank String status,
        Double casePrepareRate,
        Boolean envReady,
        Double caseExecutionRate,
        Double defectDensity,
        String summary,
        @NotNull Long testerId
) {
}
