package com.patches.plm.api.dto;

public record UserRoleResponse(
        Long id,
        Long userId,
        String roleCode,
        boolean enabled
) {
}
