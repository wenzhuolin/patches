package com.patches.plm.api.dto;

import com.patches.plm.domain.enums.QaTaskStatus;
import com.patches.plm.domain.enums.StageType;

public record QaTaskResponse(
        Long qaTaskId,
        Long patchId,
        StageType stage,
        String qaLevel,
        String assigneeType,
        String assigneeId,
        QaTaskStatus status,
        String decisionComment
) {
}
