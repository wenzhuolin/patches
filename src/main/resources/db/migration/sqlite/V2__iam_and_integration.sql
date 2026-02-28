create table if not exists sys_role (
    id integer primary key autoincrement,
    tenant_id bigint not null,
    role_code varchar(64) not null,
    role_name varchar(128) not null,
    role_level varchar(16) not null default 'GLOBAL',
    scope_ref_id bigint,
    enabled boolean not null default true,
    created_at bigint not null default (unixepoch('now') * 1000),
    created_by bigint,
    updated_at bigint not null default (unixepoch('now') * 1000),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_sys_role unique (tenant_id, role_code)
);

create table if not exists role_action_permission (
    id integer primary key autoincrement,
    tenant_id bigint not null,
    role_code varchar(64) not null,
    action varchar(64) not null,
    enabled boolean not null default true,
    created_at bigint not null default (unixepoch('now') * 1000),
    created_by bigint,
    updated_at bigint not null default (unixepoch('now') * 1000),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_role_action unique (tenant_id, role_code, action)
);

create index if not exists idx_role_action_lookup on role_action_permission (tenant_id, action, enabled);

create table if not exists user_data_scope (
    id integer primary key autoincrement,
    tenant_id bigint not null,
    user_id bigint not null,
    scope_type varchar(32) not null,
    scope_value varchar(128),
    enabled boolean not null default true,
    created_at bigint not null default (unixepoch('now') * 1000),
    created_by bigint,
    updated_at bigint not null default (unixepoch('now') * 1000),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_user_scope unique (tenant_id, user_id, scope_type, scope_value)
);

create index if not exists idx_user_scope_lookup on user_data_scope (tenant_id, user_id, enabled);

create table if not exists integration_connector (
    id integer primary key autoincrement,
    tenant_id bigint not null,
    connector_type varchar(32) not null,
    connector_name varchar(128) not null,
    base_url varchar(255),
    auth_config text,
    enabled boolean not null default true,
    created_at bigint not null default (unixepoch('now') * 1000),
    created_by bigint,
    updated_at bigint not null default (unixepoch('now') * 1000),
    updated_by bigint,
    is_deleted boolean not null default false
);

create index if not exists idx_connector_tenant on integration_connector (tenant_id, connector_type, enabled);
