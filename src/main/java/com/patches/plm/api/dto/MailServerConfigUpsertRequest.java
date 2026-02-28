package com.patches.plm.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MailServerConfigUpsertRequest(
        @NotBlank @Size(max = 64) String configName,
        @NotBlank @Size(max = 255) String smtpHost,
        @Min(1) @Max(65535) Integer smtpPort,
        String protocol,
        @Size(max = 128) String username,
        String password,
        @NotBlank @Email @Size(max = 128) String senderEmail,
        @Size(max = 128) String senderName,
        Boolean sslEnabled,
        Boolean starttlsEnabled,
        Boolean authEnabled,
        Integer timeoutMs,
        Boolean defaultConfig,
        Boolean enabled,
        String extProps
) {
}
