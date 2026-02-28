package com.patches.plm.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "mail_template")
public class MailTemplateEntity extends AbstractAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "template_code", nullable = false, length = 64)
    private String templateCode;

    @Column(name = "event_code", nullable = false, length = 64)
    private String eventCode;

    @Column(name = "subject_tpl", nullable = false, columnDefinition = "text")
    private String subjectTpl;

    @Column(name = "body_tpl", nullable = false, columnDefinition = "text")
    private String bodyTpl;

    @Column(name = "content_type", nullable = false, length = 16)
    private String contentType = "TEXT";

    @Column(name = "lang", nullable = false, length = 16)
    private String lang = "zh-CN";

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

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

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getSubjectTpl() {
        return subjectTpl;
    }

    public void setSubjectTpl(String subjectTpl) {
        this.subjectTpl = subjectTpl;
    }

    public String getBodyTpl() {
        return bodyTpl;
    }

    public void setBodyTpl(String bodyTpl) {
        this.bodyTpl = bodyTpl;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExtProps() {
        return extProps;
    }

    public void setExtProps(String extProps) {
        this.extProps = extProps;
    }
}
