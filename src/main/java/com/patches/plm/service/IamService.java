package com.patches.plm.service;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.RoleActionPermissionEntity;
import com.patches.plm.domain.entity.UserDataScopeEntity;
import com.patches.plm.domain.repository.RoleActionPermissionRepository;
import com.patches.plm.domain.repository.UserDataScopeRepository;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class IamService {

    private final RoleActionPermissionRepository roleActionPermissionRepository;
    private final UserDataScopeRepository userDataScopeRepository;
    private final AuditLogService auditLogService;

    public IamService(RoleActionPermissionRepository roleActionPermissionRepository,
                      UserDataScopeRepository userDataScopeRepository,
                      AuditLogService auditLogService) {
        this.roleActionPermissionRepository = roleActionPermissionRepository;
        this.userDataScopeRepository = userDataScopeRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public RoleActionPermissionResponse upsertRoleActionPermission(Long tenantId,
                                                                   RoleActionPermissionUpsertRequest request,
                                                                   RequestContext context) {
        assertAdmin(context);
        String roleCode = request.roleCode().toUpperCase(Locale.ROOT);
        String action = request.action().name();
        RoleActionPermissionEntity entity = roleActionPermissionRepository
                .findByTenantIdAndRoleCodeAndAction(tenantId, roleCode, action)
                .orElseGet(RoleActionPermissionEntity::new);
        boolean isNew = entity.getId() == null;
        entity.setTenantId(tenantId);
        entity.setRoleCode(roleCode);
        entity.setAction(action);
        entity.setEnabled(request.enabled() == null || request.enabled());
        if (isNew) {
            entity.setCreatedBy(context.userId());
        }
        entity.setUpdatedBy(context.userId());
        RoleActionPermissionEntity saved = roleActionPermissionRepository.save(entity);
        auditLogService.log("IAM", saved.getId(), isNew ? "CREATE_ROLE_ACTION" : "UPDATE_ROLE_ACTION", null, saved, context);
        return new RoleActionPermissionResponse(saved.getId(), saved.getRoleCode(), saved.getAction(), saved.isEnabled());
    }

    @Transactional(readOnly = true)
    public List<RoleActionPermissionResponse> listRoleActionPermissions(Long tenantId, String action) {
        return roleActionPermissionRepository.findByTenantIdAndActionOrderByRoleCodeAsc(tenantId, action.toUpperCase(Locale.ROOT))
                .stream()
                .map(item -> new RoleActionPermissionResponse(item.getId(), item.getRoleCode(), item.getAction(), item.isEnabled()))
                .toList();
    }

    @Transactional
    public UserDataScopeResponse grantDataScope(Long tenantId, UserDataScopeGrantRequest request, RequestContext context) {
        assertAdmin(context);
        String scopeType = request.scopeType().toUpperCase(Locale.ROOT);
        String scopeValue = request.scopeValue();
        if ("GLOBAL".equals(scopeType)) {
            scopeValue = null;
        }
        UserDataScopeEntity entity = userDataScopeRepository
                .findByTenantIdAndUserIdAndScopeTypeAndScopeValue(tenantId, request.userId(), scopeType, scopeValue)
                .orElseGet(UserDataScopeEntity::new);
        boolean isNew = entity.getId() == null;
        entity.setTenantId(tenantId);
        entity.setUserId(request.userId());
        entity.setScopeType(scopeType);
        entity.setScopeValue(scopeValue);
        entity.setEnabled(request.enabled() == null || request.enabled());
        if (isNew) {
            entity.setCreatedBy(context.userId());
        }
        entity.setUpdatedBy(context.userId());
        UserDataScopeEntity saved = userDataScopeRepository.save(entity);
        auditLogService.log("IAM", saved.getId(), isNew ? "CREATE_DATA_SCOPE" : "UPDATE_DATA_SCOPE", null, saved, context);
        return toDataScopeResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserDataScopeResponse> listUserDataScopes(Long tenantId, Long userId) {
        return userDataScopeRepository.findByTenantIdAndUserIdOrderByScopeTypeAscScopeValueAsc(tenantId, userId)
                .stream()
                .map(this::toDataScopeResponse)
                .toList();
    }

    private UserDataScopeResponse toDataScopeResponse(UserDataScopeEntity entity) {
        return new UserDataScopeResponse(entity.getId(), entity.getUserId(), entity.getScopeType(), entity.getScopeValue(), entity.isEnabled());
    }

    private void assertAdmin(RequestContext context) {
        if (!(context.roles().contains("SUPER_ADMIN") || context.roles().contains("LINE_ADMIN"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可执行IAM配置操作");
        }
    }
}
