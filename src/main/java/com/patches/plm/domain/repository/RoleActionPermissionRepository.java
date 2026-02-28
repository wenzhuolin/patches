package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.RoleActionPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleActionPermissionRepository extends JpaRepository<RoleActionPermissionEntity, Long> {

    @Query("""
            select distinct r.roleCode from RoleActionPermissionEntity r
            where r.tenantId = :tenantId
              and r.action = :action
              and r.enabled = true
            """)
    List<String> findEnabledRolesByAction(@Param("tenantId") Long tenantId, @Param("action") String action);

    Optional<RoleActionPermissionEntity> findByTenantIdAndRoleCodeAndAction(Long tenantId, String roleCode, String action);

    List<RoleActionPermissionEntity> findByTenantIdAndActionOrderByRoleCodeAsc(Long tenantId, String action);
}
