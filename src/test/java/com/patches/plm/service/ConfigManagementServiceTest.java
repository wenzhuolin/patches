package com.patches.plm.service;

import com.patches.plm.api.dto.ConfigRoleUpsertRequest;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.repository.*;
import com.patches.plm.web.RequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

class ConfigManagementServiceTest {

    private ConfigManagementService configManagementService;
    private SysRoleRepository sysRoleRepository;

    @BeforeEach
    void setUp() {
        sysRoleRepository = Mockito.mock(SysRoleRepository.class);
        configManagementService = new ConfigManagementService(
                Mockito.mock(DeliveryScenarioRepository.class),
                Mockito.mock(ProductRepository.class),
                Mockito.mock(ScenarioProductRelRepository.class),
                sysRoleRepository,
                Mockito.mock(PermissionDefRepository.class),
                Mockito.mock(RolePermissionRelRepository.class),
                Mockito.mock(UserRoleScopeRelRepository.class),
                Mockito.mock(SysUserRepository.class),
                Mockito.mock(ConfigAuditLogService.class)
        );
    }

    @Test
    void shouldRejectUnsupportedRoleLevel() {
        RequestContext admin = RequestContext.of(1L, 1L, Set.of("SUPER_ADMIN"), "req", "trace", "127.0.0.1", "ut");
        Mockito.when(sysRoleRepository.findByTenantIdAndRoleCode(1L, "ROLE_X")).thenReturn(Optional.empty());
        Assertions.assertThrows(BusinessException.class, () ->
                configManagementService.upsertRole(
                        1L,
                        new ConfigRoleUpsertRequest("ROLE_X", "Role X", "TEAM", null, true),
                        admin
                ));
    }
}
