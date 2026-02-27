package com.patches.plm.service;

import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.enums.PatchAction;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Service
public class AccessControlService {

    private final Map<PatchAction, Set<String>> actionRoleMapping = new EnumMap<>(PatchAction.class);

    public AccessControlService() {
        actionRoleMapping.put(PatchAction.SUBMIT_REVIEW, Set.of("PM", "DEV", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.APPROVE_REVIEW, Set.of("REVIEWER", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.REJECT_REVIEW, Set.of("REVIEWER", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.TRANSFER_TO_TEST, Set.of("PM", "TEST", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.PASS_TEST, Set.of("TEST", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.FAIL_TEST, Set.of("TEST", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.PREPARE_RELEASE, Set.of("PM", "LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.RELEASE, Set.of("LINE_ADMIN", "SUPER_ADMIN"));
        actionRoleMapping.put(PatchAction.ARCHIVE, Set.of("PM", "LINE_ADMIN", "SUPER_ADMIN"));
    }

    public void assertCanExecuteAction(Set<String> roles, PatchAction action) {
        Set<String> allowed = actionRoleMapping.get(action);
        if (allowed == null || roles.stream().noneMatch(allowed::contains)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色无权执行动作: " + action);
        }
    }

    public void assertCanQaApprove(Set<String> roles) {
        if (!(roles.contains("QA") || roles.contains("SUPER_ADMIN") || roles.contains("LINE_ADMIN"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色无权执行QA审批");
        }
    }
}
