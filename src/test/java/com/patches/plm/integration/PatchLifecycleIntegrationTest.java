package com.patches.plm.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PatchLifecycleIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("patches")
            .withUsername("patches")
            .withPassword("patches");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("app.notify.mail.worker-interval-ms", () -> "3600000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCompleteReviewWithKpiAndQaGate() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String adminHeadersRole = "SUPER_ADMIN";

        // 1) 准备角色/用户并绑定PRODUCT_LINE_QA，用于测试无X-Roles时的DB回填角色
        String roleBody = """
                {"roleCode":"PRODUCT_LINE_QA","roleName":"产品线QA","enabled":true}
                """;
        mockMvc.perform(post("/api/v1/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody)
                        .headers(headers(1L, 1L, adminHeadersRole)))
                .andExpect(status().isOk());

        String userBody = """
                {"username":"qa_%s","displayName":"QA User %s","status":"ACTIVE"}
                """.formatted(suffix, suffix);
        var createUserResp = mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody)
                        .headers(headers(1L, 1L, adminHeadersRole)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Long qaUserId = json(createUserResp).path("data").path("id").asLong();
        Assertions.assertTrue(qaUserId > 0);

        String assignBody = """
                {"userId":%d,"roleCode":"PRODUCT_LINE_QA","enabled":true}
                """.formatted(qaUserId);
        mockMvc.perform(post("/api/v1/admin/users/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody)
                        .headers(headers(1L, 1L, adminHeadersRole)))
                .andExpect(status().isOk());

        // 2) 配置KPI与QA策略
        String kpiRuleBody = """
                {
                  "ruleCode":"REVIEW_PASS_RATE_%s",
                  "stage":"REVIEW",
                  "gateType":"EXIT",
                  "metricKey":"code_review_pass_rate",
                  "compareOp":"GE",
                  "thresholdValue":90,
                  "required":true,
                  "missingDataPolicy":"FAIL",
                  "priority":1,
                  "scopeType":"GLOBAL",
                  "enabled":true
                }
                """.formatted(suffix);
        mockMvc.perform(post("/api/v1/kpi/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(kpiRuleBody)
                        .headers(headers(1L, 1L, adminHeadersRole)))
                .andExpect(status().isOk());

        String qaPolicyBody = """
                {
                  "stage":"REVIEW",
                  "approvalMode":"ALL",
                  "requiredLevels":"PRODUCT_LINE_QA",
                  "scopeType":"GLOBAL",
                  "enabled":true
                }
                """;
        mockMvc.perform(post("/api/v1/qa/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qaPolicyBody)
                        .headers(headers(1L, 1L, adminHeadersRole)))
                .andExpect(status().isOk());

        // 3) 创建补丁并推进到评审
        String createPatchBody = """
                {
                  "productLineId":101,
                  "title":"支付修复-%s",
                  "description":"integration test",
                  "severity":"HIGH",
                  "priority":"P1",
                  "sourceVersion":"v1",
                  "targetVersion":"v2",
                  "ownerPmId":1001
                }
                """.formatted(suffix);
        var createPatchResp = mockMvc.perform(post("/api/v1/patches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPatchBody)
                        .headers(headers(1L, 1001L, "PM")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Long patchId = json(createPatchResp).path("data").path("patchId").asLong();
        Assertions.assertTrue(patchId > 0);

        mockMvc.perform(post("/api/v1/patches/{id}/actions", patchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"SUBMIT_REVIEW\"}")
                        .headers(headersWithIdempotency(1L, 1001L, "PM")))
                .andExpect(status().isOk());

        // 4) 缺指标导致KPI卡点失败
        var approveWithoutMetric = mockMvc.perform(post("/api/v1/patches/{id}/actions", patchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE_REVIEW\"}")
                        .headers(headersWithIdempotency(1L, 2001L, "REVIEWER")))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertEquals("KPI_GATE_FAILED", json(approveWithoutMetric).path("code").asText());

        // 5) 上报KPI后仍会被QA卡住
        String metricBody = """
                {"metrics":[{"metricKey":"code_review_pass_rate","metricValue":95.0,"sourceType":"MANUAL"}]}
                """;
        mockMvc.perform(post("/api/v1/patches/{id}/metrics", patchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(metricBody)
                        .headers(headers(1L, 2001L, "REVIEWER")))
                .andExpect(status().isOk());

        var approveNeedQa = mockMvc.perform(post("/api/v1/patches/{id}/actions", patchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE_REVIEW\"}")
                        .headers(headersWithIdempotency(1L, 2001L, "REVIEWER")))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertEquals("QA_GATE_FAILED", json(approveNeedQa).path("code").asText());

        // 6) QA审批（不传X-Roles，依赖DB角色回填）
        var pendingQaResp = mockMvc.perform(get("/api/v1/qa/tasks/my-pending")
                        .headers(headers(1L, qaUserId, null)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode pendingArray = json(pendingQaResp).path("data");
        Assertions.assertTrue(pendingArray.isArray() && pendingArray.size() > 0);
        Long qaTaskId = pendingArray.get(0).path("qaTaskId").asLong();

        mockMvc.perform(post("/api/v1/qa/tasks/{id}/decision", qaTaskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\",\"comment\":\"ok\"}")
                        .headers(headers(1L, qaUserId, null)))
                .andExpect(status().isOk());

        // 7) 再次评审通过
        var approvePass = mockMvc.perform(post("/api/v1/patches/{id}/actions", patchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE_REVIEW\"}")
                        .headers(headersWithIdempotency(1L, 2001L, "REVIEWER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertEquals("REVIEWING", json(approvePass).path("data").path("fromState").asText());
        Assertions.assertEquals("REVIEW_PASSED", json(approvePass).path("data").path("toState").asText());

        // 8) 附件与历史查询
        String attachmentBody = """
                {
                  "stage":"REVIEW",
                  "fileName":"minutes.txt",
                  "fileUrl":"https://minio.local/minutes.txt",
                  "fileHash":"abc123",
                  "fileSize":123
                }
                """;
        mockMvc.perform(post("/api/v1/patches/{id}/attachments", patchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attachmentBody)
                        .headers(headers(1L, 2001L, "REVIEWER")))
                .andExpect(status().isOk());

        var transitions = mockMvc.perform(get("/api/v1/patches/{id}/transitions", patchId)
                        .headers(headers(1L, 2001L, "REVIEWER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertTrue(json(transitions).path("data").isArray());
        Assertions.assertTrue(json(transitions).path("data").size() >= 3);
    }

    @Test
    void shouldApplyDynamicRoleActionPermission() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        String body = """
                {
                  "roleCode":"REVIEWER",
                  "action":"SUBMIT_REVIEW",
                  "enabled":true
                }
                """;
        mockMvc.perform(post("/api/v1/iam/role-action-permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .headers(headers(2L, 1L, "SUPER_ADMIN")))
                .andExpect(status().isOk());

        String createPatchBody = """
                {
                  "productLineId":88,
                  "title":"动态权限补丁-%s",
                  "description":"integration",
                  "severity":"MEDIUM",
                  "priority":"P2",
                  "sourceVersion":"v1",
                  "targetVersion":"v1.0.1",
                  "ownerPmId":2001
                }
                """.formatted(suffix);
        var createPatchResp = mockMvc.perform(post("/api/v1/patches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPatchBody)
                        .headers(headers(2L, 2001L, "PM")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Long patchId = json(createPatchResp).path("data").path("patchId").asLong();

        var forbidden = mockMvc.perform(post("/api/v1/patches/{id}/actions", patchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"SUBMIT_REVIEW\"}")
                        .headers(headersWithIdempotency(2L, 2001L, "PM")))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertEquals("FORBIDDEN", json(forbidden).path("code").asText());

        mockMvc.perform(post("/api/v1/patches/{id}/actions", patchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"SUBMIT_REVIEW\"}")
                        .headers(headersWithIdempotency(2L, 2010L, "REVIEWER")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldManageConfigAndCreateMailLog() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long tenantId = 3L;
        String adminRole = "SUPER_ADMIN";

        String scenarioBody = """
                {
                  "scenarioCode":"FIN_%s",
                  "scenarioName":"金融场景-%s",
                  "description":"金融行业交付"
                }
                """.formatted(suffix, suffix);
        var scenarioResp = mockMvc.perform(post("/api/v1/config/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scenarioBody)
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Long scenarioId = json(scenarioResp).path("data").path("id").asLong();
        Assertions.assertTrue(scenarioId > 0);

        String productBody = """
                {
                  "productCode":"PAY_%s",
                  "productName":"支付产品-%s",
                  "description":"支付补丁管理"
                }
                """.formatted(suffix, suffix);
        var productResp = mockMvc.perform(post("/api/v1/config/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody)
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Long productId = json(productResp).path("data").path("id").asLong();
        Assertions.assertTrue(productId > 0);

        mockMvc.perform(post("/api/v1/config/scenario-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId":%d,"productIds":[%d]}
                                """.formatted(scenarioId, productId))
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk());

        String roleBody = """
                {
                  "roleCode":"SCENE_PM_%s",
                  "roleName":"场景PM-%s",
                  "roleLevel":"SCENARIO",
                  "scopeRefId":%d,
                  "enabled":true
                }
                """.formatted(suffix, suffix, scenarioId);
        var roleResp = mockMvc.perform(post("/api/v1/config/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody)
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Long roleId = json(roleResp).path("data").path("id").asLong();
        Assertions.assertTrue(roleId > 0);

        String permissionBody = """
                {
                  "permCode":"CONFIG_SCENARIO_VIEW_%s",
                  "permName":"查看场景配置",
                  "permType":"API",
                  "resource":"SCENARIO",
                  "action":"VIEW",
                  "enabled":true
                }
                """.formatted(suffix);
        var permissionResp = mockMvc.perform(post("/api/v1/config/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(permissionBody)
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Long permissionId = json(permissionResp).path("data").path("id").asLong();
        Assertions.assertTrue(permissionId > 0);

        mockMvc.perform(post("/api/v1/config/roles/{roleId}/permissions", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + permissionId + "]")
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk());

        String ownerUserBody = """
                {"username":"owner_%s","displayName":"Owner %s","email":"owner_%s@example.com","status":"ACTIVE"}
                """.formatted(suffix, suffix, suffix);
        var ownerResp = mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerUserBody)
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Long ownerUserId = json(ownerResp).path("data").path("id").asLong();
        Assertions.assertTrue(ownerUserId > 0);

        mockMvc.perform(post("/api/v1/config/user-role-scopes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":%d,
                                  "roleId":%d,
                                  "scopeLevel":"SCENARIO",
                                  "scenarioId":%d,
                                  "status":"ACTIVE"
                                }
                                """.formatted(ownerUserId, roleId, scenarioId))
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/config/users/{userId}/role-scopes", ownerUserId)
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk());

        String mailServerBody = """
                {
                  "configName":"default-%s",
                  "smtpHost":"127.0.0.1",
                  "smtpPort":2525,
                  "protocol":"smtp",
                  "username":"demo",
                  "password":"demo",
                  "senderEmail":"noreply@example.com",
                  "senderName":"Patch Bot",
                  "starttlsEnabled":false,
                  "sslEnabled":false,
                  "authEnabled":false,
                  "defaultConfig":true,
                  "enabled":true
                }
                """.formatted(suffix);
        mockMvc.perform(post("/api/v1/notify/mail/servers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mailServerBody)
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk());

        String mailTemplateBody = """
                {
                  "templateCode":"PATCH_CREATED_%s",
                  "eventCode":"PATCH_CREATED",
                  "subjectTpl":"[补丁创建] ${patchNo}",
                  "bodyTpl":"补丁 ${patchNo} 已创建，当前状态 ${currentState}，操作人 ${operatorId}",
                  "contentType":"TEXT",
                  "version":1,
                  "enabled":true
                }
                """.formatted(suffix);
        mockMvc.perform(post("/api/v1/notify/mail/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mailTemplateBody)
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk());

        String policyBody = """
                {
                  "eventCode":"PATCH_CREATED",
                  "templateCode":"PATCH_CREATED_%s",
                  "includeOwner":true,
                  "includeOperator":false,
                  "enabled":true
                }
                """.formatted(suffix);
        mockMvc.perform(post("/api/v1/notify/mail/event-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyBody)
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk());

        String createPatchBody = """
                {
                  "productLineId":%d,
                  "title":"通知测试补丁-%s",
                  "description":"mail integration",
                  "severity":"LOW",
                  "priority":"P3",
                  "sourceVersion":"v1",
                  "targetVersion":"v1.0.1",
                  "ownerPmId":%d
                }
                """.formatted(productId, suffix, ownerUserId);
        mockMvc.perform(post("/api/v1/patches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPatchBody)
                        .headers(headers(tenantId, ownerUserId, "PM")))
                .andExpect(status().isOk());

        var logsResp = mockMvc.perform(get("/api/v1/notify/mail/logs")
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode logs = json(logsResp).path("data");
        Assertions.assertTrue(logs.isArray() && logs.size() > 0);
        JsonNode first = logs.get(0);
        Assertions.assertEquals("PATCH_CREATED", first.path("eventCode").asText());
        Assertions.assertTrue(first.path("mailTo").asText().contains("owner_" + suffix + "@example.com"));
        Long logId = first.path("id").asLong();
        Assertions.assertTrue(logId > 0);

        var resendResp = mockMvc.perform(post("/api/v1/notify/mail/logs/{logId}/resend", logId)
                        .headers(headers(tenantId, 1L, adminRole)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String resendStatus = json(resendResp).path("data").path("status").asText();
        Assertions.assertTrue("RETRY".equals(resendStatus) || "PENDING".equals(resendStatus));
    }

    private org.springframework.http.HttpHeaders headers(Long tenantId, Long userId, String roles) {
        var headers = new org.springframework.http.HttpHeaders();
        headers.add("X-Tenant-Id", String.valueOf(tenantId));
        headers.add("X-User-Id", String.valueOf(userId));
        if (roles != null && !roles.isBlank()) {
            headers.add("X-Roles", roles);
        }
        headers.add("X-Trace-Id", UUID.randomUUID().toString());
        return headers;
    }

    private org.springframework.http.HttpHeaders headersWithIdempotency(Long tenantId, Long userId, String roles) {
        var headers = headers(tenantId, userId, roles);
        headers.add("Idempotency-Key", UUID.randomUUID().toString());
        return headers;
    }

    private JsonNode json(String text) throws Exception {
        return objectMapper.readTree(text);
    }
}
