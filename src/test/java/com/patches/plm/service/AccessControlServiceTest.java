package com.patches.plm.service;

import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.PatchEntity;
import com.patches.plm.domain.entity.UserDataScopeEntity;
import com.patches.plm.domain.enums.PatchAction;
import com.patches.plm.domain.repository.RoleActionPermissionRepository;
import com.patches.plm.domain.repository.UserDataScopeRepository;
import com.patches.plm.web.RequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

class AccessControlServiceTest {

    private RoleActionPermissionRepository roleActionPermissionRepository;
    private UserDataScopeRepository userDataScopeRepository;
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        roleActionPermissionRepository = Mockito.mock(RoleActionPermissionRepository.class);
        userDataScopeRepository = Mockito.mock(UserDataScopeRepository.class);
        accessControlService = new AccessControlService(roleActionPermissionRepository, userDataScopeRepository);
    }

    @Test
    void shouldAllowByDefaultRoleMappingWhenDbPolicyMissing() {
        RequestContext context = RequestContext.of(1L, 1001L, Set.of("PM"), "req-1", "trace-1", "127.0.0.1", "test");
        PatchEntity patch = new PatchEntity();
        patch.setProductLineId(200L);

        Mockito.when(roleActionPermissionRepository.findEnabledRolesByAction(1L, PatchAction.SUBMIT_REVIEW.name()))
                .thenReturn(List.of());
        Mockito.when(userDataScopeRepository.findByTenantIdAndUserIdAndEnabledTrue(1L, 1001L))
                .thenReturn(List.of());

        Assertions.assertDoesNotThrow(() ->
                accessControlService.assertCanExecuteAction(context, PatchAction.SUBMIT_REVIEW, patch));
    }

    @Test
    void shouldDenyWhenDbPolicyConfiguredAndRoleNotMatched() {
        RequestContext context = RequestContext.of(1L, 1001L, Set.of("PM"), "req-1", "trace-1", "127.0.0.1", "test");
        PatchEntity patch = new PatchEntity();
        patch.setProductLineId(200L);

        Mockito.when(roleActionPermissionRepository.findEnabledRolesByAction(1L, PatchAction.SUBMIT_REVIEW.name()))
                .thenReturn(List.of("REVIEWER"));

        Assertions.assertThrows(BusinessException.class, () ->
                accessControlService.assertCanExecuteAction(context, PatchAction.SUBMIT_REVIEW, patch));
    }

    @Test
    void shouldDenyWhenDataScopeNotMatched() {
        RequestContext context = RequestContext.of(1L, 1001L, Set.of("PM"), "req-1", "trace-1", "127.0.0.1", "test");
        PatchEntity patch = new PatchEntity();
        patch.setProductLineId(200L);

        UserDataScopeEntity scope = new UserDataScopeEntity();
        scope.setTenantId(1L);
        scope.setUserId(1001L);
        scope.setScopeType("PRODUCT_LINE");
        scope.setScopeValue("999");
        scope.setEnabled(true);

        Mockito.when(roleActionPermissionRepository.findEnabledRolesByAction(1L, PatchAction.SUBMIT_REVIEW.name()))
                .thenReturn(List.of());
        Mockito.when(userDataScopeRepository.findByTenantIdAndUserIdAndEnabledTrue(1L, 1001L))
                .thenReturn(List.of(scope));

        Assertions.assertThrows(BusinessException.class, () ->
                accessControlService.assertCanExecuteAction(context, PatchAction.SUBMIT_REVIEW, patch));
    }
}
