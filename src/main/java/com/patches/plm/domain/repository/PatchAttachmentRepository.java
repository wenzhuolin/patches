package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.PatchAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatchAttachmentRepository extends JpaRepository<PatchAttachmentEntity, Long> {
    List<PatchAttachmentEntity> findByTenantIdAndPatchIdOrderByCreatedAtDesc(Long tenantId, Long patchId);

    Optional<PatchAttachmentEntity> findByIdAndTenantId(Long id, Long tenantId);
}
