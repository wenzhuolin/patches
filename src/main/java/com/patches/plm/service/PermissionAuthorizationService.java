package com.patches.plm.service;

import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.repository.PermissionDefRepository;
import com.patches.plm.domain.repository.RolePermissionRelRepository;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PermissionAuthorizationService {

    private final PermissionDefRepository permissionDefRepository;
    private final RolePermissionRelRepository rolePermissionRelRepository;

    public PermissionAuthorizationService(PermissionDefRepository permissionDefRepository,
                                          RolePermissionRelRepository rolePermissionRelRepository) {
        this.permissionDefRepository = permissionDefRepository;
        this.rolePermissionRelRepository = rolePermissionRelRepository;
    }

    public void assertHasPermission(RequestContext context, String permCode) {
        if (context.roles().contains("SUPER_ADMIN") || context.roles().contains("LINE_ADMIN")) {
            return;
        }
        String normalized = permCode.toUpperCase(Locale.ROOT);
        // 兼容模式：若租户尚未配置权限点，不做强阻断，避免老接口行为回归。
        if (permissionDefRepository.findByTenantIdAndPermCode(context.tenantId(), normalized).isEmpty()) {
            return;
        }
        List<String> roles = normalizeRoles(context.roles());
        if (roles.isEmpty() || !rolePermissionRelRepository.existsGrant(context.tenantId(), roles, normalized)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "缺少权限: " + normalized);
        }
    }

    private List<String> normalizeRoles(Set<String> roles) {
        List<String> list = new ArrayList<>();
        for (String role : roles) {
            if (role != null && !role.isBlank()) {
                list.add(role.toUpperCase(Locale.ROOT));
            }
        }
        return list;
    }
}
