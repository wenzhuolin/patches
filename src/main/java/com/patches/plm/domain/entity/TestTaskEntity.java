package com.patches.plm.domain.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "test_task")
public class TestTaskEntity extends AbstractAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "patch_id", nullable = false)
    private Long patchId;

    @Column(name = "task_no", nullable = false, length = 64)
    private String taskNo;

    @Column(name = "tester_id")
    private Long testerId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "case_prepare_rate")
    private Double casePrepareRate;

    @Column(name = "env_ready")
    private Boolean envReady;

    @Column(name = "case_execution_rate")
    private Double caseExecutionRate;

    @Column(name = "defect_density")
    private Double defectDensity;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getPatchId() {
        return patchId;
    }

    public void setPatchId(Long patchId) {
        this.patchId = patchId;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public Long getTesterId() {
        return testerId;
    }

    public void setTesterId(Long testerId) {
        this.testerId = testerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getCasePrepareRate() {
        return casePrepareRate;
    }

    public void setCasePrepareRate(Double casePrepareRate) {
        this.casePrepareRate = casePrepareRate;
    }

    public Boolean getEnvReady() {
        return envReady;
    }

    public void setEnvReady(Boolean envReady) {
        this.envReady = envReady;
    }

    public Double getCaseExecutionRate() {
        return caseExecutionRate;
    }

    public void setCaseExecutionRate(Double caseExecutionRate) {
        this.caseExecutionRate = caseExecutionRate;
    }

    public Double getDefectDensity() {
        return defectDensity;
    }

    public void setDefectDensity(Double defectDensity) {
        this.defectDensity = defectDensity;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
