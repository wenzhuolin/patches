package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findByTenantIdAndProductCode(Long tenantId, String productCode);

    Optional<ProductEntity> findByTenantIdAndId(Long tenantId, Long id);

    List<ProductEntity> findByTenantIdOrderByUpdatedAtDesc(Long tenantId);
}
