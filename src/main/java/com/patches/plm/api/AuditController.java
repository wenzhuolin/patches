package com.patches.plm.api;

import com.patches.plm.api.dto.PatchOperationLogResponse;
import com.patches.plm.common.ApiResponse;
import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.service.AuditQueryService;
import com.patches.plm.web.RequestContext;
import com.patches.plm.web.RequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditQueryService auditQueryService;
    private final RequestContextResolver requestContextResolver;

    public AuditController(AuditQueryService auditQueryService, RequestContextResolver requestContextResolver) {
        this.auditQueryService = auditQueryService;
        this.requestContextResolver = requestContextResolver;
    }

    @GetMapping("/logs")
    public ApiResponse<List<PatchOperationLogResponse>> queryByBizType(@RequestParam String bizType,
                                                                       HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        assertAuditAccess(context);
        return ApiResponse.success(auditQueryService.queryByBizType(context.tenantId(), bizType.toUpperCase(Locale.ROOT)));
    }

    private void assertAuditAccess(RequestContext context) {
        if (!(context.roles().contains("SUPER_ADMIN")
                || context.roles().contains("LINE_ADMIN")
                || context.roles().contains("QA"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色无权访问审计日志");
        }
    }
}
