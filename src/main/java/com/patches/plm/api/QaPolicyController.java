package com.patches.plm.api;

import com.patches.plm.api.dto.QaPolicyCreateRequest;
import com.patches.plm.api.dto.QaPolicyResponse;
import com.patches.plm.common.ApiResponse;
import com.patches.plm.service.QaService;
import com.patches.plm.web.RequestContext;
import com.patches.plm.web.RequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/qa/policies")
public class QaPolicyController {

    private final QaService qaService;
    private final RequestContextResolver requestContextResolver;

    public QaPolicyController(QaService qaService, RequestContextResolver requestContextResolver) {
        this.qaService = qaService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping
    public ApiResponse<QaPolicyResponse> createPolicy(@Valid @RequestBody QaPolicyCreateRequest request,
                                                      HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(qaService.createPolicy(context.tenantId(), request, context));
    }

    @GetMapping
    public ApiResponse<List<QaPolicyResponse>> listPolicies(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(qaService.listPolicies(context.tenantId()));
    }
}
