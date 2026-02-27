package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.MailServerConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MailServerConfigRepository extends JpaRepository<MailServerConfigEntity, Long> {

    List<MailServerConfigEntity> findByTenantIdOrderByUpdatedAtDesc(Long tenantId);

    Optional<MailServerConfigEntity> findByTenantIdAndId(Long tenantId, Long id);

    Optional<MailServerConfigEntity> findByTenantIdAndConfigName(Long tenantId, String configName);

    Optional<MailServerConfigEntity> findByTenantIdAndDefaultConfigTrueAndEnabledTrue(Long tenantId);

    @Modifying
    @Query("update MailServerConfigEntity c set c.defaultConfig = false where c.tenantId = :tenantId")
    int clearDefaultByTenantId(@Param("tenantId") Long tenantId);
}
