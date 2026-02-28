package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ConfigScenarioProductBindRequest(
        @NotNull Long scenarioId,
        @NotNull List<Long> productIds
) {
}
