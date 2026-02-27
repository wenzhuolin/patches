package com.patches.plm.api;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ApiResponse;
import com.patches.plm.service.KpiService;
import com.patches.plm.service.PatchAttachmentService;
import com.patches.plm.service.PatchService;
import com.patches.plm.domain.enums.PatchState;
import com.patches.plm.web.RequestContext;
import com.patches.plm.web.RequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patches")
public class PatchController {

    private final PatchService patchService;
    private final KpiService kpiService;
    private final PatchAttachmentService patchAttachmentService;
    private final RequestContextResolver requestContextResolver;

    public PatchController(PatchService patchService, KpiService kpiService, PatchAttachmentService patchAttachmentService,
                           RequestContextResolver requestContextResolver) {
        this.patchService = patchService;
        this.kpiService = kpiService;
        this.patchAttachmentService = patchAttachmentService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping
    public ApiResponse<PatchResponse> createPatch(@Valid @RequestBody PatchCreateRequest request, HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(patchService.createPatch(request, context));
    }

    @GetMapping("/{patchId}")
    public ApiResponse<PatchResponse> getPatch(@PathVariable Long patchId, HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(patchService.getPatch(patchId, context));
    }

    @GetMapping
    public ApiResponse<List<PatchResponse>> listPatches(@RequestParam(required = false) PatchState state,
                                                        HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(patchService.listPatches(state, context));
    }

    @PostMapping("/{patchId}/actions")
    public ApiResponse<PatchActionResponse> executeAction(@PathVariable Long patchId,
                                                          @Valid @RequestBody PatchActionRequest request,
                                                          HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(patchService.executeAction(patchId, request, context));
    }

    @PostMapping("/{patchId}/metrics")
    public ApiResponse<Integer> upsertMetrics(@PathVariable Long patchId,
                                              @Valid @RequestBody MetricUpsertRequest request,
                                              HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(kpiService.upsertMetrics(context.tenantId(), patchId, request, context));
    }

    @PostMapping("/{patchId}/kpi/evaluate")
    public ApiResponse<KpiEvaluationResponse> evaluateKpi(@PathVariable Long patchId,
                                                          @Valid @RequestBody KpiEvaluateRequest request,
                                                          HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        var result = kpiService.evaluateForPatch(
                context.tenantId(),
                patchId,
                request.stage(),
                request.gateType(),
                request.triggerAction(),
                context.traceId()
        );
        return ApiResponse.success(new KpiEvaluationResponse(result.pass(), result.summary(), result.details()));
    }

    @GetMapping("/{patchId}/transitions")
    public ApiResponse<List<PatchTransitionLogResponse>> listTransitions(@PathVariable Long patchId,
                                                                         HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(patchService.listTransitions(patchId, context));
    }

    @GetMapping("/{patchId}/operation-logs")
    public ApiResponse<List<PatchOperationLogResponse>> listOperationLogs(@PathVariable Long patchId,
                                                                          HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(patchService.listOperationLogs(patchId, context));
    }

    @PostMapping("/{patchId}/attachments")
    public ApiResponse<PatchAttachmentResponse> createAttachment(@PathVariable Long patchId,
                                                                 @Valid @RequestBody PatchAttachmentCreateRequest request,
                                                                 HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(patchAttachmentService.createAttachment(context.tenantId(), patchId, request, context));
    }

    @GetMapping("/{patchId}/attachments")
    public ApiResponse<List<PatchAttachmentResponse>> listAttachments(@PathVariable Long patchId,
                                                                      HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(patchAttachmentService.listAttachments(context.tenantId(), patchId, context));
    }
}
