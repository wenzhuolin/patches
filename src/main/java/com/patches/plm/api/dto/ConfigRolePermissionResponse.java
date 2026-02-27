package com.patches.plm.api.dto;

import java.util.List;

public record ConfigRolePermissionResponse(
        Long roleId,
        List<String> permCodes
) {
}
