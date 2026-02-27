package com.patches.plm.api.dto;

import java.util.List;

public record ConfigProductResponse(
        Long id,
        String productCode,
        String productName,
        String description,
        Long ownerUserId,
        String status,
        List<Long> scenarioIds
) {
}
