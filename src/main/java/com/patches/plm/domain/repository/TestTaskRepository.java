package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.TestTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestTaskRepository extends JpaRepository<TestTaskEntity, Long> {
    java.util.Optional<TestTaskEntity> findByIdAndTenantId(Long id, Long tenantId);

    java.util.List<TestTaskEntity> findByTenantIdAndPatchId(Long tenantId, Long patchId);
}
