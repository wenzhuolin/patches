const appRoot = document.getElementById("app");
const toastRoot = document.getElementById("toast-root");

const SESSION_KEY = "patch-portal-session";
const PATCH_FORM_DRAFT_KEY = "patch-portal-patch-draft";
const CONFIG_COLUMNS_KEY = "patch-portal-columns";
const REVIEW_MATERIAL_KEY_PREFIX = "patch-portal-review-material-";
const REVIEW_DISCUSS_KEY_PREFIX = "patch-portal-review-discuss-";

const PATCH_STEPS = [
    "DRAFT",
    "REVIEWING",
    "REVIEW_PASSED",
    "TESTING",
    "TEST_PASSED",
    "RELEASE_READY",
    "RELEASED",
    "ARCHIVED"
];

const STAGE_OPTIONS = [
    "REVIEW",
    "TRANSFER_TEST",
    "RELEASE"
];

const GATE_OPTIONS = ["ENTRY", "EXIT"];

const NAV_ITEMS = [
    { key: "dashboard", label: "仪表盘", route: "#/dashboard", group: "业务导航" },
    { key: "patches", label: "补丁管理", route: "#/patches", group: "业务导航" },
    { key: "patch-new", label: "创建补丁", route: "#/patches/new", group: "业务导航" },
    { key: "transfer", label: "转测管理", route: "#/transfer", group: "业务导航" },
    { key: "reviews", label: "评审管理", route: "#/reviews", group: "业务导航" },
    { key: "review-new", label: "发起评审", route: "#/reviews/new", group: "业务导航" },
    { key: "config", label: "配置中心", route: "#/config", group: "系统配置" },
    { key: "mail", label: "邮件通知", route: "#/mail", group: "系统配置" },
    { key: "ops", label: "运维面板", route: "#/ops", group: "系统配置" }
];

const ROUTE_KEY_MAP = {
    dashboard: "dashboard",
    patches: "patches",
    "patch-new": "patch-new",
    "patch-detail": "patches",
    transfer: "transfer",
    reviews: "reviews",
    "review-new": "review-new",
    "review-detail": "reviews",
    config: "config",
    mail: "mail",
    ops: "ops"
};

const ACTION_LABELS = {
    SUBMIT_REVIEW: "提交评审",
    APPROVE_REVIEW: "评审通过",
    REJECT_REVIEW: "评审驳回",
    TRANSFER_TO_TEST: "转测申请",
    PASS_TEST: "测试通过",
    FAIL_TEST: "测试失败",
    PREPARE_RELEASE: "发布准备",
    RELEASE: "执行发布",
    ARCHIVE: "流程归档"
};

const NEXT_ACTION_BY_STATE = {
    DRAFT: "SUBMIT_REVIEW",
    REVIEWING: "APPROVE_REVIEW",
    REVIEW_PASSED: "TRANSFER_TO_TEST",
    TESTING: "PASS_TEST",
    TEST_PASSED: "PREPARE_RELEASE",
    RELEASE_READY: "RELEASE",
    RELEASED: "ARCHIVE"
};

const COLUMN_DEFAULTS = {
    patchNo: true,
    title: true,
    productLineId: true,
    currentState: true,
    kpiStatus: true,
    priority: true,
    ownerPmId: true,
    updatedAt: true
};

const state = {
    session: loadSession(),
    patchTableColumns: loadPatchColumns(),
    patchListView: "table"
};

boot();

function boot() {
    if (!window.location.hash) {
        window.location.hash = "#/dashboard";
    }
    window.addEventListener("hashchange", () => {
        renderApp().catch((error) => renderFatal(error));
    });
    renderApp().catch((error) => renderFatal(error));
}

async function renderApp() {
    if (!state.session) {
        renderLoginPage();
        return;
    }
    renderShell();
    await renderCurrentPage();
}

function renderShell() {
    const route = parseRoute();
    const activeKey = ROUTE_KEY_MAP[route.name] || "dashboard";
    const groupedNav = NAV_ITEMS.reduce((acc, item) => {
        if (!acc[item.group]) {
            acc[item.group] = [];
        }
        acc[item.group].push(item);
        return acc;
    }, {});
    const navHtml = Object.entries(groupedNav)
        .map(([group, items]) => {
            const links = items
                .map((item) => `<a class="nav-link ${item.key === activeKey ? "active" : ""}" href="${item.route}">${item.label}</a>`)
                .join("");
            return `<div class="nav-group"><div class="nav-title">${group}</div>${links}</div>`;
        })
        .join("");

    appRoot.innerHTML = `
        <div class="shell">
            <aside class="sidebar">
                <div class="brand">补丁生命周期管理</div>
                ${navHtml}
            </aside>
            <div class="main">
                <header class="topbar">
                    <div class="breadcrumb" id="breadcrumb"></div>
                    <div class="top-actions">
                        <span class="top-user">租户 ${escapeHtml(state.session.tenantId)} · 用户 ${escapeHtml(state.session.userId)} · 角色 ${escapeHtml(state.session.roles)}</span>
                        <button class="btn-outline" id="switch-context-btn">切换上下文</button>
                        <button class="btn-outline" id="logout-btn">退出</button>
                    </div>
                </header>
                <div class="content" id="page-content"></div>
            </div>
        </div>
    `;

    document.getElementById("switch-context-btn").addEventListener("click", () => {
        state.session = null;
        saveSession(null);
        renderLoginPage();
    });

    document.getElementById("logout-btn").addEventListener("click", () => {
        state.session = null;
        saveSession(null);
        toast("已退出当前会话", "success");
        renderLoginPage();
    });
}

async function renderCurrentPage() {
    const route = parseRoute();
    const breadcrumb = buildBreadcrumb(route);
    const breadcrumbDom = document.getElementById("breadcrumb");
    if (breadcrumbDom) {
        breadcrumbDom.textContent = breadcrumb.join(" / ");
    }

    switch (route.name) {
        case "dashboard":
            await renderDashboardPage();
            break;
        case "patches":
            await renderPatchListPage();
            break;
        case "patch-new":
            await renderPatchCreatePage();
            break;
        case "patch-detail":
            await renderPatchDetailPage(route.params.patchId);
            break;
        case "transfer":
            await renderTransferPage();
            break;
        case "reviews":
            await renderReviewListPage();
            break;
        case "review-new":
            await renderReviewCreatePage();
            break;
        case "review-detail":
            await renderReviewDetailPage(route.params.sessionId);
            break;
        case "config":
            await renderConfigPage(route.query.tab || "scenarios");
            break;
        case "mail":
            await renderMailPage(route.query.tab || "servers");
            break;
        case "ops":
            renderOpsPage();
            break;
        default:
            renderNotFound();
            break;
    }
}

function renderLoginPage() {
    appRoot.innerHTML = `
        <div class="app-login">
            <div class="login-card">
                <h1 class="login-title">补丁管理系统 · 登录</h1>
                <p class="login-subtitle">当前后端为 API 模式，这里使用业务上下文登录（请求头模拟）。</p>
                <form id="login-form" class="form-grid">
                    <div class="form-item">
                        <label>用户名</label>
                        <input name="username" value="admin" required />
                    </div>
                    <div class="form-item">
                        <label>密码（演示用途）</label>
                        <input name="password" type="password" value="admin123" required />
                    </div>
                    <div class="form-item">
                        <label>租户ID</label>
                        <input name="tenantId" value="1" required />
                    </div>
                    <div class="form-item">
                        <label>用户ID</label>
                        <input name="userId" value="1" required />
                    </div>
                    <div class="form-item full">
                        <label>角色（逗号分隔）</label>
                        <input name="roles" value="SUPER_ADMIN,LINE_ADMIN,PM,REVIEWER,PRODUCT_LINE_QA,TEST,DEV" required />
                    </div>
                    <div class="form-item full">
                        <button class="btn-primary" type="submit">进入系统</button>
                    </div>
                </form>
                <p class="login-subtitle">建议测试账号角色包含 SUPER_ADMIN，便于完整体验配置中心与邮件配置页面。</p>
            </div>
        </div>
    `;

    const form = document.getElementById("login-form");
    form.addEventListener("submit", (event) => {
        event.preventDefault();
        const data = new FormData(form);
        const tenantId = String(data.get("tenantId") || "").trim();
        const userId = String(data.get("userId") || "").trim();
        const roles = String(data.get("roles") || "").trim();
        const username = String(data.get("username") || "").trim();
        const password = String(data.get("password") || "").trim();

        if (!tenantId || !userId || !roles || !username || !password) {
            toast("请完整填写登录信息", "warning");
            return;
        }

        state.session = {
            tenantId,
            userId,
            roles,
            username
        };
        saveSession(state.session);
        toast("登录成功", "success");
        if (!window.location.hash || window.location.hash === "#/login") {
            window.location.hash = "#/dashboard";
        }
        renderApp().catch((error) => renderFatal(error));
    });
}

async function renderDashboardPage() {
    const content = document.getElementById("page-content");
    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">仪表盘</h1>
                <p class="page-subtitle">补丁生命周期全局概览、待办与风险预警。</p>
            </div>
            <div class="toolbar">
                <button class="btn-primary" id="quick-create-patch">新建补丁</button>
                <button class="btn-outline" id="quick-open-qa">查看待审</button>
                <button class="btn-outline" id="quick-review-new">发起评审</button>
            </div>
        </div>
        <div class="card">正在加载仪表盘数据...</div>
    `;

    const [patches, qaPending, reviewSessions, kpiRules, activities] = await Promise.all([
        safeApi(() => api.listPatches(), []),
        safeApi(() => api.listQaPending(), []),
        safeApi(() => api.listReviewSessions(), []),
        safeApi(() => api.listKpiRules(), []),
        safeApi(() => api.listAuditLogs("PATCH"), [])
    ]);

    const total = patches.length;
    const kpiBlocked = patches.filter((patch) => patch.kpiBlocked).length;
    const qaBlocked = patches.filter((patch) => patch.qaBlocked).length;
    const reviewingCount = patches.filter((patch) => patch.currentState === "REVIEWING").length;
    const testingCount = patches.filter((patch) => patch.currentState === "TESTING").length;
    const todoCount = qaPending.length + reviewSessions.filter((item) => item.status === "OPEN").length;

    const stateCountMap = PATCH_STEPS.reduce((acc, step) => {
        acc[step] = patches.filter((patch) => patch.currentState === step).length;
        return acc;
    }, {});
    const maxStateCount = Math.max(...Object.values(stateCountMap), 1);

    const warningList = patches
        .filter((patch) => patch.kpiBlocked || patch.qaBlocked)
        .slice(0, 8);

    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">仪表盘</h1>
                <p class="page-subtitle">补丁生命周期全局概览、待办与风险预警。</p>
            </div>
            <div class="toolbar">
                <button class="btn-primary" id="quick-create-patch">新建补丁</button>
                <button class="btn-outline" id="quick-open-qa">查看待审</button>
                <button class="btn-outline" id="quick-review-new">发起评审</button>
            </div>
        </div>

        <div class="metrics-grid">
            ${metricCard("补丁总量", total)}
            ${metricCard("当前待办", todoCount)}
            ${metricCard("评审中", reviewingCount)}
            ${metricCard("测试中", testingCount)}
            ${metricCard("KPI 阻断", kpiBlocked)}
            ${metricCard("QA 阻断", qaBlocked)}
            ${metricCard("待审任务", qaPending.length)}
            ${metricCard("KPI规则数", kpiRules.length)}
        </div>

        <div class="panel-grid">
            <div class="card">
                <h3>补丁状态分布</h3>
                <div class="list-reset">
                    ${PATCH_STEPS.map((step) => {
                        const count = stateCountMap[step];
                        const width = Math.max(6, Math.round((count / maxStateCount) * 100));
                        return `
                            <div class="list-item">
                                <div style="display:flex;justify-content:space-between;gap:8px;">
                                    <strong>${stateLabel(step)}</strong>
                                    <span>${count}</span>
                                </div>
                                <div style="margin-top:6px;height:8px;background:#e5e7eb;border-radius:999px;overflow:hidden;">
                                    <div style="width:${width}%;height:8px;background:#93c5fd;"></div>
                                </div>
                            </div>
                        `;
                    }).join("")}
                </div>
            </div>

            <div class="card">
                <h3>KPI卡点预警</h3>
                ${warningList.length ? `
                    <ul class="list-reset">
                        ${warningList.map((patch) => `
                            <li class="list-item">
                                <div><strong>${escapeHtml(patch.patchNo)} · ${escapeHtml(patch.title)}</strong></div>
                                <div class="timeline-time">
                                    ${patch.kpiBlocked ? "KPI阻断" : ""}${patch.kpiBlocked && patch.qaBlocked ? " / " : ""}${patch.qaBlocked ? "QA阻断" : ""}
                                </div>
                            </li>
                        `).join("")}
                    </ul>
                ` : `<p class="page-subtitle">暂无阻断预警。</p>`}
            </div>
        </div>

        <div class="panel-grid">
            <div class="card">
                <h3>我的待处理任务</h3>
                <div class="two-col">
                    <div>
                        <h4>QA待审</h4>
                        ${qaPending.length ? `
                            <ul class="list-reset">
                                ${qaPending.slice(0, 6).map((task) => `
                                    <li class="list-item">补丁 #${escapeHtml(String(task.patchId))} · ${escapeHtml(task.stage || "-")} · ${escapeHtml(task.status || "-")}</li>
                                `).join("")}
                            </ul>
                        ` : `<p class="page-subtitle">暂无QA待办。</p>`}
                    </div>
                    <div>
                        <h4>评审会任务</h4>
                        ${reviewSessions.length ? `
                            <ul class="list-reset">
                                ${reviewSessions.slice(0, 6).map((session) => `
                                    <li class="list-item">评审 #${escapeHtml(String(session.sessionId))} · 补丁${escapeHtml(String(session.patchId))} · ${escapeHtml(session.status || "-")}</li>
                                `).join("")}
                            </ul>
                        ` : `<p class="page-subtitle">暂无评审任务。</p>`}
                    </div>
                </div>
            </div>
            <div class="card">
                <h3>近期活动</h3>
                ${activities.length ? `
                    <ul class="list-reset">
                        ${activities.slice(0, 8).map((item) => `
                            <li class="list-item">
                                <div><strong>${escapeHtml(item.action || "操作")}</strong> · ${escapeHtml(item.bizType || "-")}#${escapeHtml(String(item.bizId || "-"))}</div>
                                <div class="timeline-time">${formatDateTime(item.createdAt)}</div>
                            </li>
                        `).join("")}
                    </ul>
                ` : `<p class="page-subtitle">暂无近期活动数据。</p>`}
            </div>
        </div>
    `;

    document.getElementById("quick-create-patch").addEventListener("click", () => navigate("#/patches/new"));
    document.getElementById("quick-open-qa").addEventListener("click", () => navigate("#/patches?qa=blocked"));
    document.getElementById("quick-review-new").addEventListener("click", () => navigate("#/reviews/new"));
}

