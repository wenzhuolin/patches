package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.ReviewSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewSessionRepository extends JpaRepository<ReviewSessionEntity, Long> {
    java.util.Optional<ReviewSessionEntity> findByIdAndTenantId(Long id, Long tenantId);
}
