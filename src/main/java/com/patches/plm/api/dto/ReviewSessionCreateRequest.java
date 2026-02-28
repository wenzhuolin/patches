package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewSessionCreateRequest(
        @NotNull Long patchId,
        @NotBlank String mode,
        String meetingTool,
        String meetingUrl,
        Double quorumRequired,
        Double approveRateRequired
) {
}
