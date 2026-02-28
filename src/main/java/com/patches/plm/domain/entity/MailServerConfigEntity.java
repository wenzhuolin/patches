package com.patches.plm.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "mail_server_config")
public class MailServerConfigEntity extends AbstractAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "config_name", nullable = false, length = 64)
    private String configName;

    @Column(name = "smtp_host", nullable = false, length = 255)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;

    @Column(name = "protocol", nullable = false, length = 16)
    private String protocol = "smtp";

    @Column(name = "username", length = 128)
    private String username;

    @Column(name = "password_cipher", columnDefinition = "text")
    private String passwordCipher;

    @Column(name = "sender_email", nullable = false, length = 128)
    private String senderEmail;

    @Column(name = "sender_name", length = 128)
    private String senderName;

    @Column(name = "ssl_enabled", nullable = false)
    private boolean sslEnabled;

    @Column(name = "starttls_enabled", nullable = false)
    private boolean starttlsEnabled;

    @Column(name = "auth_enabled", nullable = false)
    private boolean authEnabled = true;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs = 10000;

    @Column(name = "is_default", nullable = false)
    private boolean defaultConfig;

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

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordCipher() {
        return passwordCipher;
    }

    public void setPasswordCipher(String passwordCipher) {
        this.passwordCipher = passwordCipher;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public boolean isStarttlsEnabled() {
        return starttlsEnabled;
    }

    public void setStarttlsEnabled(boolean starttlsEnabled) {
        this.starttlsEnabled = starttlsEnabled;
    }

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public void setAuthEnabled(boolean authEnabled) {
        this.authEnabled = authEnabled;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isDefaultConfig() {
        return defaultConfig;
    }

    public void setDefaultConfig(boolean defaultConfig) {
        this.defaultConfig = defaultConfig;
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
