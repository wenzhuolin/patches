package com.patches.plm.api.dto;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        String email,
        String mobile,
        String status,
        List<String> roles
) {
}
