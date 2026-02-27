package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.MailSendLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MailSendLogRepository extends JpaRepository<MailSendLogEntity, Long> {

    Optional<MailSendLogEntity> findByTenantIdAndId(Long tenantId, Long id);

    boolean existsByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);

    List<MailSendLogEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    @Query("""
            select l from MailSendLogEntity l
            where l.status in ('PENDING','RETRY')
              and (l.nextRetryAt is null or l.nextRetryAt <= :now)
            order by l.createdAt asc
            """)
    List<MailSendLogEntity> findDueLogs(@Param("now") OffsetDateTime now, Pageable pageable);
}
