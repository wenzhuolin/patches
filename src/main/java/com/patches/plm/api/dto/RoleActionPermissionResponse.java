package com.patches.plm.api.dto;

public record RoleActionPermissionResponse(
        Long id,
        String roleCode,
        String action,
        boolean enabled
) {
}
