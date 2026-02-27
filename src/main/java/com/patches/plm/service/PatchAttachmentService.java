package com.patches.plm.service;

import com.patches.plm.api.dto.PatchAttachmentCreateRequest;
import com.patches.plm.api.dto.PatchAttachmentResponse;
import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.PatchAttachmentEntity;
import com.patches.plm.domain.entity.PatchEntity;
import com.patches.plm.domain.repository.PatchAttachmentRepository;
import com.patches.plm.domain.repository.PatchRepository;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class PatchAttachmentService {

    private final PatchAttachmentRepository patchAttachmentRepository;
    private final PatchRepository patchRepository;
    private final AccessControlService accessControlService;
    private final AuditLogService auditLogService;

    public PatchAttachmentService(PatchAttachmentRepository patchAttachmentRepository,
                                  PatchRepository patchRepository,
                                  AccessControlService accessControlService,
                                  AuditLogService auditLogService) {
        this.patchAttachmentRepository = patchAttachmentRepository;
        this.patchRepository = patchRepository;
        this.accessControlService = accessControlService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PatchAttachmentResponse createAttachment(Long tenantId, Long patchId, PatchAttachmentCreateRequest request, RequestContext context) {
        PatchEntity patch = patchRepository.findByIdAndTenantId(patchId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "补丁不存在"));
        accessControlService.assertCanViewPatch(context, patch);

        PatchAttachmentEntity entity = new PatchAttachmentEntity();
        entity.setTenantId(tenantId);
        entity.setPatchId(patchId);
        entity.setStage(request.stage().toUpperCase(Locale.ROOT));
        entity.setFileName(request.fileName());
        entity.setFileUrl(request.fileUrl());
        entity.setFileHash(request.fileHash());
        entity.setFileSize(request.fileSize());
        entity.setUploaderId(context.userId());
        entity.setScanStatus(request.scanStatus() == null || request.scanStatus().isBlank()
                ? "PENDING" : request.scanStatus().toUpperCase(Locale.ROOT));
        PatchAttachmentEntity saved = patchAttachmentRepository.save(entity);
        auditLogService.log("PATCH_ATTACHMENT", saved.getId(), "CREATE_ATTACHMENT", null, saved, context);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PatchAttachmentResponse> listAttachments(Long tenantId, Long patchId, RequestContext context) {
        PatchEntity patch = patchRepository.findByIdAndTenantId(patchId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "补丁不存在"));
        accessControlService.assertCanViewPatch(context, patch);
        return patchAttachmentRepository.findByTenantIdAndPatchIdOrderByCreatedAtDesc(tenantId, patchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PatchAttachmentResponse toResponse(PatchAttachmentEntity entity) {
        return new PatchAttachmentResponse(
                entity.getId(),
                entity.getPatchId(),
                entity.getStage(),
                entity.getFileName(),
                entity.getFileUrl(),
                entity.getFileHash(),
                entity.getFileSize(),
                entity.getUploaderId(),
                entity.getScanStatus(),
                entity.getCreatedAt()
        );
    }
}
