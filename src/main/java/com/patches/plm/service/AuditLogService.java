package com.patches.plm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patches.plm.domain.entity.PatchOperationLogEntity;
import com.patches.plm.domain.repository.PatchOperationLogRepository;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final PatchOperationLogRepository operationLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(PatchOperationLogRepository operationLogRepository, ObjectMapper objectMapper) {
        this.operationLogRepository = operationLogRepository;
        this.objectMapper = objectMapper;
    }

    public void log(String bizType, Long bizId, String action, Object before, Object after, RequestContext context) {
        PatchOperationLogEntity logEntity = new PatchOperationLogEntity();
        logEntity.setTenantId(context.tenantId());
        logEntity.setBizType(bizType);
        logEntity.setBizId(bizId);
        logEntity.setAction(action);
        logEntity.setBeforeData(toJson(before));
        logEntity.setAfterData(toJson(after));
        logEntity.setOperatorId(context.userId());
        logEntity.setTraceId(context.traceId());
        logEntity.setIp(context.ip());
        logEntity.setUserAgent(context.userAgent());
        operationLogRepository.save(logEntity);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"serializeError\":\"" + ex.getMessage() + "\"}";
        }
    }
}
