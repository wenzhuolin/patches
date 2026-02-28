package com.patches.plm.service.notification;

import java.util.Map;

public record MailNotifyCommand(
        Long tenantId,
        String eventCode,
        String bizType,
        Long bizId,
        Long operatorId,
        Long ownerUserId,
        Map<String, Object> model,
        String idempotencyKey
) {
}
