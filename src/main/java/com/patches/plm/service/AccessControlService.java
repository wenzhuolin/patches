package com.patches.plm.service;

import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.PatchEntity;
import com.patches.plm.domain.entity.UserDataScopeEntity;
import com.patches.plm.domain.enums.PatchAction;
import com.patches.plm.domain.repository.RoleActionPermissionRepository;
import com.patches.plm.domain.repository.UserDataScopeRepository;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccessControlService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String LINE_ADMIN = "LINE_ADMIN";
    private static final String GLOBAL = "GLOBAL";
    private static final String PRODUCT_LINE = "PRODUCT_LINE";

    private final Map<PatchAction, Set<String>> actionRoleMapping = new EnumMap<>(PatchAction.class);
    private final RoleActionPermissionRepository roleActionPermissionRepository;
    private final UserDataScopeRepository userDataScopeRepository;

    public AccessControlService(RoleActionPermissionRepository roleActionPermissionRepository,
                                UserDataScopeRepository userDataScopeRepository) {
        this.roleActionPermissionRepository = roleActionPermissionRepository;
        this.userDataScopeRepository = userDataScopeRepository;
        actionRoleMapping.put(PatchAction.SUBMIT_REVIEW, Set.of("PM", "DEV", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.APPROVE_REVIEW, Set.of("REVIEWER", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.REJECT_REVIEW, Set.of("REVIEWER", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.TRANSFER_TO_TEST, Set.of("PM", "TEST", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.PASS_TEST, Set.of("TEST", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.FAIL_TEST, Set.of("TEST", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.PREPARE_RELEASE, Set.of("PM", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.RELEASE, Set.of("LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.ARCHIVE, Set.of("PM", "LINE_ADMIN", "SUPER_ADMIN"));
    }

    public void assertCanCreatePatch(RequestContext context, Long productLineId) {
        Set<String> normalizedRoles = normalizeRoles(context.roles());
        if (!(normalizedRoles.contains("PM")
                || normalizedRoles.contains(LINE_ADMIN)
                || normalizedRoles.contains(SUPER_ADMIN))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅PM/产品线管理员可创建补丁");
        }
        assertDataScope(context.tenantId(), context.userId(), normalizedRoles, productLineId);
    }

    public void assertCanViewPatch(RequestContext context, PatchEntity patch) {
        Set<String> normalizedRoles = normalizeRoles(context.roles());
        assertDataScope(context.tenantId(), context.userId(), normalizedRoles, patch.getProductLineId());
    }

    public void assertCanExecuteAction(RequestContext context, PatchAction action, PatchEntity patch) {
        Set<String> normalizedRoles = normalizeRoles(context.roles());
        if (normalizedRoles.contains(SUPER_ADMIN)) {
            return;
        }

        Set<String> allowedRoles = resolveAllowedRoles(context.tenantId(), action);
        if (allowedRoles.isEmpty() || normalizedRoles.stream().noneMatch(allowedRoles::contains)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色无权执行动作: " + action);
        }
        assertDataScope(context.tenantId(), context.userId(), normalizedRoles, patch.getProductLineId());
    }

    public void assertCanQaApprove(Set<String> roles) {
        Set<String> normalizedRoles = normalizeRoles(roles);
        if (!(normalizedRoles.contains("QA")
                || normalizedRoles.contains("PRODUCT_LINE_QA")
                || normalizedRoles.contains("PROJECT_QA")
                || normalizedRoles.contains("REVIEW_BOARD")
                || normalizedRoles.contains(SUPER_ADMIN)
                || normalizedRoles.contains(LINE_ADMIN))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色无权执行QA审批");
        }
    }

    private Set<String> resolveAllowedRoles(Long tenantId, PatchAction action) {
        List<String> dbRoles = roleActionPermissionRepository.findEnabledRolesByAction(tenantId, action.name());
        if (!dbRoles.isEmpty()) {
            return dbRoles.stream().map(v -> v.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        }
        return actionRoleMapping.getOrDefault(action, Set.of()).stream()
                .map(v -> v.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private void assertDataScope(Long tenantId, Long userId, Set<String> roles, Long productLineId) {
        if (roles.contains(SUPER_ADMIN) || roles.contains(LINE_ADMIN)) {
            return;
        }
        List<UserDataScopeEntity> scopes = userDataScopeRepository.findByTenantIdAndUserIdAndEnabledTrue(tenantId, userId);
        if (scopes.isEmpty()) {
            // 没配置数据权限时保持兼容，后续可通过配置切换到严格模式。
            return;
        }
        boolean allowed = scopes.stream().anyMatch(scope ->
                GLOBAL.equalsIgnoreCase(scope.getScopeType())
                        || (PRODUCT_LINE.equalsIgnoreCase(scope.getScopeType())
                        && String.valueOf(productLineId).equals(scope.getScopeValue())));
        if (!allowed) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号不在该产品线数据权限范围内");
        }
    }

    private Set<String> normalizeRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return roles.stream().map(v -> v.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
    }
}
