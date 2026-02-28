package com.patches.plm.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "review_session")
public class ReviewSessionEntity extends AbstractAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "patch_id", nullable = false)
    private Long patchId;

    @Column(name = "mode", nullable = false, length = 16)
    private String mode;

    @Column(name = "meeting_tool", length = 64)
    private String meetingTool;

    @Column(name = "meeting_url", length = 255)
    private String meetingUrl;

    @Column(name = "quorum_required")
    private Double quorumRequired;

    @Column(name = "approve_rate_required")
    private Double approveRateRequired;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "conclusion", length = 16)
    private String conclusion;

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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMeetingTool() {
        return meetingTool;
    }

    public void setMeetingTool(String meetingTool) {
        this.meetingTool = meetingTool;
    }

    public String getMeetingUrl() {
        return meetingUrl;
    }

    public void setMeetingUrl(String meetingUrl) {
        this.meetingUrl = meetingUrl;
    }

    public Double getQuorumRequired() {
        return quorumRequired;
    }

    public void setQuorumRequired(Double quorumRequired) {
        this.quorumRequired = quorumRequired;
    }

    public Double getApproveRateRequired() {
        return approveRateRequired;
    }

    public void setApproveRateRequired(Double approveRateRequired) {
        this.approveRateRequired = approveRateRequired;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }
}