async function renderPatchListPage() {
    const content = document.getElementById("page-content");
    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">补丁列表</h1>
                <p class="page-subtitle">支持多维筛选、列表/看板视图切换、批量操作。</p>
            </div>
            <div class="toolbar">
                <button class="btn-primary" id="new-patch-btn">新建补丁</button>
            </div>
        </div>
        <div class="card">正在加载补丁数据...</div>
    `;

    const [patches, products, scenarios] = await Promise.all([
        safeApi(() => api.listPatches(), []),
        safeApi(() => api.listProducts(), []),
        safeApi(() => api.listScenarios(), [])
    ]);

    const productMap = Object.fromEntries(products.map((product) => [String(product.id), product]));
    const scenarioMap = Object.fromEntries(scenarios.map((scenario) => [String(scenario.id), scenario]));

    const view = state.patchListView || "table";
    const filters = {
        keyword: "",
        state: "",
        productLineId: "",
        scenarioId: "",
        kpi: "",
        qa: ""
    };
    const pageState = { page: 1, pageSize: 10 };
    const selectedIds = new Set();

    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">补丁列表</h1>
                <p class="page-subtitle">支持多维筛选、列表/看板视图切换、批量操作。</p>
            </div>
            <div class="toolbar">
                <button class="btn-primary" id="new-patch-btn">新建补丁</button>
                <button class="btn-outline ${view === "table" ? "active" : ""}" id="view-table-btn">列表视图</button>
                <button class="btn-outline ${view === "kanban" ? "active" : ""}" id="view-kanban-btn">看板视图</button>
            </div>
        </div>

        <div class="card">
            <div class="filters">
                <input id="filter-keyword" placeholder="搜索补丁编号/名称" />
                <select id="filter-state">
                    <option value="">全部状态</option>
                    ${PATCH_STEPS.map((step) => `<option value="${step}">${stateLabel(step)}</option>`).join("")}
                </select>
                <select id="filter-product">
                    <option value="">全部产品</option>
                    ${products.map((product) => `<option value="${product.id}">${escapeHtml(product.productName || product.productCode || String(product.id))}</option>`).join("")}
                </select>
                <select id="filter-scenario">
                    <option value="">全部场景</option>
                    ${scenarios.map((scenario) => `<option value="${scenario.id}">${escapeHtml(scenario.scenarioName || scenario.scenarioCode || String(scenario.id))}</option>`).join("")}
                </select>
                <select id="filter-kpi">
                    <option value="">KPI状态(全部)</option>
                    <option value="ok">KPI达标</option>
                    <option value="blocked">KPI阻断</option>
                </select>
                <select id="filter-qa">
                    <option value="">QA状态(全部)</option>
                    <option value="ok">QA达标</option>
                    <option value="blocked">QA阻断</option>
                </select>
            </div>
            <div class="toolbar" style="margin-top:10px;">
                <select id="batch-action">
                    <option value="">批量操作</option>
                    <option value="SUBMIT_REVIEW">批量提交评审</option>
                    <option value="TRANSFER_TO_TEST">批量转测</option>
                    <option value="RELEASE">批量发布</option>
                </select>
                <button class="btn-outline" id="batch-run-btn">执行批量操作</button>
                <details>
                    <summary class="tag" style="cursor:pointer;">列显示设置</summary>
                    <div id="column-setting-box" class="card" style="margin-top:8px;"></div>
                </details>
                <span class="page-subtitle">共 ${patches.length} 条补丁</span>
            </div>
            <div id="patch-list-container"></div>
        </div>
    `;

    document.getElementById("new-patch-btn").addEventListener("click", () => navigate("#/patches/new"));
    document.getElementById("view-table-btn").addEventListener("click", () => {
        state.patchListView = "table";
        renderPatchListPage().catch(renderFatal);
    });
    document.getElementById("view-kanban-btn").addEventListener("click", () => {
        state.patchListView = "kanban";
        renderPatchListPage().catch(renderFatal);
    });

    const columnSettingBox = document.getElementById("column-setting-box");
    columnSettingBox.innerHTML = Object.keys(COLUMN_DEFAULTS)
        .map((key) => {
            const checked = state.patchTableColumns[key] ? "checked" : "";
            return `<label style="display:block;margin-bottom:6px;"><input data-col="${key}" type="checkbox" ${checked}> ${columnLabel(key)}</label>`;
        })
        .join("");
    columnSettingBox.addEventListener("change", (event) => {
        const target = event.target;
        if (target instanceof HTMLInputElement && target.dataset.col) {
            state.patchTableColumns[target.dataset.col] = target.checked;
            savePatchColumns(state.patchTableColumns);
            renderList();
        }
    });

    document.getElementById("filter-keyword").addEventListener("input", (event) => {
        filters.keyword = event.target.value.trim();
        pageState.page = 1;
        renderList();
    });
    document.getElementById("filter-state").addEventListener("change", (event) => {
        filters.state = event.target.value;
        pageState.page = 1;
        renderList();
    });
    document.getElementById("filter-product").addEventListener("change", (event) => {
        filters.productLineId = event.target.value;
        pageState.page = 1;
        renderList();
    });
    document.getElementById("filter-scenario").addEventListener("change", (event) => {
        filters.scenarioId = event.target.value;
        pageState.page = 1;
        renderList();
    });
    document.getElementById("filter-kpi").addEventListener("change", (event) => {
        filters.kpi = event.target.value;
        pageState.page = 1;
        renderList();
    });
    document.getElementById("filter-qa").addEventListener("change", (event) => {
        filters.qa = event.target.value;
        pageState.page = 1;
        renderList();
    });

    document.getElementById("batch-run-btn").addEventListener("click", async () => {
        const action = document.getElementById("batch-action").value;
        if (!action) {
            toast("请选择批量操作类型", "warning");
            return;
        }
        if (!selectedIds.size) {
            toast("请先勾选补丁", "warning");
            return;
        }
        const ids = Array.from(selectedIds);
        let success = 0;
        for (const patchId of ids) {
            try {
                await api.executePatchAction(patchId, { action, comment: "批量操作触发" });
                success += 1;
            } catch (error) {
                toast(`补丁 ${patchId} 处理失败：${error.message}`, "error");
            }
        }
        toast(`批量操作完成：成功 ${success}/${ids.length}`, success === ids.length ? "success" : "warning");
        renderPatchListPage().catch(renderFatal);
    });

    renderList();

    function filterPatches() {
        return patches.filter((patch) => {
            if (filters.keyword) {
                const haystack = `${patch.patchNo || ""} ${patch.title || ""}`.toLowerCase();
                if (!haystack.includes(filters.keyword.toLowerCase())) {
                    return false;
                }
            }
            if (filters.state && patch.currentState !== filters.state) {
                return false;
            }
            if (filters.productLineId && String(patch.productLineId || "") !== String(filters.productLineId)) {
                return false;
            }
            if (filters.scenarioId) {
                const product = productMap[String(patch.productLineId || "")];
                const scenarioIds = product?.scenarioIds || [];
                if (!scenarioIds.map(String).includes(String(filters.scenarioId))) {
                    return false;
                }
            }
            if (filters.kpi === "ok" && patch.kpiBlocked) {
                return false;
            }
            if (filters.kpi === "blocked" && !patch.kpiBlocked) {
                return false;
            }
            if (filters.qa === "ok" && patch.qaBlocked) {
                return false;
            }
            if (filters.qa === "blocked" && !patch.qaBlocked) {
                return false;
            }
            return true;
        });
    }

    function renderList() {
        const container = document.getElementById("patch-list-container");
        const filtered = filterPatches();

        if (state.patchListView === "kanban") {
            const kanbanHtml = `
                <div class="kanban">
                    ${["DRAFT", "REVIEWING", "REVIEW_PASSED", "TESTING", "TEST_PASSED", "RELEASE_READY", "RELEASED", "ARCHIVED"].map((status) => {
                        const cards = filtered.filter((patch) => patch.currentState === status);
                        return `
                            <div class="kanban-col">
                                <h4>${stateLabel(status)} (${cards.length})</h4>
                                ${cards.map((patch) => `
                                    <div class="kanban-card">
                                        <div><strong>${escapeHtml(patch.patchNo || "-")}</strong></div>
                                        <div>${escapeHtml(patch.title || "-")}</div>
                                        <div style="margin-top:6px;">${renderKpiTag(patch)}</div>
                                        <div class="toolbar" style="margin-top:8px;">
                                            <button class="btn-outline" data-go-detail="${patch.patchId}">详情</button>
                                        </div>
                                    </div>
                                `).join("") || `<p class="page-subtitle">暂无补丁</p>`}
                            </div>
                        `;
                    }).join("")}
                </div>
            `;
            container.innerHTML = kanbanHtml;
            container.querySelectorAll("[data-go-detail]").forEach((button) => {
                button.addEventListener("click", () => navigate(`#/patches/${button.dataset.goDetail}`));
            });
            return;
        }

        const paged = paginate(filtered, pageState.page, pageState.pageSize);
        const visibleColumns = Object.entries(state.patchTableColumns).filter(([, visible]) => visible).map(([key]) => key);

        container.innerHTML = `
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th><input type="checkbox" id="select-all-patches"></th>
                        ${visibleColumns.map((column) => `<th>${columnLabel(column)}</th>`).join("")}
                        <th>操作</th>
                    </tr>
                    </thead>
                    <tbody>
                    ${paged.items.length ? paged.items.map((patch) => {
                        const product = productMap[String(patch.productLineId || "")];
                        const rowMap = {
                            patchNo: escapeHtml(patch.patchNo || "-"),
                            title: escapeHtml(patch.title || "-"),
                            productLineId: escapeHtml(product?.productName || String(patch.productLineId || "-")),
                            currentState: `<span class="tag">${stateLabel(patch.currentState)}</span>`,
                            kpiStatus: renderKpiTag(patch),
                            priority: escapeHtml(patch.priority || "-"),
                            ownerPmId: escapeHtml(String(patch.ownerPmId || "-")),
                            updatedAt: formatDateTime(patch.updatedAt)
                        };
                        const nextAction = NEXT_ACTION_BY_STATE[patch.currentState];
                        const nextActionLabel = ACTION_LABELS[nextAction] || "下一步";
                        const nextDisabledReason = nextAction ? actionDisabledReason(patch, nextAction) : "当前状态无可执行动作";
                        const disabledAttr = nextDisabledReason ? "disabled" : "";
                        return `
                            <tr>
                                <td><input type="checkbox" data-select-patch="${patch.patchId}" ${selectedIds.has(patch.patchId) ? "checked" : ""}></td>
                                ${visibleColumns.map((column) => `<td>${rowMap[column] ?? "-"}</td>`).join("")}
                                <td>
                                    <div class="toolbar">
                                        <button class="btn-outline" data-go-detail="${patch.patchId}">详情</button>
                                        <button class="btn-primary" data-run-action="${patch.patchId}" data-action="${nextAction || ""}" ${disabledAttr} title="${escapeHtml(nextDisabledReason || "")}">
                                            ${escapeHtml(nextActionLabel)}
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        `;
                    }).join("") : `<tr><td colspan="${visibleColumns.length + 2}" class="page-subtitle">暂无数据</td></tr>`}
                    </tbody>
                </table>
            </div>
            <div class="toolbar" style="margin-top:10px;">
                <button class="btn-outline" id="prev-page" ${paged.page <= 1 ? "disabled" : ""}>上一页</button>
                <span class="page-subtitle">第 ${paged.page} / ${paged.totalPages} 页（共 ${paged.total} 条）</span>
                <button class="btn-outline" id="next-page" ${paged.page >= paged.totalPages ? "disabled" : ""}>下一页</button>
                <select id="page-size-select">
                    ${[10, 20, 50].map((size) => `<option value="${size}" ${size === pageState.pageSize ? "selected" : ""}>每页 ${size} 条</option>`).join("")}
                </select>
            </div>
        `;

        const selectAll = document.getElementById("select-all-patches");
        if (selectAll) {
            selectAll.checked = paged.items.length > 0 && paged.items.every((patch) => selectedIds.has(patch.patchId));
            selectAll.addEventListener("change", () => {
                paged.items.forEach((patch) => {
                    if (selectAll.checked) {
                        selectedIds.add(patch.patchId);
                    } else {
                        selectedIds.delete(patch.patchId);
                    }
                });
                renderList();
            });
        }

        container.querySelectorAll("[data-select-patch]").forEach((checkbox) => {
            checkbox.addEventListener("change", () => {
                const id = Number(checkbox.dataset.selectPatch);
                if (checkbox.checked) {
                    selectedIds.add(id);
                } else {
                    selectedIds.delete(id);
                }
            });
        });

        container.querySelectorAll("[data-go-detail]").forEach((button) => {
            button.addEventListener("click", () => navigate(`#/patches/${button.dataset.goDetail}`));
        });

        container.querySelectorAll("[data-run-action]").forEach((button) => {
            button.addEventListener("click", async () => {
                const patchId = Number(button.dataset.runAction);
                const action = button.dataset.action;
                if (!action) {
                    toast("当前状态无下一步动作", "warning");
                    return;
                }
                const comment = window.prompt(`请输入操作备注（${ACTION_LABELS[action]}）`, "");
                try {
                    await api.executePatchAction(patchId, { action, comment: comment || "" });
                    toast(`${ACTION_LABELS[action]}成功`, "success");
                    renderPatchListPage().catch(renderFatal);
                } catch (error) {
                    toast(error.message, "error");
                }
            });
        });

        const prev = document.getElementById("prev-page");
        const next = document.getElementById("next-page");
        const sizeSelect = document.getElementById("page-size-select");
        prev.addEventListener("click", () => {
            pageState.page = Math.max(1, pageState.page - 1);
            renderList();
        });
        next.addEventListener("click", () => {
            pageState.page = Math.min(paged.totalPages, pageState.page + 1);
            renderList();
        });
        sizeSelect.addEventListener("change", () => {
            pageState.pageSize = Number(sizeSelect.value);
            pageState.page = 1;
            renderList();
        });
    }
}

