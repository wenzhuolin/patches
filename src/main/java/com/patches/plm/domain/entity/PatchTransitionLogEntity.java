package com.patches.plm.domain.entity;

import com.patches.plm.domain.enums.BlockType;
import com.patches.plm.domain.enums.FlowResult;
import com.patches.plm.domain.enums.PatchAction;
import com.patches.plm.domain.enums.PatchState;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "patch_transition_log")
public class PatchTransitionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "patch_id", nullable = false)
    private Long patchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", length = 32)
    private PatchState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", length = 32)
    private PatchState toState;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 64)
    private PatchAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 16)
    private FlowResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 16)
    private BlockType blockType;

    @Column(name = "block_reason", columnDefinition = "text")
    private String blockReason;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "request_id", nullable = false, length = 128)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

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

    public PatchState getFromState() {
        return fromState;
    }

    public void setFromState(PatchState fromState) {
        this.fromState = fromState;
    }

    public PatchState getToState() {
        return toState;
    }

    public void setToState(PatchState toState) {
        this.toState = toState;
    }

    public PatchAction getAction() {
        return action;
    }

    public void setAction(PatchAction action) {
        this.action = action;
    }

    public FlowResult getResult() {
        return result;
    }

    public void setResult(FlowResult result) {
        this.result = result;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public void setBlockType(BlockType blockType) {
        this.blockType = blockType;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
