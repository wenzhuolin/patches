package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.IntegrationConnectorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntegrationConnectorRepository extends JpaRepository<IntegrationConnectorEntity, Long> {
    List<IntegrationConnectorEntity> findByTenantIdOrderByIdDesc(Long tenantId);
}
