# Patch Lifecycle Management System

基于 **Java 21 + Spring Boot 3 + PostgreSQL + Flyway** 的补丁生命周期管理系统（后端MVP）。

## 1. 当前实现范围

已落地核心能力：

1. **补丁生命周期流转**
   - `DRAFT -> REVIEWING -> REVIEW_PASSED -> TESTING -> TEST_PASSED -> RELEASE_READY -> RELEASED -> ARCHIVED`
   - 支持状态机校验与非法流转阻断
   - 流转日志（含阻断原因、幂等请求ID）
2. **KPI卡点控制**
   - KPI规则动态配置（阶段、准入/准出、阈值、范围、缺失数据策略）
   - 指标上报与动态校验
   - 评估明细与审计落库
3. **QA权限门禁**
   - QA策略配置（ALL/ANY/SEQUENTIAL）
   - 转测、评审通过前必须QA通过（按状态机动作卡点）
   - QA任务、审批意见与审批日志
4. **转测子系统（MVP）**
   - 转测动作成功后自动创建测试任务
   - 测试结果回填接口
5. **评审子系统（MVP）**
   - 评审会创建（在线/异步）
   - 评审投票与自动结论更新（PASS/REJECT）
6. **安全审计**
   - 操作审计日志（PATCH/KPI/QA/TEST/REVIEW）
   - 关键操作保留 traceId / IP / User-Agent
7. **动态权限与数据权限（新增）**
   - RBAC动作权限支持数据库动态配置（角色-动作）
   - 支持用户数据权限范围（GLOBAL/PRODUCT_LINE）
   - 补丁创建、查询、流转均执行数据权限校验
8. **CI集成（新增）**
   - 支持集成连接器配置
   - 支持CI webhook入站，自动采集并写入KPI指标

## 2. 技术栈

- 后端：Spring Boot 3.5.0
- 数据库：PostgreSQL（Flyway 管理DDL）
- 文档：OpenAPI（Swagger UI）
- 测试：JUnit 5

## 3. 运行方式

> 需要可用 PostgreSQL 实例（默认连接参数见 `application.yml`，可用环境变量覆盖）

```bash
./mvnw clean test
./mvnw spring-boot:run
```

环境变量示例：

```bash
export DB_URL=jdbc:postgresql://127.0.0.1:5432/patches
export DB_USERNAME=patches
export DB_PASSWORD=patches
```

Swagger 地址：

- `http://localhost:8080/swagger-ui.html`

## 4. 关键请求头

所有业务API需要：

- `X-Tenant-Id`：租户ID（Long）
- `X-User-Id`：用户ID（Long）
- `X-Roles`：角色集合（逗号分隔，如 `PM,QA`）
- `Idempotency-Key`：幂等键（建议每次动作唯一）
- `X-Trace-Id`：链路追踪ID（可选，不传则自动生成）

## 5. 核心接口（节选）

### 补丁流程

- `POST /api/v1/patches` 创建补丁
- `GET /api/v1/patches/{patchId}` 查询补丁
- `POST /api/v1/patches/{patchId}/actions` 执行状态流转动作

### KPI

- `POST /api/v1/kpi/rules` 新建KPI规则
- `GET /api/v1/kpi/rules` 查询KPI规则
- `POST /api/v1/patches/{patchId}/metrics` 上报指标数据
- `POST /api/v1/patches/{patchId}/kpi/evaluate` 手动触发KPI校验

### QA

- `POST /api/v1/qa/policies` 配置QA策略
- `GET /api/v1/qa/policies` 查询QA策略
- `GET /api/v1/qa/tasks/my-pending` 查询我的QA待办
- `POST /api/v1/qa/tasks/{qaTaskId}/decision` QA审批

### IAM（动态权限）

- `POST /api/v1/iam/role-action-permissions` 配置角色动作权限
- `GET /api/v1/iam/role-action-permissions?action=TRANSFER_TO_TEST` 查询动作权限
- `POST /api/v1/iam/user-data-scopes` 授予用户数据范围
- `GET /api/v1/iam/users/{userId}/data-scopes` 查询用户数据范围

### 集成（CI/CD）

- `POST /api/v1/integrations/connectors` 配置集成连接器
- `GET /api/v1/integrations/connectors` 查询连接器
- `POST /api/v1/integrations/ci/webhook` CI指标入站并落库

### 转测与评审

- `GET /api/v1/patches/{patchId}/test-tasks` 查询转测任务
- `POST /api/v1/test-tasks/{taskId}/results` 回填测试结果
- `POST /api/v1/review-sessions` 创建评审会
- `POST /api/v1/review-sessions/{sessionId}/votes` 评审投票

## 6. 数据库

Flyway 脚本：

- `src/main/resources/db/migration/V1__init_schema.sql`
- `src/main/resources/db/migration/V2__iam_and_integration.sql`

已包含核心表：`patch / patch_transition_log / kpi_* / qa_* / test_task / review_* / patch_operation_log / role_action_permission / user_data_scope / integration_connector`

## 7. 设计说明

1. 关键动作采用“**权限校验 -> KPI校验 -> QA校验 -> 状态变更**”链路
2. 补丁流转使用行级锁 + 幂等键，避免高并发重复流转
3. KPI规则按租户+阶段+范围动态匹配，可扩展接入CI/CD自动采集
4. 采用模块化单体架构，后续可平滑拆分（KPI服务、QA服务、流程编排服务）