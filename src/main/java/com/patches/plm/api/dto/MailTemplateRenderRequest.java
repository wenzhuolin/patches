package com.patches.plm.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public record MailTemplateRenderRequest(
        @NotBlank String templateCode,
        JsonNode model
) {
}
