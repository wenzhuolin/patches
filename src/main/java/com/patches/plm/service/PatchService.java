package com.patches.plm.service;

import com.patches.plm.api.dto.PatchActionRequest;
import com.patches.plm.api.dto.PatchActionResponse;
import com.patches.plm.api.dto.PatchCreateRequest;
import com.patches.plm.api.dto.PatchResponse;
import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.PatchEntity;
import com.patches.plm.domain.entity.PatchTransitionLogEntity;
import com.patches.plm.domain.entity.TestTaskEntity;
import com.patches.plm.domain.enums.*;
import com.patches.plm.domain.repository.PatchRepository;
import com.patches.plm.domain.repository.PatchTransitionLogRepository;
import com.patches.plm.domain.repository.TestTaskRepository;
import com.patches.plm.service.dto.KpiCheckResult;
import com.patches.plm.service.dto.QaCheckResult;
import com.patches.plm.service.workflow.PatchStateMachine;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PatchService {

    private final PatchRepository patchRepository;
    private final PatchTransitionLogRepository transitionLogRepository;
    private final PatchStateMachine patchStateMachine;
    private final AccessControlService accessControlService;
    private final KpiService kpiService;
    private final QaService qaService;
    private final TestTaskRepository testTaskRepository;
    private final AuditLogService auditLogService;

    public PatchService(PatchRepository patchRepository, PatchTransitionLogRepository transitionLogRepository,
                        PatchStateMachine patchStateMachine, AccessControlService accessControlService,
                        KpiService kpiService, QaService qaService, TestTaskRepository testTaskRepository,
                        AuditLogService auditLogService) {
        this.patchRepository = patchRepository;
        this.transitionLogRepository = transitionLogRepository;
        this.patchStateMachine = patchStateMachine;
        this.accessControlService = accessControlService;
        this.kpiService = kpiService;
        this.qaService = qaService;
        this.testTaskRepository = testTaskRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PatchResponse createPatch(PatchCreateRequest request, RequestContext context) {
        if (!(context.roles().contains("PM") || context.roles().contains("LINE_ADMIN") || context.roles().contains("SUPER_ADMIN"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅PM/产品线管理员可创建补丁");
        }

        PatchEntity entity = new PatchEntity();
        entity.setTenantId(context.tenantId());
        entity.setProductLineId(request.productLineId());
        entity.setPatchNo(generatePatchNo(context.tenantId()));
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setSeverity(request.severity());
        entity.setPriority(request.priority());
        entity.setSourceVersion(request.sourceVersion());
        entity.setTargetVersion(request.targetVersion());
        entity.setCurrentState(PatchState.DRAFT);
        entity.setOwnerPmId(request.ownerPmId());
        entity.setKpiBlocked(false);
        entity.setQaBlocked(false);
        entity.setCreatedBy(context.userId());
        entity.setUpdatedBy(context.userId());

        PatchEntity saved = patchRepository.save(entity);
        auditLogService.log("PATCH", saved.getId(), "CREATE", null, saved, context);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PatchResponse getPatch(Long patchId, RequestContext context) {
        PatchEntity patch = patchRepository.findByIdAndTenantId(patchId, context.tenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "补丁不存在"));
        return toResponse(patch);
    }

    @Transactional
    public PatchActionResponse executeAction(Long patchId, PatchActionRequest request, RequestContext context) {
        accessControlService.assertCanExecuteAction(context.roles(), request.action());
        PatchEntity patch = patchRepository.lockByIdAndTenantId(patchId, context.tenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "补丁不存在"));
        if (transitionLogRepository.existsByPatchIdAndRequestId(patchId, context.requestId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST, "重复请求，请更换Idempotency-Key");
        }

        PatchState fromState = patch.getCurrentState();
        PatchStateMachine.TransitionDefinition transition = patchStateMachine.next(fromState, request.action())
                .orElseThrow(() -> {
                    saveTransitionLog(patch, request.action(), fromState, null, FlowResult.FAILED, BlockType.NONE,
                            "非法状态流转", context);
                    return new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "当前状态不允许该操作");
                });

        List<String> reasons = new ArrayList<>();
        boolean kpiPassed = true;
        boolean qaPassed = true;

        if (transition.requireKpi()) {
            KpiCheckResult kpiResult = kpiService.evaluate(
                    context.tenantId(),
                    patch.getId(),
                    patch.getProductLineId(),
                    transition.stage(),
                    transition.gateType(),
                    request.action().name(),
                    context.traceId()
            );
            kpiPassed = kpiResult.pass();
            if (!kpiPassed) {
                reasons.add(kpiResult.summary());
                patch.setKpiBlocked(true);
                patch.setQaBlocked(false);
                patch.setUpdatedBy(context.userId());
                patchRepository.save(patch);
                saveTransitionLog(patch, request.action(), fromState, transition.toState(),
                        FlowResult.FAILED, BlockType.KPI, String.join("; ", reasons), context);
                throw new BusinessException(ErrorCode.KPI_GATE_FAILED, "KPI未达标，流转被阻断", kpiResult.details());
            }
        }

        if (transition.requireQa()) {
            QaCheckResult qaResult = qaService.ensureQaGate(
                    context.tenantId(),
                    patch.getId(),
                    patch.getProductLineId(),
                    transition.stage(),
                    context
            );
            qaPassed = qaResult.pass();
            if (!qaPassed) {
                reasons.addAll(qaResult.reasons());
                patch.setKpiBlocked(false);
                patch.setQaBlocked(true);
                patch.setUpdatedBy(context.userId());
                patchRepository.save(patch);
                saveTransitionLog(patch, request.action(), fromState, transition.toState(),
                        FlowResult.FAILED, BlockType.QA, String.join("; ", reasons), context);
                String code = qaResult.rejected() ? ErrorCode.QA_REJECTED : ErrorCode.QA_GATE_FAILED;
                throw new BusinessException(code, "QA未通过，流转被阻断", qaResult.reasons());
            }
        }

        patch.setCurrentState(transition.toState());
        patch.setKpiBlocked(false);
        patch.setQaBlocked(false);
        patch.setUpdatedBy(context.userId());
        patchRepository.save(patch);

        if (request.action() == PatchAction.TRANSFER_TO_TEST) {
            createTestTask(patch, context.userId());
        }

        saveTransitionLog(patch, request.action(), fromState, transition.toState(), FlowResult.SUCCESS, BlockType.NONE, null, context);
        auditLogService.log("PATCH", patch.getId(), request.action().name(), fromState, transition.toState(), context);
        return new PatchActionResponse(fromState, transition.toState(), kpiPassed, qaPassed, reasons);
    }

    private void createTestTask(PatchEntity patch, Long operatorId) {
        TestTaskEntity task = new TestTaskEntity();
        task.setTenantId(patch.getTenantId());
        task.setPatchId(patch.getId());
        task.setTaskNo("TT-" + patch.getPatchNo());
        task.setStatus("PENDING");
        task.setCreatedBy(operatorId);
        task.setUpdatedBy(operatorId);
        testTaskRepository.save(task);
    }

    private void saveTransitionLog(PatchEntity patch, PatchAction action, PatchState fromState, PatchState toState,
                                   FlowResult result, BlockType blockType, String reason, RequestContext context) {
        PatchTransitionLogEntity log = new PatchTransitionLogEntity();
        log.setTenantId(context.tenantId());
        log.setPatchId(patch.getId());
        log.setFromState(fromState);
        log.setToState(toState);
        log.setAction(action);
        log.setResult(result);
        log.setBlockType(blockType);
        log.setBlockReason(reason);
        log.setOperatorId(context.userId());
        log.setRequestId(context.requestId());
        transitionLogRepository.save(log);
    }

    private String generatePatchNo(Long tenantId) {
        long seq = patchRepository.countByTenantId(tenantId) + 1;
        return "P" + LocalDate.now().toString().replace("-", "") + "-" + String.format("%04d", seq);
    }

    private PatchResponse toResponse(PatchEntity patch) {
        return new PatchResponse(
                patch.getId(),
                patch.getPatchNo(),
                patch.getCurrentState(),
                patch.isKpiBlocked(),
                patch.isQaBlocked(),
                patch.getTitle(),
                patch.getSeverity(),
                patch.getPriority()
        );
    }
}
