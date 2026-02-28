package com.patches.plm.api;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ApiResponse;
import com.patches.plm.service.UserManagementService;
import com.patches.plm.web.RequestContext;
import com.patches.plm.web.RequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final RequestContextResolver requestContextResolver;

    public UserManagementController(UserManagementService userManagementService,
                                    RequestContextResolver requestContextResolver) {
        this.userManagementService = userManagementService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping("/roles")
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request,
                                                HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(userManagementService.createOrUpdateRole(context.tenantId(), request, context));
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> listRoles(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(userManagementService.listRoles(context.tenantId()));
    }

    @PostMapping("/users")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request,
                                                HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(userManagementService.createOrUpdateUser(context.tenantId(), request, context));
    }

    @GetMapping("/users")
    public ApiResponse<List<UserResponse>> listUsers(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(userManagementService.listUsers(context.tenantId()));
    }

    @PostMapping("/users/roles")
    public ApiResponse<UserRoleResponse> assignUserRole(@Valid @RequestBody UserRoleAssignRequest request,
                                                        HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(userManagementService.assignRole(context.tenantId(), request, context));
    }

    @GetMapping("/users/{userId}/roles")
    public ApiResponse<List<UserRoleResponse>> listUserRoles(@PathVariable Long userId,
                                                             HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(userManagementService.listUserRoles(context.tenantId(), userId));
    }
}
