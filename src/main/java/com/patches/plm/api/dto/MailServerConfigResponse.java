package com.patches.plm.api.dto;

import java.time.OffsetDateTime;

public record MailServerConfigResponse(
        Long id,
        String configName,
        String smtpHost,
        Integer smtpPort,
        String protocol,
        String username,
        String senderEmail,
        String senderName,
        boolean sslEnabled,
        boolean starttlsEnabled,
        boolean authEnabled,
        Integer timeoutMs,
        boolean defaultConfig,
        boolean enabled,
        OffsetDateTime updatedAt
) {
}
