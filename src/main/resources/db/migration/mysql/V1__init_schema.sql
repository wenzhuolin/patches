create table if not exists patch (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    product_line_id bigint not null,
    patch_no varchar(64) not null,
    title varchar(255) not null,
    description text,
    severity varchar(32),
    priority varchar(32),
    source_version varchar(64),
    target_version varchar(64),
    current_state varchar(32) not null,
    owner_pm_id bigint not null,
    kpi_blocked boolean not null default false,
    qa_blocked boolean not null default false,
    version bigint not null default 0,
    created_at datetime(6) not null default current_timestamp(6),
    created_by bigint,
    updated_at datetime(6) not null default current_timestamp(6),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_patch_tenant_no unique (tenant_id, patch_no)
);

create index idx_patch_list on patch (tenant_id, product_line_id, current_state, updated_at);
create index idx_patch_owner on patch (tenant_id, owner_pm_id, current_state);

create table if not exists patch_transition_log (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    patch_id bigint not null references patch(id),
    from_state varchar(32),
    to_state varchar(32),
    action varchar(64) not null,
    result varchar(16) not null,
    block_type varchar(16) not null,
    block_reason text,
    operator_id bigint not null,
    request_id varchar(128) not null,
    created_at datetime(6) not null default current_timestamp(6),
    constraint uk_transition_request unique (patch_id, request_id)
);

create index idx_transition_patch_time on patch_transition_log (patch_id, created_at);

create table if not exists patch_operation_log (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    biz_type varchar(32) not null,
    biz_id bigint not null,
    action varchar(64) not null,
    before_data json,
    after_data json,
    operator_id bigint not null,
    trace_id varchar(64),
    ip varchar(64),
    user_agent varchar(255),
    created_at datetime(6) not null default current_timestamp(6)
);

create index idx_operation_biz on patch_operation_log (tenant_id, biz_type, biz_id, created_at);

create table if not exists kpi_rule (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    rule_code varchar(64) not null,
    stage varchar(32) not null,
    gate_type varchar(16) not null,
    metric_key varchar(64) not null,
    compare_op varchar(16) not null,
    threshold_value double,
    threshold_value2 double,
    required boolean not null default true,
    missing_data_policy varchar(16) not null default 'FAIL',
    priority int not null default 100,
    scope_type varchar(32) not null default 'GLOBAL',
    scope_value varchar(64),
    effective_from datetime(6),
    effective_to datetime(6),
    enabled boolean not null default true,
    created_at datetime(6) not null default current_timestamp(6),
    created_by bigint,
    updated_at datetime(6) not null default current_timestamp(6),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_kpi_rule unique (tenant_id, rule_code)
);

create index idx_kpi_rule_match on kpi_rule (tenant_id, stage, gate_type, enabled, priority);

create table if not exists kpi_metric_value (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    patch_id bigint not null references patch(id),
    metric_key varchar(64) not null,
    metric_value double not null,
    source_type varchar(32) not null,
    collected_at datetime(6) not null,
    created_at datetime(6) not null default current_timestamp(6)
);

create index idx_kpi_metric_patch on kpi_metric_value (tenant_id, patch_id, metric_key, collected_at);

create table if not exists kpi_evaluation (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    patch_id bigint not null references patch(id),
    stage varchar(32) not null,
    gate_type varchar(16) not null,
    trigger_action varchar(64) not null,
    result varchar(16) not null,
    summary text,
    evaluated_at datetime(6) not null default current_timestamp(6),
    trace_id varchar(64)
);

create index idx_kpi_eval_patch on kpi_evaluation (tenant_id, patch_id, evaluated_at);

create table if not exists kpi_evaluation_detail (
    id bigint auto_increment primary key,
    evaluation_id bigint not null references kpi_evaluation(id),
    rule_id bigint not null references kpi_rule(id),
    metric_value double,
    threshold_snapshot varchar(128),
    pass boolean not null,
    reason text,
    evidence text
);

create index idx_kpi_detail_eval on kpi_evaluation_detail (evaluation_id, rule_id);

create table if not exists qa_policy (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    stage varchar(32) not null,
    approval_mode varchar(16) not null,
    required_levels varchar(255) not null,
    scope_type varchar(32) not null default 'GLOBAL',
    scope_value varchar(64),
    enabled boolean not null default true,
    effective_from datetime(6),
    effective_to datetime(6),
    created_at datetime(6) not null default current_timestamp(6),
    created_by bigint,
    updated_at datetime(6) not null default current_timestamp(6),
    updated_by bigint,
    is_deleted boolean not null default false
);

create index idx_qa_policy_match on qa_policy (tenant_id, stage, enabled, scope_type, scope_value);

create table if not exists qa_task (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    patch_id bigint not null references patch(id),
    stage varchar(32) not null,
    qa_level varchar(32) not null,
    assignee_type varchar(16) not null,
    assignee_id varchar(64) not null,
    sequence_no int not null,
    status varchar(16) not null,
    decision_comment text,
    decided_at datetime(6),
    created_at datetime(6) not null default current_timestamp(6),
    created_by bigint,
    updated_at datetime(6) not null default current_timestamp(6),
    updated_by bigint,
    is_deleted boolean not null default false
);

create index idx_qa_pending on qa_task (tenant_id, assignee_id, status, stage);
create index idx_qa_patch on qa_task (patch_id, stage, qa_level);

create table if not exists qa_decision_log (
    id bigint auto_increment primary key,
    qa_task_id bigint not null references qa_task(id),
    decision varchar(16) not null,
    comment text,
    operator_id bigint not null,
    created_at datetime(6) not null default current_timestamp(6)
);

create table if not exists test_task (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    patch_id bigint not null references patch(id),
    task_no varchar(64) not null,
    tester_id bigint,
    status varchar(32) not null,
    case_prepare_rate double,
    env_ready boolean,
    case_execution_rate double,
    defect_density double,
    started_at datetime(6),
    completed_at datetime(6),
    created_at datetime(6) not null default current_timestamp(6),
    created_by bigint,
    updated_at datetime(6) not null default current_timestamp(6),
    updated_by bigint,
    is_deleted boolean not null default false,
    constraint uk_test_task unique (tenant_id, task_no)
);

create table if not exists review_session (
    id bigint auto_increment primary key,
    tenant_id bigint not null,
    patch_id bigint not null references patch(id),
    mode varchar(16) not null,
    meeting_tool varchar(64),
    meeting_url varchar(255),
    quorum_required double,
    approve_rate_required double,
    status varchar(16) not null,
    conclusion varchar(16),
    created_at datetime(6) not null default current_timestamp(6),
    created_by bigint,
    updated_at datetime(6) not null default current_timestamp(6),
    updated_by bigint,
    is_deleted boolean not null default false
);

create table if not exists review_vote (
    id bigint auto_increment primary key,
    session_id bigint not null references review_session(id),
    voter_id bigint not null,
    vote varchar(16) not null,
    comment text,
    voted_at datetime(6) not null default current_timestamp(6),
    constraint uk_review_vote unique (session_id, voter_id)
);
