package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.QaTaskEntity;
import com.patches.plm.domain.enums.QaTaskStatus;
import com.patches.plm.domain.enums.StageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QaTaskRepository extends JpaRepository<QaTaskEntity, Long> {
    List<QaTaskEntity> findByTenantIdAndPatchIdAndStageOrderBySequenceNoAsc(Long tenantId, Long patchId, StageType stage);

    Optional<QaTaskEntity> findByIdAndTenantId(Long id, Long tenantId);

    List<QaTaskEntity> findByTenantIdAndStatusAndAssigneeIdIn(Long tenantId, QaTaskStatus status, List<String> assigneeIds);
}
