package com.patches.plm.api;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ApiResponse;
import com.patches.plm.service.IamService;
import com.patches.plm.web.RequestContext;
import com.patches.plm.web.RequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/iam")
public class IamController {

    private final IamService iamService;
    private final RequestContextResolver requestContextResolver;

    public IamController(IamService iamService, RequestContextResolver requestContextResolver) {
        this.iamService = iamService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping("/role-action-permissions")
    public ApiResponse<RoleActionPermissionResponse> upsertRoleActionPermission(
            @Valid @RequestBody RoleActionPermissionUpsertRequest request,
            HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(iamService.upsertRoleActionPermission(context.tenantId(), request, context));
    }

    @GetMapping("/role-action-permissions")
    public ApiResponse<List<RoleActionPermissionResponse>> listRoleActionPermissions(
            @RequestParam String action,
            HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(iamService.listRoleActionPermissions(context.tenantId(), action));
    }

    @PostMapping("/user-data-scopes")
    public ApiResponse<UserDataScopeResponse> grantUserDataScope(
            @Valid @RequestBody UserDataScopeGrantRequest request,
            HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(iamService.grantDataScope(context.tenantId(), request, context));
    }

    @GetMapping("/users/{userId}/data-scopes")
    public ApiResponse<List<UserDataScopeResponse>> listUserDataScopes(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(iamService.listUserDataScopes(context.tenantId(), userId));
    }
}
