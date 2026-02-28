package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.UserDataScopeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDataScopeRepository extends JpaRepository<UserDataScopeEntity, Long> {

    List<UserDataScopeEntity> findByTenantIdAndUserIdAndEnabledTrue(Long tenantId, Long userId);

    Optional<UserDataScopeEntity> findByTenantIdAndUserIdAndScopeTypeAndScopeValue(
            Long tenantId, Long userId, String scopeType, String scopeValue
    );

    List<UserDataScopeEntity> findByTenantIdAndUserIdOrderByScopeTypeAscScopeValueAsc(Long tenantId, Long userId);
}
