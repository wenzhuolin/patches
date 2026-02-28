package com.patches.plm.service;

import com.patches.plm.api.dto.PatchOperationLogResponse;
import com.patches.plm.domain.repository.PatchOperationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditQueryService {

    private final PatchOperationLogRepository patchOperationLogRepository;

    public AuditQueryService(PatchOperationLogRepository patchOperationLogRepository) {
        this.patchOperationLogRepository = patchOperationLogRepository;
    }

    @Transactional(readOnly = true)
    public List<PatchOperationLogResponse> queryByBizType(Long tenantId, String bizType) {
        return patchOperationLogRepository.findByTenantIdAndBizTypeOrderByCreatedAtDesc(tenantId, bizType)
                .stream()
                .map(log -> new PatchOperationLogResponse(
                        log.getId(),
                        log.getBizType(),
                        log.getBizId(),
                        log.getAction(),
                        log.getBeforeData(),
                        log.getAfterData(),
                        log.getOperatorId(),
                        log.getTraceId(),
                        log.getIp(),
                        log.getUserAgent(),
                        log.getCreatedAt()
                ))
                .toList();
    }
}