async function renderPatchCreatePage() {
    const content = document.getElementById("page-content");
    const [products, scenarios] = await Promise.all([
        safeApi(() => api.listProducts(), []),
        safeApi(() => api.listScenarios(), [])
    ]);
    const draft = loadJson(PATCH_FORM_DRAFT_KEY, null);

    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">创建 / 编辑补丁</h1>
                <p class="page-subtitle">填写基础信息、关联产品、优先级与附件后提交流程。</p>
            </div>
        </div>
        <div class="card">
            <form id="patch-form" class="form-grid">
                <div class="form-item">
                    <label>补丁名称 *</label>
                    <input name="title" required value="${escapeHtml(draft?.title || "")}">
                </div>
                <div class="form-item">
                    <label>补丁优先级</label>
                    <select name="priority">
                        ${["P0", "P1", "P2", "P3"].map((level) => `<option value="${level}" ${draft?.priority === level ? "selected" : ""}>${level}</option>`).join("")}
                    </select>
                </div>
                <div class="form-item">
                    <label>交付场景</label>
                    <select name="scenarioId" id="patch-scenario-select">
                        <option value="">请选择场景</option>
                        ${scenarios.map((scenario) => `<option value="${scenario.id}" ${String(draft?.scenarioId || "") === String(scenario.id) ? "selected" : ""}>${escapeHtml(scenario.scenarioName || scenario.scenarioCode)}</option>`).join("")}
                    </select>
                </div>
                <div class="form-item">
                    <label>关联产品 *</label>
                    <select name="productLineId" id="patch-product-select" required></select>
                </div>
                <div class="form-item">
                    <label>严重程度</label>
                    <select name="severity">
                        ${["LOW", "MEDIUM", "HIGH", "CRITICAL"].map((level) => `<option value="${level}" ${draft?.severity === level ? "selected" : ""}>${level}</option>`).join("")}
                    </select>
                </div>
                <div class="form-item">
                    <label>负责人用户ID *</label>
                    <input name="ownerPmId" required value="${escapeHtml(draft?.ownerPmId || state.session.userId)}">
                </div>
                <div class="form-item">
                    <label>源版本</label>
                    <input name="sourceVersion" value="${escapeHtml(draft?.sourceVersion || "")}">
                </div>
                <div class="form-item">
                    <label>目标版本</label>
                    <input name="targetVersion" value="${escapeHtml(draft?.targetVersion || "")}">
                </div>
                <div class="form-item full">
                    <label>补丁描述</label>
                    <textarea name="description">${escapeHtml(draft?.description || "")}</textarea>
                </div>
                <div class="form-item full">
                    <label><input type="checkbox" name="autoSubmitReview" ${draft?.autoSubmitReview ? "checked" : ""}> 提交后自动进入评审（执行 SUBMIT_REVIEW）</label>
                </div>
            </form>
        </div>
        <div class="card">
            <h3>附件（可选）</h3>
            <div id="attachment-editor"></div>
            <div class="toolbar">
                <button class="btn-outline" id="add-attachment-row">新增附件行</button>
            </div>
        </div>
        <div class="card">
            <div class="toolbar">
                <button class="btn-outline" id="save-draft-btn">保存草稿</button>
                <button class="btn-primary" id="submit-patch-btn">提交补丁</button>
                <button class="btn-outline" id="cancel-create-btn">返回列表</button>
            </div>
        </div>
    `;

    const attachmentEditor = document.getElementById("attachment-editor");
    let attachmentRows = Array.isArray(draft?.attachments) && draft.attachments.length
        ? draft.attachments
        : [{ stage: "DRAFT", fileName: "", fileUrl: "" }];

    const scenarioSelect = document.getElementById("patch-scenario-select");
    const productSelect = document.getElementById("patch-product-select");
    renderProductOptions();
    renderAttachmentRows();

    scenarioSelect.addEventListener("change", () => renderProductOptions());
    document.getElementById("add-attachment-row").addEventListener("click", () => {
        attachmentRows.push({ stage: "DRAFT", fileName: "", fileUrl: "" });
        renderAttachmentRows();
    });
    document.getElementById("cancel-create-btn").addEventListener("click", () => navigate("#/patches"));

    document.getElementById("save-draft-btn").addEventListener("click", () => {
        const draftData = collectFormData();
        saveJson(PATCH_FORM_DRAFT_KEY, draftData);
        toast("草稿已保存", "success");
    });

    document.getElementById("submit-patch-btn").addEventListener("click", async () => {
        const payload = collectPayload();
        if (!payload) {
            return;
        }
        const submitButton = document.getElementById("submit-patch-btn");
        submitButton.disabled = true;
        submitButton.textContent = "提交中...";
        try {
            const patch = await api.createPatch(payload.base);
            for (const attachment of payload.attachments) {
                await api.createAttachment(patch.patchId, attachment);
            }
            if (payload.autoSubmitReview) {
                await api.executePatchAction(patch.patchId, { action: "SUBMIT_REVIEW", comment: "创建页自动提交评审" });
            }
            localStorage.removeItem(PATCH_FORM_DRAFT_KEY);
            toast("补丁创建成功", "success");
            navigate(`#/patches/${patch.patchId}`);
        } catch (error) {
            toast(error.message, "error");
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "提交补丁";
        }
    });

    function collectFormData() {
        const form = document.getElementById("patch-form");
        const data = new FormData(form);
        return {
            title: String(data.get("title") || "").trim(),
            priority: String(data.get("priority") || ""),
            scenarioId: String(data.get("scenarioId") || ""),
            productLineId: String(data.get("productLineId") || ""),
            severity: String(data.get("severity") || ""),
            ownerPmId: String(data.get("ownerPmId") || "").trim(),
            sourceVersion: String(data.get("sourceVersion") || "").trim(),
            targetVersion: String(data.get("targetVersion") || "").trim(),
            description: String(data.get("description") || "").trim(),
            autoSubmitReview: !!data.get("autoSubmitReview"),
            attachments: attachmentRows
        };
    }

    function collectPayload() {
        const draftData = collectFormData();
        if (!draftData.title || !draftData.productLineId || !draftData.ownerPmId) {
            toast("请填写补丁名称、产品、负责人", "warning");
            return null;
        }
        const base = {
            productLineId: Number(draftData.productLineId),
            title: draftData.title,
            description: draftData.description,
            severity: draftData.severity,
            priority: draftData.priority,
            sourceVersion: draftData.sourceVersion,
            targetVersion: draftData.targetVersion,
            ownerPmId: Number(draftData.ownerPmId)
        };
        const attachments = draftData.attachments
            .filter((item) => item.fileName && item.fileUrl)
            .map((item) => ({
                stage: item.stage || "DRAFT",
                fileName: item.fileName,
                fileUrl: item.fileUrl,
                fileHash: "",
                fileSize: 0
            }));
        return {
            base,
            attachments,
            autoSubmitReview: draftData.autoSubmitReview
        };
    }

    function renderProductOptions() {
        const scenarioId = scenarioSelect.value;
        const filteredProducts = scenarioId
            ? products.filter((product) => (product.scenarioIds || []).map(String).includes(String(scenarioId)))
            : products;
        const current = String(draft?.productLineId || "");
        productSelect.innerHTML = `
            <option value="">请选择产品</option>
            ${filteredProducts.map((product) => `
                <option value="${product.id}" ${String(product.id) === current ? "selected" : ""}>
                    ${escapeHtml(product.productName || product.productCode || String(product.id))}
                </option>
            `).join("")}
        `;
    }

    function renderAttachmentRows() {
        attachmentEditor.innerHTML = `
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>阶段</th>
                        <th>文件名</th>
                        <th>文件URL</th>
                        <th>操作</th>
                    </tr>
                    </thead>
                    <tbody>
                    ${attachmentRows.map((row, index) => `
                        <tr>
                            <td>
                                <select data-attachment-stage="${index}">
                                    ${PATCH_STEPS.map((step) => `<option value="${step}" ${row.stage === step ? "selected" : ""}>${stateLabel(step)}</option>`).join("")}
                                </select>
                            </td>
                            <td><input data-attachment-name="${index}" value="${escapeHtml(row.fileName || "")}" placeholder="release-note.txt"></td>
                            <td><input data-attachment-url="${index}" value="${escapeHtml(row.fileUrl || "")}" placeholder="https://..."></td>
                            <td><button class="btn-outline" data-remove-attachment="${index}">删除</button></td>
                        </tr>
                    `).join("")}
                    </tbody>
                </table>
            </div>
        `;
        attachmentEditor.querySelectorAll("[data-attachment-stage]").forEach((input) => {
            input.addEventListener("change", () => {
                const idx = Number(input.dataset.attachmentStage);
                attachmentRows[idx].stage = input.value;
            });
        });
        attachmentEditor.querySelectorAll("[data-attachment-name]").forEach((input) => {
            input.addEventListener("input", () => {
                const idx = Number(input.dataset.attachmentName);
                attachmentRows[idx].fileName = input.value;
            });
        });
        attachmentEditor.querySelectorAll("[data-attachment-url]").forEach((input) => {
            input.addEventListener("input", () => {
                const idx = Number(input.dataset.attachmentUrl);
                attachmentRows[idx].fileUrl = input.value;
            });
        });
        attachmentEditor.querySelectorAll("[data-remove-attachment]").forEach((button) => {
            button.addEventListener("click", () => {
                if (attachmentRows.length <= 1) {
                    attachmentRows[0] = { stage: "DRAFT", fileName: "", fileUrl: "" };
                } else {
                    attachmentRows = attachmentRows.filter((_, index) => index !== Number(button.dataset.removeAttachment));
                }
                renderAttachmentRows();
            });
        });
    }
}

