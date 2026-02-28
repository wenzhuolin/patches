package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.UserRoleRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRoleRelationRepository extends JpaRepository<UserRoleRelationEntity, Long> {
    Optional<UserRoleRelationEntity> findByTenantIdAndUserIdAndRoleCode(Long tenantId, Long userId, String roleCode);

    List<UserRoleRelationEntity> findByTenantIdAndUserIdOrderByRoleCodeAsc(Long tenantId, Long userId);

    @Query("""
            select distinct r.roleCode from UserRoleRelationEntity r
            where r.tenantId = :tenantId
              and r.userId = :userId
              and r.enabled = true
            """)
    List<String> findEnabledRoleCodes(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Query("""
            select distinct r.userId from UserRoleRelationEntity r
            where r.tenantId = :tenantId
              and r.roleCode in :roleCodes
              and r.enabled = true
            """)
    List<Long> findEnabledUserIdsByRoleCodes(@Param("tenantId") Long tenantId, @Param("roleCodes") List<String> roleCodes);
}
