package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfigPermissionUpsertRequest(
        @NotBlank @Size(max = 128) String permCode,
        @NotBlank @Size(max = 128) String permName,
        @NotBlank @Size(max = 16) String permType,
        @Size(max = 64) String resource,
        @Size(max = 64) String action,
        Long parentId,
        Boolean enabled
) {
}