async function renderPatchDetailPage(patchId) {
    const content = document.getElementById("page-content");
    content.innerHTML = `<div class="card">正在加载补丁详情...</div>`;
    const numericPatchId = Number(patchId);
    if (!numericPatchId) {
        content.innerHTML = `<div class="card">无效补丁ID</div>`;
        return;
    }

    const [patch, transitions, operationLogs, attachments, testTasks, qaPending, reviewSessions, kpiRules] = await Promise.all([
        safeApi(() => api.getPatch(numericPatchId), null),
        safeApi(() => api.listPatchTransitions(numericPatchId), []),
        safeApi(() => api.listPatchOperationLogs(numericPatchId), []),
        safeApi(() => api.listPatchAttachments(numericPatchId), []),
        safeApi(() => api.listPatchTestTasks(numericPatchId), []),
        safeApi(() => api.listQaPending(), []),
        safeApi(() => api.listReviewSessions(numericPatchId), []),
        safeApi(() => api.listKpiRules(), [])
    ]);

    if (!patch) {
        content.innerHTML = `<div class="card">补丁不存在或无权限访问。</div>`;
        return;
    }

    const activeStepIndex = PATCH_STEPS.indexOf(patch.currentState);
    const qaTask = qaPending.find((task) => Number(task.patchId) === Number(patch.patchId));
    const nextAction = NEXT_ACTION_BY_STATE[patch.currentState] || "";
    const allActions = listActionCandidates(patch.currentState);
    const timelineData = [
        ...transitions.map((item) => ({
            time: item.createdAt,
            title: `状态流转 · ${item.action}`,
            detail: `${item.fromState || "-"} → ${item.toState || "-"} / ${item.result || "-"}`,
            reason: item.blockReason || ""
        })),
        ...operationLogs.map((item) => ({
            time: item.createdAt,
            title: `操作日志 · ${item.action}`,
            detail: `${item.bizType || "-"} #${item.bizId || "-"}`,
            reason: item.traceId ? `Trace: ${item.traceId}` : ""
        }))
    ].sort((a, b) => new Date(b.time || 0).getTime() - new Date(a.time || 0).getTime());

    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">${escapeHtml(patch.patchNo || "补丁详情")} · ${escapeHtml(patch.title || "")}</h1>
                <p class="page-subtitle">状态：${stateLabel(patch.currentState)} · ${renderKpiTag(patch)}</p>
            </div>
            <div class="toolbar">
                <button class="btn-outline" id="back-to-list-btn">返回列表</button>
                <button class="btn-primary" id="goto-transfer-btn">转测申请页</button>
            </div>
        </div>

        <div class="card">
            <h3>生命周期进度</h3>
            <div class="status-steps">
                ${PATCH_STEPS.map((step, index) => `<div class="step ${index <= activeStepIndex ? "active" : ""}">${stateLabel(step)}</div>`).join("")}
            </div>
        </div>

        <div class="detail-grid">
            <div>
                <div class="card">
                    <h3>基础信息</h3>
                    <div class="two-col">
                        <div><strong>补丁编号：</strong>${escapeHtml(patch.patchNo || "-")}</div>
                        <div><strong>当前状态：</strong>${stateLabel(patch.currentState)}</div>
                        <div><strong>产品ID：</strong>${escapeHtml(String(patch.productLineId || "-"))}</div>
                        <div><strong>负责人：</strong>${escapeHtml(String(patch.ownerPmId || "-"))}</div>
                        <div><strong>优先级：</strong>${escapeHtml(patch.priority || "-")}</div>
                        <div><strong>严重程度：</strong>${escapeHtml(patch.severity || "-")}</div>
                        <div><strong>源版本：</strong>${escapeHtml(patch.sourceVersion || "-")}</div>
                        <div><strong>目标版本：</strong>${escapeHtml(patch.targetVersion || "-")}</div>
                        <div><strong>创建时间：</strong>${formatDateTime(patch.createdAt)}</div>
                        <div><strong>更新时间：</strong>${formatDateTime(patch.updatedAt)}</div>
                    </div>
                    <div style="margin-top:10px;">
                        <strong>描述：</strong>
                        <div class="page-subtitle">${escapeHtml(patch.description || "无")}</div>
                    </div>
                </div>

                <div class="card">
                    <h3>KPI卡点看板</h3>
                    <div class="two-col">
                        <div class="metric">
                            <div class="metric-label">当前KPI状态</div>
                            <div class="metric-value">${patch.kpiBlocked ? "阻断" : "正常"}</div>
                        </div>
                        <div class="metric">
                            <div class="metric-label">当前QA状态</div>
                            <div class="metric-value">${patch.qaBlocked ? "阻断" : "正常"}</div>
                        </div>
                    </div>
                    <div class="toolbar" style="margin-top:10px;">
                        <select id="kpi-eval-stage">${STAGE_OPTIONS.map((stage) => `<option value="${stage}">${stage}</option>`).join("")}</select>
                        <select id="kpi-eval-gate">${GATE_OPTIONS.map((gate) => `<option value="${gate}">${gate}</option>`).join("")}</select>
                        <input id="kpi-eval-action" value="${nextAction || "SUBMIT_REVIEW"}" placeholder="触发动作名">
                        <button class="btn-outline" id="kpi-eval-btn">执行KPI预校验</button>
                    </div>
                    <div id="kpi-eval-result" class="page-subtitle">可手动触发校验查看详情。</div>
                    <h4 style="margin-top:12px;">规则清单（当前租户）</h4>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>规则</th><th>阶段</th><th>门类型</th><th>指标</th><th>阈值</th><th>启用</th></tr></thead>
                            <tbody>
                            ${(kpiRules || []).slice(0, 12).map((rule) => `
                                <tr>
                                    <td>${escapeHtml(rule.ruleCode || "-")}</td>
                                    <td>${escapeHtml(rule.stage || "-")}</td>
                                    <td>${escapeHtml(rule.gateType || "-")}</td>
                                    <td>${escapeHtml(rule.metricKey || "-")}</td>
                                    <td>${escapeHtml(String(rule.thresholdValue ?? "-"))}</td>
                                    <td>${rule.enabled ? "是" : "否"}</td>
                                </tr>
                            `).join("") || `<tr><td colspan="6">暂无规则</td></tr>`}
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="card">
                    <h3>操作记录时间轴</h3>
                    ${timelineData.length ? timelineData.map((item) => `
                        <div class="timeline-item">
                            <div class="timeline-time">${formatDateTime(item.time)}</div>
                            <div class="timeline-title">${escapeHtml(item.title)}</div>
                            <div class="timeline-desc">${escapeHtml(item.detail || "-")}</div>
                            ${item.reason ? `<div class="timeline-time">${escapeHtml(item.reason)}</div>` : ""}
                        </div>
                    `).join("") : `<p class="page-subtitle">暂无操作记录。</p>`}
                </div>

                <div class="card">
                    <h3>附件与关联任务</h3>
                    <div class="two-col">
                        <div>
                            <h4>附件</h4>
                            ${attachments.length ? `
                                <ul class="list-reset">
                                    ${attachments.map((file) => `
                                        <li class="list-item">
                                            <strong>${escapeHtml(file.fileName || "-")}</strong>
                                            <div class="timeline-time">${escapeHtml(file.stage || "-")} · ${formatDateTime(file.createdAt)}</div>
                                            <div><a target="_blank" rel="noreferrer" href="${escapeAttr(file.fileUrl || "#")}">打开附件</a></div>
                                        </li>
                                    `).join("")}
                                </ul>
                            ` : `<p class="page-subtitle">暂无附件。</p>`}
                            <div class="toolbar" style="margin-top:8px;">
                                <input id="new-attach-name" placeholder="附件名">
                                <input id="new-attach-url" placeholder="附件URL">
                                <button class="btn-outline" id="add-attach-btn">新增附件</button>
                            </div>
                        </div>
                        <div>
                            <h4>关联任务</h4>
                            <ul class="list-reset">
                                <li class="list-item">
                                    <strong>评审会：</strong> ${reviewSessions.length} 条
                                    <div class="timeline-time">${reviewSessions.map((session) => `#${session.sessionId}(${session.status})`).join("，") || "暂无"}</div>
                                </li>
                                <li class="list-item">
                                    <strong>测试任务：</strong> ${testTasks.length} 条
                                    <div class="timeline-time">${testTasks.map((task) => `${task.taskNo || task.id}(${task.status})`).join("，") || "暂无"}</div>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
            <div class="right-sticky">
                <div class="card">
                    <h3>下一步操作</h3>
                    <div class="toolbar">
                        ${allActions.length ? allActions.map((action) => {
                            const reason = actionDisabledReason(patch, action);
                            return `
                                <button class="btn-primary" data-detail-action="${action}" ${reason ? "disabled" : ""} title="${escapeHtml(reason || "")}">
                                    ${escapeHtml(ACTION_LABELS[action] || action)}
                                </button>
                            `;
                        }).join("") : `<span class="page-subtitle">当前状态无可执行动作</span>`}
                    </div>
                </div>
                <div class="card">
                    <h3>QA审核面板</h3>
                    ${qaTask ? `
                        <div class="page-subtitle">待审任务：#${qaTask.qaTaskId} · ${escapeHtml(qaTask.stage || "-")} · ${escapeHtml(qaTask.status || "-")}</div>
                        <textarea id="qa-comment-input" placeholder="请输入审核意见"></textarea>
                        <div class="toolbar" style="margin-top:8px;">
                            <button class="btn-primary" id="qa-approve-btn">QA通过</button>
                            <button class="btn-danger" id="qa-reject-btn">QA驳回</button>
                        </div>
                    ` : `<p class="page-subtitle">当前补丁暂无待处理QA任务。</p>`}
                </div>
                <div class="card">
                    <h3>风险提示</h3>
                    <ul class="list-reset">
                        <li class="list-item">${patch.kpiBlocked ? "KPI卡点未通过，需补全指标后再流转。" : "KPI卡点正常。"}</li>
                        <li class="list-item">${patch.qaBlocked ? "QA门禁阻断，请先完成QA审核。" : "QA门禁正常。"}</li>
                    </ul>
                </div>
            </div>
        </div>
    `;

    document.getElementById("back-to-list-btn").addEventListener("click", () => navigate("#/patches"));
    document.getElementById("goto-transfer-btn").addEventListener("click", () => navigate(`#/transfer?patchId=${patch.patchId}`));

    document.querySelectorAll("[data-detail-action]").forEach((button) => {
        button.addEventListener("click", async () => {
            const action = button.dataset.detailAction;
            const comment = window.prompt(`请输入${ACTION_LABELS[action]}备注`, "");
            try {
                await api.executePatchAction(patch.patchId, { action, comment: comment || "" });
                toast(`${ACTION_LABELS[action]}成功`, "success");
                renderPatchDetailPage(patch.patchId).catch(renderFatal);
            } catch (error) {
                toast(error.message, "error");
            }
        });
    });

    const evalButton = document.getElementById("kpi-eval-btn");
    evalButton.addEventListener("click", async () => {
        const stage = document.getElementById("kpi-eval-stage").value;
        const gateType = document.getElementById("kpi-eval-gate").value;
        const triggerAction = document.getElementById("kpi-eval-action").value.trim();
        const resultDom = document.getElementById("kpi-eval-result");
        resultDom.textContent = "校验中...";
        try {
            const result = await api.evaluatePatchKpi(patch.patchId, { stage, gateType, triggerAction });
            resultDom.innerHTML = `
                <div>${result.pass ? "✅ 通过" : "❌ 未通过"} · ${escapeHtml(result.summary || "-")}</div>
                ${(result.details || []).map((detail) => `
                    <div class="timeline-time">${escapeHtml(detail.ruleCode || "-")} / ${escapeHtml(detail.metricKey || "-")} / ${detail.pass ? "PASS" : "FAIL"} / ${escapeHtml(detail.reason || "-")}</div>
                `).join("")}
            `;
        } catch (error) {
            resultDom.textContent = `校验失败：${error.message}`;
        }
    });

    const addAttachButton = document.getElementById("add-attach-btn");
    addAttachButton.addEventListener("click", async () => {
        const fileName = document.getElementById("new-attach-name").value.trim();
        const fileUrl = document.getElementById("new-attach-url").value.trim();
        if (!fileName || !fileUrl) {
            toast("请填写附件名与URL", "warning");
            return;
        }
        try {
            await api.createAttachment(patch.patchId, {
                stage: patch.currentState,
                fileName,
                fileUrl,
                fileHash: "",
                fileSize: 0
            });
            toast("附件已新增", "success");
            renderPatchDetailPage(patch.patchId).catch(renderFatal);
        } catch (error) {
            toast(error.message, "error");
        }
    });

    if (qaTask) {
        const qaCommentInput = document.getElementById("qa-comment-input");
        document.getElementById("qa-approve-btn").addEventListener("click", () => submitQaDecision("APPROVE", qaCommentInput.value.trim()));
        document.getElementById("qa-reject-btn").addEventListener("click", () => submitQaDecision("REJECT", qaCommentInput.value.trim()));
    }

    async function submitQaDecision(decision, comment) {
        try {
            await api.decideQaTask(qaTask.qaTaskId, { decision, comment });
            toast(`QA ${decision === "APPROVE" ? "通过" : "驳回"}成功`, "success");
            renderPatchDetailPage(patch.patchId).catch(renderFatal);
        } catch (error) {
            toast(error.message, "error");
        }
    }
}

async function renderTransferPage() {
    const route = parseRoute();
    const patchIdFromQuery = route.query.patchId ? Number(route.query.patchId) : null;
    const content = document.getElementById("page-content");
    const patches = await safeApi(() => api.listPatches(), []);
    const eligiblePatches = patches.filter((patch) => patch.currentState === "REVIEW_PASSED");
    const defaultPatch = patchIdFromQuery && eligiblePatches.some((patch) => patch.patchId === patchIdFromQuery)
        ? patchIdFromQuery
        : (eligiblePatches[0]?.patchId || "");

    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">转测申请</h1>
                <p class="page-subtitle">选择测试环境与用例，执行转测前KPI校验并提交。</p>
            </div>
        </div>
        <div class="card">
            <div class="form-grid">
                <div class="form-item">
                    <label>选择补丁</label>
                    <select id="transfer-patch-select">
                        <option value="">请选择可转测补丁（状态 REVIEW_PASSED）</option>
                        ${eligiblePatches.map((patch) => `<option value="${patch.patchId}" ${String(defaultPatch) === String(patch.patchId) ? "selected" : ""}>${escapeHtml(patch.patchNo)} · ${escapeHtml(patch.title)}</option>`).join("")}
                    </select>
                </div>
                <div class="form-item">
                    <label>测试环境</label>
                    <div>
                        <label><input type="checkbox" value="SIT" class="env-checkbox" checked> SIT</label>
                        <label><input type="checkbox" value="UAT" class="env-checkbox"> UAT</label>
                        <label><input type="checkbox" value="PRE" class="env-checkbox"> PRE</label>
                    </div>
                </div>
                <div class="form-item full">
                    <label>关联测试用例（每行一条）</label>
                    <textarea id="transfer-cases" placeholder="CASE-1001&#10;CASE-1002"></textarea>
                </div>
                <div class="form-item full">
                    <label>QA审核意见（可选）</label>
                    <textarea id="transfer-qa-comment" placeholder="补充说明..."></textarea>
                </div>
            </div>
        </div>
        <div class="card">
            <h3>KPI卡点状态预览</h3>
            <div class="toolbar">
                <button class="btn-outline" id="transfer-preview-btn">执行预校验</button>
                <button class="btn-primary" id="transfer-submit-btn">提交转测申请</button>
            </div>
            <div id="transfer-preview-result" class="page-subtitle">点击“执行预校验”查看结果。</div>
        </div>
    `;

    document.getElementById("transfer-preview-btn").addEventListener("click", async () => {
        const selectedPatchId = Number(document.getElementById("transfer-patch-select").value || 0);
        const resultDom = document.getElementById("transfer-preview-result");
        if (!selectedPatchId) {
            toast("请选择补丁", "warning");
            return;
        }
        resultDom.textContent = "预校验中...";
        try {
            const result = await api.evaluatePatchKpi(selectedPatchId, {
                stage: "TRANSFER_TEST",
                gateType: "ENTRY",
                triggerAction: "TRANSFER_TO_TEST"
            });
            resultDom.innerHTML = `
                <div>${result.pass ? "✅ 转测KPI准入通过" : "❌ 转测KPI准入未通过"} · ${escapeHtml(result.summary || "-")}</div>
                ${(result.details || []).map((item) => `<div class="timeline-time">${escapeHtml(item.ruleCode || "-")} / ${item.pass ? "PASS" : "FAIL"} / ${escapeHtml(item.reason || "-")}</div>`).join("")}
            `;
        } catch (error) {
            resultDom.textContent = `预校验失败：${error.message}`;
        }
    });

    document.getElementById("transfer-submit-btn").addEventListener("click", async () => {
        const selectedPatchId = Number(document.getElementById("transfer-patch-select").value || 0);
        if (!selectedPatchId) {
            toast("请选择补丁", "warning");
            return;
        }
        const envs = Array.from(document.querySelectorAll(".env-checkbox:checked")).map((input) => input.value);
        const cases = document.getElementById("transfer-cases").value
            .split("\n")
            .map((line) => line.trim())
            .filter(Boolean);
        const qaComment = document.getElementById("transfer-qa-comment").value.trim();
        const comment = `转测环境=${envs.join(",") || "NONE"};用例=${cases.join(",") || "NONE"};QA意见=${qaComment || "无"}`;
        try {
            await api.executePatchAction(selectedPatchId, {
                action: "TRANSFER_TO_TEST",
                comment
            });
            toast("转测申请提交成功", "success");
            navigate(`#/patches/${selectedPatchId}`);
        } catch (error) {
            toast(error.message, "error");
        }
    });
}

