package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.PatchTransitionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatchTransitionLogRepository extends JpaRepository<PatchTransitionLogEntity, Long> {
    boolean existsByPatchIdAndRequestId(Long patchId, String requestId);

    java.util.List<PatchTransitionLogEntity> findByTenantIdAndPatchIdOrderByCreatedAtDesc(Long tenantId, Long patchId);
}
