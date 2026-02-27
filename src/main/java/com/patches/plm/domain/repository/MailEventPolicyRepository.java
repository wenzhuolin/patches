package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.MailEventPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MailEventPolicyRepository extends JpaRepository<MailEventPolicyEntity, Long> {

    Optional<MailEventPolicyEntity> findByTenantIdAndEventCode(Long tenantId, String eventCode);

    Optional<MailEventPolicyEntity> findByTenantIdAndEventCodeAndEnabledTrue(Long tenantId, String eventCode);

    List<MailEventPolicyEntity> findByTenantIdOrderByUpdatedAtDesc(Long tenantId);
}
