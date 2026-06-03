package dev.jasmine.carrybabyanimals.modstatus;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusKit;
import dev.jasmine.carrybabyanimals.internal.modstatus.VersionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CarryBabyAnimalsModStatusTest {
    @Test
    void configUsesCarryBabyAnimalsIdentityAndPayloadChannel() {
        assertEquals(CarryBabyAnimals.MOD_ID, CarryBabyAnimalsModStatus.CONFIG.modId());
        assertEquals("Carry Baby Animals", CarryBabyAnimalsModStatus.CONFIG.displayName());
        assertEquals("https://modrinth.com/mod/carrybabyanimals", CarryBabyAnimalsModStatus.CONFIG.updateUrl());
        assertEquals("carrybabyanimals", CarryBabyAnimalsModStatus.CONFIG.payloadNamespace());
        assertEquals("server_version", CarryBabyAnimalsModStatus.CONFIG.payloadPath());
        assertEquals("carrybabyanimals:server_version", CarryBabyAnimalsModStatus.CONFIG.payloadChannel());
    }

    @Test
    void mismatchDisplayIsPassiveAndDoesNotClaimGameplayIncompatibility() {
        var display = ModStatusKit.display(
                CarryBabyAnimalsModStatus.CONFIG,
                ModStatusKit.connected(CarryBabyAnimalsModStatus.CONFIG, "999.0.0")
        );

        assertEquals(VersionStatus.DIFFERENT, ModStatusKit.connected(CarryBabyAnimalsModStatus.CONFIG, "999.0.0").status());
        assertEquals("Different versions", display.statusLabel());
        assertEquals("Different versions may miss or hide optional visuals. Gameplay remains compatible.", display.helpText());
    }
}
