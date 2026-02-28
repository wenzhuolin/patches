package com.patches.plm.api.dto;

import com.patches.plm.domain.enums.PatchAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoleActionPermissionUpsertRequest(
        @NotBlank String roleCode,
        @NotNull PatchAction action,
        Boolean enabled
) {
}
