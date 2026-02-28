create table if not exists sys_user (
    id bigserial primary key,
    tenant_id bigint not null,
    username varchar(64) not null,
    display_name varchar(128) not null,
    email varchar(128),
    mobile varchar(32),
    status varchar(32) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    created_by bigint,
    updated_at timestamptz not null default now(),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_sys_user unique (tenant_id, username)
);

create index if not exists idx_sys_user_tenant on sys_user (tenant_id, status);

create table if not exists user_role_relation (
    id bigserial primary key,
    tenant_id bigint not null,
    user_id bigint not null references sys_user(id),
    role_code varchar(64) not null,
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    created_by bigint,
    updated_at timestamptz not null default now(),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_user_role_relation unique (tenant_id, user_id, role_code)
);

create index if not exists idx_user_role_lookup on user_role_relation (tenant_id, user_id, enabled);

create table if not exists patch_attachment (
    id bigserial primary key,
    tenant_id bigint not null,
    patch_id bigint not null references patch(id),
    stage varchar(32) not null,
    file_name varchar(255) not null,
    file_url varchar(1024) not null,
    file_hash varchar(128),
    file_size bigint,
    uploader_id bigint not null,
    scan_status varchar(16) not null default 'PENDING',
    created_at timestamptz not null default now()
);

create index if not exists idx_patch_attachment on patch_attachment (tenant_id, patch_id, stage, created_at desc);
