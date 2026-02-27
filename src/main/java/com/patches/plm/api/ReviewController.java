package com.patches.plm.api;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ApiResponse;
import com.patches.plm.service.ReviewService;
import com.patches.plm.web.RequestContext;
import com.patches.plm.web.RequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/review-sessions")
public class ReviewController {

    private final ReviewService reviewService;
    private final RequestContextResolver requestContextResolver;

    public ReviewController(ReviewService reviewService, RequestContextResolver requestContextResolver) {
        this.reviewService = reviewService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping
    public ApiResponse<ReviewSessionResponse> create(@Valid @RequestBody ReviewSessionCreateRequest request,
                                                     HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(reviewService.createSession(context.tenantId(), request, context));
    }

    @PostMapping("/{sessionId}/votes")
    public ApiResponse<ReviewVoteResponse> vote(@PathVariable Long sessionId,
                                                @Valid @RequestBody ReviewVoteRequest request,
                                                HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(reviewService.vote(context.tenantId(), sessionId, request, context));
    }
}
