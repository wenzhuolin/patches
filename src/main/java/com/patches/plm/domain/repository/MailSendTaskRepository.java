package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.MailSendTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MailSendTaskRepository extends JpaRepository<MailSendTaskEntity, Long> {

    List<MailSendTaskEntity> findTop20ByTaskStatusAndAvailableAtLessThanEqualOrderByAvailableAtAsc(String taskStatus, OffsetDateTime now);

    Optional<MailSendTaskEntity> findByLogId(Long logId);
}
