package com.patches.plm.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public record CiWebhookIngestRequest(
        @NotNull Long patchId,
        String pipelineName,
        String pipelineRunId,
        @NotEmpty List<@Valid Metric> metrics,
        OffsetDateTime collectedAt
) {
    public record Metric(
            @NotNull String metricKey,
            @NotNull Double metricValue
    ) {
    }
}
