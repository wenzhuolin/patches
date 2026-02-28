package com.patches.plm.domain.entity;

import com.patches.plm.domain.enums.GateType;
import com.patches.plm.domain.enums.StageType;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "kpi_evaluation")
public class KpiEvaluationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "patch_id", nullable = false)
    private Long patchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 32)
    private StageType stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_type", nullable = false, length = 16)
    private GateType gateType;

    @Column(name = "trigger_action", nullable = false, length = 64)
    private String triggerAction;

    @Column(name = "result", nullable = false, length = 16)
    private String result;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt = OffsetDateTime.now();

    @Column(name = "trace_id", length = 64)
    private String traceId;

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

    public StageType getStage() {
        return stage;
    }

    public void setStage(StageType stage) {
        this.stage = stage;
    }

    public GateType getGateType() {
        return gateType;
    }

    public void setGateType(GateType gateType) {
        this.gateType = gateType;
    }

    public String getTriggerAction() {
        return triggerAction;
    }

    public void setTriggerAction(String triggerAction) {
        this.triggerAction = triggerAction;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public OffsetDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(OffsetDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
