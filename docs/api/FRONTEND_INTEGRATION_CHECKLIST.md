# 前端联调清单（邮件通知 + 配置管理增量）

## 1. 通用约定

- Base URL：`/api/v1`
- 统一响应：
  - 成功：`{ "code": "0", "message": "OK", "data": ... }`
  - 失败：`{ "code": "<错误码>", "message": "<错误信息>", "data": ... }`
- 通用请求头：
  - `X-Tenant-Id`（必填）
  - `X-User-Id`（必填）
  - `X-Roles`（建议填，逗号分隔；不填时后端会回填）
  - `X-Trace-Id`（建议填，便于排障）

## 2. 页面与接口映射

## 2.1 邮件通知系统

### A. SMTP 配置页
- 新增/编辑：`POST /notify/mail/servers`
- 列表：`GET /notify/mail/servers`

### B. 模板管理页
- 新增/编辑模板：`POST /notify/mail/templates`
- 模板列表：`GET /notify/mail/templates`
- 模板预览：`POST /notify/mail/templates/render`

### C. 事件策略页
- 新增/编辑策略：`POST /notify/mail/event-policies`
- 策略列表：`GET /notify/mail/event-policies`

### D. 发送日志页
- 日志列表：`GET /notify/mail/logs?limit=50`
- 手动重发：`POST /notify/mail/logs/{logId}/resend`

---

## 2.2 配置管理系统

### A. 交付场景页
- 新增/编辑：`POST /config/scenarios`
- 列表：`GET /config/scenarios`

### B. 产品管理页
- 新增/编辑：`POST /config/products`
- 列表：`GET /config/products`

### C. 场景绑定产品页
- 绑定提交（覆盖式）：`POST /config/scenario-products`

### D. 角色管理页
- 新增/编辑：`POST /config/roles`
- 列表：`GET /config/roles`

### E. 权限点管理页
- 新增/编辑：`POST /config/permissions`
- 列表：`GET /config/permissions`
- 角色授权（覆盖式）：`POST /config/roles/{roleId}/permissions`
- 查询角色授权：`GET /config/roles/{roleId}/permissions`

### F. 用户角色作用域页
- 分配：`POST /config/user-role-scopes`
- 查询：`GET /config/users/{userId}/role-scopes`

## 3. 联调建议顺序（强烈建议）

1. **先建场景** -> `POST /config/scenarios`
2. **再建产品** -> `POST /config/products`
3. **场景绑定产品** -> `POST /config/scenario-products`
4. **建立角色与权限点** -> `POST /config/roles` / `POST /config/permissions`
5. **角色授权** -> `POST /config/roles/{roleId}/permissions`
6. **用户绑定角色作用域** -> `POST /config/user-role-scopes`
7. **配置邮件服务器、模板、策略**
8. **创建/流转补丁并验证通知日志**

## 4. 权限与可见性验收点

- 用户无权限点时：
  - 若租户尚未配置该权限点，接口默认兼容放行（避免历史行为回归）。
  - 若租户已配置权限点但角色未授权，接口返回 `FORBIDDEN`。
- `SUPER_ADMIN` / `LINE_ADMIN` 拥有管理兜底权限（拦截层直接放行）。

## 5. 默认初始化数据（Flyway V5）

`V5__seed_notify_and_config_data.sql` 已内置：

- 配置管理权限点（`CONFIG_*`）
- 邮件通知权限点（`NOTIFY_MAIL_*`）
- 默认邮件模板（如 `TPL_PATCH_CREATED`、`TPL_KPI_GATE_FAILED`）
- 默认事件策略（`PATCH_CREATED`、`PATCH_SUBMIT_REVIEW` 等）
- `SUPER_ADMIN` / `LINE_ADMIN` 的默认角色授权

## 6. 前端注意事项

- 模板预览 `model` 为任意 JSON 对象，建议 UI 提供 Key-Value 编辑器。
- `roleId -> permissions` 是**覆盖式分配**，提交前请把保留项一并带上。
- `scenario-products` 也是覆盖式，提交新绑定前先读当前列表做 diff。
- 邮件日志状态枚举：`PENDING` / `RETRY` / `SENT` / `FAILED`。
- 后续若接入按钮级权限，建议使用 `permCode` 作为前端指令（如 `v-perm`）。
