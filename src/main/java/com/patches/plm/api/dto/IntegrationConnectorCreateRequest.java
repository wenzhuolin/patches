package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;

public record IntegrationConnectorCreateRequest(
        @NotBlank String connectorType,
        @NotBlank String connectorName,
        String baseUrl,
        String authConfig,
        Boolean enabled
) {
}
