package com.patches.plm.service;

import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.PermissionDefEntity;
import com.patches.plm.domain.repository.PermissionDefRepository;
import com.patches.plm.domain.repository.RolePermissionRelRepository;
import com.patches.plm.web.RequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

class PermissionAuthorizationServiceTest {

    private PermissionDefRepository permissionDefRepository;
    private RolePermissionRelRepository rolePermissionRelRepository;
    private PermissionAuthorizationService permissionAuthorizationService;

    @BeforeEach
    void setUp() {
        permissionDefRepository = Mockito.mock(PermissionDefRepository.class);
        rolePermissionRelRepository = Mockito.mock(RolePermissionRelRepository.class);
        permissionAuthorizationService = new PermissionAuthorizationService(permissionDefRepository, rolePermissionRelRepository);
    }

    @Test
    void shouldAllowWhenPermissionNotConfiguredForTenant() {
        RequestContext context = RequestContext.of(1L, 1001L, Set.of("PM"), "req", "trace", "127.0.0.1", "ut");
        Mockito.when(permissionDefRepository.findByTenantIdAndPermCode(1L, "CONFIG_ROLE_VIEW"))
                .thenReturn(Optional.empty());
        Assertions.assertDoesNotThrow(() -> permissionAuthorizationService.assertHasPermission(context, "CONFIG_ROLE_VIEW"));
    }

    @Test
    void shouldDenyWhenPermissionConfiguredButRoleNotGranted() {
        RequestContext context = RequestContext.of(1L, 1001L, Set.of("PM"), "req", "trace", "127.0.0.1", "ut");
        PermissionDefEntity permission = new PermissionDefEntity();
        permission.setTenantId(1L);
        permission.setPermCode("CONFIG_ROLE_VIEW");
        permission.setEnabled(true);
        Mockito.when(permissionDefRepository.findByTenantIdAndPermCode(1L, "CONFIG_ROLE_VIEW"))
                .thenReturn(Optional.of(permission));
        Mockito.when(rolePermissionRelRepository.existsGrant(1L, java.util.List.of("PM"), "CONFIG_ROLE_VIEW"))
                .thenReturn(false);
        Assertions.assertThrows(BusinessException.class,
                () -> permissionAuthorizationService.assertHasPermission(context, "CONFIG_ROLE_VIEW"));
    }

    @Test
    void shouldAllowWhenPermissionConfiguredAndGranted() {
        RequestContext context = RequestContext.of(1L, 1001L, Set.of("PM"), "req", "trace", "127.0.0.1", "ut");
        PermissionDefEntity permission = new PermissionDefEntity();
        permission.setTenantId(1L);
        permission.setPermCode("CONFIG_ROLE_VIEW");
        permission.setEnabled(true);
        Mockito.when(permissionDefRepository.findByTenantIdAndPermCode(1L, "CONFIG_ROLE_VIEW"))
                .thenReturn(Optional.of(permission));
        Mockito.when(rolePermissionRelRepository.existsGrant(1L, java.util.List.of("PM"), "CONFIG_ROLE_VIEW"))
                .thenReturn(true);
        Assertions.assertDoesNotThrow(() -> permissionAuthorizationService.assertHasPermission(context, "CONFIG_ROLE_VIEW"));
    }
}
