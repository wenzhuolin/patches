package com.patches.plm.api.dto;

public record RoleResponse(
        Long id,
        String roleCode,
        String roleName,
        boolean enabled
) {
}
