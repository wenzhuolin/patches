package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.DeliveryScenarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryScenarioRepository extends JpaRepository<DeliveryScenarioEntity, Long> {
    Optional<DeliveryScenarioEntity> findByTenantIdAndScenarioCode(Long tenantId, String scenarioCode);

    Optional<DeliveryScenarioEntity> findByTenantIdAndId(Long tenantId, Long id);

    List<DeliveryScenarioEntity> findByTenantIdOrderByUpdatedAtDesc(Long tenantId);
}