async function renderReviewListPage() {
    const content = document.getElementById("page-content");
    content.innerHTML = `<div class="card">正在加载评审数据...</div>`;
    const sessions = await safeApi(() => api.listReviewSessions(), []);
    const filters = { keyword: "", status: "", mode: "" };

    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">评审管理</h1>
                <p class="page-subtitle">管理评审任务、查看投票进度与结论。</p>
            </div>
            <div class="toolbar">
                <button class="btn-primary" id="new-review-btn">发起评审</button>
            </div>
        </div>
        <div class="card">
            <div class="filters">
                <input id="review-filter-keyword" placeholder="搜索评审ID或补丁ID">
                <select id="review-filter-status">
                    <option value="">全部状态</option>
                    <option value="OPEN">OPEN</option>
                    <option value="CLOSED">CLOSED</option>
                </select>
                <select id="review-filter-mode">
                    <option value="">全部模式</option>
                    <option value="ONLINE">ONLINE</option>
                    <option value="ASYNC">ASYNC</option>
                </select>
            </div>
            <div id="review-list-table"></div>
        </div>
    `;

    document.getElementById("new-review-btn").addEventListener("click", () => navigate("#/reviews/new"));
    document.getElementById("review-filter-keyword").addEventListener("input", (event) => {
        filters.keyword = event.target.value.trim();
        renderTable();
    });
    document.getElementById("review-filter-status").addEventListener("change", (event) => {
        filters.status = event.target.value;
        renderTable();
    });
    document.getElementById("review-filter-mode").addEventListener("change", (event) => {
        filters.mode = event.target.value;
        renderTable();
    });

    renderTable();

    function renderTable() {
        const filtered = sessions.filter((session) => {
            if (filters.keyword) {
                const haystack = `${session.sessionId || ""} ${session.patchId || ""}`.toLowerCase();
                if (!haystack.includes(filters.keyword.toLowerCase())) {
                    return false;
                }
            }
            if (filters.status && session.status !== filters.status) {
                return false;
            }
            if (filters.mode && session.mode !== filters.mode) {
                return false;
            }
            return true;
        });

        const table = document.getElementById("review-list-table");
        table.innerHTML = `
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>评审ID</th>
                        <th>补丁ID</th>
                        <th>模式</th>
                        <th>状态</th>
                        <th>结论</th>
                        <th>通过率</th>
                        <th>操作</th>
                    </tr>
                    </thead>
                    <tbody>
                    ${filtered.length ? filtered.map((session) => `
                        <tr>
                            <td>#${session.sessionId}</td>
                            <td>${session.patchId}</td>
                            <td>${escapeHtml(session.mode || "-")}</td>
                            <td>${escapeHtml(session.status || "-")}</td>
                            <td>${escapeHtml(session.conclusion || "-")}</td>
                            <td>${Number(session.approveRate || 0).toFixed(1)}%</td>
                            <td>
                                <div class="toolbar">
                                    <button class="btn-outline" data-review-detail="${session.sessionId}">详情</button>
                                    <button class="btn-outline" data-review-vote="${session.sessionId}" data-vote="PASS">投PASS</button>
                                    <button class="btn-outline" data-review-vote="${session.sessionId}" data-vote="REJECT">投REJECT</button>
                                </div>
                            </td>
                        </tr>
                    `).join("") : `<tr><td colspan="7">暂无评审任务</td></tr>`}
                    </tbody>
                </table>
            </div>
        `;

        table.querySelectorAll("[data-review-detail]").forEach((button) => {
            button.addEventListener("click", () => navigate(`#/reviews/${button.dataset.reviewDetail}`));
        });
        table.querySelectorAll("[data-review-vote]").forEach((button) => {
            button.addEventListener("click", async () => {
                const sessionId = Number(button.dataset.reviewVote);
                const vote = button.dataset.vote;
                const comment = window.prompt(`请输入投票意见（${vote}）`, "");
                try {
                    await api.voteReviewSession(sessionId, { vote, comment: comment || "" });
                    toast("投票成功", "success");
                    renderReviewListPage().catch(renderFatal);
                } catch (error) {
                    toast(error.message, "error");
                }
            });
        });
    }
}

async function renderReviewCreatePage() {
    const content = document.getElementById("page-content");
    const patches = await safeApi(() => api.listPatches(), []);
    const options = patches.filter((patch) => ["REVIEWING", "REVIEW_PASSED", "DRAFT"].includes(patch.currentState));

    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">发起评审</h1>
                <p class="page-subtitle">设置评审模式、投票规则并提交。</p>
            </div>
        </div>
        <div class="card">
            <form id="review-create-form" class="form-grid">
                <div class="form-item">
                    <label>补丁 *</label>
                    <select name="patchId" required>
                        <option value="">请选择补丁</option>
                        ${options.map((patch) => `<option value="${patch.patchId}">${escapeHtml(patch.patchNo)} · ${escapeHtml(patch.title)}</option>`).join("")}
                    </select>
                </div>
                <div class="form-item">
                    <label>评审模式 *</label>
                    <select name="mode" required>
                        <option value="ONLINE">ONLINE</option>
                        <option value="ASYNC">ASYNC</option>
                    </select>
                </div>
                <div class="form-item">
                    <label>会议工具</label>
                    <input name="meetingTool" placeholder="Tencent Meeting / Feishu">
                </div>
                <div class="form-item">
                    <label>会议链接</label>
                    <input name="meetingUrl" placeholder="https://...">
                </div>
                <div class="form-item">
                    <label>法定参与比例（0-100）</label>
                    <input name="quorumRequired" value="60">
                </div>
                <div class="form-item">
                    <label>通过率阈值（0-100）</label>
                    <input name="approveRateRequired" value="75">
                </div>
                <div class="form-item full">
                    <label>评审材料（每行一个链接，仅前端展示）</label>
                    <textarea name="materials" placeholder="https://docs.example.com/xxx"></textarea>
                </div>
            </form>
        </div>
        <div class="card">
            <div class="toolbar">
                <button class="btn-primary" id="create-review-btn">发起评审</button>
                <button class="btn-outline" id="cancel-review-create-btn">返回评审列表</button>
            </div>
        </div>
    `;

    document.getElementById("cancel-review-create-btn").addEventListener("click", () => navigate("#/reviews"));
    document.getElementById("create-review-btn").addEventListener("click", async () => {
        const form = document.getElementById("review-create-form");
        const data = new FormData(form);
        const patchId = Number(data.get("patchId") || 0);
        if (!patchId) {
            toast("请选择补丁", "warning");
            return;
        }
        const payload = {
            patchId,
            mode: String(data.get("mode") || "ONLINE"),
            meetingTool: String(data.get("meetingTool") || "").trim(),
            meetingUrl: String(data.get("meetingUrl") || "").trim(),
            quorumRequired: Number(data.get("quorumRequired") || 0),
            approveRateRequired: Number(data.get("approveRateRequired") || 0)
        };
        const materials = String(data.get("materials") || "")
            .split("\n")
            .map((line) => line.trim())
            .filter(Boolean);
        try {
            const session = await api.createReviewSession(payload);
            saveJson(`${REVIEW_MATERIAL_KEY_PREFIX}${session.sessionId}`, materials);
            toast("评审创建成功", "success");
            navigate(`#/reviews/${session.sessionId}`);
        } catch (error) {
            toast(error.message, "error");
        }
    });
}

