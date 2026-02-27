package com.patches.plm.api;

import com.patches.plm.api.dto.TestTaskResponse;
import com.patches.plm.api.dto.TestTaskResultRequest;
import com.patches.plm.common.ApiResponse;
import com.patches.plm.service.TestTaskService;
import com.patches.plm.web.RequestContext;
import com.patches.plm.web.RequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TestTaskController {

    private final TestTaskService testTaskService;
    private final RequestContextResolver requestContextResolver;

    public TestTaskController(TestTaskService testTaskService, RequestContextResolver requestContextResolver) {
        this.testTaskService = testTaskService;
        this.requestContextResolver = requestContextResolver;
    }

    @GetMapping("/api/v1/patches/{patchId}/test-tasks")
    public ApiResponse<List<TestTaskResponse>> listByPatch(@PathVariable Long patchId,
                                                           HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(testTaskService.listByPatch(context.tenantId(), patchId));
    }

    @PostMapping("/api/v1/test-tasks/{taskId}/results")
    public ApiResponse<TestTaskResponse> fillResult(@PathVariable Long taskId,
                                                    @Valid @RequestBody TestTaskResultRequest request,
                                                    HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(testTaskService.fillResult(context.tenantId(), taskId, request, context));
    }
}
