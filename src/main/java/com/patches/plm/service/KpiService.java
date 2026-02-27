package com.patches.plm.service;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.*;
import com.patches.plm.domain.enums.GateType;
import com.patches.plm.domain.enums.MissingDataPolicy;
import com.patches.plm.domain.enums.StageType;
import com.patches.plm.domain.repository.*;
import com.patches.plm.service.dto.KpiCheckResult;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class KpiService {

    private final KpiRuleRepository kpiRuleRepository;
    private final KpiMetricValueRepository kpiMetricValueRepository;
    private final KpiEvaluationRepository kpiEvaluationRepository;
    private final KpiEvaluationDetailRepository kpiEvaluationDetailRepository;
    private final PatchRepository patchRepository;
    private final AuditLogService auditLogService;

    public KpiService(KpiRuleRepository kpiRuleRepository, KpiMetricValueRepository kpiMetricValueRepository,
                      KpiEvaluationRepository kpiEvaluationRepository,
                      KpiEvaluationDetailRepository kpiEvaluationDetailRepository,
                      PatchRepository patchRepository, AuditLogService auditLogService) {
        this.kpiRuleRepository = kpiRuleRepository;
        this.kpiMetricValueRepository = kpiMetricValueRepository;
        this.kpiEvaluationRepository = kpiEvaluationRepository;
        this.kpiEvaluationDetailRepository = kpiEvaluationDetailRepository;
        this.patchRepository = patchRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public KpiRuleResponse createRule(KpiRuleCreateRequest request, RequestContext context) {
        KpiRuleEntity entity = new KpiRuleEntity();
        entity.setTenantId(context.tenantId());
        entity.setRuleCode(request.ruleCode().toUpperCase(Locale.ROOT));
        entity.setStage(request.stage());
        entity.setGateType(request.gateType());
        entity.setMetricKey(request.metricKey());
        entity.setCompareOp(request.compareOp());
        entity.setThresholdValue(request.thresholdValue());
        entity.setThresholdValue2(request.thresholdValue2());
        entity.setRequired(request.required());
        entity.setMissingDataPolicy(request.missingDataPolicy());
        entity.setPriority(request.priority() == null ? 100 : request.priority());
        entity.setScopeType(request.scopeType() == null || request.scopeType().isBlank() ? "GLOBAL" : request.scopeType());
        entity.setScopeValue(request.scopeValue());
        entity.setEffectiveFrom(request.effectiveFrom());
        entity.setEffectiveTo(request.effectiveTo());
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setCreatedBy(context.userId());
        entity.setUpdatedBy(context.userId());
        KpiRuleEntity saved = kpiRuleRepository.save(entity);
        auditLogService.log("KPI_RULE", saved.getId(), "CREATE", null, saved, context);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<KpiRuleResponse> listRules(Long tenantId) {
        return kpiRuleRepository.findByTenantIdOrderByStageAscPriorityAsc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public int upsertMetrics(Long tenantId, Long patchId, MetricUpsertRequest request, RequestContext context) {
        patchRepository.findByIdAndTenantId(patchId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "补丁不存在"));
        int count = 0;
        for (MetricUpsertRequest.MetricItem metric : request.metrics()) {
            KpiMetricValueEntity entity = new KpiMetricValueEntity();
            entity.setTenantId(tenantId);
            entity.setPatchId(patchId);
            entity.setMetricKey(metric.metricKey());
            entity.setMetricValue(metric.metricValue());
            entity.setSourceType(metric.sourceType() == null || metric.sourceType().isBlank() ? "MANUAL" : metric.sourceType());
            entity.setCollectedAt(metric.collectedAt() == null ? OffsetDateTime.now() : metric.collectedAt());
            kpiMetricValueRepository.save(entity);
            count++;
        }
        auditLogService.log("KPI_METRIC", patchId, "UPSERT_METRIC", null, request, context);
        return count;
    }

    @Transactional
    public KpiCheckResult evaluateForPatch(Long tenantId, Long patchId, StageType stage, GateType gateType,
                                           String triggerAction, String traceId) {
        PatchEntity patch = patchRepository.findByIdAndTenantId(patchId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "补丁不存在"));
        return evaluate(tenantId, patchId, patch.getProductLineId(), stage, gateType, triggerAction, traceId);
    }

    @Transactional
    public KpiCheckResult evaluate(Long tenantId, Long patchId, Long productLineId,
                                   StageType stage, GateType gateType, String triggerAction,
                                   String traceId) {
        List<KpiRuleEntity> rules = kpiRuleRepository.findActiveRules(
                tenantId, stage, gateType, String.valueOf(productLineId), OffsetDateTime.now()
        );
        if (rules.isEmpty()) {
            KpiEvaluationEntity noRuleEval = new KpiEvaluationEntity();
            noRuleEval.setTenantId(tenantId);
            noRuleEval.setPatchId(patchId);
            noRuleEval.setStage(stage);
            noRuleEval.setGateType(gateType);
            noRuleEval.setTriggerAction(triggerAction);
            noRuleEval.setResult("PASS");
            noRuleEval.setSummary("未命中KPI规则");
            noRuleEval.setTraceId(traceId);
            kpiEvaluationRepository.save(noRuleEval);
            return new KpiCheckResult(true, "未命中KPI规则", List.of());
        }

        boolean allRequiredPassed = true;
        List<String> failedReasons = new ArrayList<>();
        List<KpiEvaluationResponse.Detail> details = new ArrayList<>();

        KpiEvaluationEntity evaluation = new KpiEvaluationEntity();
        evaluation.setTenantId(tenantId);
        evaluation.setPatchId(patchId);
        evaluation.setStage(stage);
        evaluation.setGateType(gateType);
        evaluation.setTriggerAction(triggerAction);
        evaluation.setTraceId(traceId);
        kpiEvaluationRepository.save(evaluation);

        for (KpiRuleEntity rule : rules) {
            Double metricValue = null;
            boolean pass;
            String reason = "PASS";
            var metricOpt = kpiMetricValueRepository.findFirstByTenantIdAndPatchIdAndMetricKeyOrderByCollectedAtDesc(
                    tenantId, patchId, rule.getMetricKey()
            );
            if (metricOpt.isEmpty()) {
                pass = rule.getMissingDataPolicy() != MissingDataPolicy.FAIL;
                reason = "缺少指标数据: " + rule.getMetricKey();
            } else {
                metricValue = metricOpt.get().getMetricValue();
                pass = rule.getCompareOp().compare(metricValue, rule.getThresholdValue(), rule.getThresholdValue2());
                if (!pass) {
                    reason = "指标不达标, 当前=" + metricValue + ", 阈值=" + thresholdText(rule);
                }
            }

            if (rule.isRequired() && !pass) {
                allRequiredPassed = false;
                failedReasons.add(rule.getRuleCode() + ": " + reason);
            }

            KpiEvaluationDetailEntity detailEntity = new KpiEvaluationDetailEntity();
            detailEntity.setEvaluationId(evaluation.getId());
            detailEntity.setRuleId(rule.getId());
            detailEntity.setMetricValue(metricValue);
            detailEntity.setPass(pass);
            detailEntity.setThresholdSnapshot(thresholdText(rule));
            detailEntity.setReason(reason);
            detailEntity.setEvidence("{\"metricKey\":\"" + rule.getMetricKey() + "\"}");
            kpiEvaluationDetailRepository.save(detailEntity);

            details.add(new KpiEvaluationResponse.Detail(
                    rule.getRuleCode(),
                    rule.getMetricKey(),
                    metricValue,
                    thresholdText(rule),
                    pass,
                    reason
            ));
        }

        String summary = allRequiredPassed ? "KPI校验通过" : "KPI校验失败: " + String.join("; ", failedReasons);
        evaluation.setResult(allRequiredPassed ? "PASS" : "FAIL");
        evaluation.setSummary(summary);
        kpiEvaluationRepository.save(evaluation);

        return new KpiCheckResult(allRequiredPassed, summary, details);
    }

    private KpiRuleResponse toResponse(KpiRuleEntity entity) {
        return new KpiRuleResponse(
                entity.getId(),
                entity.getRuleCode(),
                entity.getStage(),
                entity.getGateType(),
                entity.getMetricKey(),
                entity.getCompareOp(),
                entity.getThresholdValue(),
                entity.getThresholdValue2(),
                entity.isRequired(),
                entity.getMissingDataPolicy().name(),
                entity.getPriority(),
                entity.getScopeType(),
                entity.getScopeValue(),
                entity.isEnabled()
        );
    }

    private String thresholdText(KpiRuleEntity rule) {
        if (rule.getThresholdValue2() == null) {
            return rule.getCompareOp().name() + " " + rule.getThresholdValue();
        }
        return rule.getCompareOp().name() + " " + rule.getThresholdValue() + "," + rule.getThresholdValue2();
    }
}
