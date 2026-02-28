package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.QaDecisionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QaDecisionLogRepository extends JpaRepository<QaDecisionLogEntity, Long> {
}
