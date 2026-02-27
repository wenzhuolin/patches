-- =========================
-- Mail notification system
-- =========================

create table if not exists mail_server_config (
    id bigserial primary key,
    tenant_id bigint not null,
    config_name varchar(64) not null,
    smtp_host varchar(255) not null,
    smtp_port int not null,
    protocol varchar(16) not null default 'smtp',
    username varchar(128),
    password_cipher text,
    sender_email varchar(128) not null,
    sender_name varchar(128),
    ssl_enabled boolean not null default false,
    starttls_enabled boolean not null default false,
    auth_enabled boolean not null default true,
    timeout_ms int not null default 10000,
    is_default boolean not null default false,
    enabled boolean not null default true,
    ext_props jsonb,
    created_at timestamptz not null default now(),
    created_by bigint,
    updated_at timestamptz not null default now(),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_mail_server_config unique (tenant_id, config_name)
);

create index if not exists idx_mail_server_default on mail_server_config (tenant_id, is_default, enabled);

create table if not exists mail_template (
    id bigserial primary key,
    tenant_id bigint not null,
    template_code varchar(64) not null,
    event_code varchar(64) not null,
    subject_tpl text not null,
    body_tpl text not null,
    content_type varchar(16) not null default 'TEXT',
    lang varchar(16) not null default 'zh-CN',
    version int not null default 1,
    enabled boolean not null default true,
    ext_props jsonb,
    created_at timestamptz not null default now(),
    created_by bigint,
    updated_at timestamptz not null default now(),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_mail_template unique (tenant_id, template_code, version)
);

create index if not exists idx_mail_template_event on mail_template (tenant_id, event_code, enabled);

create table if not exists mail_event_policy (
    id bigserial primary key,
    tenant_id bigint not null,
    event_code varchar(64) not null,
    template_code varchar(64) not null,
    to_roles jsonb,
    cc_roles jsonb,
    include_owner boolean not null default true,
    include_operator boolean not null default false,
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    created_by bigint,
    updated_at timestamptz not null default now(),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_mail_event_policy unique (tenant_id, event_code)
);

create index if not exists idx_mail_event_policy_enabled on mail_event_policy (tenant_id, enabled, event_code);

create table if not exists mail_send_log (
    id bigserial primary key,
    tenant_id bigint not null,
    biz_type varchar(32),
    biz_id bigint,
    event_code varchar(64) not null,
    template_id bigint references mail_template(id),
    mail_to text,
    mail_cc text,
    mail_bcc text,
    subject_rendered text,
    body_rendered text,
    status varchar(16) not null default 'PENDING',
    provider_msg_id varchar(128),
    retry_count int not null default 0,
    max_retry int not null default 5,
    next_retry_at timestamptz,
    error_code varchar(64),
    error_message text,
    idempotency_key varchar(128),
    created_at timestamptz not null default now(),
    sent_at timestamptz
);

create index if not exists idx_mail_send_log_status on mail_send_log (tenant_id, status, created_at desc);
create index if not exists idx_mail_send_log_retry on mail_send_log (status, next_retry_at);
create index if not exists idx_mail_send_log_biz on mail_send_log (tenant_id, biz_type, biz_id);

