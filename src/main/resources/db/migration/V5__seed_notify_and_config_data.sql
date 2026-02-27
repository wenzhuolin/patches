-- 默认初始化数据（增量）
-- 目标：提供配置管理与邮件通知的最小可用种子数据，便于开箱即用与联调。

with tenant_source as (
    select tenant_id from sys_role
    union
    select tenant_id from sys_user
    union
    select tenant_id from patch
    union
    select 1::bigint as tenant_id
),
permission_seed as (
    select *
    from (values
        ('CONFIG_SCENARIO_VIEW', '查看交付场景', 'API', 'SCENARIO', 'VIEW'),
        ('CONFIG_SCENARIO_MANAGE', '管理交付场景', 'API', 'SCENARIO', 'MANAGE'),
        ('CONFIG_PRODUCT_VIEW', '查看产品配置', 'API', 'PRODUCT', 'VIEW'),
        ('CONFIG_PRODUCT_MANAGE', '管理产品配置', 'API', 'PRODUCT', 'MANAGE'),
        ('CONFIG_ROLE_VIEW', '查看角色配置', 'API', 'ROLE', 'VIEW'),
        ('CONFIG_ROLE_MANAGE', '管理角色配置', 'API', 'ROLE', 'MANAGE'),
        ('CONFIG_PERMISSION_VIEW', '查看权限点', 'API', 'PERMISSION', 'VIEW'),
        ('CONFIG_PERMISSION_MANAGE', '管理权限点', 'API', 'PERMISSION', 'MANAGE'),
        ('CONFIG_USER_ASSIGN', '用户角色作用域分配', 'API', 'USER_ROLE_SCOPE', 'ASSIGN'),
        ('NOTIFY_MAIL_VIEW', '查看邮件通知配置与日志', 'API', 'MAIL', 'VIEW'),
        ('NOTIFY_MAIL_MANAGE', '管理邮件通知配置与策略', 'API', 'MAIL', 'MANAGE')
    ) as t(perm_code, perm_name, perm_type, resource, action)
)
insert into permission_def (
    tenant_id, perm_code, perm_name, perm_type, resource, action,
    parent_id, enabled, created_by, updated_by, is_deleted
)
select ts.tenant_id, ps.perm_code, ps.perm_name, ps.perm_type, ps.resource, ps.action,
       null, true, 0, 0, false
from tenant_source ts
cross join permission_seed ps
on conflict (tenant_id, perm_code) do update
set perm_name = excluded.perm_name,
    perm_type = excluded.perm_type,
    resource = excluded.resource,
    action = excluded.action,
    enabled = true,
    is_deleted = false,
    updated_at = now(),
    updated_by = 0;

-- 给 SUPER_ADMIN / LINE_ADMIN 预置授权（仅针对新增权限点）
insert into role_permission_rel (
    tenant_id, role_id, permission_id, grant_type, created_by, updated_by, is_deleted
)
select r.tenant_id, r.id, p.id, 'ALLOW', 0, 0, false
from sys_role r
join permission_def p
    on p.tenant_id = r.tenant_id
   and p.perm_code in (
       'CONFIG_SCENARIO_VIEW', 'CONFIG_SCENARIO_MANAGE',
       'CONFIG_PRODUCT_VIEW', 'CONFIG_PRODUCT_MANAGE',
       'CONFIG_ROLE_VIEW', 'CONFIG_ROLE_MANAGE',
       'CONFIG_PERMISSION_VIEW', 'CONFIG_PERMISSION_MANAGE',
       'CONFIG_USER_ASSIGN',
       'NOTIFY_MAIL_VIEW', 'NOTIFY_MAIL_MANAGE'
   )
where r.role_code in ('SUPER_ADMIN', 'LINE_ADMIN')
  and r.enabled = true
  and r.is_deleted = false
on conflict (tenant_id, role_id, permission_id) do update
set grant_type = 'ALLOW',
    is_deleted = false,
    updated_at = now(),
    updated_by = 0;

