package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRoleAssignRequest(
        @NotNull Long userId,
        @NotBlank String roleCode,
        Boolean enabled
) {
}
