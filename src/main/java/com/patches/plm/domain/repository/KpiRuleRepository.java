package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.KpiRuleEntity;
import com.patches.plm.domain.enums.GateType;
import com.patches.plm.domain.enums.StageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface KpiRuleRepository extends JpaRepository<KpiRuleEntity, Long> {

    @Query("""
            select r from KpiRuleEntity r
            where r.tenantId = :tenantId
              and r.stage = :stage
              and r.gateType = :gateType
              and r.enabled = true
              and (r.effectiveFrom is null or r.effectiveFrom <= :now)
              and (r.effectiveTo is null or r.effectiveTo >= :now)
              and (
                r.scopeType = 'GLOBAL'
                or (r.scopeType = 'PRODUCT_LINE' and r.scopeValue = :productLineId)
              )
            order by r.priority asc
            """)
    List<KpiRuleEntity> findActiveRules(@Param("tenantId") Long tenantId,
                                        @Param("stage") StageType stage,
                                        @Param("gateType") GateType gateType,
                                        @Param("productLineId") String productLineId,
                                        @Param("now") OffsetDateTime now);

    List<KpiRuleEntity> findByTenantIdOrderByStageAscPriorityAsc(Long tenantId);
}
