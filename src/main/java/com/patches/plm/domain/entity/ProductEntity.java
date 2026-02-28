package com.patches.plm.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product")
public class ProductEntity extends AbstractAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "product_code", nullable = false, length = 64)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 128)
    private String productName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "ACTIVE";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ext_props", columnDefinition = "jsonb")
    private String extProps;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExtProps() {
        return extProps;
    }

    public void setExtProps(String extProps) {
        this.extProps = extProps;
    }
}
