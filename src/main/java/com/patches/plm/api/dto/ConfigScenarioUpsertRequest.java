package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfigScenarioUpsertRequest(
        @NotBlank @Size(max = 64) String scenarioCode,
        @NotBlank @Size(max = 128) String scenarioName,
        String description,
        @Size(max = 32) String status,
        String extProps
) {
}
