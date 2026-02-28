package com.patches.plm.api;

import com.patches.plm.api.dto.QaDecisionRequest;
import com.patches.plm.api.dto.QaTaskResponse;
import com.patches.plm.common.ApiResponse;
import com.patches.plm.service.QaService;
import com.patches.plm.web.RequestContext;
import com.patches.plm.web.RequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/qa/tasks")
public class QaController {

    private final QaService qaService;
    private final RequestContextResolver requestContextResolver;

    public QaController(QaService qaService, RequestContextResolver requestContextResolver) {
        this.qaService = qaService;
        this.requestContextResolver = requestContextResolver;
    }

    @GetMapping("/my-pending")
    public ApiResponse<List<QaTaskResponse>> myPending(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(qaService.listMyPending(context.tenantId(), context));
    }

    @PostMapping("/{qaTaskId}/decision")
    public ApiResponse<QaTaskResponse> decision(@PathVariable Long qaTaskId,
                                                @Valid @RequestBody QaDecisionRequest request,
                                                HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(qaService.decideTask(context.tenantId(), qaTaskId, request, context));
    }
}
