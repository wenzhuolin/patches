package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ConfigRolePermissionAssignRequest(
        @NotNull Long roleId,
        @NotNull List<Long> permissionIds
) {
}
