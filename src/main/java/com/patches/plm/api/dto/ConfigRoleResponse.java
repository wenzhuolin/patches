package com.patches.plm.api.dto;

public record ConfigRoleResponse(
        Long id,
        String roleCode,
        String roleName,
        String roleLevel,
        Long scopeRefId,
        boolean enabled
) {
}
