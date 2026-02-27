package com.patches.plm.api;

import com.patches.plm.api.dto.KpiRuleCreateRequest;
import com.patches.plm.api.dto.KpiRuleResponse;
import com.patches.plm.common.ApiResponse;
import com.patches.plm.service.KpiService;
import com.patches.plm.web.RequestContext;
import com.patches.plm.web.RequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kpi/rules")
public class KpiController {

    private final KpiService kpiService;
    private final RequestContextResolver requestContextResolver;

    public KpiController(KpiService kpiService, RequestContextResolver requestContextResolver) {
        this.kpiService = kpiService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping
    public ApiResponse<KpiRuleResponse> createRule(@Valid @RequestBody KpiRuleCreateRequest request,
                                                   HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(kpiService.createRule(request, context));
    }

    @GetMapping
    public ApiResponse<List<KpiRuleResponse>> listRules(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(kpiService.listRules(context.tenantId()));
    }
}
