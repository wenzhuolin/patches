package com.patches.plm.service;

import com.patches.plm.api.dto.TestTaskResponse;
import com.patches.plm.api.dto.TestTaskResultRequest;
import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.TestTaskEntity;
import com.patches.plm.domain.repository.TestTaskRepository;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TestTaskService {

    private final TestTaskRepository testTaskRepository;
    private final AuditLogService auditLogService;

    public TestTaskService(TestTaskRepository testTaskRepository, AuditLogService auditLogService) {
        this.testTaskRepository = testTaskRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<TestTaskResponse> listByPatch(Long tenantId, Long patchId) {
        return testTaskRepository.findByTenantIdAndPatchId(tenantId, patchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TestTaskResponse fillResult(Long tenantId, Long taskId, TestTaskResultRequest request, RequestContext context) {
        assertTestPermission(context);
        TestTaskEntity task = testTaskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "测试任务不存在"));

        task.setStatus(request.status());
        task.setCasePrepareRate(request.casePrepareRate());
        task.setEnvReady(request.envReady());
        task.setCaseExecutionRate(request.caseExecutionRate());
        task.setDefectDensity(request.defectDensity());
        task.setTesterId(request.testerId());
        task.setCompletedAt(OffsetDateTime.now());
        task.setUpdatedBy(context.userId());
        testTaskRepository.save(task);
        auditLogService.log("TEST_TASK", task.getId(), "FILL_RESULT", null, task, context);
        return toResponse(task);
    }

    private void assertTestPermission(RequestContext context) {
        if (!(context.roles().contains("TEST")
                || context.roles().contains("LINE_ADMIN")
                || context.roles().contains("SUPER_ADMIN"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色无权回填测试结果");
        }
    }

    private TestTaskResponse toResponse(TestTaskEntity entity) {
        return new TestTaskResponse(
                entity.getId(),
                entity.getPatchId(),
                entity.getTaskNo(),
                entity.getStatus(),
                entity.getCasePrepareRate(),
                entity.getEnvReady(),
                entity.getCaseExecutionRate(),
                entity.getDefectDensity(),
                entity.getTesterId()
        );
    }
}
