package com.patches.plm.service;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.*;
import com.patches.plm.domain.repository.*;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ConfigManagementService {

    private final DeliveryScenarioRepository deliveryScenarioRepository;
    private final ProductRepository productRepository;
    private final ScenarioProductRelRepository scenarioProductRelRepository;
    private final SysRoleRepository sysRoleRepository;
    private final PermissionDefRepository permissionDefRepository;
    private final RolePermissionRelRepository rolePermissionRelRepository;
    private final UserRoleScopeRelRepository userRoleScopeRelRepository;
    private final SysUserRepository sysUserRepository;
    private final ConfigAuditLogService configAuditLogService;

    public ConfigManagementService(DeliveryScenarioRepository deliveryScenarioRepository,
                                   ProductRepository productRepository,
                                   ScenarioProductRelRepository scenarioProductRelRepository,
                                   SysRoleRepository sysRoleRepository,
                                   PermissionDefRepository permissionDefRepository,
                                   RolePermissionRelRepository rolePermissionRelRepository,
                                   UserRoleScopeRelRepository userRoleScopeRelRepository,
                                   SysUserRepository sysUserRepository,
                                   ConfigAuditLogService configAuditLogService) {
        this.deliveryScenarioRepository = deliveryScenarioRepository;
        this.productRepository = productRepository;
        this.scenarioProductRelRepository = scenarioProductRelRepository;
        this.sysRoleRepository = sysRoleRepository;
        this.permissionDefRepository = permissionDefRepository;
        this.rolePermissionRelRepository = rolePermissionRelRepository;
        this.userRoleScopeRelRepository = userRoleScopeRelRepository;
        this.sysUserRepository = sysUserRepository;
        this.configAuditLogService = configAuditLogService;
    }

    @Transactional
    public ConfigScenarioResponse upsertScenario(Long tenantId, ConfigScenarioUpsertRequest request, RequestContext context) {
        assertAdmin(context);
        DeliveryScenarioEntity scenario = deliveryScenarioRepository
                .findByTenantIdAndScenarioCode(tenantId, request.scenarioCode())
                .orElseGet(DeliveryScenarioEntity::new);
        boolean isNew = scenario.getId() == null;
        DeliveryScenarioEntity before = cloneScenario(scenario);
        scenario.setTenantId(tenantId);
        scenario.setScenarioCode(request.scenarioCode().toUpperCase(Locale.ROOT));
        scenario.setScenarioName(request.scenarioName());
        scenario.setDescription(request.description());
        scenario.setStatus(normalizeStatus(request.status(), "ACTIVE"));
        scenario.setExtProps(request.extProps());
        if (isNew) {
            scenario.setCreatedBy(context.userId());
        }
        scenario.setUpdatedBy(context.userId());
        scenario.setDeleted(false);
        DeliveryScenarioEntity saved = deliveryScenarioRepository.save(scenario);
        configAuditLogService.log("SCENARIO", saved.getId(), isNew ? "CREATE" : "UPDATE", before, saved, context);
        return toScenarioResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConfigScenarioResponse> listScenarios(Long tenantId) {
        return deliveryScenarioRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId).stream()
                .filter(item -> !item.isDeleted())
                .map(this::toScenarioResponse)
                .toList();
    }

    @Transactional
    public ConfigProductResponse upsertProduct(Long tenantId, ConfigProductUpsertRequest request, RequestContext context) {
        assertAdmin(context);
        ProductEntity product = productRepository.findByTenantIdAndProductCode(tenantId, request.productCode())
                .orElseGet(ProductEntity::new);
        boolean isNew = product.getId() == null;
        ProductEntity before = cloneProduct(product);
        product.setTenantId(tenantId);
        product.setProductCode(request.productCode().toUpperCase(Locale.ROOT));
        product.setProductName(request.productName());
        product.setDescription(request.description());
        product.setOwnerUserId(request.ownerUserId());
        product.setStatus(normalizeStatus(request.status(), "ACTIVE"));
        product.setExtProps(request.extProps());
        if (isNew) {
            product.setCreatedBy(context.userId());
        }
        product.setUpdatedBy(context.userId());
        product.setDeleted(false);
        ProductEntity saved = productRepository.save(product);
        configAuditLogService.log("PRODUCT", saved.getId(), isNew ? "CREATE" : "UPDATE", before, saved, context);
        return toProductResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConfigProductResponse> listProducts(Long tenantId) {
        return productRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId).stream()
                .filter(item -> !item.isDeleted())
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional
    public List<Long> bindScenarioProducts(Long tenantId, ConfigScenarioProductBindRequest request, RequestContext context) {
        assertAdmin(context);
        DeliveryScenarioEntity scenario = deliveryScenarioRepository.findByTenantIdAndId(tenantId, request.scenarioId())
                .filter(item -> !item.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "交付场景不存在"));
        Set<Long> targetProductIds = new HashSet<>(request.productIds());
        if (targetProductIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "productIds不能为空");
        }

        for (Long productId : targetProductIds) {
            ProductEntity product = productRepository.findByTenantIdAndId(tenantId, productId)
                    .filter(item -> !item.isDeleted())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "产品不存在: " + productId));
            ScenarioProductRelEntity rel = scenarioProductRelRepository
                    .findByTenantIdAndScenarioIdAndProductId(tenantId, scenario.getId(), product.getId())
                    .orElseGet(ScenarioProductRelEntity::new);
            if (rel.getId() == null) {
                rel.setCreatedBy(context.userId());
            }
            rel.setTenantId(tenantId);
            rel.setScenarioId(scenario.getId());
            rel.setProductId(product.getId());
            rel.setStatus("ACTIVE");
            rel.setDeleted(false);
            rel.setUpdatedBy(context.userId());
            scenarioProductRelRepository.save(rel);
        }

        List<ScenarioProductRelEntity> existing = scenarioProductRelRepository
                .findByTenantIdAndScenarioIdAndStatusOrderByProductIdAsc(tenantId, scenario.getId(), "ACTIVE");
        for (ScenarioProductRelEntity rel : existing) {
            if (!targetProductIds.contains(rel.getProductId())) {
                rel.setStatus("INACTIVE");
                rel.setUpdatedBy(context.userId());
                scenarioProductRelRepository.save(rel);
            }
        }
        configAuditLogService.log("SCENARIO_PRODUCT_REL", scenario.getId(), "BIND_PRODUCTS", null, targetProductIds, context);

        return targetProductIds.stream().sorted().toList();
    }

    @Transactional
    public ConfigRoleResponse upsertRole(Long tenantId, ConfigRoleUpsertRequest request, RequestContext context) {
        assertAdmin(context);
        SysRoleEntity role = sysRoleRepository.findByTenantIdAndRoleCode(tenantId, request.roleCode().toUpperCase(Locale.ROOT))
                .orElseGet(SysRoleEntity::new);
        boolean isNew = role.getId() == null;
        SysRoleEntity before = cloneRole(role);
        role.setTenantId(tenantId);
        role.setRoleCode(request.roleCode().toUpperCase(Locale.ROOT));
        role.setRoleName(request.roleName());
        role.setRoleLevel(normalizeRoleLevel(request.roleLevel()));
        role.setScopeRefId(request.scopeRefId());
        role.setEnabled(request.enabled() == null || request.enabled());
        role.setDeleted(false);
        if (isNew) {
            role.setCreatedBy(context.userId());
        }
        role.setUpdatedBy(context.userId());
        SysRoleEntity saved = sysRoleRepository.save(role);
        configAuditLogService.log("ROLE", saved.getId(), isNew ? "CREATE" : "UPDATE", before, saved, context);
        return toRoleResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConfigRoleResponse> listRoles(Long tenantId) {
        return sysRoleRepository.findByTenantIdOrderByRoleCodeAsc(tenantId).stream()
                .filter(item -> !item.isDeleted())
                .map(this::toRoleResponse)
                .toList();
    }

    @Transactional
    public ConfigPermissionResponse upsertPermission(Long tenantId, ConfigPermissionUpsertRequest request, RequestContext context) {
        assertAdmin(context);
        PermissionDefEntity permission = permissionDefRepository
                .findByTenantIdAndPermCode(tenantId, request.permCode().toUpperCase(Locale.ROOT))
                .orElseGet(PermissionDefEntity::new);
        boolean isNew = permission.getId() == null;
        PermissionDefEntity before = clonePermission(permission);
        permission.setTenantId(tenantId);
        permission.setPermCode(request.permCode().toUpperCase(Locale.ROOT));
        permission.setPermName(request.permName());
        permission.setPermType(request.permType().toUpperCase(Locale.ROOT));
        permission.setResource(request.resource());
        permission.setAction(request.action());
        permission.setParentId(request.parentId());
        permission.setEnabled(request.enabled() == null || request.enabled());
        permission.setDeleted(false);
        if (isNew) {
            permission.setCreatedBy(context.userId());
        }
        permission.setUpdatedBy(context.userId());
        PermissionDefEntity saved = permissionDefRepository.save(permission);
        configAuditLogService.log("PERMISSION", saved.getId(), isNew ? "CREATE" : "UPDATE", before, saved, context);
        return toPermissionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConfigPermissionResponse> listPermissions(Long tenantId) {
        return permissionDefRepository.findByTenantIdOrderByPermCodeAsc(tenantId).stream()
                .filter(item -> !item.isDeleted())
                .map(this::toPermissionResponse)
                .toList();
    }

    @Transactional
    public ConfigRolePermissionResponse assignRolePermissions(Long tenantId,
                                                              ConfigRolePermissionAssignRequest request,
                                                              RequestContext context) {
        assertAdmin(context);
        SysRoleEntity role = sysRoleRepository.findByTenantIdAndId(tenantId, request.roleId())
                .filter(item -> !item.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在"));

        Set<Long> targetIds = new HashSet<>(request.permissionIds());
        if (targetIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "permissionIds不能为空");
        }
        for (Long permissionId : targetIds) {
            PermissionDefEntity permission = permissionDefRepository.findByTenantIdAndId(tenantId, permissionId)
                    .filter(item -> !item.isDeleted())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "权限不存在: " + permissionId));
            RolePermissionRelEntity rel = rolePermissionRelRepository
                    .findByTenantIdAndRoleIdAndPermissionId(tenantId, role.getId(), permission.getId())
                    .orElseGet(RolePermissionRelEntity::new);
            if (rel.getId() == null) {
                rel.setCreatedBy(context.userId());
            }
            rel.setTenantId(tenantId);
            rel.setRoleId(role.getId());
            rel.setPermissionId(permission.getId());
            rel.setGrantType("ALLOW");
            rel.setDeleted(false);
            rel.setUpdatedBy(context.userId());
            rolePermissionRelRepository.save(rel);
        }
        List<RolePermissionRelEntity> existing = rolePermissionRelRepository.findByTenantIdAndRoleIdOrderByPermissionIdAsc(tenantId, role.getId());
        for (RolePermissionRelEntity rel : existing) {
            if (!targetIds.contains(rel.getPermissionId()) && !rel.isDeleted()) {
                rel.setDeleted(true);
                rel.setUpdatedBy(context.userId());
                rolePermissionRelRepository.save(rel);
            }
        }
        List<String> permCodes = permissionDefRepository.findByTenantIdAndIdIn(tenantId, targetIds).stream()
                .map(PermissionDefEntity::getPermCode)
                .sorted()
                .toList();
        configAuditLogService.log("ROLE_PERMISSION", role.getId(), "ASSIGN", null, permCodes, context);
        return new ConfigRolePermissionResponse(role.getId(), permCodes);
    }

    @Transactional(readOnly = true)
    public ConfigRolePermissionResponse listRolePermissions(Long tenantId, Long roleId) {
        SysRoleEntity role = sysRoleRepository.findByTenantIdAndId(tenantId, roleId)
                .filter(item -> !item.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在"));
        List<Long> permissionIds = rolePermissionRelRepository.findByTenantIdAndRoleIdOrderByPermissionIdAsc(tenantId, role.getId())
                .stream()
                .filter(item -> !item.isDeleted())
                .map(RolePermissionRelEntity::getPermissionId)
                .toList();
        if (permissionIds.isEmpty()) {
            return new ConfigRolePermissionResponse(roleId, List.of());
        }
        List<String> permCodes = permissionDefRepository.findByTenantIdAndIdIn(tenantId, permissionIds).stream()
                .filter(item -> !item.isDeleted())
                .map(PermissionDefEntity::getPermCode)
                .sorted()
                .toList();
        return new ConfigRolePermissionResponse(roleId, permCodes);
    }

    @Transactional
    public ConfigUserRoleScopeResponse assignUserRoleScope(Long tenantId,
                                                           ConfigUserRoleScopeAssignRequest request,
                                                           RequestContext context) {
        assertAdmin(context);
        sysUserRepository.findByIdAndTenantId(request.userId(), tenantId)
                .filter(item -> !item.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        SysRoleEntity role = sysRoleRepository.findByTenantIdAndId(tenantId, request.roleId())
                .filter(item -> !item.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在"));

        String scopeLevel = normalizeRoleLevel(request.scopeLevel());
        UserRoleScopeRelEntity rel = userRoleScopeRelRepository
                .findByTenantIdAndUserIdAndRoleIdAndScopeLevelAndScenarioIdAndProductId(
                        tenantId, request.userId(), role.getId(), scopeLevel, request.scenarioId(), request.productId()
                ).orElseGet(UserRoleScopeRelEntity::new);

        boolean isNew = rel.getId() == null;
        UserRoleScopeRelEntity before = cloneRoleScope(rel);
        rel.setTenantId(tenantId);
        rel.setUserId(request.userId());
        rel.setRoleId(role.getId());
        rel.setScopeLevel(scopeLevel);
        rel.setScenarioId(request.scenarioId());
        rel.setProductId(request.productId());
        rel.setStatus(normalizeStatus(request.status(), "ACTIVE"));
        rel.setDeleted(false);
        if (isNew) {
            rel.setCreatedBy(context.userId());
        }
        rel.setUpdatedBy(context.userId());
        UserRoleScopeRelEntity saved = userRoleScopeRelRepository.save(rel);
        configAuditLogService.log("USER_ROLE_SCOPE", saved.getId(), isNew ? "CREATE" : "UPDATE", before, saved, context);
        return toRoleScopeResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConfigUserRoleScopeResponse> listUserRoleScopes(Long tenantId, Long userId) {
        return userRoleScopeRelRepository.findByTenantIdAndUserIdAndStatusOrderByIdDesc(tenantId, userId, "ACTIVE")
                .stream()
                .filter(item -> !item.isDeleted())
                .map(this::toRoleScopeResponse)
                .toList();
    }

    private ConfigScenarioResponse toScenarioResponse(DeliveryScenarioEntity saved) {
        return new ConfigScenarioResponse(
                saved.getId(),
                saved.getScenarioCode(),
                saved.getScenarioName(),
                saved.getDescription(),
                saved.getStatus()
        );
    }

    private ConfigProductResponse toProductResponse(ProductEntity saved) {
        List<Long> scenarioIds = scenarioProductRelRepository
                .findByTenantIdAndProductIdAndStatusOrderByScenarioIdAsc(saved.getTenantId(), saved.getId(), "ACTIVE")
                .stream()
                .filter(rel -> !rel.isDeleted())
                .map(ScenarioProductRelEntity::getScenarioId)
                .toList();
        return new ConfigProductResponse(
                saved.getId(),
                saved.getProductCode(),
                saved.getProductName(),
                saved.getDescription(),
                saved.getOwnerUserId(),
                saved.getStatus(),
                scenarioIds
        );
    }

    private ConfigRoleResponse toRoleResponse(SysRoleEntity saved) {
        return new ConfigRoleResponse(
                saved.getId(),
                saved.getRoleCode(),
                saved.getRoleName(),
                saved.getRoleLevel(),
                saved.getScopeRefId(),
                saved.isEnabled()
        );
    }

    private ConfigPermissionResponse toPermissionResponse(PermissionDefEntity saved) {
        return new ConfigPermissionResponse(
                saved.getId(),
                saved.getPermCode(),
                saved.getPermName(),
                saved.getPermType(),
                saved.getResource(),
                saved.getAction(),
                saved.getParentId(),
                saved.isEnabled()
        );
    }

    private ConfigUserRoleScopeResponse toRoleScopeResponse(UserRoleScopeRelEntity saved) {
        return new ConfigUserRoleScopeResponse(
                saved.getId(),
                saved.getUserId(),
                saved.getRoleId(),
                saved.getScopeLevel(),
                saved.getScenarioId(),
                saved.getProductId(),
                saved.getStatus()
        );
    }

    private void assertAdmin(RequestContext context) {
        if (!(context.roles().contains("SUPER_ADMIN") || context.roles().contains("LINE_ADMIN"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可执行配置管理操作");
        }
    }

    private String normalizeStatus(String status, String defaultValue) {
        return status == null || status.isBlank() ? defaultValue : status.toUpperCase(Locale.ROOT);
    }

    private String normalizeRoleLevel(String roleLevel) {
        if (roleLevel == null || roleLevel.isBlank()) {
            return "GLOBAL";
        }
        String level = roleLevel.toUpperCase(Locale.ROOT);
        if (!Set.of("GLOBAL", "SCENARIO", "PRODUCT").contains(level)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "roleLevel仅支持GLOBAL/SCENARIO/PRODUCT");
        }
        return level;
    }

    private DeliveryScenarioEntity cloneScenario(DeliveryScenarioEntity source) {
        DeliveryScenarioEntity copied = new DeliveryScenarioEntity();
        copied.setId(source.getId());
        copied.setTenantId(source.getTenantId());
        copied.setScenarioCode(source.getScenarioCode());
        copied.setScenarioName(source.getScenarioName());
        copied.setDescription(source.getDescription());
        copied.setStatus(source.getStatus());
        copied.setExtProps(source.getExtProps());
        copied.setCreatedAt(source.getCreatedAt());
        copied.setCreatedBy(source.getCreatedBy());
        copied.setUpdatedAt(source.getUpdatedAt());
        copied.setUpdatedBy(source.getUpdatedBy());
        copied.setDeleted(source.isDeleted());
        return copied;
    }

    private ProductEntity cloneProduct(ProductEntity source) {
        ProductEntity copied = new ProductEntity();
        copied.setId(source.getId());
        copied.setTenantId(source.getTenantId());
        copied.setProductCode(source.getProductCode());
        copied.setProductName(source.getProductName());
        copied.setDescription(source.getDescription());
        copied.setOwnerUserId(source.getOwnerUserId());
        copied.setStatus(source.getStatus());
        copied.setExtProps(source.getExtProps());
        copied.setCreatedAt(source.getCreatedAt());
        copied.setCreatedBy(source.getCreatedBy());
        copied.setUpdatedAt(source.getUpdatedAt());
        copied.setUpdatedBy(source.getUpdatedBy());
        copied.setDeleted(source.isDeleted());
        return copied;
    }

    private SysRoleEntity cloneRole(SysRoleEntity source) {
        SysRoleEntity copied = new SysRoleEntity();
        copied.setId(source.getId());
        copied.setTenantId(source.getTenantId());
        copied.setRoleCode(source.getRoleCode());
        copied.setRoleName(source.getRoleName());
        copied.setRoleLevel(source.getRoleLevel());
        copied.setScopeRefId(source.getScopeRefId());
        copied.setEnabled(source.isEnabled());
        copied.setCreatedAt(source.getCreatedAt());
        copied.setCreatedBy(source.getCreatedBy());
        copied.setUpdatedAt(source.getUpdatedAt());
        copied.setUpdatedBy(source.getUpdatedBy());
        copied.setDeleted(source.isDeleted());
        return copied;
    }

    private PermissionDefEntity clonePermission(PermissionDefEntity source) {
        PermissionDefEntity copied = new PermissionDefEntity();
        copied.setId(source.getId());
        copied.setTenantId(source.getTenantId());
        copied.setPermCode(source.getPermCode());
        copied.setPermName(source.getPermName());
        copied.setPermType(source.getPermType());
        copied.setResource(source.getResource());
        copied.setAction(source.getAction());
        copied.setParentId(source.getParentId());
        copied.setEnabled(source.isEnabled());
        copied.setCreatedAt(source.getCreatedAt());
        copied.setCreatedBy(source.getCreatedBy());
        copied.setUpdatedAt(source.getUpdatedAt());
        copied.setUpdatedBy(source.getUpdatedBy());
        copied.setDeleted(source.isDeleted());
        return copied;
    }

    private UserRoleScopeRelEntity cloneRoleScope(UserRoleScopeRelEntity source) {
        UserRoleScopeRelEntity copied = new UserRoleScopeRelEntity();
        copied.setId(source.getId());
        copied.setTenantId(source.getTenantId());
        copied.setUserId(source.getUserId());
        copied.setRoleId(source.getRoleId());
        copied.setScopeLevel(source.getScopeLevel());
        copied.setScenarioId(source.getScenarioId());
        copied.setProductId(source.getProductId());
        copied.setStatus(source.getStatus());
        copied.setCreatedAt(source.getCreatedAt());
        copied.setCreatedBy(source.getCreatedBy());
        copied.setUpdatedAt(source.getUpdatedAt());
        copied.setUpdatedBy(source.getUpdatedBy());
        copied.setDeleted(source.isDeleted());
        return copied;
    }
}
