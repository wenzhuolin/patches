package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UserCreateRequest(
        @NotBlank String username,
        @NotBlank String displayName,
        String email,
        String mobile,
        String status
) {
}