async function renderReviewDetailPage(sessionId) {
    const content = document.getElementById("page-content");
    content.innerHTML = `<div class="card">正在加载评审详情...</div>`;
    const id = Number(sessionId || 0);
    if (!id) {
        content.innerHTML = `<div class="card">无效评审ID</div>`;
        return;
    }

    const detail = await safeApi(() => api.getReviewSession(id), null);
    if (!detail) {
        content.innerHTML = `<div class="card">评审会不存在或无权访问。</div>`;
        return;
    }
    const materials = loadJson(`${REVIEW_MATERIAL_KEY_PREFIX}${id}`, []);
    const discussions = loadJson(`${REVIEW_DISCUSS_KEY_PREFIX}${id}`, []);

    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">评审详情 #${detail.sessionId}</h1>
                <p class="page-subtitle">补丁 ${detail.patchId} · ${escapeHtml(detail.mode || "-")} · ${escapeHtml(detail.status || "-")}</p>
            </div>
            <div class="toolbar">
                <button class="btn-outline" id="back-review-list-btn">返回评审列表</button>
            </div>
        </div>

        <div class="two-col">
            <div class="card">
                <h3>评审材料预览</h3>
                ${materials.length ? `
                    <ul class="list-reset">
                        ${materials.map((material, index) => `
                            <li class="list-item">
                                <a href="${escapeAttr(material)}" target="_blank" rel="noreferrer">${escapeHtml(material)}</a>
                                <button class="btn-outline" data-remove-material="${index}">删除</button>
                            </li>
                        `).join("")}
                    </ul>
                ` : `<p class="page-subtitle">暂无材料，请在下方添加。</p>`}
                <div class="toolbar" style="margin-top:8px;">
                    <input id="new-material-input" placeholder="https://..." />
                    <button class="btn-outline" id="add-material-btn">添加材料</button>
                </div>
            </div>
            <div class="card">
                <h3>投票统计</h3>
                <div class="metrics-grid" style="grid-template-columns:repeat(2,minmax(0,1fr));">
                    ${metricCard("总投票", detail.totalVotes || 0)}
                    ${metricCard("通过率", `${Number(detail.approveRate || 0).toFixed(1)}%`)}
                    ${metricCard("PASS", detail.passVotes || 0)}
                    ${metricCard("REJECT", detail.rejectVotes || 0)}
                </div>
                <div class="page-subtitle">结论：${escapeHtml(detail.conclusion || "-")} · 阈值 ${detail.approveRateRequired ?? "-"}%</div>
                <div class="toolbar">
                    <button class="btn-primary" data-review-detail-vote="PASS">投PASS</button>
                    <button class="btn-danger" data-review-detail-vote="REJECT">投REJECT</button>
                    <button class="btn-outline" data-review-detail-vote="ABSTAIN">投ABSTAIN</button>
                </div>
            </div>
        </div>

        <div class="card">
            <h3>评委意见板</h3>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>投票ID</th>
                        <th>评委</th>
                        <th>票型</th>
                        <th>意见</th>
                        <th>时间</th>
                    </tr>
                    </thead>
                    <tbody>
                    ${(detail.votes || []).length ? (detail.votes || []).map((vote) => `
                        <tr>
                            <td>${vote.voteId}</td>
                            <td>${vote.voterId}</td>
                            <td>${escapeHtml(vote.vote || "-")}</td>
                            <td>${escapeHtml(vote.comment || "-")}</td>
                            <td>${formatDateTime(vote.votedAt)}</td>
                        </tr>
                    `).join("") : `<tr><td colspan="5">暂无投票</td></tr>`}
                    </tbody>
                </table>
            </div>
        </div>

        <div class="card">
            <h3>讨论区（前端协同备注）</h3>
            <ul class="list-reset">
                ${discussions.length ? discussions.map((item) => `
                    <li class="list-item">
                        <div><strong>${escapeHtml(item.author)}</strong></div>
                        <div>${escapeHtml(item.content)}</div>
                        <div class="timeline-time">${escapeHtml(item.time)}</div>
                    </li>
                `).join("") : `<li class="list-item">暂无讨论内容</li>`}
            </ul>
            <div class="toolbar">
                <input id="discussion-input" placeholder="输入讨论内容">
                <button class="btn-outline" id="add-discussion-btn">发布</button>
            </div>
        </div>
    `;

    document.getElementById("back-review-list-btn").addEventListener("click", () => navigate("#/reviews"));
    document.querySelectorAll("[data-review-detail-vote]").forEach((button) => {
        button.addEventListener("click", async () => {
            const vote = button.dataset.reviewDetailVote;
            const comment = window.prompt(`请输入投票意见（${vote}）`, "");
            try {
                await api.voteReviewSession(id, { vote, comment: comment || "" });
                toast("投票成功", "success");
                renderReviewDetailPage(id).catch(renderFatal);
            } catch (error) {
                toast(error.message, "error");
            }
        });
    });
    document.getElementById("add-material-btn").addEventListener("click", () => {
        const input = document.getElementById("new-material-input");
        const value = input.value.trim();
        if (!value) {
            return;
        }
        const next = [...materials, value];
        saveJson(`${REVIEW_MATERIAL_KEY_PREFIX}${id}`, next);
        renderReviewDetailPage(id).catch(renderFatal);
    });
    content.querySelectorAll("[data-remove-material]").forEach((button) => {
        button.addEventListener("click", () => {
            const index = Number(button.dataset.removeMaterial);
            const next = materials.filter((_, idx) => idx !== index);
            saveJson(`${REVIEW_MATERIAL_KEY_PREFIX}${id}`, next);
            renderReviewDetailPage(id).catch(renderFatal);
        });
    });
    document.getElementById("add-discussion-btn").addEventListener("click", () => {
        const input = document.getElementById("discussion-input");
        const value = input.value.trim();
        if (!value) {
            return;
        }
        const next = [...discussions, {
            author: state.session.username || `用户${state.session.userId}`,
            content: value,
            time: new Date().toLocaleString("zh-CN")
        }];
        saveJson(`${REVIEW_DISCUSS_KEY_PREFIX}${id}`, next);
        renderReviewDetailPage(id).catch(renderFatal);
    });
}

async function renderConfigPage(defaultTab) {
    const content = document.getElementById("page-content");
    content.innerHTML = `<div class="card">正在加载配置中心数据...</div>`;
    const [scenarios, products, roles, permissions, users] = await Promise.all([
        safeApi(() => api.listScenarios(), []),
        safeApi(() => api.listProducts(), []),
        safeApi(() => api.listConfigRoles(), []),
        safeApi(() => api.listPermissions(), []),
        safeApi(() => api.listUsers(), [])
    ]);

    let tab = defaultTab || "scenarios";
    const rolePermissionCache = {};

    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">配置中心</h1>
                <p class="page-subtitle">交付场景、产品、角色权限、人员管理统一入口。</p>
            </div>
        </div>
        <div class="tabs">
            <button class="tab-btn ${tab === "scenarios" ? "active" : ""}" data-config-tab="scenarios">场景与产品</button>
            <button class="tab-btn ${tab === "permissions" ? "active" : ""}" data-config-tab="permissions">角色权限</button>
            <button class="tab-btn ${tab === "users" ? "active" : ""}" data-config-tab="users">人员管理</button>
        </div>
        <div id="config-tab-content"></div>
    `;

    content.querySelectorAll("[data-config-tab]").forEach((button) => {
        button.addEventListener("click", () => {
            tab = button.dataset.configTab;
            renderConfigPage(tab).catch(renderFatal);
        });
    });

    const tabDom = document.getElementById("config-tab-content");
    if (tab === "scenarios") {
        tabDom.innerHTML = `
            <div class="two-col">
                <div class="card">
                    <h3>交付场景管理</h3>
                    <form id="scenario-form" class="form-grid">
                        <div class="form-item"><label>场景编码</label><input name="scenarioCode" required></div>
                        <div class="form-item"><label>场景名称</label><input name="scenarioName" required></div>
                        <div class="form-item full"><label>描述</label><textarea name="description"></textarea></div>
                        <div class="form-item"><label>状态</label><select name="status"><option value="ACTIVE">ACTIVE</option><option value="DISABLED">DISABLED</option></select></div>
                    </form>
                    <div class="toolbar"><button class="btn-primary" id="save-scenario-btn">保存场景</button></div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>ID</th><th>编码</th><th>名称</th><th>状态</th></tr></thead>
                            <tbody>${scenarios.map((item) => `<tr><td>${item.id}</td><td>${escapeHtml(item.scenarioCode)}</td><td>${escapeHtml(item.scenarioName)}</td><td>${escapeHtml(item.status || "-")}</td></tr>`).join("") || `<tr><td colspan="4">暂无场景</td></tr>`}</tbody>
                        </table>
                    </div>
                </div>
                <div class="card">
                    <h3>产品管理</h3>
                    <form id="product-form" class="form-grid">
                        <div class="form-item"><label>产品编码</label><input name="productCode" required></div>
                        <div class="form-item"><label>产品名称</label><input name="productName" required></div>
                        <div class="form-item full"><label>描述</label><textarea name="description"></textarea></div>
                        <div class="form-item"><label>负责人ID</label><input name="ownerUserId"></div>
                        <div class="form-item"><label>状态</label><select name="status"><option value="ACTIVE">ACTIVE</option><option value="DISABLED">DISABLED</option></select></div>
                    </form>
                    <div class="toolbar"><button class="btn-primary" id="save-product-btn">保存产品</button></div>
                    <h4>场景绑定</h4>
                    <div class="toolbar">
                        <select id="bind-scenario-id">${scenarios.map((item) => `<option value="${item.id}">${escapeHtml(item.scenarioName || item.scenarioCode)}</option>`).join("")}</select>
                        <select id="bind-product-id">${products.map((item) => `<option value="${item.id}">${escapeHtml(item.productName || item.productCode)}</option>`).join("")}</select>
                        <button class="btn-outline" id="bind-scenario-product-btn">绑定</button>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>ID</th><th>产品</th><th>场景</th><th>状态</th></tr></thead>
                            <tbody>
                            ${products.map((item) => {
                                const scenarioNames = (item.scenarioIds || [])
                                    .map((scenarioId) => scenarios.find((scenario) => scenario.id === scenarioId))
                                    .filter(Boolean)
                                    .map((scenario) => scenario.scenarioName || scenario.scenarioCode);
                                return `<tr><td>${item.id}</td><td>${escapeHtml(item.productName || item.productCode)}</td><td>${escapeHtml(scenarioNames.join("，") || "-")}</td><td>${escapeHtml(item.status || "-")}</td></tr>`;
                            }).join("") || `<tr><td colspan="4">暂无产品</td></tr>`}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;

        document.getElementById("save-scenario-btn").addEventListener("click", async () => {
            const form = document.getElementById("scenario-form");
            const data = new FormData(form);
            try {
                await api.upsertScenario({
                    scenarioCode: String(data.get("scenarioCode") || "").trim(),
                    scenarioName: String(data.get("scenarioName") || "").trim(),
                    description: String(data.get("description") || "").trim(),
                    status: String(data.get("status") || "ACTIVE"),
                    extProps: "{}"
                });
                toast("场景保存成功", "success");
                renderConfigPage("scenarios").catch(renderFatal);
            } catch (error) {
                toast(error.message, "error");
            }
        });
        document.getElementById("save-product-btn").addEventListener("click", async () => {
            const form = document.getElementById("product-form");
            const data = new FormData(form);
            try {
                await api.upsertProduct({
                    productCode: String(data.get("productCode") || "").trim(),
                    productName: String(data.get("productName") || "").trim(),
                    description: String(data.get("description") || "").trim(),
                    ownerUserId: Number(data.get("ownerUserId") || 0) || null,
                    status: String(data.get("status") || "ACTIVE"),
                    extProps: "{}"
                });
                toast("产品保存成功", "success");
                renderConfigPage("scenarios").catch(renderFatal);
            } catch (error) {
                toast(error.message, "error");
            }
        });
        document.getElementById("bind-scenario-product-btn").addEventListener("click", async () => {
            const scenarioId = Number(document.getElementById("bind-scenario-id").value || 0);
            const productId = Number(document.getElementById("bind-product-id").value || 0);
            if (!scenarioId || !productId) {
                toast("请选择场景和产品", "warning");
                return;
            }
            try {
                await api.bindScenarioProducts({ scenarioId, productIds: [productId] });
                toast("场景产品绑定成功", "success");
                renderConfigPage("scenarios").catch(renderFatal);
            } catch (error) {
                toast(error.message, "error");
            }
        });
        return;
    }

    if (tab === "permissions") {
        const selectedRoleId = roles[0]?.id || "";
        tabDom.innerHTML = `
            <div class="two-col">
                <div class="card">
                    <h3>角色树</h3>
                    <form id="role-form" class="form-grid">
                        <div class="form-item"><label>角色编码</label><input name="roleCode" required></div>
                        <div class="form-item"><label>角色名称</label><input name="roleName" required></div>
                        <div class="form-item"><label>角色层级</label><select name="roleLevel"><option value="GLOBAL">GLOBAL</option><option value="SCENARIO">SCENARIO</option><option value="PRODUCT">PRODUCT</option></select></div>
                        <div class="form-item"><label>作用域ID</label><input name="scopeRefId"></div>
                    </form>
                    <div class="toolbar"><button class="btn-primary" id="save-role-btn">保存角色</button></div>
                    <ul class="list-reset">
                        ${roles.map((role) => `<li class="list-item"><strong>#${role.id}</strong> ${escapeHtml(role.roleCode)} · ${escapeHtml(role.roleName)}</li>`).join("") || `<li class="list-item">暂无角色</li>`}
                    </ul>
                </div>
                <div class="card">
                    <h3>权限勾选面板</h3>
                    <form id="permission-form" class="form-grid">
                        <div class="form-item"><label>权限编码</label><input name="permCode" required></div>
                        <div class="form-item"><label>权限名称</label><input name="permName" required></div>
                        <div class="form-item"><label>权限类型</label><input name="permType" value="API"></div>
                        <div class="form-item"><label>资源</label><input name="resource"></div>
                        <div class="form-item"><label>动作</label><input name="action"></div>
                    </form>
                    <div class="toolbar"><button class="btn-outline" id="save-permission-btn">保存权限点</button></div>
                    <div class="toolbar">
                        <select id="role-select-for-permission">
                            ${roles.map((role) => `<option value="${role.id}" ${String(role.id) === String(selectedRoleId) ? "selected" : ""}>${escapeHtml(role.roleName)} (${escapeHtml(role.roleCode)})</option>`).join("")}
                        </select>
                        <button class="btn-outline" id="load-role-permission-btn">加载角色权限</button>
                        <button class="btn-primary" id="save-role-permission-btn">保存勾选权限</button>
                    </div>
                    <div id="permission-checklist" class="card"></div>
                </div>
            </div>
        `;

        const checklistDom = document.getElementById("permission-checklist");
        let checkedPermissionIds = [];

        async function loadRolePermissions(roleId) {
            if (!roleId) {
                checkedPermissionIds = [];
                renderChecklist();
                return;
            }
            const response = await safeApi(() => api.getRolePermissions(roleId), { permissionIds: [] }, true);
            checkedPermissionIds = response.permissionIds || [];
            rolePermissionCache[roleId] = checkedPermissionIds;
            renderChecklist();
        }

        function renderChecklist() {
            checklistDom.innerHTML = permissions.length ? permissions.map((permission) => {
                const checked = checkedPermissionIds.includes(permission.id) ? "checked" : "";
                return `<label style="display:block;margin-bottom:6px;"><input type="checkbox" data-permission-check="${permission.id}" ${checked}> ${escapeHtml(permission.permName)} (${escapeHtml(permission.permCode)})</label>`;
            }).join("") : `<p class="page-subtitle">暂无权限点。</p>`;
            checklistDom.querySelectorAll("[data-permission-check]").forEach((checkbox) => {
                checkbox.addEventListener("change", () => {
                    const id = Number(checkbox.dataset.permissionCheck);
                    if (checkbox.checked) {
                        if (!checkedPermissionIds.includes(id)) {
                            checkedPermissionIds.push(id);
                        }
                    } else {
                        checkedPermissionIds = checkedPermissionIds.filter((item) => item !== id);
                    }
                });
            });
        }

        document.getElementById("save-role-btn").addEventListener("click", async () => {
            const form = document.getElementById("role-form");
            const data = new FormData(form);
            try {
                await api.upsertConfigRole({
                    roleCode: String(data.get("roleCode") || "").trim(),
                    roleName: String(data.get("roleName") || "").trim(),
                    roleLevel: String(data.get("roleLevel") || "GLOBAL"),
                    scopeRefId: Number(data.get("scopeRefId") || 0) || null,
                    enabled: true
                });
                toast("角色保存成功", "success");
                renderConfigPage("permissions").catch(renderFatal);
            } catch (error) {
                toast(error.message, "error");
            }
        });

        document.getElementById("save-permission-btn").addEventListener("click", async () => {
            const form = document.getElementById("permission-form");
            const data = new FormData(form);
            try {
                await api.upsertPermission({
                    permCode: String(data.get("permCode") || "").trim(),
                    permName: String(data.get("permName") || "").trim(),
                    permType: String(data.get("permType") || "API"),
                    resource: String(data.get("resource") || "").trim(),
                    action: String(data.get("action") || "").trim(),
                    parentId: null,
                    enabled: true
                });
                toast("权限保存成功", "success");
                renderConfigPage("permissions").catch(renderFatal);
            } catch (error) {
                toast(error.message, "error");
            }
        });

        document.getElementById("load-role-permission-btn").addEventListener("click", async () => {
            const roleId = Number(document.getElementById("role-select-for-permission").value || 0);
            await loadRolePermissions(roleId);
            toast("角色权限已加载", "success");
        });

        document.getElementById("save-role-permission-btn").addEventListener("click", async () => {
            const roleId = Number(document.getElementById("role-select-for-permission").value || 0);
            if (!roleId) {
                toast("请先选择角色", "warning");
                return;
            }
            try {
                await api.assignRolePermissions(roleId, checkedPermissionIds);
                toast("角色权限保存成功", "success");
            } catch (error) {
                toast(error.message, "error");
            }
        });

        await loadRolePermissions(selectedRoleId);
        return;
    }

    tabDom.innerHTML = `
        <div class="two-col">
            <div class="card">
                <h3>用户列表</h3>
                <form id="user-form" class="form-grid">
                    <div class="form-item"><label>用户名</label><input name="username" required></div>
                    <div class="form-item"><label>显示名</label><input name="displayName" required></div>
                    <div class="form-item"><label>邮箱</label><input name="email"></div>
                    <div class="form-item"><label>手机号</label><input name="mobile"></div>
                    <div class="form-item"><label>状态</label><select name="status"><option value="ACTIVE">ACTIVE</option><option value="DISABLED">DISABLED</option></select></div>
                </form>
                <div class="toolbar"><button class="btn-primary" id="save-user-btn">保存用户</button></div>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>ID</th><th>用户名</th><th>显示名</th><th>角色</th><th>状态</th></tr></thead>
                        <tbody>
                        ${users.map((user) => `<tr><td>${user.id}</td><td>${escapeHtml(user.username)}</td><td>${escapeHtml(user.displayName)}</td><td>${escapeHtml((user.roles || []).join(",") || "-")}</td><td>${escapeHtml(user.status || "-")}</td></tr>`).join("") || `<tr><td colspan="5">暂无用户</td></tr>`}
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="card">
                <h3>角色分配与数据范围</h3>
                <div class="toolbar">
                    <select id="assign-user-id">${users.map((user) => `<option value="${user.id}">${escapeHtml(user.displayName || user.username)}(#${user.id})</option>`).join("")}</select>
                    <select id="assign-role-code">${roles.map((role) => `<option value="${escapeAttr(role.roleCode)}">${escapeHtml(role.roleName)}(${escapeHtml(role.roleCode)})</option>`).join("")}</select>
                    <button class="btn-outline" id="assign-role-btn">分配角色</button>
                </div>
                <div class="toolbar">
                    <select id="scope-role-id">${roles.map((role) => `<option value="${role.id}">${escapeHtml(role.roleName)}(#${role.id})</option>`).join("")}</select>
                    <select id="scope-level"><option value="GLOBAL">GLOBAL</option><option value="SCENARIO">SCENARIO</option><option value="PRODUCT">PRODUCT</option></select>
                    <input id="scope-scenario-id" placeholder="scenarioId(可选)">
                    <input id="scope-product-id" placeholder="productId(可选)">
                    <button class="btn-outline" id="assign-scope-btn">分配作用域</button>
                </div>
                <div class="toolbar">
                    <button class="btn-outline" id="query-user-roles-btn">查询用户角色</button>
                    <button class="btn-outline" id="query-user-scopes-btn">查询用户作用域</button>
                </div>
                <div id="user-assignment-result" class="page-subtitle">可查看当前用户角色与作用域分配结果。</div>
            </div>
        </div>
    `;

    document.getElementById("save-user-btn").addEventListener("click", async () => {
        const form = document.getElementById("user-form");
        const data = new FormData(form);
        try {
            await api.upsertUser({
                username: String(data.get("username") || "").trim(),
                displayName: String(data.get("displayName") || "").trim(),
                email: String(data.get("email") || "").trim(),
                mobile: String(data.get("mobile") || "").trim(),
                status: String(data.get("status") || "ACTIVE")
            });
            toast("用户保存成功", "success");
            renderConfigPage("users").catch(renderFatal);
        } catch (error) {
            toast(error.message, "error");
        }
    });

    document.getElementById("assign-role-btn").addEventListener("click", async () => {
        const userId = Number(document.getElementById("assign-user-id").value || 0);
        const roleCode = document.getElementById("assign-role-code").value;
        try {
            await api.assignUserRole({ userId, roleCode, enabled: true });
            toast("角色分配成功", "success");
        } catch (error) {
            toast(error.message, "error");
        }
    });

    document.getElementById("assign-scope-btn").addEventListener("click", async () => {
        const userId = Number(document.getElementById("assign-user-id").value || 0);
        const roleId = Number(document.getElementById("scope-role-id").value || 0);
        const scopeLevel = document.getElementById("scope-level").value;
        const scenarioId = Number(document.getElementById("scope-scenario-id").value || 0) || null;
        const productId = Number(document.getElementById("scope-product-id").value || 0) || null;
        try {
            await api.assignUserRoleScope({
                userId,
                roleId,
                scopeLevel,
                scenarioId,
                productId,
                status: "ACTIVE"
            });
            toast("作用域分配成功", "success");
        } catch (error) {
            toast(error.message, "error");
        }
    });

    document.getElementById("query-user-roles-btn").addEventListener("click", async () => {
        const userId = Number(document.getElementById("assign-user-id").value || 0);
        const result = await safeApi(() => api.listUserRoles(userId), []);
        document.getElementById("user-assignment-result").innerHTML = `用户角色：${escapeHtml(result.map((item) => item.roleCode).join(", ") || "无")}`;
    });

    document.getElementById("query-user-scopes-btn").addEventListener("click", async () => {
        const userId = Number(document.getElementById("assign-user-id").value || 0);
        const result = await safeApi(() => api.listUserRoleScopes(userId), []);
        document.getElementById("user-assignment-result").innerHTML = `用户作用域：${escapeHtml(result.map((item) => `${item.scopeLevel || "-"}(role#${item.roleId || "-"})`).join("；") || "无")}`;
    });
}

async function renderMailPage(defaultTab) {
    const content = document.getElementById("page-content");
    content.innerHTML = `<div class="card">正在加载邮件配置...</div>`;
    const [servers, templates, policies, logs] = await Promise.all([
        safeApi(() => api.listMailServers(), []),
        safeApi(() => api.listMailTemplates(), []),
        safeApi(() => api.listMailPolicies(), []),
        safeApi(() => api.listMailLogs(20), [])
    ]);
    let tab = defaultTab || "servers";

    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">邮件通知配置</h1>
                <p class="page-subtitle">SMTP、模板、事件策略与发送日志管理。</p>
            </div>
        </div>
        <div class="tabs">
            <button class="tab-btn ${tab === "servers" ? "active" : ""}" data-mail-tab="servers">SMTP配置</button>
            <button class="tab-btn ${tab === "templates" ? "active" : ""}" data-mail-tab="templates">模板管理</button>
            <button class="tab-btn ${tab === "policies" ? "active" : ""}" data-mail-tab="policies">事件策略</button>
            <button class="tab-btn ${tab === "logs" ? "active" : ""}" data-mail-tab="logs">发送日志</button>
        </div>
        <div id="mail-tab-content"></div>
    `;

    content.querySelectorAll("[data-mail-tab]").forEach((button) => {
        button.addEventListener("click", () => renderMailPage(button.dataset.mailTab).catch(renderFatal));
    });

    const tabDom = document.getElementById("mail-tab-content");
    if (tab === "servers") {
        tabDom.innerHTML = `
            <div class="card">
                <h3>SMTP服务器配置</h3>
                <form id="smtp-form" class="form-grid">
                    <div class="form-item"><label>配置名称</label><input name="configName" required></div>
                    <div class="form-item"><label>SMTP Host</label><input name="smtpHost" value="127.0.0.1" required></div>
                    <div class="form-item"><label>端口</label><input name="smtpPort" value="25"></div>
                    <div class="form-item"><label>协议</label><input name="protocol" value="smtp"></div>
                    <div class="form-item"><label>用户名</label><input name="username"></div>
                    <div class="form-item"><label>密码</label><input name="password" type="password"></div>
                    <div class="form-item"><label>发件邮箱</label><input name="senderEmail" value="noreply@example.com" required></div>
                    <div class="form-item"><label>发件人</label><input name="senderName" value="Patch Bot"></div>
                    <div class="form-item"><label>默认配置</label><select name="defaultConfig"><option value="true">true</option><option value="false">false</option></select></div>
                    <div class="form-item"><label>启用</label><select name="enabled"><option value="true">true</option><option value="false">false</option></select></div>
                </form>
                <div class="toolbar"><button class="btn-primary" id="save-smtp-btn">保存SMTP配置</button></div>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>ID</th><th>配置名</th><th>Host</th><th>端口</th><th>默认</th><th>启用</th></tr></thead>
                        <tbody>${servers.map((item) => `<tr><td>${item.id}</td><td>${escapeHtml(item.configName)}</td><td>${escapeHtml(item.smtpHost)}</td><td>${item.smtpPort}</td><td>${item.defaultConfig ? "是" : "否"}</td><td>${item.enabled ? "是" : "否"}</td></tr>`).join("") || `<tr><td colspan="6">暂无服务器配置</td></tr>`}</tbody>
                    </table>
                </div>
            </div>
        `;
        document.getElementById("save-smtp-btn").addEventListener("click", async () => {
            const form = document.getElementById("smtp-form");
            const data = new FormData(form);
            try {
                await api.upsertMailServer({
                    configName: String(data.get("configName") || "").trim(),
                    smtpHost: String(data.get("smtpHost") || "").trim(),
                    smtpPort: Number(data.get("smtpPort") || 25),
                    protocol: String(data.get("protocol") || "smtp").trim(),
                    username: String(data.get("username") || "").trim(),
                    password: String(data.get("password") || "").trim(),
                    senderEmail: String(data.get("senderEmail") || "").trim(),
                    senderName: String(data.get("senderName") || "").trim(),
                    sslEnabled: false,
                    starttlsEnabled: false,
                    authEnabled: true,
                    timeoutMs: 10000,
                    defaultConfig: String(data.get("defaultConfig")) === "true",
                    enabled: String(data.get("enabled")) === "true",
                    extProps: "{}"
                });
                toast("SMTP配置保存成功", "success");
                renderMailPage("servers").catch(renderFatal);
            } catch (error) {
                toast(error.message, "error");
            }
        });
        return;
    }

    if (tab === "templates") {
        tabDom.innerHTML = `
            <div class="card">
                <h3>邮件模板管理</h3>
                <form id="template-form" class="form-grid">
                    <div class="form-item"><label>模板编码</label><input name="templateCode" required></div>
                    <div class="form-item"><label>事件编码</label><input name="eventCode" required value="PATCH_CREATED"></div>
                    <div class="form-item full"><label>主题模板</label><input name="subjectTpl" required value="[补丁通知] \${patchNo}"></div>
                    <div class="form-item full"><label>正文模板</label><textarea name="bodyTpl" required>补丁 \${patchNo} 当前状态 \${currentState}</textarea></div>
                    <div class="form-item"><label>内容类型</label><select name="contentType"><option value="TEXT">TEXT</option><option value="HTML">HTML</option></select></div>
                    <div class="form-item"><label>语言</label><input name="lang" value="zh-CN"></div>
                    <div class="form-item"><label>版本</label><input name="version" value="1"></div>
                </form>
                <div class="toolbar">
                    <button class="btn-outline" id="insert-template-var-btn">插入变量 \${patchNo}</button>
                    <button class="btn-primary" id="save-template-btn">保存模板</button>
                    <button class="btn-outline" id="render-template-btn">预览渲染</button>
                </div>
                <div id="template-render-result" class="page-subtitle">可渲染预览模板效果。</div>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>ID</th><th>模板编码</th><th>事件</th><th>版本</th><th>启用</th></tr></thead>
                        <tbody>${templates.map((item) => `<tr><td>${item.id}</td><td>${escapeHtml(item.templateCode)}</td><td>${escapeHtml(item.eventCode)}</td><td>${item.version}</td><td>${item.enabled ? "是" : "否"}</td></tr>`).join("") || `<tr><td colspan="5">暂无模板</td></tr>`}</tbody>
                    </table>
                </div>
            </div>
        `;
        document.getElementById("insert-template-var-btn").addEventListener("click", () => {
            const textarea = document.querySelector("#template-form textarea[name=bodyTpl]");
            textarea.value = `${textarea.value}\n补丁标题：\${title}`;
        });
        document.getElementById("save-template-btn").addEventListener("click", async () => {
            const form = document.getElementById("template-form");
            const data = new FormData(form);
            try {
                await api.upsertMailTemplate({
                    templateCode: String(data.get("templateCode") || "").trim(),
                    eventCode: String(data.get("eventCode") || "").trim(),
                    subjectTpl: String(data.get("subjectTpl") || "").trim(),
                    bodyTpl: String(data.get("bodyTpl") || "").trim(),
                    contentType: String(data.get("contentType") || "TEXT"),
                    lang: String(data.get("lang") || "zh-CN"),
                    version: Number(data.get("version") || 1),
                    enabled: true,
                    extProps: "{}"
                });
                toast("模板保存成功", "success");
                renderMailPage("templates").catch(renderFatal);
            } catch (error) {
                toast(error.message, "error");
            }
        });
        document.getElementById("render-template-btn").addEventListener("click", async () => {
            const form = document.getElementById("template-form");
            const data = new FormData(form);
            try {
                const result = await api.renderMailTemplate({
                    templateCode: String(data.get("templateCode") || "").trim(),
                    eventCode: String(data.get("eventCode") || "").trim(),
                    model: {
                        patchNo: "P20260228-0001",
                        title: "演示补丁",
                        currentState: "REVIEWING",
                        operatorId: state.session.userId
                    }
                });
                document.getElementById("template-render-result").innerHTML = `
                    <div><strong>渲染主题：</strong>${escapeHtml(result.subjectRendered || "-")}</div>
                    <div><strong>渲染正文：</strong>${escapeHtml(result.bodyRendered || "-")}</div>
                `;
            } catch (error) {
                document.getElementById("template-render-result").textContent = `渲染失败：${error.message}`;
            }
        });
        return;
    }

    if (tab === "policies") {
        tabDom.innerHTML = `
            <div class="card">
                <h3>事件策略配置</h3>
                <form id="policy-form" class="form-grid">
                    <div class="form-item"><label>事件编码</label><input name="eventCode" value="PATCH_CREATED" required></div>
                    <div class="form-item"><label>模板编码</label><input name="templateCode" required></div>
                    <div class="form-item"><label>To角色（逗号）</label><input name="toRoleCodes" value="PM,REVIEWER"></div>
                    <div class="form-item"><label>Cc角色（逗号）</label><input name="ccRoleCodes"></div>
                    <div class="form-item"><label>包含Owner</label><select name="includeOwner"><option value="true">true</option><option value="false">false</option></select></div>
                    <div class="form-item"><label>包含操作人</label><select name="includeOperator"><option value="true">true</option><option value="false">false</option></select></div>
                </form>
                <div class="toolbar"><button class="btn-primary" id="save-policy-btn">保存策略</button></div>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>ID</th><th>事件</th><th>模板</th><th>To</th><th>Cc</th><th>启用</th></tr></thead>
                        <tbody>${policies.map((item) => `<tr><td>${item.id}</td><td>${escapeHtml(item.eventCode)}</td><td>${escapeHtml(item.templateCode)}</td><td>${escapeHtml((item.toRoleCodes || []).join(",") || "-")}</td><td>${escapeHtml((item.ccRoleCodes || []).join(",") || "-")}</td><td>${item.enabled ? "是" : "否"}</td></tr>`).join("") || `<tr><td colspan="6">暂无事件策略</td></tr>`}</tbody>
                    </table>
                </div>
            </div>
        `;
        document.getElementById("save-policy-btn").addEventListener("click", async () => {
            const form = document.getElementById("policy-form");
            const data = new FormData(form);
            try {
                await api.upsertMailPolicy({
                    eventCode: String(data.get("eventCode") || "").trim(),
                    templateCode: String(data.get("templateCode") || "").trim(),
                    toRoleCodes: splitCommaList(String(data.get("toRoleCodes") || "")),
                    ccRoleCodes: splitCommaList(String(data.get("ccRoleCodes") || "")),
                    includeOwner: String(data.get("includeOwner") || "true") === "true",
                    includeOperator: String(data.get("includeOperator") || "false") === "true",
                    enabled: true
                });
                toast("事件策略保存成功", "success");
                renderMailPage("policies").catch(renderFatal);
            } catch (error) {
                toast(error.message, "error");
            }
        });
        return;
    }

    tabDom.innerHTML = `
        <div class="card">
            <h3>发送日志</h3>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>ID</th><th>事件</th><th>状态</th><th>收件人</th><th>创建时间</th><th>操作</th></tr></thead>
                    <tbody>
                    ${logs.map((item) => `
                        <tr>
                            <td>${item.id}</td>
                            <td>${escapeHtml(item.eventCode || "-")}</td>
                            <td>${escapeHtml(item.status || "-")}</td>
                            <td>${escapeHtml(item.mailTo || "-")}</td>
                            <td>${formatDateTime(item.createdAt)}</td>
                            <td><button class="btn-outline" data-resend-log="${item.id}">重发</button></td>
                        </tr>
                    `).join("") || `<tr><td colspan="6">暂无发送日志</td></tr>`}
                    </tbody>
                </table>
            </div>
        </div>
    `;
    tabDom.querySelectorAll("[data-resend-log]").forEach((button) => {
        button.addEventListener("click", async () => {
            try {
                await api.resendMailLog(Number(button.dataset.resendLog));
                toast("已触发重发", "success");
                renderMailPage("logs").catch(renderFatal);
            } catch (error) {
                toast(error.message, "error");
            }
        });
    });
}

function renderOpsPage() {
    const content = document.getElementById("page-content");
    const host = window.location.host || "127.0.0.1:18080";
    content.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">运维面板</h1>
                <p class="page-subtitle">快速访问服务健康、接口文档与监控入口。</p>
            </div>
        </div>
        <div class="card">
            <ul class="list-reset">
                <li class="list-item"><a href="http://${host}/swagger-ui.html" target="_blank" rel="noreferrer">Swagger UI</a></li>
                <li class="list-item"><a href="http://${host}/actuator/health" target="_blank" rel="noreferrer">Actuator Health</a></li>
                <li class="list-item"><a href="http://${host}/actuator/prometheus" target="_blank" rel="noreferrer">Prometheus Metrics</a></li>
            </ul>
            <p class="page-subtitle">生产环境建议通过 Nginx + HTTPS 暴露运维入口，并叠加 IP 白名单。</p>
        </div>
    `;
}

function renderNotFound() {
    const content = document.getElementById("page-content");
    if (!content) {
        renderLoginPage();
        return;
    }
    content.innerHTML = `
        <div class="card">
            <h3>页面不存在</h3>
            <p class="page-subtitle">请从左侧导航进入有效页面。</p>
            <button class="btn-primary" onclick="location.hash='#/dashboard'">返回仪表盘</button>
        </div>
    `;
}

function renderFatal(error) {
    console.error(error);
    const message = error instanceof Error ? error.message : String(error);
    if (appRoot) {
        appRoot.innerHTML = `
            <div class="app-login">
                <div class="login-card">
                    <h2 class="login-title">页面渲染异常</h2>
                    <p class="login-subtitle">${escapeHtml(message)}</p>
                    <button class="btn-primary" onclick="location.reload()">刷新页面</button>
                </div>
            </div>
        `;
    }
}

function metricCard(label, value) {
    return `
        <div class="metric">
            <div class="metric-label">${escapeHtml(label)}</div>
            <div class="metric-value">${escapeHtml(String(value))}</div>
        </div>
    `;
}

function parseRoute() {
    const hash = (window.location.hash || "#/dashboard").replace(/^#/, "");
    const [pathPart, queryPart = ""] = hash.split("?");
    const path = pathPart || "/dashboard";
    const query = parseQuery(queryPart);

    let match = path.match(/^\/patches\/(\d+)$/);
    if (match) {
        return { name: "patch-detail", params: { patchId: Number(match[1]) }, query };
    }
    match = path.match(/^\/reviews\/(\d+)$/);
    if (match) {
        return { name: "review-detail", params: { sessionId: Number(match[1]) }, query };
    }

    const named = path.replace(/^\//, "");
    if (!named || named === "dashboard") {
        return { name: "dashboard", params: {}, query };
    }
    if (named === "patches") {
        return { name: "patches", params: {}, query };
    }
    if (named === "patches/new") {
        return { name: "patch-new", params: {}, query };
    }
    if (named === "transfer") {
        return { name: "transfer", params: {}, query };
    }
    if (named === "reviews") {
        return { name: "reviews", params: {}, query };
    }
    if (named === "reviews/new") {
        return { name: "review-new", params: {}, query };
    }
    if (named === "config") {
        return { name: "config", params: {}, query };
    }
    if (named === "mail") {
        return { name: "mail", params: {}, query };
    }
    if (named === "ops") {
        return { name: "ops", params: {}, query };
    }
    return { name: "404", params: {}, query };
}

function buildBreadcrumb(route) {
    const map = {
        dashboard: ["首页", "仪表盘"],
        patches: ["首页", "补丁管理", "补丁列表"],
        "patch-new": ["首页", "补丁管理", "创建补丁"],
        "patch-detail": ["首页", "补丁管理", `补丁详情 #${route.params.patchId}`],
        transfer: ["首页", "转测管理", "转测申请"],
        reviews: ["首页", "评审管理", "评审列表"],
        "review-new": ["首页", "评审管理", "发起评审"],
        "review-detail": ["首页", "评审管理", `评审详情 #${route.params.sessionId}`],
        config: ["首页", "配置中心"],
        mail: ["首页", "邮件通知配置"],
        ops: ["首页", "运维面板"]
    };
    return map[route.name] || ["首页", "未知页面"];
}