create table if not exists mail_send_task (
    id bigserial primary key,
    tenant_id bigint not null,
    log_id bigint not null references mail_send_log(id) on delete cascade,
    task_status varchar(16) not null default 'PENDING',
    available_at timestamptz not null default now(),
    locked_by varchar(64),
    locked_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_mail_send_task_available on mail_send_task (task_status, available_at);
create index if not exists idx_mail_send_task_log_id on mail_send_task (log_id);

-- =========================
-- Config management system
-- =========================

create table if not exists delivery_scenario (
    id bigserial primary key,
    tenant_id bigint not null,
    scenario_code varchar(64) not null,
    scenario_name varchar(128) not null,
    description text,
    status varchar(32) not null default 'ACTIVE',
    ext_props jsonb,
    created_at timestamptz not null default now(),
    created_by bigint,
    updated_at timestamptz not null default now(),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_delivery_scenario unique (tenant_id, scenario_code)
);

create index if not exists idx_delivery_scenario_status on delivery_scenario (tenant_id, status);

create table if not exists product (
    id bigserial primary key,
    tenant_id bigint not null,
    product_code varchar(64) not null,
    product_name varchar(128) not null,
    description text,
    owner_user_id bigint,
    status varchar(32) not null default 'ACTIVE',
    ext_props jsonb,
    created_at timestamptz not null default now(),
    created_by bigint,
    updated_at timestamptz not null default now(),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_product unique (tenant_id, product_code)
);

create index if not exists idx_product_status on product (tenant_id, status);

create table if not exists scenario_product_rel (
    id bigserial primary key,
    tenant_id bigint not null,
    scenario_id bigint not null references delivery_scenario(id) on delete cascade,
    product_id bigint not null references product(id) on delete cascade,
    status varchar(32) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    created_by bigint,
    updated_at timestamptz not null default now(),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_scenario_product_rel unique (tenant_id, scenario_id, product_id)
);

create index if not exists idx_scenario_product_rel_product on scenario_product_rel (tenant_id, product_id, status);
create index if not exists idx_scenario_product_rel_scenario on scenario_product_rel (tenant_id, scenario_id, status);

alter table sys_role add column if not exists role_level varchar(16) not null default 'GLOBAL';
alter table sys_role add column if not exists scope_ref_id bigint;

create index if not exists idx_sys_role_level on sys_role (tenant_id, role_level, enabled);

create table if not exists permission_def (
    id bigserial primary key,
    tenant_id bigint not null,
    perm_code varchar(128) not null,
    perm_name varchar(128) not null,
    perm_type varchar(16) not null,
    resource varchar(64),
    action varchar(64),
    parent_id bigint,
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    created_by bigint,
    updated_at timestamptz not null default now(),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_permission_def unique (tenant_id, perm_code)
);

create index if not exists idx_permission_def_parent on permission_def (tenant_id, parent_id, enabled);

create table if not exists role_permission_rel (
    id bigserial primary key,
    tenant_id bigint not null,
    role_id bigint not null references sys_role(id) on delete cascade,
    permission_id bigint not null references permission_def(id) on delete cascade,
    grant_type varchar(16) not null default 'ALLOW',
    created_at timestamptz not null default now(),
    created_by bigint,
    updated_at timestamptz not null default now(),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_role_permission_rel unique (tenant_id, role_id, permission_id)
);

create index if not exists idx_role_permission_rel_role on role_permission_rel (tenant_id, role_id);
create index if not exists idx_role_permission_rel_perm on role_permission_rel (tenant_id, permission_id);

create table if not exists user_role_scope_rel (
    id bigserial primary key,
    tenant_id bigint not null,
    user_id bigint not null references sys_user(id) on delete cascade,
    role_id bigint not null references sys_role(id) on delete cascade,
    scope_level varchar(16) not null default 'GLOBAL',
    scenario_id bigint references delivery_scenario(id) on delete cascade,
    product_id bigint references product(id) on delete cascade,
    status varchar(32) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    created_by bigint,
    updated_at timestamptz not null default now(),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_user_role_scope_rel unique (tenant_id, user_id, role_id, scope_level, scenario_id, product_id)
);

create index if not exists idx_user_role_scope_rel_user on user_role_scope_rel (tenant_id, user_id, status);
create index if not exists idx_user_role_scope_rel_scope on user_role_scope_rel (tenant_id, scope_level, scenario_id, product_id, status);

create table if not exists data_scope_policy (
    id bigserial primary key,
    tenant_id bigint not null,
    role_id bigint not null references sys_role(id) on delete cascade,
    resource_type varchar(32) not null,
    scope_expr jsonb not null,
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    created_by bigint,
    updated_at timestamptz not null default now(),
    updated_by bigint,
    is_deleted boolean not null default false
);

create index if not exists idx_data_scope_policy_role on data_scope_policy (tenant_id, role_id, resource_type, enabled);

create table if not exists config_change_log (
    id bigserial primary key,
    tenant_id bigint not null,
    config_type varchar(32) not null,
    config_id bigint not null,
    action varchar(32) not null,
    before_data jsonb,
    after_data jsonb,
    operator_id bigint not null,
    trace_id varchar(64),
    ip varchar(64),
    user_agent varchar(255),
    created_at timestamptz not null default now()
);

create index if not exists idx_config_change_log on config_change_log (tenant_id, config_type, config_id, created_at desc);
