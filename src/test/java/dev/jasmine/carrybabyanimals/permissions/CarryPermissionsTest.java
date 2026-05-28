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
        AtomicBoolean fallbackCalled = new AtomicBoolean(false);

        boolean allowed = CarryPermissions.nurseryBypassPermission(false, () -> {
            called.set(true);
            return false;
        }, () -> {
            fallbackCalled.set(true);
            return true;
        });

        assertTrue(allowed);
        assertFalse(called.get());
        assertTrue(fallbackCalled.get());
    }

    @Test
    void presentPermissionsApiUsesProviderForNurseryBypass() {
        AtomicBoolean called = new AtomicBoolean(false);
        AtomicBoolean fallbackCalled = new AtomicBoolean(false);

        boolean allowed = CarryPermissions.nurseryBypassPermission(true, () -> {
            called.set(true);
            return false;
        }, () -> {
            fallbackCalled.set(true);
            return true;
        });

        assertFalse(allowed);
        assertTrue(called.get());
        assertFalse(fallbackCalled.get());
    }

    @Test
    void presentPermissionsApiGrantsNurseryBypassWhenProviderReturnsTrue() {
        AtomicBoolean called = new AtomicBoolean(false);
        AtomicBoolean fallbackCalled = new AtomicBoolean(false);

        boolean allowed = CarryPermissions.nurseryBypassPermission(true, () -> {
            called.set(true);
            return true;
        }, () -> {
            fallbackCalled.set(true);
            return false;
        });

        assertTrue(allowed);
        assertTrue(called.get());
        assertFalse(fallbackCalled.get());
    }

    @Test
    void presentPermissionsApiDefaultsNurseryBypassToFalse() {
        AtomicBoolean fallbackCalled = new AtomicBoolean(false);

        assertFalse(CarryPermissions.nurseryBypassFallback(true, () -> {
            fallbackCalled.set(true);
            return true;
        }));
        assertFalse(fallbackCalled.get());
    }

    @Test
    void absentPermissionsApiGrantsNurseryBypassToGameMaster() {
        assertTrue(CarryPermissions.nurseryBypassFallback(false, () -> true));
    }

    @Test
    void absentPermissionsApiDeniesNurseryBypassToNonGameMaster() {
        assertFalse(CarryPermissions.nurseryBypassFallback(false, () -> false));
    }
}
