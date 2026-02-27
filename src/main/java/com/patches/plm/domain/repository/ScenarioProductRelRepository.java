package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.ScenarioProductRelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioProductRelRepository extends JpaRepository<ScenarioProductRelEntity, Long> {
    Optional<ScenarioProductRelEntity> findByTenantIdAndScenarioIdAndProductId(Long tenantId, Long scenarioId, Long productId);

    List<ScenarioProductRelEntity> findByTenantIdAndScenarioIdAndStatusOrderByProductIdAsc(Long tenantId, Long scenarioId, String status);

    List<ScenarioProductRelEntity> findByTenantIdAndProductIdAndStatusOrderByScenarioIdAsc(Long tenantId, Long productId, String status);
}
