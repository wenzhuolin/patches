package com.patches.plm.domain.entity;

import com.patches.plm.domain.enums.CompareOp;
import com.patches.plm.domain.enums.GateType;
import com.patches.plm.domain.enums.MissingDataPolicy;
import com.patches.plm.domain.enums.StageType;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "kpi_rule")
public class KpiRuleEntity extends AbstractAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 32)
    private StageType stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_type", nullable = false, length = 16)
    private GateType gateType;

    @Column(name = "metric_key", nullable = false, length = 64)
    private String metricKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "compare_op", nullable = false, length = 16)
    private CompareOp compareOp;

    @Column(name = "threshold_value")
    private Double thresholdValue;

    @Column(name = "threshold_value2")
    private Double thresholdValue2;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Enumerated(EnumType.STRING)
    @Column(name = "missing_data_policy", nullable = false, length = 16)
    private MissingDataPolicy missingDataPolicy;

    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    @Column(name = "scope_type", nullable = false, length = 32)
    private String scopeType = "GLOBAL";

    @Column(name = "scope_value", length = 64)
    private String scopeValue;

    @Column(name = "effective_from")
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

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

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
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

    public String getMetricKey() {
        return metricKey;
    }

    public void setMetricKey(String metricKey) {
        this.metricKey = metricKey;
    }

    public CompareOp getCompareOp() {
        return compareOp;
    }

    public void setCompareOp(CompareOp compareOp) {
        this.compareOp = compareOp;
    }

    public Double getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(Double thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public Double getThresholdValue2() {
        return thresholdValue2;
    }

    public void setThresholdValue2(Double thresholdValue2) {
        this.thresholdValue2 = thresholdValue2;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public MissingDataPolicy getMissingDataPolicy() {
        return missingDataPolicy;
    }

    public void setMissingDataPolicy(MissingDataPolicy missingDataPolicy) {
        this.missingDataPolicy = missingDataPolicy;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeValue() {
        return scopeValue;
    }

    public void setScopeValue(String scopeValue) {
        this.scopeValue = scopeValue;
    }

    public OffsetDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(OffsetDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public OffsetDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(OffsetDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
