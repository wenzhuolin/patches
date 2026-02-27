package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.SysUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUserEntity, Long> {
    Optional<SysUserEntity> findByIdAndTenantId(Long id, Long tenantId);

    Optional<SysUserEntity> findByTenantIdAndUsername(Long tenantId, String username);

    List<SysUserEntity> findByTenantIdOrderByIdDesc(Long tenantId);
}
