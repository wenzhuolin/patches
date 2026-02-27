package com.patches.plm.domain.entity;

import com.patches.plm.domain.enums.PatchState;
import jakarta.persistence.*;

@Entity
@Table(name = "patch")
public class PatchEntity extends AbstractAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "product_line_id", nullable = false)
    private Long productLineId;

    @Column(name = "patch_no", nullable = false, length = 64)
    private String patchNo;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "severity", length = 32)
    private String severity;

    @Column(name = "priority", length = 32)
    private String priority;

    @Column(name = "source_version", length = 64)
    private String sourceVersion;

    @Column(name = "target_version", length = 64)
    private String targetVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", nullable = false, length = 32)
    private PatchState currentState;

    @Column(name = "owner_pm_id", nullable = false)
    private Long ownerPmId;

    @Column(name = "kpi_blocked", nullable = false)
    private boolean kpiBlocked;

    @Column(name = "qa_blocked", nullable = false)
    private boolean qaBlocked;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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

    public Long getProductLineId() {
        return productLineId;
    }

    public void setProductLineId(Long productLineId) {
        this.productLineId = productLineId;
    }

    public String getPatchNo() {
        return patchNo;
    }

    public void setPatchNo(String patchNo) {
        this.patchNo = patchNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public PatchState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(PatchState currentState) {
        this.currentState = currentState;
    }

    public Long getOwnerPmId() {
        return ownerPmId;
    }

    public void setOwnerPmId(Long ownerPmId) {
        this.ownerPmId = ownerPmId;
    }

    public boolean isKpiBlocked() {
        return kpiBlocked;
    }

    public void setKpiBlocked(boolean kpiBlocked) {
        this.kpiBlocked = kpiBlocked;
    }

    public boolean isQaBlocked() {
        return qaBlocked;
    }

    public void setQaBlocked(boolean qaBlocked) {
        this.qaBlocked = qaBlocked;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
