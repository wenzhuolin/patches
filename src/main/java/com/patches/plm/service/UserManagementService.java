package com.patches.plm.service;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.SysRoleEntity;
import com.patches.plm.domain.entity.SysUserEntity;
import com.patches.plm.domain.entity.UserRoleRelationEntity;
import com.patches.plm.domain.repository.SysRoleRepository;
import com.patches.plm.domain.repository.SysUserRepository;
import com.patches.plm.domain.repository.UserRoleRelationRepository;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserManagementService {

    private final SysRoleRepository sysRoleRepository;
    private final SysUserRepository sysUserRepository;
    private final UserRoleRelationRepository userRoleRelationRepository;
    private final AuditLogService auditLogService;

    public UserManagementService(SysRoleRepository sysRoleRepository, SysUserRepository sysUserRepository,
                                 UserRoleRelationRepository userRoleRelationRepository,
                                 AuditLogService auditLogService) {
        this.sysRoleRepository = sysRoleRepository;
        this.sysUserRepository = sysUserRepository;
        this.userRoleRelationRepository = userRoleRelationRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public RoleResponse createOrUpdateRole(Long tenantId, RoleCreateRequest request, RequestContext context) {
        assertAdmin(context);
        String roleCode = request.roleCode().toUpperCase(Locale.ROOT);
        SysRoleEntity role = sysRoleRepository.findByTenantIdAndRoleCode(tenantId, roleCode)
                .orElseGet(SysRoleEntity::new);
        boolean isNew = role.getId() == null;
        role.setTenantId(tenantId);
        role.setRoleCode(roleCode);
        role.setRoleName(request.roleName());
        role.setEnabled(request.enabled() == null || request.enabled());
        if (isNew) {
            role.setCreatedBy(context.userId());
        }
        role.setUpdatedBy(context.userId());
        SysRoleEntity saved = sysRoleRepository.save(role);
        auditLogService.log("IAM_ROLE", saved.getId(), isNew ? "CREATE_ROLE" : "UPDATE_ROLE", null, saved, context);
        return new RoleResponse(saved.getId(), saved.getRoleCode(), saved.getRoleName(), saved.isEnabled());
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles(Long tenantId) {
        return sysRoleRepository.findByTenantIdOrderByRoleCodeAsc(tenantId).stream()
                .map(role -> new RoleResponse(role.getId(), role.getRoleCode(), role.getRoleName(), role.isEnabled()))
                .toList();
    }

    @Transactional
    public UserResponse createOrUpdateUser(Long tenantId, UserCreateRequest request, RequestContext context) {
        assertAdmin(context);
        SysUserEntity user = sysUserRepository.findByTenantIdAndUsername(tenantId, request.username())
                .orElseGet(SysUserEntity::new);
        boolean isNew = user.getId() == null;
        user.setTenantId(tenantId);
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setMobile(request.mobile());
        user.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status().toUpperCase(Locale.ROOT));
        if (isNew) {
            user.setCreatedBy(context.userId());
        }
        user.setUpdatedBy(context.userId());
        SysUserEntity saved = sysUserRepository.save(user);
        auditLogService.log("IAM_USER", saved.getId(), isNew ? "CREATE_USER" : "UPDATE_USER", null, saved, context);
        List<String> roles = userRoleRelationRepository.findEnabledRoleCodes(tenantId, saved.getId());
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getDisplayName(), saved.getEmail(), saved.getMobile(), saved.getStatus(), roles);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(Long tenantId) {
        return sysUserRepository.findByTenantIdOrderByIdDesc(tenantId).stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getMobile(),
                        user.getStatus(),
                        userRoleRelationRepository.findEnabledRoleCodes(tenantId, user.getId())
                ))
                .toList();
    }

    @Transactional
    public UserRoleResponse assignRole(Long tenantId, UserRoleAssignRequest request, RequestContext context) {
        assertAdmin(context);
        SysUserEntity user = sysUserRepository.findByIdAndTenantId(request.userId(), tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        String roleCode = request.roleCode().toUpperCase(Locale.ROOT);
        SysRoleEntity role = sysRoleRepository.findByTenantIdAndRoleCode(tenantId, roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在"));
        if (!role.isEnabled()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色已禁用");
        }
        UserRoleRelationEntity relation = userRoleRelationRepository
                .findByTenantIdAndUserIdAndRoleCode(tenantId, user.getId(), roleCode)
                .orElseGet(UserRoleRelationEntity::new);
        boolean isNew = relation.getId() == null;
        relation.setTenantId(tenantId);
        relation.setUserId(user.getId());
        relation.setRoleCode(roleCode);
        relation.setEnabled(request.enabled() == null || request.enabled());
        if (isNew) {
            relation.setCreatedBy(context.userId());
        }
        relation.setUpdatedBy(context.userId());
        UserRoleRelationEntity saved = userRoleRelationRepository.save(relation);
        auditLogService.log("IAM_USER_ROLE", saved.getId(), isNew ? "ASSIGN_ROLE" : "UPDATE_USER_ROLE", null, saved, context);
        return new UserRoleResponse(saved.getId(), saved.getUserId(), saved.getRoleCode(), saved.isEnabled());
    }

    @Transactional(readOnly = true)
    public List<UserRoleResponse> listUserRoles(Long tenantId, Long userId) {
        return userRoleRelationRepository.findByTenantIdAndUserIdOrderByRoleCodeAsc(tenantId, userId)
                .stream()
                .map(item -> new UserRoleResponse(item.getId(), item.getUserId(), item.getRoleCode(), item.isEnabled()))
                .toList();
    }

    private void assertAdmin(RequestContext context) {
        if (!(context.roles().contains("SUPER_ADMIN") || context.roles().contains("LINE_ADMIN"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可执行用户角色管理操作");
        }
    }
}
