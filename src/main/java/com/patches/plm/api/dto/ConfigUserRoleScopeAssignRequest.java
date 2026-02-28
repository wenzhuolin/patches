package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfigUserRoleScopeAssignRequest(
        @NotNull Long userId,
        @NotNull Long roleId,
        @Size(max = 16) String scopeLevel,
        Long scenarioId,
        Long productId,
        @Size(max = 32) String status
) {
}
