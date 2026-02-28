package com.patches.plm.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "mail_event_policy")
public class MailEventPolicyEntity extends AbstractAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "event_code", nullable = false, length = 64)
    private String eventCode;

    @Column(name = "template_code", nullable = false, length = 64)
    private String templateCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "to_roles", columnDefinition = "jsonb")
    private String toRoles;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cc_roles", columnDefinition = "jsonb")
    private String ccRoles;

    @Column(name = "include_owner", nullable = false)
    private boolean includeOwner = true;

    @Column(name = "include_operator", nullable = false)
    private boolean includeOperator;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

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

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getToRoles() {
        return toRoles;
    }

    public void setToRoles(String toRoles) {
        this.toRoles = toRoles;
    }

    public String getCcRoles() {
        return ccRoles;
    }

    public void setCcRoles(String ccRoles) {
        this.ccRoles = ccRoles;
    }

    public boolean isIncludeOwner() {
        return includeOwner;
    }

    public void setIncludeOwner(boolean includeOwner) {
        this.includeOwner = includeOwner;
    }

    public boolean isIncludeOperator() {
        return includeOperator;
    }

    public void setIncludeOperator(boolean includeOperator) {
        this.includeOperator = includeOperator;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
