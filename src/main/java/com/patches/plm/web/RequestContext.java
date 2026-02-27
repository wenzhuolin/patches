package com.patches.plm.web;

import java.util.Collections;
import java.util.Set;

public record RequestContext(Long tenantId, Long userId, Set<String> roles, String requestId,
                             String traceId, String ip, String userAgent) {

    public static RequestContext of(Long tenantId, Long userId, Set<String> roles,
                                    String requestId, String traceId, String ip, String userAgent) {
        return new RequestContext(tenantId, userId, roles == null ? Collections.emptySet() : roles,
                requestId, traceId, ip, userAgent);
    }
}
