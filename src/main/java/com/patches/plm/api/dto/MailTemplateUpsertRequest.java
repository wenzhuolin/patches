package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MailTemplateUpsertRequest(
        @NotBlank @Size(max = 64) String templateCode,
        @NotBlank @Size(max = 64) String eventCode,
        @NotBlank String subjectTpl,
        @NotBlank String bodyTpl,
        @Pattern(regexp = "TEXT|HTML", message = "contentType仅支持TEXT/HTML") String contentType,
        @Size(max = 16) String lang,
        Integer version,
        Boolean enabled,
        String extProps
) {
}
