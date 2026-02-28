package com.patches.plm.api;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ApiResponse;
import com.patches.plm.service.ConfigManagementService;
import com.patches.plm.web.RequirePermission;
import com.patches.plm.web.RequestContext;
import com.patches.plm.web.RequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigManagementController {

    private final ConfigManagementService configManagementService;
    private final RequestContextResolver requestContextResolver;

    public ConfigManagementController(ConfigManagementService configManagementService,
                                      RequestContextResolver requestContextResolver) {
        this.configManagementService = configManagementService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping("/scenarios")
    @RequirePermission("CONFIG_SCENARIO_MANAGE")
    public ApiResponse<ConfigScenarioResponse> upsertScenario(@Valid @RequestBody ConfigScenarioUpsertRequest request,
                                                              HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(configManagementService.upsertScenario(context.tenantId(), request, context));
    }

    @GetMapping("/scenarios")
    @RequirePermission("CONFIG_SCENARIO_VIEW")
    public ApiResponse<List<ConfigScenarioResponse>> listScenarios(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(configManagementService.listScenarios(context.tenantId()));
    }

    @PostMapping("/products")
    @RequirePermission("CONFIG_PRODUCT_MANAGE")
    public ApiResponse<ConfigProductResponse> upsertProduct(@Valid @RequestBody ConfigProductUpsertRequest request,
                                                            HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(configManagementService.upsertProduct(context.tenantId(), request, context));
    }

    @GetMapping("/products")
    @RequirePermission("CONFIG_PRODUCT_VIEW")
    public ApiResponse<List<ConfigProductResponse>> listProducts(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(configManagementService.listProducts(context.tenantId()));
    }

    @PostMapping("/scenario-products")
    @RequirePermission("CONFIG_PRODUCT_MANAGE")
    public ApiResponse<List<Long>> bindScenarioProducts(@Valid @RequestBody ConfigScenarioProductBindRequest request,
                                                        HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(configManagementService.bindScenarioProducts(context.tenantId(), request, context));
    }

    @PostMapping("/roles")
    @RequirePermission("CONFIG_ROLE_MANAGE")
    public ApiResponse<ConfigRoleResponse> upsertRole(@Valid @RequestBody ConfigRoleUpsertRequest request,
                                                      HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(configManagementService.upsertRole(context.tenantId(), request, context));
    }

    @GetMapping("/roles")
    @RequirePermission("CONFIG_ROLE_VIEW")
    public ApiResponse<List<ConfigRoleResponse>> listRoles(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(configManagementService.listRoles(context.tenantId()));
    }

    @PostMapping("/permissions")
    @RequirePermission("CONFIG_PERMISSION_MANAGE")
    public ApiResponse<ConfigPermissionResponse> upsertPermission(@Valid @RequestBody ConfigPermissionUpsertRequest request,
                                                                  HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(configManagementService.upsertPermission(context.tenantId(), request, context));
    }

    @GetMapping("/permissions")
    @RequirePermission("CONFIG_PERMISSION_VIEW")
    public ApiResponse<List<ConfigPermissionResponse>> listPermissions(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(configManagementService.listPermissions(context.tenantId()));
    }

    @PostMapping("/roles/{roleId}/permissions")
    @RequirePermission("CONFIG_PERMISSION_MANAGE")
    public ApiResponse<ConfigRolePermissionResponse> assignRolePermissions(@PathVariable Long roleId,
                                                                           @RequestBody List<Long> permissionIds,
                                                                           HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        ConfigRolePermissionAssignRequest request = new ConfigRolePermissionAssignRequest(roleId, permissionIds);
        return ApiResponse.success(configManagementService.assignRolePermissions(context.tenantId(), request, context));
    }

    @GetMapping("/roles/{roleId}/permissions")
    @RequirePermission("CONFIG_PERMISSION_VIEW")
    public ApiResponse<ConfigRolePermissionResponse> listRolePermissions(@PathVariable Long roleId,
                                                                         HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(configManagementService.listRolePermissions(context.tenantId(), roleId));
    }

    @PostMapping("/user-role-scopes")
    @RequirePermission("CONFIG_USER_ASSIGN")
    public ApiResponse<ConfigUserRoleScopeResponse> assignUserRoleScope(
            @Valid @RequestBody ConfigUserRoleScopeAssignRequest request,
            HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(configManagementService.assignUserRoleScope(context.tenantId(), request, context));
    }

    @GetMapping("/users/{userId}/role-scopes")
    @RequirePermission("CONFIG_USER_ASSIGN")
    public ApiResponse<List<ConfigUserRoleScopeResponse>> listUserRoleScopes(@PathVariable Long userId,
                                                                              HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(configManagementService.listUserRoleScopes(context.tenantId(), userId));
    }
}
