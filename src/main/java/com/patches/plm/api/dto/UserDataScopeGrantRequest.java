package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserDataScopeGrantRequest(
        @NotNull Long userId,
        @NotBlank String scopeType,
        String scopeValue,
        Boolean enabled
) {
}
