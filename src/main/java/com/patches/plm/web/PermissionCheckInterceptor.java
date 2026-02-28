package com.patches.plm.web;

import com.patches.plm.service.PermissionAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionCheckInterceptor implements HandlerInterceptor {

    private final RequestContextResolver requestContextResolver;
    private final PermissionAuthorizationService permissionAuthorizationService;

    public PermissionCheckInterceptor(RequestContextResolver requestContextResolver,
                                      PermissionAuthorizationService permissionAuthorizationService) {
        this.requestContextResolver = requestContextResolver;
        this.permissionAuthorizationService = permissionAuthorizationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            requirePermission = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (requirePermission == null) {
            return true;
        }
        permissionAuthorizationService.assertHasPermission(
                requestContextResolver.resolve(request),
                requirePermission.value()
        );
        return true;
    }
}
