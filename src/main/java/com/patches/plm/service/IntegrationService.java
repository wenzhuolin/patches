package com.patches.plm.service;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.IntegrationConnectorEntity;
import com.patches.plm.domain.repository.IntegrationConnectorRepository;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class IntegrationService {

    private final IntegrationConnectorRepository integrationConnectorRepository;
    private final KpiService kpiService;
    private final AuditLogService auditLogService;

    public IntegrationService(IntegrationConnectorRepository integrationConnectorRepository,
                              KpiService kpiService,
                              AuditLogService auditLogService) {
        this.integrationConnectorRepository = integrationConnectorRepository;
        this.kpiService = kpiService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public IntegrationConnectorResponse createConnector(Long tenantId, IntegrationConnectorCreateRequest request, RequestContext context) {
        assertIntegrationAdmin(context);
        IntegrationConnectorEntity entity = new IntegrationConnectorEntity();
        entity.setTenantId(tenantId);
        entity.setConnectorType(request.connectorType().toUpperCase(Locale.ROOT));
        entity.setConnectorName(request.connectorName());
        entity.setBaseUrl(request.baseUrl());
        entity.setAuthConfig(request.authConfig());
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setCreatedBy(context.userId());
        entity.setUpdatedBy(context.userId());
        IntegrationConnectorEntity saved = integrationConnectorRepository.save(entity);
        auditLogService.log("INTEGRATION", saved.getId(), "CREATE_CONNECTOR", null, saved, context);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IntegrationConnectorResponse> listConnectors(Long tenantId) {
        return integrationConnectorRepository.findByTenantIdOrderByIdDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CiWebhookIngestResponse ingestCiWebhook(Long tenantId, CiWebhookIngestRequest request, RequestContext context) {
        var metricItems = request.metrics().stream()
                .map(item -> new MetricUpsertRequest.MetricItem(
                        item.metricKey(),
                        item.metricValue(),
                        "CI",
                        request.collectedAt() == null ? OffsetDateTime.now() : request.collectedAt()
                )).toList();
        MetricUpsertRequest metricUpsertRequest = new MetricUpsertRequest(metricItems);
        int accepted = kpiService.upsertMetrics(tenantId, request.patchId(), metricUpsertRequest, context);
        auditLogService.log("INTEGRATION", request.patchId(), "CI_WEBHOOK_INGEST", null, request, context);
        return new CiWebhookIngestResponse(request.patchId(), accepted, request.pipelineName(), request.pipelineRunId());
    }

    private IntegrationConnectorResponse toResponse(IntegrationConnectorEntity entity) {
        return new IntegrationConnectorResponse(
                entity.getId(),
                entity.getConnectorType(),
                entity.getConnectorName(),
                entity.getBaseUrl(),
                entity.isEnabled()
        );
    }

    private void assertIntegrationAdmin(RequestContext context) {
        if (!(context.roles().contains("SUPER_ADMIN") || context.roles().contains("LINE_ADMIN"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可配置集成连接器");
        }
    }
}
