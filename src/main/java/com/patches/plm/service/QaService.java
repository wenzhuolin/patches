package com.patches.plm.service;

import com.patches.plm.api.dto.QaDecisionRequest;
import com.patches.plm.api.dto.QaPolicyCreateRequest;
import com.patches.plm.api.dto.QaPolicyResponse;
import com.patches.plm.api.dto.QaTaskResponse;
import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.QaDecisionLogEntity;
import com.patches.plm.domain.entity.QaPolicyEntity;
import com.patches.plm.domain.entity.QaTaskEntity;
import com.patches.plm.domain.enums.Decision;
import com.patches.plm.domain.enums.QaApprovalMode;
import com.patches.plm.domain.enums.QaTaskStatus;
import com.patches.plm.domain.enums.StageType;
import com.patches.plm.domain.repository.QaDecisionLogRepository;
import com.patches.plm.domain.repository.QaPolicyRepository;
import com.patches.plm.domain.repository.QaTaskRepository;
import com.patches.plm.service.dto.QaCheckResult;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class QaService {

    private final QaPolicyRepository qaPolicyRepository;
    private final QaTaskRepository qaTaskRepository;
    private final QaDecisionLogRepository qaDecisionLogRepository;
    private final AuditLogService auditLogService;
    private final AccessControlService accessControlService;

    public QaService(QaPolicyRepository qaPolicyRepository, QaTaskRepository qaTaskRepository,
                     QaDecisionLogRepository qaDecisionLogRepository, AuditLogService auditLogService,
                     AccessControlService accessControlService) {
        this.qaPolicyRepository = qaPolicyRepository;
        this.qaTaskRepository = qaTaskRepository;
        this.qaDecisionLogRepository = qaDecisionLogRepository;
        this.auditLogService = auditLogService;
        this.accessControlService = accessControlService;
    }

    @Transactional
    public QaPolicyResponse createPolicy(Long tenantId, QaPolicyCreateRequest request, RequestContext context) {
        if (!(context.roles().contains("SUPER_ADMIN") || context.roles().contains("LINE_ADMIN"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可配置QA策略");
        }
        QaPolicyEntity policy = new QaPolicyEntity();
        policy.setTenantId(tenantId);
        policy.setStage(request.stage());
        policy.setApprovalMode(request.approvalMode());
        policy.setRequiredLevels(request.requiredLevels());
        policy.setScopeType(request.scopeType() == null || request.scopeType().isBlank() ? "GLOBAL" : request.scopeType());
        policy.setScopeValue(request.scopeValue());
        policy.setEnabled(request.enabled());
        policy.setEffectiveFrom(request.effectiveFrom());
        policy.setEffectiveTo(request.effectiveTo());
        policy.setCreatedBy(context.userId());
        policy.setUpdatedBy(context.userId());
        QaPolicyEntity saved = qaPolicyRepository.save(policy);
        auditLogService.log("QA_POLICY", saved.getId(), "CREATE", null, saved, context);
        return toPolicyResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<QaPolicyResponse> listPolicies(Long tenantId) {
        return qaPolicyRepository.findByTenantIdOrderByStageAsc(tenantId).stream()
                .map(this::toPolicyResponse)
                .toList();
    }

    @Transactional
    public QaCheckResult ensureQaGate(Long tenantId, Long patchId, Long productLineId, StageType stage, RequestContext context) {
        List<QaPolicyEntity> policies = qaPolicyRepository.findActivePolicies(
                tenantId, stage, String.valueOf(productLineId), OffsetDateTime.now()
        );
        if (policies.isEmpty()) {
            return new QaCheckResult(true, false, List.of("未配置QA策略"));
        }
        QaPolicyEntity policy = policies.stream()
                .sorted(Comparator.comparing((QaPolicyEntity p) -> "PRODUCT_LINE".equalsIgnoreCase(p.getScopeType()) ? 0 : 1))
                .findFirst()
                .orElseThrow();

        List<QaTaskEntity> tasks = qaTaskRepository.findByTenantIdAndPatchIdAndStageOrderBySequenceNoAsc(tenantId, patchId, stage);
        if (tasks.isEmpty()) {
            createQaTasks(policy, tenantId, patchId, stage, context.userId());
            return new QaCheckResult(false, false, List.of("已创建QA任务，请先完成QA审批"));
        }

        boolean rejected = tasks.stream().anyMatch(t -> t.getStatus() == QaTaskStatus.REJECTED);
        if (rejected) {
            return new QaCheckResult(false, true, List.of("QA已拒绝，需修复后重提"));
        }

        boolean passed = switch (policy.getApprovalMode()) {
            case ALL, SEQUENTIAL -> tasks.stream().allMatch(t -> t.getStatus() == QaTaskStatus.APPROVED);
            case ANY -> tasks.stream().anyMatch(t -> t.getStatus() == QaTaskStatus.APPROVED);
        };
        if (passed) {
            return new QaCheckResult(true, false, List.of("QA审批通过"));
        }
        return new QaCheckResult(false, false, List.of("QA审批尚未完成"));
    }

    @Transactional(readOnly = true)
    public List<QaTaskResponse> listMyPending(Long tenantId, RequestContext context) {
        List<String> assigneeIds = new ArrayList<>();
        assigneeIds.add(String.valueOf(context.userId()));
        assigneeIds.addAll(context.roles());
        return qaTaskRepository.findByTenantIdAndStatusAndAssigneeIdIn(tenantId, QaTaskStatus.PENDING, assigneeIds)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public QaTaskResponse decideTask(Long tenantId, Long qaTaskId, QaDecisionRequest request, RequestContext context) {
        accessControlService.assertCanQaApprove(context.roles());

        QaTaskEntity task = qaTaskRepository.findByIdAndTenantId(qaTaskId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "QA任务不存在"));
        if (task.getStatus() != QaTaskStatus.PENDING) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "QA任务已处理");
        }
        assertTaskAssignee(context, task);

        task.setStatus(request.decision() == Decision.APPROVE ? QaTaskStatus.APPROVED : QaTaskStatus.REJECTED);
        task.setDecisionComment(request.comment());
        task.setDecidedAt(OffsetDateTime.now());
        task.setUpdatedBy(context.userId());
        qaTaskRepository.save(task);

        QaDecisionLogEntity decisionLog = new QaDecisionLogEntity();
        decisionLog.setQaTaskId(task.getId());
        decisionLog.setDecision(request.decision());
        decisionLog.setComment(request.comment());
        decisionLog.setOperatorId(context.userId());
        qaDecisionLogRepository.save(decisionLog);

        auditLogService.log("QA_TASK", task.getId(), "QA_DECISION", null, task, context);
        return toResponse(task);
    }

    private void assertTaskAssignee(RequestContext context, QaTaskEntity task) {
        Set<String> roles = context.roles().stream().map(v -> v.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        if ("USER".equalsIgnoreCase(task.getAssigneeType())) {
            if (!String.valueOf(context.userId()).equals(task.getAssigneeId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户不是该QA任务处理人");
            }
            return;
        }
        if (!roles.contains(task.getAssigneeId().toUpperCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色不匹配该QA任务");
        }
    }

    private void createQaTasks(QaPolicyEntity policy, Long tenantId, Long patchId, StageType stage, Long operatorId) {
        String[] levels = policy.getRequiredLevels().split(",");
        for (int i = 0; i < levels.length; i++) {
            String level = levels[i].trim().toUpperCase(Locale.ROOT);
            if (level.isBlank()) {
                continue;
            }
            QaTaskEntity task = new QaTaskEntity();
            task.setTenantId(tenantId);
            task.setPatchId(patchId);
            task.setStage(stage);
            task.setQaLevel(level);
            task.setAssigneeType("ROLE");
            task.setAssigneeId("QA");
            task.setSequenceNo(i + 1);
            task.setStatus(QaTaskStatus.PENDING);
            task.setCreatedBy(operatorId);
            task.setUpdatedBy(operatorId);
            qaTaskRepository.save(task);
        }
    }

    private QaTaskResponse toResponse(QaTaskEntity task) {
        return new QaTaskResponse(
                task.getId(),
                task.getPatchId(),
                task.getStage(),
                task.getQaLevel(),
                task.getAssigneeType(),
                task.getAssigneeId(),
                task.getStatus(),
                task.getDecisionComment()
        );
    }

    private QaPolicyResponse toPolicyResponse(QaPolicyEntity policy) {
        return new QaPolicyResponse(
                policy.getId(),
                policy.getStage(),
                policy.getApprovalMode(),
                policy.getRequiredLevels(),
                policy.getScopeType(),
                policy.getScopeValue(),
                policy.isEnabled()
        );
    }
}
