package com.patches.plm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patches.plm.domain.entity.ConfigChangeLogEntity;
import com.patches.plm.domain.repository.ConfigChangeLogRepository;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;

@Service
public class ConfigAuditLogService {

    private final ConfigChangeLogRepository configChangeLogRepository;
    private final ObjectMapper objectMapper;

    public ConfigAuditLogService(ConfigChangeLogRepository configChangeLogRepository, ObjectMapper objectMapper) {
        this.configChangeLogRepository = configChangeLogRepository;
        this.objectMapper = objectMapper;
    }

    public void log(String configType, Long configId, String action, Object before, Object after, RequestContext context) {
        ConfigChangeLogEntity log = new ConfigChangeLogEntity();
        log.setTenantId(context.tenantId());
        log.setConfigType(configType);
        log.setConfigId(configId);
        log.setAction(action);
        log.setBeforeData(toJson(before));
        log.setAfterData(toJson(after));
        log.setOperatorId(context.userId());
        log.setTraceId(context.traceId());
        log.setIp(context.ip());
        log.setUserAgent(context.userAgent());
        configChangeLogRepository.save(log);
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
