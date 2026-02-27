package com.patches.plm.api.dto;

public record MailTemplateRenderResponse(
        String subject,
        String body
) {
}
