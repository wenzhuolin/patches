package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleCreateRequest(
        @NotBlank String roleCode,
        @NotBlank String roleName,
        Boolean enabled
) {
}
