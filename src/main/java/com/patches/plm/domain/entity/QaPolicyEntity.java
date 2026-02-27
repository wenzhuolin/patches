package com.patches.plm.domain.entity;

import com.patches.plm.domain.enums.QaApprovalMode;
import com.patches.plm.domain.enums.StageType;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "qa_policy")
public class QaPolicyEntity extends AbstractAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 32)
    private StageType stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_mode", nullable = false, length = 16)
    private QaApprovalMode approvalMode;

    @Column(name = "required_levels", nullable = false, length = 255)
    private String requiredLevels;

    @Column(name = "scope_type", nullable = false, length = 32)
    private String scopeType = "GLOBAL";

    @Column(name = "scope_value", length = 64)
    private String scopeValue;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "effective_from")
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

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

    public StageType getStage() {
        return stage;
    }

    public void setStage(StageType stage) {
        this.stage = stage;
    }

    public QaApprovalMode getApprovalMode() {
        return approvalMode;
    }

    public void setApprovalMode(QaApprovalMode approvalMode) {
        this.approvalMode = approvalMode;
    }

    public String getRequiredLevels() {
        return requiredLevels;
    }

    public void setRequiredLevels(String requiredLevels) {
        this.requiredLevels = requiredLevels;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
}
