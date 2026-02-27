package com.patches.plm.api.dto;

import java.time.OffsetDateTime;

public record MailTemplateResponse(
        Long id,
        String templateCode,
        String eventCode,
        String subjectTpl,
        String bodyTpl,
        String contentType,
        String lang,
        Integer version,
        boolean enabled,
        OffsetDateTime updatedAt
) {
}
