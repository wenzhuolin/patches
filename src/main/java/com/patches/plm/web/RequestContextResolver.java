package com.patches.plm.web;

import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.repository.UserRoleRelationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RequestContextResolver {

    private final UserRoleRelationRepository userRoleRelationRepository;

    public RequestContextResolver(UserRoleRelationRepository userRoleRelationRepository) {
        this.userRoleRelationRepository = userRoleRelationRepository;
    }

    public RequestContext resolve(HttpServletRequest request) {
        Long tenantId = parseLongOrThrow(request.getHeader("X-Tenant-Id"), "X-Tenant-Id");
        Long userId = parseLongOrThrow(request.getHeader("X-User-Id"), "X-User-Id");
        String rolesHeader = request.getHeader("X-Roles");
        Set<String> roles = rolesHeader == null || rolesHeader.isBlank()
                ? userRoleRelationRepository.findEnabledRoleCodes(tenantId, userId)
                    .stream().map(v -> v.toUpperCase(Locale.ROOT)).collect(Collectors.toSet())
                : Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> v.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        String requestId = defaultIfBlank(request.getHeader("Idempotency-Key"), UUID.randomUUID().toString());
        String traceId = defaultIfBlank(request.getHeader("X-Trace-Id"), UUID.randomUUID().toString());
        return RequestContext.of(tenantId, userId, roles, requestId, traceId, request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    private Long parseLongOrThrow(String value, String headerName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少请求头: " + headerName);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求头格式非法: " + headerName);
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
