package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.RolePermissionRelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRelRepository extends JpaRepository<RolePermissionRelEntity, Long> {
    Optional<RolePermissionRelEntity> findByTenantIdAndRoleIdAndPermissionId(Long tenantId, Long roleId, Long permissionId);

    List<RolePermissionRelEntity> findByTenantIdAndRoleIdOrderByPermissionIdAsc(Long tenantId, Long roleId);

    @Query("""
            select distinct p.permCode
            from RolePermissionRelEntity rp
            join SysRoleEntity r on r.id = rp.roleId
            join PermissionDefEntity p on p.id = rp.permissionId
            where rp.tenantId = :tenantId
              and r.tenantId = :tenantId
              and p.tenantId = :tenantId
              and r.roleCode in :roleCodes
              and r.enabled = true
              and p.enabled = true
              and r.deleted = false
              and p.deleted = false
              and rp.grantType = 'ALLOW'
              and rp.deleted = false
            """)
    List<String> findGrantedPermCodes(@Param("tenantId") Long tenantId, @Param("roleCodes") List<String> roleCodes);

    @Query("""
            select case when count(rp.id) > 0 then true else false end
            from RolePermissionRelEntity rp
            join SysRoleEntity r on r.id = rp.roleId
            join PermissionDefEntity p on p.id = rp.permissionId
            where rp.tenantId = :tenantId
              and r.tenantId = :tenantId
              and p.tenantId = :tenantId
              and r.roleCode in :roleCodes
              and p.permCode = :permCode
              and r.enabled = true
              and p.enabled = true
              and r.deleted = false
              and p.deleted = false
              and rp.grantType = 'ALLOW'
              and rp.deleted = false
            """)
    boolean existsGrant(@Param("tenantId") Long tenantId,
                        @Param("roleCodes") List<String> roleCodes,
                        @Param("permCode") String permCode);
}