function stateLabel(stateName) {
    const map = {
        DRAFT: "创建",
        REVIEWING: "评审中",
        REVIEW_PASSED: "评审通过",
        TESTING: "转测中",
        TEST_PASSED: "测试通过",
        RELEASE_READY: "待发布",
        RELEASED: "已发布",
        ARCHIVED: "已归档"
    };
    return map[stateName] || stateName || "-";
}

function renderKpiTag(patch) {
    if (patch.kpiBlocked) {
        return `<span class="tag" title="KPI卡点未通过"><span class="dot danger"></span>KPI阻断</span>`;
    }
    if (patch.qaBlocked) {
        return `<span class="tag" title="QA审核未通过"><span class="dot warning"></span>QA阻断</span>`;
    }
    return `<span class="tag" title="KPI与QA状态正常"><span class="dot success"></span>通过</span>`;
}

function actionDisabledReason(patch, action) {
    const allowed = listActionCandidates(patch.currentState);
    if (!allowed.includes(action)) {
        return "当前状态不可执行该动作";
    }
    if (patch.kpiBlocked && ["APPROVE_REVIEW", "TRANSFER_TO_TEST", "PREPARE_RELEASE", "RELEASE"].includes(action)) {
        return "KPI卡点阻断，请先补齐指标";
    }
    if (patch.qaBlocked && ["APPROVE_REVIEW", "TRANSFER_TO_TEST"].includes(action)) {
        return "QA卡点阻断，请先完成审核";
    }
    return "";
}

