package com.patches.plm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patches.plm.api.dto.MailTemplateRenderRequest;
import com.patches.plm.api.dto.MailTemplateRenderResponse;
import com.patches.plm.domain.entity.MailEventPolicyEntity;
import com.patches.plm.domain.entity.MailSendLogEntity;
import com.patches.plm.domain.entity.MailTemplateEntity;
import com.patches.plm.domain.entity.SysUserEntity;
import com.patches.plm.domain.repository.*;
import com.patches.plm.service.notification.MailNotifyCommand;
import com.patches.plm.web.RequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class MailNotificationServiceTest {

    private MailServerConfigRepository mailServerConfigRepository;
    private MailTemplateRepository mailTemplateRepository;
    private MailEventPolicyRepository mailEventPolicyRepository;
    private MailSendLogRepository mailSendLogRepository;
    private MailSendTaskRepository mailSendTaskRepository;
    private UserRoleRelationRepository userRoleRelationRepository;
    private SysUserRepository sysUserRepository;
    private ConfigAuditLogService configAuditLogService;
    private MailNotificationService mailNotificationService;

    @BeforeEach
    void setUp() {
        mailServerConfigRepository = Mockito.mock(MailServerConfigRepository.class);
        mailTemplateRepository = Mockito.mock(MailTemplateRepository.class);
        mailEventPolicyRepository = Mockito.mock(MailEventPolicyRepository.class);
        mailSendLogRepository = Mockito.mock(MailSendLogRepository.class);
        mailSendTaskRepository = Mockito.mock(MailSendTaskRepository.class);
        userRoleRelationRepository = Mockito.mock(UserRoleRelationRepository.class);
        sysUserRepository = Mockito.mock(SysUserRepository.class);
        configAuditLogService = Mockito.mock(ConfigAuditLogService.class);
        mailNotificationService = new MailNotificationService(
                mailServerConfigRepository,
                mailTemplateRepository,
                mailEventPolicyRepository,
                mailSendLogRepository,
                mailSendTaskRepository,
                userRoleRelationRepository,
                sysUserRepository,
                new ObjectMapper(),
                configAuditLogService
        );
    }

    @Test
    void shouldEnqueueMailLogAndTaskWhenPolicyTemplateAndRecipientsReady() {
        MailEventPolicyEntity policy = new MailEventPolicyEntity();
        policy.setTenantId(1L);
        policy.setEventCode("PATCH_CREATED");
        policy.setTemplateCode("PATCH_CREATED_TPL");
        policy.setToRoles("[\"PM\"]");
        policy.setIncludeOwner(true);
        policy.setIncludeOperator(false);
        policy.setEnabled(true);
        policy.setDeleted(false);

        MailTemplateEntity template = new MailTemplateEntity();
        template.setId(9L);
        template.setTenantId(1L);
        template.setTemplateCode("PATCH_CREATED_TPL");
        template.setSubjectTpl("Patch ${patchNo}");
        template.setBodyTpl("State ${currentState}");
        template.setContentType("TEXT");
        template.setEnabled(true);
        template.setDeleted(false);

        SysUserEntity owner = new SysUserEntity();
        owner.setId(1001L);
        owner.setTenantId(1L);
        owner.setStatus("ACTIVE");
        owner.setEmail("owner@example.com");

        Mockito.when(mailSendLogRepository.existsByTenantIdAndIdempotencyKey(1L, "idp-1"))
                .thenReturn(false);
        Mockito.when(mailEventPolicyRepository.findByTenantIdAndEventCodeAndEnabledTrue(1L, "PATCH_CREATED"))
                .thenReturn(Optional.of(policy));
        Mockito.when(mailTemplateRepository.findByTenantIdAndTemplateCodeAndEnabledTrueOrderByVersionDesc(1L, "PATCH_CREATED_TPL"))
                .thenReturn(List.of(template));
        Mockito.when(userRoleRelationRepository.findEnabledUserIdsByRoleCodes(1L, List.of("PM")))
                .thenReturn(List.of(1001L));
        Mockito.when(sysUserRepository.findByTenantIdAndIdIn(1L, Set.of(1001L)))
                .thenReturn(List.of(owner));

        Mockito.when(mailSendLogRepository.save(Mockito.any(MailSendLogEntity.class)))
                .thenAnswer(invocation -> {
                    MailSendLogEntity entity = invocation.getArgument(0);
                    entity.setId(123L);
                    return entity;
                });

        mailNotificationService.safeNotify(new MailNotifyCommand(
                1L, "PATCH_CREATED", "PATCH", 99L, 2001L, 1001L,
                Map.of("patchNo", "P20260227-0001", "currentState", "DRAFT"), "idp-1"
        ));

        ArgumentCaptor<MailSendLogEntity> logCaptor = ArgumentCaptor.forClass(MailSendLogEntity.class);
        Mockito.verify(mailSendLogRepository, Mockito.times(1)).save(logCaptor.capture());
        MailSendLogEntity saved = logCaptor.getValue();
        Assertions.assertEquals("PATCH_CREATED", saved.getEventCode());
        Assertions.assertTrue(saved.getMailTo().contains("owner@example.com"));
        Assertions.assertEquals("Patch P20260227-0001", saved.getSubjectRendered());
        Mockito.verify(mailSendTaskRepository, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    void shouldRenderTemplatePreview() {
        MailTemplateEntity template = new MailTemplateEntity();
        template.setTemplateCode("DEMO");
        template.setSubjectTpl("[${state}] ${patchNo}");
        template.setBodyTpl("Patch ${patchNo} is ${state}");
        template.setEnabled(true);
        template.setDeleted(false);
        Mockito.when(mailTemplateRepository.findByTenantIdAndTemplateCodeAndEnabledTrueOrderByVersionDesc(1L, "DEMO"))
                .thenReturn(List.of(template));

        ObjectNode model = new ObjectMapper().createObjectNode();
        model.put("state", "REVIEWING");
        model.put("patchNo", "P001");
        MailTemplateRenderResponse response = mailNotificationService.renderTemplate(
                1L,
                new MailTemplateRenderRequest("DEMO", model),
                RequestContext.of(1L, 1L, Set.of("SUPER_ADMIN"), "req", "trace", "127.0.0.1", "ut")
        );
        Assertions.assertEquals("[REVIEWING] P001", response.subject());
        Assertions.assertTrue(response.body().contains("P001"));
    }
}
