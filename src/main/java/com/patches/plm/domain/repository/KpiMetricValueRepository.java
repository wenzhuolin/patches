package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.KpiMetricValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KpiMetricValueRepository extends JpaRepository<KpiMetricValueEntity, Long> {
    Optional<KpiMetricValueEntity> findFirstByTenantIdAndPatchIdAndMetricKeyOrderByCollectedAtDesc(
            Long tenantId, Long patchId, String metricKey
    );
}
