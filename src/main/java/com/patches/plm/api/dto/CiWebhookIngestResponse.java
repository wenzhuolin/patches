package com.patches.plm.api.dto;

public record CiWebhookIngestResponse(
        Long patchId,
        int acceptedMetrics,
        String pipelineName,
        String pipelineRunId
) {
}