function listActionCandidates(currentState) {
    const map = {
        DRAFT: ["SUBMIT_REVIEW"],
        REVIEWING: ["APPROVE_REVIEW", "REJECT_REVIEW"],
        REVIEW_PASSED: ["TRANSFER_TO_TEST"],
        TESTING: ["PASS_TEST", "FAIL_TEST"],
        TEST_PASSED: ["PREPARE_RELEASE"],
        RELEASE_READY: ["RELEASE"],
        RELEASED: ["ARCHIVE"],
        ARCHIVED: []
    };
    return map[currentState] || [];
}

function columnLabel(columnKey) {
    const map = {
        patchNo: "补丁编号",
        title: "补丁名称",
        productLineId: "关联产品",
        currentState: "当前阶段",
        kpiStatus: "KPI状态",
        priority: "优先级",
        ownerPmId: "负责人",
        updatedAt: "更新时间"
    };
    return map[columnKey] || columnKey;
}

function navigate(hash) {
    window.location.hash = hash;
}

function parseQuery(queryString) {
    if (!queryString) {
        return {};
    }
    return queryString.split("&").reduce((acc, pair) => {
        const [key, value = ""] = pair.split("=");
        acc[decodeURIComponent(key)] = decodeURIComponent(value);
        return acc;
    }, {});
}

function paginate(items, page, pageSize) {
    const safeSize = Math.max(1, pageSize || 10);
    const total = items.length;
    const totalPages = Math.max(1, Math.ceil(total / safeSize));
    const currentPage = Math.min(Math.max(1, page || 1), totalPages);
    const start = (currentPage - 1) * safeSize;
    return {
        page: currentPage,
        pageSize: safeSize,
        total,
        totalPages,
        items: items.slice(start, start + safeSize)
    };
}

function splitCommaList(text) {
    return text.split(",").map((item) => item.trim()).filter(Boolean);
}

function formatDateTime(value) {
    if (!value) {
        return "-";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        if (typeof value === "number") {
            const numeric = new Date(value);
            if (!Number.isNaN(numeric.getTime())) {
                return numeric.toLocaleString("zh-CN");
            }
        }
        return String(value);
    }
    return date.toLocaleString("zh-CN");
}

function toast(message, type = "info") {
    if (!toastRoot) {
        return;
    }
    const div = document.createElement("div");
    div.className = `toast ${type}`;
    div.textContent = message;
    toastRoot.appendChild(div);
    setTimeout(() => {
        div.remove();
    }, 2600);
}

function saveSession(session) {
    if (!session) {
        localStorage.removeItem(SESSION_KEY);
        return;
    }
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

function loadSession() {
    return loadJson(SESSION_KEY, null);
}

function savePatchColumns(columns) {
    localStorage.setItem(CONFIG_COLUMNS_KEY, JSON.stringify(columns));
}

function loadPatchColumns() {
    const saved = loadJson(CONFIG_COLUMNS_KEY, null);
    if (!saved || typeof saved !== "object") {
        return { ...COLUMN_DEFAULTS };
    }
    return {
        ...COLUMN_DEFAULTS,
        ...saved
    };
}

function loadJson(key, fallback) {
    try {
        const raw = localStorage.getItem(key);
        if (!raw) {
            return fallback;
        }
        return JSON.parse(raw);
    } catch (error) {
        console.warn("JSON parse failed", key, error);
        return fallback;
    }
}

function saveJson(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function escapeAttr(value) {
    return escapeHtml(value).replace(/`/g, "");
}

function uuid() {
    if (window.crypto?.randomUUID) {
        return window.crypto.randomUUID();
    }
    return `id-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

async function safeApi(fn, fallback, silent = false) {
    try {
        return await fn();
    } catch (error) {
        if (!silent) {
            toast(error.message || String(error), "error");
        }
        return fallback;
    }
}

async function request(url, options = {}) {
    if (!state.session) {
        throw new Error("请先登录系统");
    }
    const method = options.method || "GET";
    const headers = {
        "Content-Type": "application/json",
        "X-Tenant-Id": String(state.session.tenantId),
        "X-User-Id": String(state.session.userId),
        "X-Roles": String(state.session.roles),
        "X-Trace-Id": uuid()
    };
    if (options.idempotent) {
        headers["Idempotency-Key"] = uuid();
    }
    const response = await fetch(url, {
        method,
        headers: { ...headers, ...(options.headers || {}) },
        body: options.body ? JSON.stringify(options.body) : undefined
    });

    let payload = null;
    try {
        payload = await response.json();
    } catch (error) {
        payload = null;
    }

    if (!response.ok) {
        const message = payload?.message || `请求失败: HTTP ${response.status}`;
        throw new Error(message);
    }
    if (!payload || payload.code !== "0") {
        throw new Error(payload?.message || "业务请求失败");
    }
    return payload.data;
}

const api = {
    listPatches(stateFilter) {
        const query = stateFilter ? `?state=${encodeURIComponent(stateFilter)}` : "";
        return request(`/api/v1/patches${query}`);
    },
    getPatch(patchId) {
        return request(`/api/v1/patches/${patchId}`);
    },
    createPatch(payload) {
        return request("/api/v1/patches", { method: "POST", body: payload });
    },
    executePatchAction(patchId, payload) {
        return request(`/api/v1/patches/${patchId}/actions`, { method: "POST", body: payload, idempotent: true });
    },
    upsertPatchMetrics(patchId, payload) {
        return request(`/api/v1/patches/${patchId}/metrics`, { method: "POST", body: payload });
    },
    evaluatePatchKpi(patchId, payload) {
        return request(`/api/v1/patches/${patchId}/kpi/evaluate`, { method: "POST", body: payload });
    },
    listPatchTransitions(patchId) {
        return request(`/api/v1/patches/${patchId}/transitions`);
    },
    listPatchOperationLogs(patchId) {
        return request(`/api/v1/patches/${patchId}/operation-logs`);
    },
    createAttachment(patchId, payload) {
        return request(`/api/v1/patches/${patchId}/attachments`, { method: "POST", body: payload });
    },
    listPatchAttachments(patchId) {
        return request(`/api/v1/patches/${patchId}/attachments`);
    },
    listPatchTestTasks(patchId) {
        return request(`/api/v1/patches/${patchId}/test-tasks`);
    },
    submitTestTaskResult(taskId, payload) {
        return request(`/api/v1/test-tasks/${taskId}/results`, { method: "POST", body: payload });
    },

    listKpiRules() {
        return request("/api/v1/kpi/rules");
    },
    createKpiRule(payload) {
        return request("/api/v1/kpi/rules", { method: "POST", body: payload });
    },

    listQaPending() {
        return request("/api/v1/qa/tasks/my-pending");
    },
    decideQaTask(taskId, payload) {
        return request(`/api/v1/qa/tasks/${taskId}/decision`, { method: "POST", body: payload });
    },
    listQaPolicies() {
        return request("/api/v1/qa/policies");
    },
    createQaPolicy(payload) {
        return request("/api/v1/qa/policies", { method: "POST", body: payload });
    },

    listReviewSessions(patchId) {
        const query = patchId ? `?patchId=${encodeURIComponent(patchId)}` : "";
        return request(`/api/v1/review-sessions${query}`);
    },
    getReviewSession(sessionId) {
        return request(`/api/v1/review-sessions/${sessionId}`);
    },
    createReviewSession(payload) {
        return request("/api/v1/review-sessions", { method: "POST", body: payload });
    },
    voteReviewSession(sessionId, payload) {
        return request(`/api/v1/review-sessions/${sessionId}/votes`, { method: "POST", body: payload });
    },

    listScenarios() {
        return request("/api/v1/config/scenarios");
    },
    upsertScenario(payload) {
        return request("/api/v1/config/scenarios", { method: "POST", body: payload });
    },
    listProducts() {
        return request("/api/v1/config/products");
    },
    upsertProduct(payload) {
        return request("/api/v1/config/products", { method: "POST", body: payload });
    },
    bindScenarioProducts(payload) {
        return request("/api/v1/config/scenario-products", { method: "POST", body: payload });
    },
    listConfigRoles() {
        return request("/api/v1/config/roles");
    },
    upsertConfigRole(payload) {
        return request("/api/v1/config/roles", { method: "POST", body: payload });
    },
    listPermissions() {
        return request("/api/v1/config/permissions");
    },
    upsertPermission(payload) {
        return request("/api/v1/config/permissions", { method: "POST", body: payload });
    },
    getRolePermissions(roleId) {
        return request(`/api/v1/config/roles/${roleId}/permissions`);
    },
    assignRolePermissions(roleId, permissionIds) {
        return request(`/api/v1/config/roles/${roleId}/permissions`, { method: "POST", body: permissionIds });
    },
    assignUserRoleScope(payload) {
        return request("/api/v1/config/user-role-scopes", { method: "POST", body: payload });
    },
    listUserRoleScopes(userId) {
        return request(`/api/v1/config/users/${userId}/role-scopes`);
    },

    upsertRoleActionPermission(payload) {
        return request("/api/v1/iam/role-action-permissions", { method: "POST", body: payload });
    },
    listRoleActionPermissions(action) {
        return request(`/api/v1/iam/role-action-permissions?action=${encodeURIComponent(action)}`);
    },
    grantUserDataScope(payload) {
        return request("/api/v1/iam/user-data-scopes", { method: "POST", body: payload });
    },
    listUserDataScopes(userId) {
        return request(`/api/v1/iam/users/${userId}/data-scopes`);
    },

    listAdminRoles() {
        return request("/api/v1/admin/roles");
    },
    upsertAdminRole(payload) {
        return request("/api/v1/admin/roles", { method: "POST", body: payload });
    },
    listUsers() {
        return request("/api/v1/admin/users");
    },
    upsertUser(payload) {
        return request("/api/v1/admin/users", { method: "POST", body: payload });
    },
    assignUserRole(payload) {
        return request("/api/v1/admin/users/roles", { method: "POST", body: payload });
    },
    listUserRoles(userId) {
        return request(`/api/v1/admin/users/${userId}/roles`);
    },

    listMailServers() {
        return request("/api/v1/notify/mail/servers");
    },
    upsertMailServer(payload) {
        return request("/api/v1/notify/mail/servers", { method: "POST", body: payload });
    },
    listMailTemplates() {
        return request("/api/v1/notify/mail/templates");
    },
    upsertMailTemplate(payload) {
        return request("/api/v1/notify/mail/templates", { method: "POST", body: payload });
    },
    renderMailTemplate(payload) {
        return request("/api/v1/notify/mail/templates/render", { method: "POST", body: payload });
    },
    listMailPolicies() {
        return request("/api/v1/notify/mail/event-policies");
    },
    upsertMailPolicy(payload) {
        return request("/api/v1/notify/mail/event-policies", { method: "POST", body: payload });
    },
    listMailLogs(limit = 20) {
        return request(`/api/v1/notify/mail/logs?limit=${encodeURIComponent(limit)}`);
    },
    resendMailLog(logId) {
        return request(`/api/v1/notify/mail/logs/${logId}/resend`, { method: "POST" });
    },

    listAuditLogs(bizType) {
        return request(`/api/v1/audit/logs?bizType=${encodeURIComponent(bizType)}`);
    }
};
