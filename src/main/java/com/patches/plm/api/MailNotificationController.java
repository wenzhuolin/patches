package com.patches.plm.api;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ApiResponse;
import com.patches.plm.service.MailNotificationService;
import com.patches.plm.web.RequirePermission;
import com.patches.plm.web.RequestContext;
import com.patches.plm.web.RequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notify/mail")
public class MailNotificationController {

    private final MailNotificationService mailNotificationService;
    private final RequestContextResolver requestContextResolver;

    public MailNotificationController(MailNotificationService mailNotificationService,
                                      RequestContextResolver requestContextResolver) {
        this.mailNotificationService = mailNotificationService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping("/servers")
    @RequirePermission("NOTIFY_MAIL_MANAGE")
    public ApiResponse<MailServerConfigResponse> upsertServer(@Valid @RequestBody MailServerConfigUpsertRequest request,
                                                              HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(mailNotificationService.upsertServerConfig(context.tenantId(), request, context));
    }

    @GetMapping("/servers")
    @RequirePermission("NOTIFY_MAIL_VIEW")
    public ApiResponse<List<MailServerConfigResponse>> listServers(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(mailNotificationService.listServerConfigs(context.tenantId(), context));
    }

    @PostMapping("/templates")
    @RequirePermission("NOTIFY_MAIL_MANAGE")
    public ApiResponse<MailTemplateResponse> upsertTemplate(@Valid @RequestBody MailTemplateUpsertRequest request,
                                                            HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(mailNotificationService.upsertTemplate(context.tenantId(), request, context));
    }

    @GetMapping("/templates")
    @RequirePermission("NOTIFY_MAIL_VIEW")
    public ApiResponse<List<MailTemplateResponse>> listTemplates(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(mailNotificationService.listTemplates(context.tenantId(), context));
    }

    @PostMapping("/templates/render")
    @RequirePermission("NOTIFY_MAIL_MANAGE")
    public ApiResponse<MailTemplateRenderResponse> renderTemplate(@Valid @RequestBody MailTemplateRenderRequest request,
                                                                  HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(mailNotificationService.renderTemplate(context.tenantId(), request, context));
    }

    @PostMapping("/event-policies")
    @RequirePermission("NOTIFY_MAIL_MANAGE")
    public ApiResponse<MailEventPolicyResponse> upsertEventPolicy(@Valid @RequestBody MailEventPolicyUpsertRequest request,
                                                                  HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(mailNotificationService.upsertEventPolicy(context.tenantId(), request, context));
    }

    @GetMapping("/event-policies")
    @RequirePermission("NOTIFY_MAIL_VIEW")
    public ApiResponse<List<MailEventPolicyResponse>> listEventPolicies(HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(mailNotificationService.listEventPolicies(context.tenantId(), context));
    }

    @GetMapping("/logs")
    @RequirePermission("NOTIFY_MAIL_VIEW")
    public ApiResponse<List<MailSendLogResponse>> listLogs(@RequestParam(required = false) Integer limit,
                                                           HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(mailNotificationService.listSendLogs(context.tenantId(), limit, context));
    }

    @PostMapping("/logs/{logId}/resend")
    @RequirePermission("NOTIFY_MAIL_MANAGE")
    public ApiResponse<MailSendLogResponse> resend(@PathVariable Long logId, HttpServletRequest httpRequest) {
        RequestContext context = requestContextResolver.resolve(httpRequest);
        return ApiResponse.success(mailNotificationService.resend(context.tenantId(), logId, context));
    }
}
