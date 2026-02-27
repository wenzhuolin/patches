package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PatchCreateRequest(
        @NotNull Long productLineId,
        @NotBlank String title,
        String description,
        String severity,
        String priority,
        String sourceVersion,
        String targetVersion,
        @NotNull Long ownerPmId
) {
}
