package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.MailTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MailTemplateRepository extends JpaRepository<MailTemplateEntity, Long> {

    List<MailTemplateEntity> findByTenantIdOrderByUpdatedAtDesc(Long tenantId);

    Optional<MailTemplateEntity> findByTenantIdAndId(Long tenantId, Long id);

    List<MailTemplateEntity> findByTenantIdAndEventCodeAndEnabledTrueOrderByVersionDesc(Long tenantId, String eventCode);

    List<MailTemplateEntity> findByTenantIdAndTemplateCodeAndEnabledTrueOrderByVersionDesc(Long tenantId, String templateCode);
}
