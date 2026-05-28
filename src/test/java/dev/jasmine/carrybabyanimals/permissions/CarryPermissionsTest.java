package dev.jasmine.carrybabyanimals.permissions;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarryPermissionsTest {
    @Test
    void absentPermissionsApiUsesFallbackWithoutCallingPermissionCheck() {
        AtomicBoolean called = new AtomicBoolean(false);

        boolean allowed = CarryPermissions.resolvedPermission(false, () -> {
            called.set(true);
            return false;
        }, true);

        assertTrue(allowed);
        assertFalse(called.get());
    }

    @Test
    void presentPermissionsApiUsesPermissionCheck() {
        AtomicBoolean called = new AtomicBoolean(false);

        boolean allowed = CarryPermissions.resolvedPermission(true, () -> {
            called.set(true);
            return false;
        }, true);

        assertFalse(allowed);
        assertTrue(called.get());
    }

    @Test
    void absentPermissionsApiUsesGameMasterFallbackForNurseryBypass() {
        AtomicBoolean called = new AtomicBoolean(false);

        boolean allowed = CarryPermissions.nurseryBypassPermission(false, () -> {
            called.set(true);
            return false;
        }, true);

        assertTrue(allowed);
        assertFalse(called.get());
    }

    @Test
    void presentPermissionsApiUsesProviderForNurseryBypass() {
        AtomicBoolean called = new AtomicBoolean(false);

        boolean allowed = CarryPermissions.nurseryBypassPermission(true, () -> {
            called.set(true);
            return false;
        }, true);

        assertFalse(allowed);
        assertTrue(called.get());
    }
}
