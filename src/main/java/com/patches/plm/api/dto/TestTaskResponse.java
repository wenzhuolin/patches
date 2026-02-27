package com.patches.plm.api.dto;

public record TestTaskResponse(
        Long taskId,
        Long patchId,
        String taskNo,
        String status,
        Double casePrepareRate,
        Boolean envReady,
        Double caseExecutionRate,
        Double defectDensity,
        Long testerId
) {
}
