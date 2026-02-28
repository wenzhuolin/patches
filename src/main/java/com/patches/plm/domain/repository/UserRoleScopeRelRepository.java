package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.UserRoleScopeRelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRoleScopeRelRepository extends JpaRepository<UserRoleScopeRelEntity, Long> {

    Optional<UserRoleScopeRelEntity> findByTenantIdAndUserIdAndRoleIdAndScopeLevelAndScenarioIdAndProductId(
            Long tenantId, Long userId, Long roleId, String scopeLevel, Long scenarioId, Long productId
    );

    List<UserRoleScopeRelEntity> findByTenantIdAndUserIdAndStatusOrderByIdDesc(Long tenantId, Long userId, String status);

    @Query("""
            select distinct r.roleCode
            from UserRoleScopeRelEntity rel
            join SysRoleEntity r on r.id = rel.roleId
            where rel.tenantId = :tenantId
              and rel.userId = :userId
              and rel.status = 'ACTIVE'
              and rel.deleted = false
              and r.deleted = false
              and r.enabled = true
            """)
    List<String> findEnabledRoleCodes(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
