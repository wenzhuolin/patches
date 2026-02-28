package com.patches.plm.api.dto;

import java.time.OffsetDateTime;

public record MailSendLogResponse(
        Long id,
        String bizType,
        Long bizId,
        String eventCode,
        String mailTo,
        String mailCc,
        String status,
        Integer retryCount,
        Integer maxRetry,
        String errorCode,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime sentAt
) {
}
