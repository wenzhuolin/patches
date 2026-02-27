package com.patches.plm.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MailEventPolicyResponse(
        Long id,
        String eventCode,
        String templateCode,
        List<String> toRoleCodes,
        List<String> ccRoleCodes,
        boolean includeOwner,
        boolean includeOperator,
        boolean enabled,
        OffsetDateTime updatedAt
) {
}
