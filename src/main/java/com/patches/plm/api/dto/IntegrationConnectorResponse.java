package com.patches.plm.api.dto;

public record IntegrationConnectorResponse(
        Long id,
        String connectorType,
        String connectorName,
        String baseUrl,
        boolean enabled
) {
}
