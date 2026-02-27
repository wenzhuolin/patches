package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.SysRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysRoleRepository extends JpaRepository<SysRoleEntity, Long> {
    Optional<SysRoleEntity> findByTenantIdAndRoleCode(Long tenantId, String roleCode);

    List<SysRoleEntity> findByTenantIdOrderByRoleCodeAsc(Long tenantId);
}
