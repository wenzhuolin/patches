package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfigProductUpsertRequest(
        @NotBlank @Size(max = 64) String productCode,
        @NotBlank @Size(max = 128) String productName,
        String description,
        Long ownerUserId,
        @Size(max = 32) String status,
        String extProps
) {
}
