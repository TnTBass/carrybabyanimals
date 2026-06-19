package dev.jasmine.carrybabyanimals.fabric.permissions;

import dev.jasmine.carrybabyanimals.permissions.CarryPermissions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FabricCarryPermissionsTest {
    @Test
    void modernFabricPermissionApiIsPreferredWhenPresent() {
        RecordingProvider modern = new RecordingProvider(true, false);
        RecordingProvider legacy = new RecordingProvider(true, true);
        RecordingWarnings warnings = new RecordingWarnings();
        FabricCarryPermissions permissions = new FabricCarryPermissions(modern, legacy, warnings);

        assertFalse(permissions.check(null, CarryPermissions.CARRY, true));

        assertEquals(List.of(CarryPermissions.CARRY + ":true"), modern.calls);
        assertTrue(legacy.calls.isEmpty());
        assertTrue(warnings.messages.isEmpty());
    }

    @Test
    void legacyLuckoCompatibilityPathIsUsedWhenModernApiIsAbsent() {
        RecordingProvider modern = new RecordingProvider(false, true);
        RecordingProvider legacy = new RecordingProvider(true, false);
        RecordingWarnings warnings = new RecordingWarnings();
        FabricCarryPermissions permissions = new FabricCarryPermissions(modern, legacy, warnings);

        assertFalse(permissions.check(null, CarryPermissions.CARRY_TAMED, true));

        assertTrue(modern.calls.isEmpty());
        assertEquals(List.of(CarryPermissions.CARRY_TAMED + ":true"), legacy.calls);
        assertEquals(
                List.of("Old deprecated permissions API detected. Please update to Fabric API version 0.152.2+26.2 or newer."),
                warnings.messages
        );
    }

    @Test
    void legacyWarningIsOnlyLoggedOnceWhenLegacyPathIsUsedRepeatedly() {
        RecordingProvider legacy = new RecordingProvider(true, true);
        RecordingWarnings warnings = new RecordingWarnings();
        FabricCarryPermissions permissions = new FabricCarryPermissions(
                new RecordingProvider(false, false),
                legacy,
                warnings
        );

        assertTrue(permissions.check(null, CarryPermissions.CARRY, false));
        assertTrue(permissions.check(null, CarryPermissions.CARRY_TAMED, false));

        assertEquals(2, legacy.calls.size());
        assertEquals(1, warnings.messages.size());
    }

    @Test
    void fallbackIsUsedWhenNoFabricPermissionProviderIsPresent() {
        RecordingProvider modern = new RecordingProvider(false, false);
        RecordingProvider legacy = new RecordingProvider(false, false);
        RecordingWarnings warnings = new RecordingWarnings();
        FabricCarryPermissions permissions = new FabricCarryPermissions(modern, legacy, warnings);

        assertTrue(permissions.check(null, CarryPermissions.CARRY, true));
        assertFalse(permissions.check(null, CarryPermissions.CARRY_OTHERS_TAMED, false));

        assertTrue(modern.calls.isEmpty());
        assertTrue(legacy.calls.isEmpty());
        assertTrue(warnings.messages.isEmpty());
    }

    @Test
    void noProviderNurseryBypassUsesGameMasterFallbackWithoutProviderCalls() {
        RecordingProvider modern = new RecordingProvider(false, false);
        RecordingProvider legacy = new RecordingProvider(false, false);
        FabricCarryPermissions permissions = new FabricCarryPermissions(modern, legacy, new RecordingWarnings());

        assertTrue(permissions.checkNurseryBypass(null, () -> true));

        assertTrue(modern.calls.isEmpty());
        assertTrue(legacy.calls.isEmpty());
    }

    @Test
    void nurseryBypassUsesProviderGrantWhenFabricPermissionProviderIsPresent() {
        RecordingProvider modern = new RecordingProvider(true, true);
        AtomicInteger fallbackCalls = new AtomicInteger();
        FabricCarryPermissions permissions = new FabricCarryPermissions(
                modern,
                new RecordingProvider(false, false),
                new RecordingWarnings()
        );

        assertTrue(permissions.checkNurseryBypass(null, () -> {
            fallbackCalls.incrementAndGet();
            return false;
        }));

        assertEquals(List.of(CarryPermissions.NURSERY_BYPASS + ":false"), modern.calls);
        assertEquals(0, fallbackCalls.get());
    }

    private static final class RecordingProvider implements FabricCarryPermissions.Provider {
        private final boolean present;
        private final boolean result;
        private final List<String> calls = new ArrayList<>();

        private RecordingProvider(boolean present, boolean result) {
            this.present = present;
            this.result = result;
        }

        @Override
        public boolean present() {
            return present;
        }

        @Override
        public boolean check(net.minecraft.server.level.ServerPlayer player, String permission, boolean fallback) {
            calls.add(permission + ":" + fallback);
            return result;
        }
    }

    private static final class RecordingWarnings implements FabricCarryPermissions.WarningSink {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void warn(String message) {
            messages.add(message);
        }
    }
}
