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
