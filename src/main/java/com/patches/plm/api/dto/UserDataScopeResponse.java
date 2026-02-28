package com.patches.plm.api.dto;

public record UserDataScopeResponse(
        Long id,
        Long userId,
        String scopeType,
        String scopeValue,
        boolean enabled
) {
}
