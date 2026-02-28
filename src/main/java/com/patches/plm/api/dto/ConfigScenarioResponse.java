package com.patches.plm.api.dto;

public record ConfigScenarioResponse(
        Long id,
        String scenarioCode,
        String scenarioName,
        String description,
        String status
) {
}
