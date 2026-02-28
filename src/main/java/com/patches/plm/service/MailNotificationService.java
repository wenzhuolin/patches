package com.patches.plm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patches.plm.api.dto.*;
import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.*;
import com.patches.plm.domain.repository.*;
import com.patches.plm.service.notification.MailNotifyCommand;
import com.patches.plm.web.RequestContext;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Properties;

@Service
public class MailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MailNotificationService.class);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([a-zA-Z0-9_.-]+)}");

    private final MailServerConfigRepository mailServerConfigRepository;
    private final MailTemplateRepository mailTemplateRepository;
    private final MailEventPolicyRepository mailEventPolicyRepository;
    private final MailSendLogRepository mailSendLogRepository;
    private final MailSendTaskRepository mailSendTaskRepository;
    private final UserRoleRelationRepository userRoleRelationRepository;
    private final SysUserRepository sysUserRepository;
    private final ObjectMapper objectMapper;
    private final ConfigAuditLogService configAuditLogService;

    public MailNotificationService(MailServerConfigRepository mailServerConfigRepository,
                                   MailTemplateRepository mailTemplateRepository,
                                   MailEventPolicyRepository mailEventPolicyRepository,
                                   MailSendLogRepository mailSendLogRepository,
                                   MailSendTaskRepository mailSendTaskRepository,
                                   UserRoleRelationRepository userRoleRelationRepository,
                                   SysUserRepository sysUserRepository,
                                   ObjectMapper objectMapper,
                                   ConfigAuditLogService configAuditLogService) {
        this.mailServerConfigRepository = mailServerConfigRepository;
        this.mailTemplateRepository = mailTemplateRepository;
        this.mailEventPolicyRepository = mailEventPolicyRepository;
        this.mailSendLogRepository = mailSendLogRepository;
        this.mailSendTaskRepository = mailSendTaskRepository;
        this.userRoleRelationRepository = userRoleRelationRepository;
        this.sysUserRepository = sysUserRepository;
        this.objectMapper = objectMapper;
        this.configAuditLogService = configAuditLogService;
    }

    @Transactional
    public MailServerConfigResponse upsertServerConfig(Long tenantId, MailServerConfigUpsertRequest request, RequestContext context) {
        assertAdmin(context);
        MailServerConfigEntity entity = mailServerConfigRepository
                .findByTenantIdAndConfigName(tenantId, request.configName())
                .orElseGet(MailServerConfigEntity::new);
        boolean isNew = entity.getId() == null;
        MailServerConfigEntity before = cloneServer(entity);

        entity.setTenantId(tenantId);
        entity.setConfigName(request.configName());
        entity.setSmtpHost(request.smtpHost());
        entity.setSmtpPort(request.smtpPort() == null ? 25 : request.smtpPort());
        entity.setProtocol(request.protocol() == null || request.protocol().isBlank() ? "smtp" : request.protocol().toLowerCase(Locale.ROOT));
        entity.setUsername(request.username());
        if (request.password() != null) {
            entity.setPasswordCipher(request.password());
        }
        entity.setSenderEmail(request.senderEmail());
        entity.setSenderName(request.senderName());
        entity.setSslEnabled(Boolean.TRUE.equals(request.sslEnabled()));
        entity.setStarttlsEnabled(Boolean.TRUE.equals(request.starttlsEnabled()));
        entity.setAuthEnabled(request.authEnabled() == null || request.authEnabled());
        entity.setTimeoutMs(request.timeoutMs() == null ? 10000 : request.timeoutMs());
        entity.setDefaultConfig(Boolean.TRUE.equals(request.defaultConfig()));
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setExtProps(request.extProps());
        entity.setDeleted(false);
        if (isNew) {
            entity.setCreatedBy(context.userId());
        }
        entity.setUpdatedBy(context.userId());

        if (entity.isDefaultConfig()) {
            mailServerConfigRepository.clearDefaultByTenantId(tenantId);
        }
        MailServerConfigEntity saved = mailServerConfigRepository.save(entity);
        configAuditLogService.log("MAIL_SERVER", saved.getId(), isNew ? "CREATE" : "UPDATE", before, saved, context);
        return toServerResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MailServerConfigResponse> listServerConfigs(Long tenantId, RequestContext context) {
        assertAdmin(context);
        return mailServerConfigRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId).stream()
                .filter(item -> !item.isDeleted())
                .map(this::toServerResponse)
                .toList();
    }

    @Transactional
    public MailTemplateResponse upsertTemplate(Long tenantId, MailTemplateUpsertRequest request, RequestContext context) {
        assertAdmin(context);
        int version = request.version() == null || request.version() <= 0 ? 1 : request.version();
        MailTemplateEntity entity = mailTemplateRepository
                .findByTenantIdAndTemplateCodeAndEnabledTrueOrderByVersionDesc(tenantId, request.templateCode())
                .stream()
                .filter(item -> Objects.equals(item.getVersion(), version))
                .findFirst()
                .orElseGet(MailTemplateEntity::new);
        boolean isNew = entity.getId() == null;
        MailTemplateEntity before = cloneTemplate(entity);
        entity.setTenantId(tenantId);
        entity.setTemplateCode(request.templateCode());
        entity.setEventCode(request.eventCode());
        entity.setSubjectTpl(request.subjectTpl());
        entity.setBodyTpl(request.bodyTpl());
        entity.setContentType(request.contentType() == null || request.contentType().isBlank()
                ? "TEXT" : request.contentType().toUpperCase(Locale.ROOT));
        entity.setLang(request.lang() == null || request.lang().isBlank() ? "zh-CN" : request.lang());
        entity.setVersion(version);
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setExtProps(request.extProps());
        entity.setDeleted(false);
        if (isNew) {
            entity.setCreatedBy(context.userId());
        }
        entity.setUpdatedBy(context.userId());
        MailTemplateEntity saved = mailTemplateRepository.save(entity);
        configAuditLogService.log("MAIL_TEMPLATE", saved.getId(), isNew ? "CREATE" : "UPDATE", before, saved, context);
        return toTemplateResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MailTemplateResponse> listTemplates(Long tenantId, RequestContext context) {
        assertAdmin(context);
        return mailTemplateRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId).stream()
                .filter(item -> !item.isDeleted())
                .map(this::toTemplateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MailTemplateRenderResponse renderTemplate(Long tenantId, MailTemplateRenderRequest request, RequestContext context) {
        assertAdmin(context);
        MailTemplateEntity template = mailTemplateRepository
                .findByTenantIdAndTemplateCodeAndEnabledTrueOrderByVersionDesc(tenantId, request.templateCode())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "邮件模板不存在"));
        Map<String, Object> model = toModelMap(request.model());
        return new MailTemplateRenderResponse(
                render(template.getSubjectTpl(), model),
                render(template.getBodyTpl(), model)
        );
    }

    @Transactional
    public MailEventPolicyResponse upsertEventPolicy(Long tenantId, MailEventPolicyUpsertRequest request, RequestContext context) {
        assertAdmin(context);
        MailEventPolicyEntity entity = mailEventPolicyRepository
                .findByTenantIdAndEventCode(tenantId, request.eventCode())
                .orElseGet(MailEventPolicyEntity::new);
        boolean isNew = entity.getId() == null;
        MailEventPolicyEntity before = clonePolicy(entity);

        entity.setTenantId(tenantId);
        entity.setEventCode(request.eventCode());
        entity.setTemplateCode(request.templateCode());
        entity.setToRoles(toJsonArray(request.toRoleCodes()));
        entity.setCcRoles(toJsonArray(request.ccRoleCodes()));
        entity.setIncludeOwner(request.includeOwner() == null || request.includeOwner());
        entity.setIncludeOperator(Boolean.TRUE.equals(request.includeOperator()));
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setDeleted(false);
        if (isNew) {
            entity.setCreatedBy(context.userId());
        }
        entity.setUpdatedBy(context.userId());
        MailEventPolicyEntity saved = mailEventPolicyRepository.save(entity);
        configAuditLogService.log("MAIL_EVENT_POLICY", saved.getId(), isNew ? "CREATE" : "UPDATE", before, saved, context);
        return toPolicyResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MailEventPolicyResponse> listEventPolicies(Long tenantId, RequestContext context) {
        assertAdmin(context);
        return mailEventPolicyRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId).stream()
                .filter(item -> !item.isDeleted())
                .map(this::toPolicyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MailSendLogResponse> listSendLogs(Long tenantId, Integer limit, RequestContext context) {
        assertAdmin(context);
        int size = limit == null ? 50 : Math.min(Math.max(limit, 1), 200);
        return mailSendLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, size))
                .stream().map(this::toLogResponse).toList();
    }

    @Transactional
    public MailSendLogResponse resend(Long tenantId, Long logId, RequestContext context) {
        assertAdmin(context);
        MailSendLogEntity logEntity = mailSendLogRepository.findByTenantIdAndId(tenantId, logId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "邮件日志不存在"));
        logEntity.setStatus("RETRY");
        logEntity.setNextRetryAt(OffsetDateTime.now());
        logEntity.setErrorCode(null);
        logEntity.setErrorMessage(null);
        MailSendLogEntity saved = mailSendLogRepository.save(logEntity);
        MailSendTaskEntity task = mailSendTaskRepository.findByLogId(saved.getId()).orElseGet(MailSendTaskEntity::new);
        task.setTenantId(saved.getTenantId());
        task.setLogId(saved.getId());
        task.setTaskStatus("PENDING");
        task.setAvailableAt(OffsetDateTime.now());
        task.setLockedAt(null);
        task.setLockedBy(null);
        mailSendTaskRepository.save(task);
        configAuditLogService.log("MAIL_SEND_LOG", saved.getId(), "RESEND", null, saved, context);
        return toLogResponse(saved);
    }

    @Transactional
    public void safeNotify(MailNotifyCommand command) {
        try {
            if (command.idempotencyKey() != null
                    && !command.idempotencyKey().isBlank()
                    && mailSendLogRepository.existsByTenantIdAndIdempotencyKey(command.tenantId(), command.idempotencyKey())) {
                return;
            }
            MailEventPolicyEntity policy = mailEventPolicyRepository
                    .findByTenantIdAndEventCodeAndEnabledTrue(command.tenantId(), command.eventCode())
                    .filter(item -> !item.isDeleted())
                    .orElse(null);
            MailTemplateEntity template = chooseTemplate(command.tenantId(), command.eventCode(), policy);
            if (template == null) {
                return;
            }
            RecipientAddress recipients = resolveRecipients(command, policy);
            if (recipients.to().isEmpty()) {
                return;
            }
            Map<String, Object> model = command.model() == null ? Map.of() : command.model();
            String subject = render(template.getSubjectTpl(), model);
            String body = render(template.getBodyTpl(), model);

            MailSendLogEntity sendLog = new MailSendLogEntity();
            sendLog.setTenantId(command.tenantId());
            sendLog.setBizType(command.bizType());
            sendLog.setBizId(command.bizId());
            sendLog.setEventCode(command.eventCode());
            sendLog.setTemplateId(template.getId());
            sendLog.setMailTo(String.join(",", recipients.to()));
            sendLog.setMailCc(String.join(",", recipients.cc()));
            sendLog.setSubjectRendered(subject);
            sendLog.setBodyRendered(body);
            sendLog.setStatus("PENDING");
            sendLog.setRetryCount(0);
            sendLog.setMaxRetry(5);
            sendLog.setNextRetryAt(OffsetDateTime.now());
            sendLog.setIdempotencyKey(command.idempotencyKey());
            MailSendLogEntity saved = mailSendLogRepository.save(sendLog);

            MailSendTaskEntity task = new MailSendTaskEntity();
            task.setTenantId(command.tenantId());
            task.setLogId(saved.getId());
            task.setTaskStatus("PENDING");
            task.setAvailableAt(OffsetDateTime.now());
            mailSendTaskRepository.save(task);
        } catch (Exception ex) {
            log.warn("mail notify enqueue failed, tenant={}, event={}, bizType={}, bizId={}, reason={}",
                    command.tenantId(), command.eventCode(), command.bizType(), command.bizId(), ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${app.notify.mail.worker-interval-ms:5000}")
    @Transactional
    public void dispatchDueTasks() {
        List<MailSendTaskEntity> tasks = mailSendTaskRepository
                .findTop20ByTaskStatusAndAvailableAtLessThanEqualOrderByAvailableAtAsc("PENDING", OffsetDateTime.now());
        for (MailSendTaskEntity task : tasks) {
            processTask(task);
        }
    }

    private void processTask(MailSendTaskEntity task) {
        task.setTaskStatus("PROCESSING");
        task.setLockedAt(OffsetDateTime.now());
        task.setLockedBy("local-worker");
        mailSendTaskRepository.save(task);
        MailSendLogEntity logEntity = mailSendLogRepository.findById(task.getLogId()).orElse(null);
        if (logEntity == null) {
            task.setTaskStatus("FAILED");
            mailSendTaskRepository.save(task);
            return;
        }
        if (Set.of("SENT", "FAILED").contains(logEntity.getStatus())) {
            task.setTaskStatus("DONE");
            task.setLockedAt(null);
            task.setLockedBy(null);
            mailSendTaskRepository.save(task);
            return;
        }

        try {
            send(logEntity);
            logEntity.setStatus("SENT");
            logEntity.setSentAt(OffsetDateTime.now());
            logEntity.setErrorCode(null);
            logEntity.setErrorMessage(null);
            logEntity.setNextRetryAt(null);
            mailSendLogRepository.save(logEntity);

            task.setTaskStatus("DONE");
            task.setLockedAt(null);
            task.setLockedBy(null);
            mailSendTaskRepository.save(task);
        } catch (Exception ex) {
            int nextRetry = (logEntity.getRetryCount() == null ? 0 : logEntity.getRetryCount()) + 1;
            logEntity.setRetryCount(nextRetry);
            logEntity.setErrorCode(ex.getClass().getSimpleName());
            logEntity.setErrorMessage(cut(ex.getMessage(), 1000));
            if (nextRetry >= logEntity.getMaxRetry()) {
                logEntity.setStatus("FAILED");
                task.setTaskStatus("FAILED");
            } else {
                long delay = Math.min(300L, (long) Math.pow(2, nextRetry) * 5L);
                OffsetDateTime nextRetryAt = OffsetDateTime.now().plusSeconds(delay);
                logEntity.setStatus("RETRY");
                logEntity.setNextRetryAt(nextRetryAt);
                task.setTaskStatus("PENDING");
                task.setAvailableAt(nextRetryAt);
            }
            mailSendLogRepository.save(logEntity);
            task.setLockedAt(null);
            task.setLockedBy(null);
            mailSendTaskRepository.save(task);
        }
    }

    private void send(MailSendLogEntity logEntity) throws Exception {
        MailServerConfigEntity config = mailServerConfigRepository
                .findByTenantIdAndDefaultConfigTrueAndEnabledTrue(logEntity.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "未配置可用的默认SMTP服务器"));
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getSmtpHost());
        sender.setPort(config.getSmtpPort());
        sender.setProtocol(config.getProtocol());
        sender.setUsername(config.getUsername());
        sender.setPassword(config.getPasswordCipher());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(config.isAuthEnabled()));
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isStarttlsEnabled()));
        props.put("mail.smtp.ssl.enable", String.valueOf(config.isSslEnabled()));
        props.put("mail.smtp.connectiontimeout", String.valueOf(config.getTimeoutMs()));
        props.put("mail.smtp.timeout", String.valueOf(config.getTimeoutMs()));
        props.put("mail.smtp.writetimeout", String.valueOf(config.getTimeoutMs()));

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        if (config.getSenderName() != null && !config.getSenderName().isBlank()) {
            helper.setFrom(config.getSenderEmail(), config.getSenderName());
        } else {
            helper.setFrom(config.getSenderEmail());
        }
        helper.setTo(splitAddresses(logEntity.getMailTo()));
        if (logEntity.getMailCc() != null && !logEntity.getMailCc().isBlank()) {
            helper.setCc(splitAddresses(logEntity.getMailCc()));
        }
        helper.setSubject(Optional.ofNullable(logEntity.getSubjectRendered()).orElse("(无主题)"));

        boolean html = false;
        if (logEntity.getTemplateId() != null) {
            html = mailTemplateRepository.findById(logEntity.getTemplateId())
                    .map(t -> "HTML".equalsIgnoreCase(t.getContentType()))
                    .orElse(false);
        }
        helper.setText(Optional.ofNullable(logEntity.getBodyRendered()).orElse(""), html);
        sender.send(message);
        logEntity.setProviderMsgId(message.getMessageID());
    }

    private MailTemplateEntity chooseTemplate(Long tenantId, String eventCode, MailEventPolicyEntity policy) {
        if (policy != null && policy.getTemplateCode() != null) {
            List<MailTemplateEntity> byCode = mailTemplateRepository
                    .findByTenantIdAndTemplateCodeAndEnabledTrueOrderByVersionDesc(tenantId, policy.getTemplateCode());
            return byCode.stream().filter(item -> !item.isDeleted()).findFirst().orElse(null);
        }
        List<MailTemplateEntity> byEvent = mailTemplateRepository
                .findByTenantIdAndEventCodeAndEnabledTrueOrderByVersionDesc(tenantId, eventCode);
        return byEvent.stream().filter(item -> !item.isDeleted()).findFirst().orElse(null);
    }

    private RecipientAddress resolveRecipients(MailNotifyCommand command, MailEventPolicyEntity policy) {
        Set<Long> toUserIds = new LinkedHashSet<>();
        Set<Long> ccUserIds = new LinkedHashSet<>();
        if (policy != null) {
            List<String> toRoles = parseRoleCodes(policy.getToRoles());
            if (!toRoles.isEmpty()) {
                toUserIds.addAll(userRoleRelationRepository.findEnabledUserIdsByRoleCodes(command.tenantId(), toRoles));
            }
            List<String> ccRoles = parseRoleCodes(policy.getCcRoles());
            if (!ccRoles.isEmpty()) {
                ccUserIds.addAll(userRoleRelationRepository.findEnabledUserIdsByRoleCodes(command.tenantId(), ccRoles));
            }
            if (policy.isIncludeOwner() && command.ownerUserId() != null) {
                toUserIds.add(command.ownerUserId());
            }
            if (policy.isIncludeOperator() && command.operatorId() != null) {
                ccUserIds.add(command.operatorId());
            }
        } else {
            if (command.ownerUserId() != null) {
                toUserIds.add(command.ownerUserId());
            }
            if (command.operatorId() != null) {
                ccUserIds.add(command.operatorId());
            }
        }
        ccUserIds.removeAll(toUserIds);

        Set<Long> allUserIds = new LinkedHashSet<>(toUserIds);
        allUserIds.addAll(ccUserIds);
        if (allUserIds.isEmpty()) {
            return new RecipientAddress(List.of(), List.of());
        }

        Map<Long, SysUserEntity> userMap = sysUserRepository.findByTenantIdAndIdIn(command.tenantId(), allUserIds)
                .stream()
                .collect(Collectors.toMap(SysUserEntity::getId, v -> v));

        List<String> toEmails = toUserIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatus()))
                .map(SysUserEntity::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();

        List<String> ccEmails = ccUserIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatus()))
                .map(SysUserEntity::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .filter(email -> !toEmails.contains(email))
                .distinct()
                .toList();

        return new RecipientAddress(toEmails, ccEmails);
    }

    private String render(String template, Map<String, Object> model) {
        if (template == null) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = model.get(key);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String[] splitAddresses(String joined) {
        return Arrays.stream(Optional.ofNullable(joined).orElse("").split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .toArray(String[]::new);
    }

    private List<String> parseRoleCodes(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> raw = objectMapper.readValue(json, new TypeReference<>() {
            });
            return raw.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(v -> !v.isBlank())
                    .map(v -> v.toUpperCase(Locale.ROOT))
                    .toList();
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String toJsonArray(List<String> values) {
        if (values == null) {
            return null;
        }
        List<String> normalized = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> v.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色列表格式非法");
        }
    }

    private Map<String, Object> toModelMap(JsonNode model) {
        if (model == null || model.isNull()) {
            return Map.of();
        }
        return objectMapper.convertValue(model, new TypeReference<>() {
        });
    }

    private MailServerConfigResponse toServerResponse(MailServerConfigEntity saved) {
        return new MailServerConfigResponse(
                saved.getId(),
                saved.getConfigName(),
                saved.getSmtpHost(),
                saved.getSmtpPort(),
                saved.getProtocol(),
                saved.getUsername(),
                saved.getSenderEmail(),
                saved.getSenderName(),
                saved.isSslEnabled(),
                saved.isStarttlsEnabled(),
                saved.isAuthEnabled(),
                saved.getTimeoutMs(),
                saved.isDefaultConfig(),
                saved.isEnabled(),
                saved.getUpdatedAt()
        );
    }

    private MailTemplateResponse toTemplateResponse(MailTemplateEntity saved) {
        return new MailTemplateResponse(
                saved.getId(),
                saved.getTemplateCode(),
                saved.getEventCode(),
                saved.getSubjectTpl(),
                saved.getBodyTpl(),
                saved.getContentType(),
                saved.getLang(),
                saved.getVersion(),
                saved.isEnabled(),
                saved.getUpdatedAt()
        );
    }

    private MailEventPolicyResponse toPolicyResponse(MailEventPolicyEntity saved) {
        return new MailEventPolicyResponse(
                saved.getId(),
                saved.getEventCode(),
                saved.getTemplateCode(),
                parseRoleCodes(saved.getToRoles()),
                parseRoleCodes(saved.getCcRoles()),
                saved.isIncludeOwner(),
                saved.isIncludeOperator(),
                saved.isEnabled(),
                saved.getUpdatedAt()
        );
    }

    private MailSendLogResponse toLogResponse(MailSendLogEntity saved) {
        return new MailSendLogResponse(
                saved.getId(),
                saved.getBizType(),
                saved.getBizId(),
                saved.getEventCode(),
                saved.getMailTo(),
                saved.getMailCc(),
                saved.getStatus(),
                saved.getRetryCount(),
                saved.getMaxRetry(),
                saved.getErrorCode(),
                saved.getErrorMessage(),
                saved.getCreatedAt(),
                saved.getSentAt()
        );
    }

    private void assertAdmin(RequestContext context) {
        if (!(context.roles().contains("SUPER_ADMIN") || context.roles().contains("LINE_ADMIN"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可执行邮件配置操作");
        }
    }

    private MailServerConfigEntity cloneServer(MailServerConfigEntity source) {
        MailServerConfigEntity copied = new MailServerConfigEntity();
        copied.setId(source.getId());
        copied.setTenantId(source.getTenantId());
        copied.setConfigName(source.getConfigName());
        copied.setSmtpHost(source.getSmtpHost());
        copied.setSmtpPort(source.getSmtpPort());
        copied.setProtocol(source.getProtocol());
        copied.setUsername(source.getUsername());
        copied.setPasswordCipher(source.getPasswordCipher());
        copied.setSenderEmail(source.getSenderEmail());
        copied.setSenderName(source.getSenderName());
        copied.setSslEnabled(source.isSslEnabled());
        copied.setStarttlsEnabled(source.isStarttlsEnabled());
        copied.setAuthEnabled(source.isAuthEnabled());
        copied.setTimeoutMs(source.getTimeoutMs());
        copied.setDefaultConfig(source.isDefaultConfig());
        copied.setEnabled(source.isEnabled());
        copied.setExtProps(source.getExtProps());
        copied.setCreatedAt(source.getCreatedAt());
        copied.setCreatedBy(source.getCreatedBy());
        copied.setUpdatedAt(source.getUpdatedAt());
        copied.setUpdatedBy(source.getUpdatedBy());
        copied.setDeleted(source.isDeleted());
        return copied;
    }

    private MailTemplateEntity cloneTemplate(MailTemplateEntity source) {
        MailTemplateEntity copied = new MailTemplateEntity();
        copied.setId(source.getId());
        copied.setTenantId(source.getTenantId());
        copied.setTemplateCode(source.getTemplateCode());
        copied.setEventCode(source.getEventCode());
        copied.setSubjectTpl(source.getSubjectTpl());
        copied.setBodyTpl(source.getBodyTpl());
        copied.setContentType(source.getContentType());
        copied.setLang(source.getLang());
        copied.setVersion(source.getVersion());
        copied.setEnabled(source.isEnabled());
        copied.setExtProps(source.getExtProps());
        copied.setCreatedAt(source.getCreatedAt());
        copied.setCreatedBy(source.getCreatedBy());
        copied.setUpdatedAt(source.getUpdatedAt());
        copied.setUpdatedBy(source.getUpdatedBy());
        copied.setDeleted(source.isDeleted());
        return copied;
    }

    private MailEventPolicyEntity clonePolicy(MailEventPolicyEntity source) {
        MailEventPolicyEntity copied = new MailEventPolicyEntity();
        copied.setId(source.getId());
        copied.setTenantId(source.getTenantId());
        copied.setEventCode(source.getEventCode());
        copied.setTemplateCode(source.getTemplateCode());
        copied.setToRoles(source.getToRoles());
        copied.setCcRoles(source.getCcRoles());
        copied.setIncludeOwner(source.isIncludeOwner());
        copied.setIncludeOperator(source.isIncludeOperator());
        copied.setEnabled(source.isEnabled());
        copied.setCreatedAt(source.getCreatedAt());
        copied.setCreatedBy(source.getCreatedBy());
        copied.setUpdatedAt(source.getUpdatedAt());
        copied.setUpdatedBy(source.getUpdatedBy());
        copied.setDeleted(source.isDeleted());
        return copied;
    }

    private String cut(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    private record RecipientAddress(List<String> to, List<String> cc) {
    }
}
