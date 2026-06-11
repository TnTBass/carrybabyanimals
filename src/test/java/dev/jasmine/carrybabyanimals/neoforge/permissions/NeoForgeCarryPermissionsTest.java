package dev.jasmine.carrybabyanimals.neoforge.permissions;

import dev.jasmine.carrybabyanimals.permissions.CarryPermissions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NeoForgeCarryPermissionsTest {
    @Test
    void permissionChecksUseVanillaFallbacksUntilANeoForgePermissionIntegrationExists() {
        NeoForgeCarryPermissions permissions = new NeoForgeCarryPermissions();

        assertTrue(permissions.check(null, CarryPermissions.CARRY, true));
        assertFalse(permissions.check(null, CarryPermissions.CARRY_OTHERS_TAMED, false));
    }

    @Test
    void nurseryBypassDefaultsToDisabledUntilANeoForgePermissionIntegrationExists() {
        NeoForgeCarryPermissions permissions = new NeoForgeCarryPermissions();

        assertFalse(permissions.checkNurseryBypass(null, () -> true));
        assertFalse(permissions.checkNurseryBypass(null, () -> false));
    }
}
