package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfigRoleUpsertRequest(
        @NotBlank @Size(max = 64) String roleCode,
        @NotBlank @Size(max = 128) String roleName,
        @Size(max = 16) String roleLevel,
        Long scopeRefId,
        Boolean enabled
) {
}
