package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.PatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PatchRepository extends JpaRepository<PatchEntity, Long> {
    Optional<PatchEntity> findByIdAndTenantId(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PatchEntity p where p.id = :id and p.tenantId = :tenantId")
    Optional<PatchEntity> lockByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    long countByTenantId(Long tenantId);

    List<PatchEntity> findByTenantIdOrderByUpdatedAtDesc(Long tenantId);

    List<PatchEntity> findByTenantIdAndCurrentStateOrderByUpdatedAtDesc(Long tenantId, com.patches.plm.domain.enums.PatchState currentState);
}
