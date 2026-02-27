package com.patches.plm.api.dto;

public record ConfigUserRoleScopeResponse(
        Long id,
        Long userId,
        Long roleId,
        String scopeLevel,
        Long scenarioId,
        Long productId,
        String status
) {
}