-- 邮件模板默认值（每租户、版本=1）
with tenant_source as (
    select tenant_id from sys_role
    union
    select tenant_id from sys_user
    union
    select tenant_id from patch
    union
    select 1::bigint as tenant_id
),
template_seed as (
    select *
    from (values
        ('TPL_PATCH_CREATED', 'PATCH_CREATED',
         '[补丁创建] ${patchNo}',
         '补丁【${patchNo}】已创建。标题：${title}；当前状态：${currentState}；操作人：${operatorId}。下一步：${nextStep}。'),

        ('TPL_PATCH_SUBMIT_REVIEW', 'PATCH_SUBMIT_REVIEW',
         '[补丁评审] ${patchNo} 已提交评审',
         '补丁【${patchNo}】已进入评审环节。当前状态：${toState}；操作人：${operatorId}。请评审委员与QA及时处理。'),

        ('TPL_PATCH_REVIEW_APPROVED', 'PATCH_REVIEW_APPROVED',
         '[评审通过] ${patchNo}',
         '补丁【${patchNo}】评审通过。当前状态：${toState}；操作人：${operatorId}。下一步：${nextStep}。'),

        ('TPL_PATCH_TRANSFER_TO_TEST', 'PATCH_TRANSFER_TO_TEST',
         '[转测通知] ${patchNo}',
         '补丁【${patchNo}】已转测。当前状态：${toState}；操作人：${operatorId}。请测试人员尽快执行测试任务。'),

        ('TPL_PATCH_RELEASE', 'PATCH_RELEASE',
         '[发布完成] ${patchNo}',
         '补丁【${patchNo}】已发布。当前状态：${toState}；操作人：${operatorId}。请关注发布后稳定性。'),

        ('TPL_PATCH_ARCHIVE', 'PATCH_ARCHIVE',
         '[补丁归档] ${patchNo}',
         '补丁【${patchNo}】已归档。当前状态：${toState}；操作人：${operatorId}。流程结束。'),

        ('TPL_KPI_GATE_FAILED', 'KPI_GATE_FAILED',
         '[KPI未达标] ${patchNo}',
         '补丁【${patchNo}】在流转时被KPI门禁阻断。状态：${fromState} -> ${toState}；原因：${reason}；操作人：${operatorId}。'),

        ('TPL_QA_GATE_BLOCKED', 'QA_GATE_BLOCKED',
         '[QA门禁阻断] ${patchNo}',
         '补丁【${patchNo}】在流转时被QA门禁阻断。状态：${fromState} -> ${toState}；原因：${reason}；操作人：${operatorId}。')
    ) as t(template_code, event_code, subject_tpl, body_tpl)
)
insert into mail_template (
    tenant_id, template_code, event_code,
    subject_tpl, body_tpl, content_type, lang, version,
    enabled, created_by, updated_by, is_deleted
)
select ts.tenant_id, tm.template_code, tm.event_code,
       tm.subject_tpl, tm.body_tpl, 'TEXT', 'zh-CN', 1,
       true, 0, 0, false
from tenant_source ts
cross join template_seed tm
on conflict (tenant_id, template_code, version) do update
set event_code = excluded.event_code,
    subject_tpl = excluded.subject_tpl,
    body_tpl = excluded.body_tpl,
    content_type = excluded.content_type,
    lang = excluded.lang,
    enabled = true,
    is_deleted = false,
    updated_at = now(),
    updated_by = 0;

-- 默认事件策略
with tenant_source as (
    select tenant_id from sys_role
    union
    select tenant_id from sys_user
    union
    select tenant_id from patch
    union
    select 1::bigint as tenant_id
),
policy_seed as (
    select *
    from (values
        ('PATCH_CREATED', 'TPL_PATCH_CREATED', '[]', '[]', true, false, true),
        ('PATCH_SUBMIT_REVIEW', 'TPL_PATCH_SUBMIT_REVIEW', '["REVIEWER","PRODUCT_LINE_QA"]', '["PM"]', true, false, true),
        ('PATCH_REVIEW_APPROVED', 'TPL_PATCH_REVIEW_APPROVED', '["PM","TEST"]', '["PRODUCT_LINE_QA"]', true, true, true),
        ('PATCH_TRANSFER_TO_TEST', 'TPL_PATCH_TRANSFER_TO_TEST', '["TEST","PRODUCT_LINE_QA"]', '["PM"]', true, true, true),
        ('PATCH_RELEASE', 'TPL_PATCH_RELEASE', '["PM","LINE_ADMIN"]', '["PRODUCT_LINE_QA"]', true, true, true),
        ('PATCH_ARCHIVE', 'TPL_PATCH_ARCHIVE', '["PM"]', '["LINE_ADMIN"]', true, false, true),
        ('KPI_GATE_FAILED', 'TPL_KPI_GATE_FAILED', '["PM","REVIEWER"]', '["PRODUCT_LINE_QA"]', true, true, true),
        ('QA_GATE_BLOCKED', 'TPL_QA_GATE_BLOCKED', '["PM","PRODUCT_LINE_QA"]', '["LINE_ADMIN"]', true, true, true)
    ) as t(event_code, template_code, to_roles_json, cc_roles_json, include_owner, include_operator, enabled)
)
insert into mail_event_policy (
    tenant_id, event_code, template_code, to_roles, cc_roles,
    include_owner, include_operator, enabled,
    created_by, updated_by, is_deleted
)
select ts.tenant_id, ps.event_code, ps.template_code,
       ps.to_roles_json::jsonb, ps.cc_roles_json::jsonb,
       ps.include_owner, ps.include_operator, ps.enabled,
       0, 0, false
from tenant_source ts
cross join policy_seed ps
on conflict (tenant_id, event_code) do update
set template_code = excluded.template_code,
    to_roles = excluded.to_roles,
    cc_roles = excluded.cc_roles,
    include_owner = excluded.include_owner,
    include_operator = excluded.include_operator,
    enabled = excluded.enabled,
    is_deleted = false,
    updated_at = now(),
    updated_by = 0;
