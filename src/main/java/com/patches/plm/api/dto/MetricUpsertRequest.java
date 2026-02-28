package com.patches.plm.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public record MetricUpsertRequest(
        @NotEmpty List<@Valid MetricItem> metrics
) {
    public record MetricItem(
            @NotNull String metricKey,
            @NotNull Double metricValue,
            String sourceType,
            OffsetDateTime collectedAt
    ) {
    }
}
