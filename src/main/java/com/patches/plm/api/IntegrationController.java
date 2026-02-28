package com.patches.plm.api;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ApiResponse;
import com.patches.plm.service.IntegrationService;
import com.patches.plm.web.RequestContext;
import com.patches.plm.web.RequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;
    private final RequestContextResolver requestContextResolver;

    public IntegrationController(IntegrationService integrationService, RequestContextResolver requestContextResolver) {
        this.integrationService = integrationService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping("/connectors")
    public ApiResponse<IntegrationConnectorResponse> createConnector(
            @Valid @RequestBody IntegrationConnectorCreateRequest request,
            HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(integrationService.createConnector(context.tenantId(), request, context));
    }

    @GetMapping("/connectors")
    public ApiResponse<List<IntegrationConnectorResponse>> listConnectors(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(integrationService.listConnectors(context.tenantId()));
    }

    @PostMapping("/ci/webhook")
    public ApiResponse<CiWebhookIngestResponse> ingestCiWebhook(
            @Valid @RequestBody CiWebhookIngestRequest request,
            HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(integrationService.ingestCiWebhook(context.tenantId(), request, context));
    }
}
