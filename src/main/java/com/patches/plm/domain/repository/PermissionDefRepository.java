package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.PermissionDefEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermissionDefRepository extends JpaRepository<PermissionDefEntity, Long> {
    Optional<PermissionDefEntity> findByTenantIdAndPermCode(Long tenantId, String permCode);

    Optional<PermissionDefEntity> findByTenantIdAndId(Long tenantId, Long id);

    List<PermissionDefEntity> findByTenantIdOrderByPermCodeAsc(Long tenantId);

    List<PermissionDefEntity> findByTenantIdAndIdIn(Long tenantId, Collection<Long> ids);
}
