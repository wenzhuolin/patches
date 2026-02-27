package com.patches.plm.service;

import com.patches.plm.api.dto.QaDecisionRequest;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.PatchEntity;
import com.patches.plm.domain.entity.QaPolicyEntity;
import com.patches.plm.domain.entity.QaTaskEntity;
import com.patches.plm.domain.enums.Decision;
import com.patches.plm.domain.enums.QaApprovalMode;
import com.patches.plm.domain.enums.QaTaskStatus;
import com.patches.plm.domain.enums.StageType;
import com.patches.plm.domain.repository.PatchRepository;
import com.patches.plm.domain.repository.QaDecisionLogRepository;
import com.patches.plm.domain.repository.QaPolicyRepository;
import com.patches.plm.domain.repository.QaTaskRepository;
import com.patches.plm.service.dto.QaCheckResult;
import com.patches.plm.web.RequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class QaServiceTest {

    private QaPolicyRepository qaPolicyRepository;
    private QaTaskRepository qaTaskRepository;
    private QaDecisionLogRepository qaDecisionLogRepository;
    private PatchRepository patchRepository;
    private AuditLogService auditLogService;
    private AccessControlService accessControlService;
    private QaService qaService;

    @BeforeEach
    void setUp() {
        qaPolicyRepository = Mockito.mock(QaPolicyRepository.class);
        qaTaskRepository = Mockito.mock(QaTaskRepository.class);
        qaDecisionLogRepository = Mockito.mock(QaDecisionLogRepository.class);
        patchRepository = Mockito.mock(PatchRepository.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        accessControlService = Mockito.mock(AccessControlService.class);
        qaService = new QaService(
                qaPolicyRepository,
                qaTaskRepository,
                qaDecisionLogRepository,
                patchRepository,
                auditLogService,
                accessControlService
        );
    }

    @Test
    void shouldReturnPendingLevelForSequentialGate() {
        QaPolicyEntity policy = new QaPolicyEntity();
        policy.setApprovalMode(QaApprovalMode.SEQUENTIAL);
        policy.setScopeType("GLOBAL");
        policy.setRequiredLevels("PRODUCT_LINE_QA,PROJECT_QA");

        QaTaskEntity t1 = new QaTaskEntity();
        t1.setSequenceNo(1);
        t1.setQaLevel("PRODUCT_LINE_QA");
        t1.setStatus(QaTaskStatus.APPROVED);
        QaTaskEntity t2 = new QaTaskEntity();
        t2.setSequenceNo(2);
        t2.setQaLevel("PROJECT_QA");
        t2.setStatus(QaTaskStatus.PENDING);

        Mockito.when(qaPolicyRepository.findActivePolicies(Mockito.eq(1L), Mockito.eq(StageType.TRANSFER_TEST), Mockito.eq("200"), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(policy));
        Mockito.when(qaTaskRepository.findByTenantIdAndPatchIdAndStageOrderBySequenceNoAsc(1L, 10L, StageType.TRANSFER_TEST))
                .thenReturn(List.of(t1, t2));

        QaCheckResult result = qaService.ensureQaGate(
                1L, 10L, 200L, StageType.TRANSFER_TEST,
                RequestContext.of(1L, 3001L, Set.of("PM"), "req", "trace", "127.0.0.1", "ut")
        );

        Assertions.assertFalse(result.pass());
        Assertions.assertTrue(result.reasons().getFirst().contains("PROJECT_QA"));
    }

    @Test
    void shouldBlockDecideWhenSequentialPreviousNodePending() {
        QaTaskEntity task2 = new QaTaskEntity();
        task2.setId(2002L);
        task2.setTenantId(1L);
        task2.setPatchId(10L);
        task2.setStage(StageType.TRANSFER_TEST);
        task2.setSequenceNo(2);
        task2.setAssigneeType("ROLE");
        task2.setAssigneeId("PROJECT_QA");
        task2.setStatus(QaTaskStatus.PENDING);

        QaTaskEntity task1 = new QaTaskEntity();
        task1.setId(2001L);
        task1.setTenantId(1L);
        task1.setPatchId(10L);
        task1.setStage(StageType.TRANSFER_TEST);
        task1.setSequenceNo(1);
        task1.setAssigneeType("ROLE");
        task1.setAssigneeId("PRODUCT_LINE_QA");
        task1.setStatus(QaTaskStatus.PENDING);

        QaPolicyEntity policy = new QaPolicyEntity();
        policy.setApprovalMode(QaApprovalMode.SEQUENTIAL);
        policy.setScopeType("GLOBAL");
        policy.setRequiredLevels("PRODUCT_LINE_QA,PROJECT_QA");

        PatchEntity patch = new PatchEntity();
        patch.setId(10L);
        patch.setTenantId(1L);
        patch.setProductLineId(200L);

        Mockito.when(qaTaskRepository.findByIdAndTenantId(2002L, 1L)).thenReturn(Optional.of(task2));
        Mockito.when(patchRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(patch));
        Mockito.when(qaPolicyRepository.findActivePolicies(Mockito.eq(1L), Mockito.eq(StageType.TRANSFER_TEST), Mockito.eq("200"), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(policy));
        Mockito.when(qaTaskRepository.findByTenantIdAndPatchIdAndStageOrderBySequenceNoAsc(1L, 10L, StageType.TRANSFER_TEST))
                .thenReturn(List.of(task1, task2));

        RequestContext context = RequestContext.of(1L, 3001L, Set.of("PROJECT_QA"), "req", "trace", "127.0.0.1", "ut");
        Mockito.doNothing().when(accessControlService).assertCanQaApprove(context.roles());

        Assertions.assertThrows(BusinessException.class, () ->
                qaService.decideTask(1L, 2002L, new QaDecisionRequest(Decision.APPROVE, "ok"), context));
    }
}
