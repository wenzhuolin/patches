# Patch Lifecycle Management System

基于 **Java 21 + Spring Boot 3 + Flyway + 可切换数据库（SQLite/MySQL/PostgreSQL）** 的补丁生命周期管理系统（后端MVP）。

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
9. **用户/角色与附件（新增）**
   - 支持用户、角色、用户角色关系管理
   - RequestContext 在未传 `X-Roles` 时可从数据库回填用户角色
   - 支持补丁流程附件元数据管理（各阶段上传记录）
10. **流程可观测（新增）**
   - 支持补丁列表查询（按状态）
   - 支持流转历史查询与补丁级操作日志查询
   - 支持按业务类型查询审计日志（KPI/QA/TEST/REVIEW等）

## 2. 技术栈

- 后端：Spring Boot 3.5.0
- 数据库：SQLite（测试默认）/ MySQL / PostgreSQL（Flyway 管理DDL）
- 文档：OpenAPI（Swagger UI）
- 测试：JUnit 5

## 3. 运行与部署

### 3.1 本地运行

> 默认使用 SQLite（单文件，无外部依赖），可通过 `DB_TYPE` 切换到 MySQL 或 PostgreSQL。

```bash
./mvnw clean test
./mvnw spring-boot:run
```

环境变量示例：

```bash
# DB_TYPE: sqlite / mysql / postgres
export DB_TYPE=sqlite
export SQLITE_PATH=./data/patches-dev.db

# MySQL 示例
# export DB_TYPE=mysql
# export MYSQL_HOST=127.0.0.1
# export MYSQL_PORT=3306
# export MYSQL_DATABASE=patches
# export MYSQL_USER=patches
# export MYSQL_PASSWORD=patches

# PostgreSQL 示例
# export DB_TYPE=postgres
# export POSTGRES_HOST=127.0.0.1
# export POSTGRES_PORT=5432
# export POSTGRES_DATABASE=patches
# export POSTGRES_USER=patches
# export POSTGRES_PASSWORD=patches
```

Swagger 地址：

- `http://localhost:8080/swagger-ui.html`

### 3.1.1 测试环境一键启动（SQLite）

```bash
# 启动（默认端口 18080）
bash scripts/start-test-env.sh start

# 查看状态/健康
bash scripts/start-test-env.sh status
bash scripts/start-test-env.sh health

# 查看日志
bash scripts/start-test-env.sh logs

# 停止
bash scripts/start-test-env.sh stop
```

启动成功后页面入口：

- 业务门户（前端界面）：`http://<host>:<port>/`
- Swagger：`http://<host>:<port>/swagger-ui.html`

首次进入业务门户时，需要填写“业务上下文登录信息”（租户ID、用户ID、角色集合）。
建议测试使用角色：`SUPER_ADMIN,LINE_ADMIN,PM,REVIEWER,PRODUCT_LINE_QA,TEST,DEV`。

可选环境变量：

```bash
export APP_PORT=18080
export SQLITE_PATH=./.runtime/test-env/data/patches-test.db
export SERVER_ADDRESS=0.0.0.0
export PUBLIC_HOST=<你的服务器公网IP或域名>
export SKIP_BUILD=false
export START_TIMEOUT_SEC=90
```

常见报错处理：

- 报错 `maven-compiler-plugin: release version 21 not supported`
  - 原因：当前 Maven 使用的 Java 不是 21（或仅安装了 JRE）。
  - 修复：
    ```bash
    # Ubuntu
    sudo apt update && sudo apt install -y openjdk-21-jdk
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
    export PATH="$JAVA_HOME/bin:$PATH"

    java -version
    ./mvnw -v   # 必须显示 Java 21
    ```

### 3.2 华为云 Linux 一键部署（Docker Compose）

适用场景：你有一台带公网IP的 Linux 服务器，想快速部署和联调。

1) 服务器初始化（安装 Docker / Compose / 防火墙放行）

```bash
sudo APP_PORT=8080 DB_PORT=5432 bash scripts/huawei/server-init.sh
```

2) 准备环境变量

```bash
cp .env.example .env
# 修改 POSTGRES_PASSWORD、GRAFANA_ADMIN_PASSWORD 等配置
```

3) 在服务器上部署（仓库已在服务器）

```bash
bash scripts/huawei/deploy.sh
```

4) 从本地机器远程发布到服务器（可选）

```bash
bash scripts/huawei/deploy-remote.sh ubuntu@<你的公网IP> /opt/patch-lifecycle
```

5) 健康检查与日志

```bash
bash scripts/huawei/healthcheck.sh http://127.0.0.1:8080
docker compose -f docker-compose.prod.yml logs -f app
```

6) 数据库备份（可选）

```bash
bash scripts/huawei/db-backup.sh
bash scripts/huawei/prune-backups.sh 14
# 恢复(危险): bash scripts/huawei/db-restore.sh backups/xxxx.sql
```

7) 预检与上线后验收（建议）

```bash
bash scripts/huawei/preflight-check.sh api.yourdomain.com
bash scripts/huawei/post-deploy-smoke.sh http://127.0.0.1:8080
```

8) 安装监控（Prometheus + Grafana）

```bash
bash scripts/huawei/install-monitoring.sh
bash scripts/huawei/monitoring-smoke.sh
bash scripts/huawei/test-alerting.sh
bash scripts/huawei/reload-monitoring-config.sh
sudo bash scripts/huawei/install-monitoring-systemd-service.sh /opt/patch-lifecycle
```

### 3.3 域名、HTTPS、systemd、自启动与蓝绿发布

1) 安装 Nginx 并配置反向代理（域名替换成你的）

```bash
sudo bash scripts/huawei/install-reverse-proxy.sh api.yourdomain.com 127.0.0.1:8080
```

