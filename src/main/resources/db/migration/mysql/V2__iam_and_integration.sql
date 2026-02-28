create table if not exists sys_role (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    role_code varchar(64) not null,
    role_name varchar(128) not null,
    role_level varchar(16) not null default 'GLOBAL',
    scope_ref_id bigint,
    enabled boolean not null default true,
    created_at datetime(6) not null default current_timestamp(6),
    created_by bigint,
    updated_at datetime(6) not null default current_timestamp(6),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_sys_role unique (tenant_id, role_code)
);

create table if not exists role_action_permission (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    role_code varchar(64) not null,
    action varchar(64) not null,
    enabled boolean not null default true,
    created_at datetime(6) not null default current_timestamp(6),
    created_by bigint,
    updated_at datetime(6) not null default current_timestamp(6),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_role_action unique (tenant_id, role_code, action)
);

create index idx_role_action_lookup on role_action_permission (tenant_id, action, enabled);

create table if not exists user_data_scope (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    user_id bigint not null,
    scope_type varchar(32) not null,
    scope_value varchar(128),
    enabled boolean not null default true,
    created_at datetime(6) not null default current_timestamp(6),
    created_by bigint,
    updated_at datetime(6) not null default current_timestamp(6),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_user_scope unique (tenant_id, user_id, scope_type, scope_value)
);

create index idx_user_scope_lookup on user_data_scope (tenant_id, user_id, enabled);

create table if not exists integration_connector (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    connector_type varchar(32) not null,
    connector_name varchar(128) not null,
    base_url varchar(255),
    auth_config text,
    enabled boolean not null default true,
    created_at datetime(6) not null default current_timestamp(6),
    created_by bigint,
    updated_at datetime(6) not null default current_timestamp(6),
    updated_by bigint,
    is_deleted boolean not null default false
);

create index idx_connector_tenant on integration_connector (tenant_id, connector_type, enabled);
