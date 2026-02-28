package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.DataScopePolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataScopePolicyRepository extends JpaRepository<DataScopePolicyEntity, Long> {
    List<DataScopePolicyEntity> findByTenantIdAndRoleIdAndResourceTypeAndEnabledTrue(Long tenantId, Long roleId, String resourceType);
}