2) 启用 HTTPS（Let's Encrypt）

```bash
sudo bash scripts/huawei/enable-https.sh api.yourdomain.com your@email.com
```

3) 安装 systemd 守护（服务器重启后自动拉起）

```bash
sudo bash scripts/huawei/install-systemd-service.sh /opt/patch-lifecycle prod
systemctl status patch-lifecycle.service --no-pager
```

4) 切换到蓝绿发布（零停机回滚）

```bash
# 先把 Nginx upstream 指向 blue/green 端口之一
sudo bash scripts/huawei/install-reverse-proxy.sh api.yourdomain.com 127.0.0.1:18080

# 执行蓝绿部署（自动部署到闲置槽位并切流）
sudo bash scripts/huawei/deploy-bluegreen.sh

# 回滚（切回另一个槽位）
sudo bash scripts/huawei/rollback-bluegreen.sh
```

5) 证书续期（手动触发）

```bash
sudo bash scripts/huawei/renew-cert.sh
```

### 3.4 一键首发（包含 init + deploy + nginx + https + systemd）

```bash
sudo bash scripts/huawei/bootstrap-production.sh \
  --domain api.yourdomain.com \
  --email your@email.com \
  --mode bluegreen \
  --with-monitoring true \
  --with-maintenance-cron true \
  --app-dir /opt/patch-lifecycle
```

### 3.5 运维 Runbook

- `docs/ops/PROD_RUNBOOK.md`

### 3.6 定时维护任务（备份+证书续期）

```bash
sudo bash scripts/huawei/install-maintenance-cron.sh /opt/patch-lifecycle
```

### 3.7 GitHub Actions 自动化（可选）

已内置：
- `.github/workflows/ci.yml`（单元/集成测试流程）
- `.github/workflows/docker-build.yml`（镜像构建验证）
- `.github/workflows/deploy-huawei.yml`（手动触发远程部署）

使用部署工作流前，请在仓库 Secrets 配置：
- `SSH_PRIVATE_KEY`

### 3.8 运维工具清单（新增）

- 预检：`scripts/huawei/preflight-check.sh`
- 首发总控：`scripts/huawei/bootstrap-production.sh`
- 蓝绿发布：`scripts/huawei/deploy-bluegreen.sh`
- 回滚：`scripts/huawei/rollback-bluegreen.sh`
- 监控安装：`scripts/huawei/install-monitoring.sh`
- 监控守护：`scripts/huawei/install-monitoring-systemd-service.sh`
- 告警自测：`scripts/huawei/test-alerting.sh`
- 监控热加载：`scripts/huawei/reload-monitoring-config.sh`
- 维护任务：`scripts/huawei/install-maintenance-cron.sh`

## 4. 关键请求头

所有业务API需要：

- `X-Tenant-Id`：租户ID（Long）
- `X-User-Id`：用户ID（Long）
- `X-Roles`：角色集合（逗号分隔，如 `PM,QA`）；可选，不传时会尝试从 `user_role_relation` 回填
- `Idempotency-Key`：幂等键（建议每次动作唯一）
- `X-Trace-Id`：链路追踪ID（可选，不传则自动生成）

## 5. 核心接口（节选）

### 补丁流程

- `POST /api/v1/patches` 创建补丁
- `GET /api/v1/patches?state=REVIEWING` 查询补丁列表
- `GET /api/v1/patches/{patchId}` 查询补丁
- `POST /api/v1/patches/{patchId}/actions` 执行状态流转动作
- `GET /api/v1/patches/{patchId}/transitions` 查询流转历史
- `GET /api/v1/patches/{patchId}/operation-logs` 查询补丁操作日志
- `POST /api/v1/patches/{patchId}/attachments` 新增附件记录
- `GET /api/v1/patches/{patchId}/attachments` 查询附件记录

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

### 用户角色管理

- `POST /api/v1/admin/roles` 创建/更新角色
- `GET /api/v1/admin/roles` 查询角色
- `POST /api/v1/admin/users` 创建/更新用户
- `GET /api/v1/admin/users` 查询用户
- `POST /api/v1/admin/users/roles` 绑定用户角色
- `GET /api/v1/admin/users/{userId}/roles` 查询用户角色

### 集成（CI/CD）

- `POST /api/v1/integrations/connectors` 配置集成连接器
- `GET /api/v1/integrations/connectors` 查询连接器
- `POST /api/v1/integrations/ci/webhook` CI指标入站并落库

### 审计查询

- `GET /api/v1/audit/logs?bizType=KPI_RULE` 按业务类型查询审计日志

### 转测与评审

- `GET /api/v1/patches/{patchId}/test-tasks` 查询转测任务
- `POST /api/v1/test-tasks/{taskId}/results` 回填测试结果
- `POST /api/v1/review-sessions` 创建评审会
- `POST /api/v1/review-sessions/{sessionId}/votes` 评审投票

## 6. 数据库

Flyway 脚本：

- PostgreSQL: `src/main/resources/db/migration/*.sql`
- MySQL: `src/main/resources/db/migration/mysql/*.sql`
- SQLite: `src/main/resources/db/migration/sqlite/*.sql`

已包含核心表：`patch / patch_transition_log / kpi_* / qa_* / test_task / review_* / patch_operation_log / role_action_permission / user_data_scope / integration_connector / sys_user / user_role_relation / patch_attachment`

## 7. 设计说明

1. 关键动作采用“**权限校验 -> KPI校验 -> QA校验 -> 状态变更**”链路
2. 补丁流转使用行级锁 + 幂等键，避免高并发重复流转
3. KPI规则按租户+阶段+范围动态匹配，可扩展接入CI/CD自动采集
4. 采用模块化单体架构，后续可平滑拆分（KPI服务、QA服务、流程编排服务）