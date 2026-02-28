package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.KpiEvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KpiEvaluationRepository extends JpaRepository<KpiEvaluationEntity, Long> {
}
