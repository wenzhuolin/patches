package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.QaPolicyEntity;
import com.patches.plm.domain.enums.StageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface QaPolicyRepository extends JpaRepository<QaPolicyEntity, Long> {

    List<QaPolicyEntity> findByTenantIdOrderByStageAsc(Long tenantId);

    @Query("""
            select p from QaPolicyEntity p
            where p.tenantId = :tenantId
              and p.stage = :stage
              and p.enabled = true
              and (p.effectiveFrom is null or p.effectiveFrom <= :now)
              and (p.effectiveTo is null or p.effectiveTo >= :now)
              and (
                p.scopeType = 'GLOBAL'
                or (p.scopeType = 'PRODUCT_LINE' and p.scopeValue = :productLineId)
              )
            """)
    List<QaPolicyEntity> findActivePolicies(@Param("tenantId") Long tenantId,
                                            @Param("stage") StageType stage,
                                            @Param("productLineId") String productLineId,
                                            @Param("now") OffsetDateTime now);
}
