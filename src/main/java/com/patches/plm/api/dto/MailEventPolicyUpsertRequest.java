package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MailEventPolicyUpsertRequest(
        @NotBlank @Size(max = 64) String eventCode,
        @NotBlank @Size(max = 64) String templateCode,
        List<String> toRoleCodes,
        List<String> ccRoleCodes,
        Boolean includeOwner,
        Boolean includeOperator,
        Boolean enabled
) {
}
