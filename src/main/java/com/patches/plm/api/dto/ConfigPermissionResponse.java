package com.patches.plm.api.dto;

public record ConfigPermissionResponse(
        Long id,
        String permCode,
        String permName,
        String permType,
        String resource,
        String action,
        Long parentId,
        boolean enabled
) {
}
