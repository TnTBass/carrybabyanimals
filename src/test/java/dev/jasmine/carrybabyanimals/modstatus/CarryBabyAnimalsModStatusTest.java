package dev.jasmine.carrybabyanimals.modstatus;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import dev.jasmine.carrybabyanimals.BuildInfo;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusConfig;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusKit;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusServerStatus;
import dev.jasmine.carrybabyanimals.internal.modstatus.StatusTone;
import dev.jasmine.carrybabyanimals.internal.modstatus.VersionMismatchSeverity;
import dev.jasmine.carrybabyanimals.internal.modstatus.VersionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void configUsesGeneratedBuildMetadataWithoutChangingBaseVersion() {
        assertEquals(BuildInfo.BUILD_NUMBER, CarryBabyAnimalsModStatus.CONFIG.clientBuild());
        assertEquals(
                CarryBabyAnimalsModStatus.CONFIG.clientVersion(),
                CarryBabyAnimalsModStatus.CONFIG.clientVersionInfo().version()
        );
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

    @Test
    void warnVersionMismatchUsesOrangeSeverityTone() {
        var snapshot = ModStatusKit.connected(
                CarryBabyAnimalsModStatus.CONFIG,
                ModStatusServerStatus.of("999.0.0", null, VersionMismatchSeverity.WARN)
        );
        var display = ModStatusKit.display(CarryBabyAnimalsModStatus.CONFIG, snapshot);

        assertEquals(VersionStatus.DIFFERENT, snapshot.status());
        assertEquals(VersionMismatchSeverity.WARN, snapshot.versionMismatchSeverity());
        assertEquals(StatusTone.ORANGE, display.tone());
        assertEquals("Different versions", display.statusLabel());
        assertEquals("Different versions may miss or hide optional visuals. Gameplay remains compatible.", display.helpText());
    }

    @Test
    void breakingVersionMismatchCanUseRedSeverityTone() {
        var snapshot = ModStatusKit.connected(
                CarryBabyAnimalsModStatus.CONFIG,
                ModStatusServerStatus.of("999.0.0", null, VersionMismatchSeverity.BREAKING)
        );
        var display = ModStatusKit.display(CarryBabyAnimalsModStatus.CONFIG, snapshot);

        assertEquals(VersionStatus.DIFFERENT, snapshot.status());
        assertEquals(VersionMismatchSeverity.BREAKING, snapshot.versionMismatchSeverity());
        assertEquals(StatusTone.RED, display.tone());
        assertEquals("Different versions", display.statusLabel());
        assertEquals("Different versions may miss or hide optional visuals. Gameplay remains compatible.", display.helpText());
    }

    @Test
    void invalidServerVersionIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ModStatusKit.connected(CarryBabyAnimalsModStatus.CONFIG, " ")
        );
        assertThrows(
                NullPointerException.class,
                () -> ModStatusKit.connected(CarryBabyAnimalsModStatus.CONFIG, (String) null)
        );
    }

    @Test
    void buildMetadataMismatchShowsDiagnosticCompatibleStatus() {
        var snapshot = ModStatusKit.connected(
                CarryBabyAnimalsModStatus.CONFIG,
                CarryBabyAnimalsModStatus.CONFIG.clientVersion() + "+server456"
        );
        var display = ModStatusKit.display(CarryBabyAnimalsModStatus.CONFIG, snapshot);

        assertEquals(VersionStatus.BUILD_DIFFERENT, snapshot.status());
        assertEquals(VersionMismatchSeverity.WARN, snapshot.versionMismatchSeverity());
        assertEquals(CarryBabyAnimalsModStatus.CONFIG.clientVersion(), snapshot.serverVersion());
        assertEquals("server456", snapshot.serverBuild());
        assertEquals(StatusTone.TEAL, display.tone());
        assertEquals("Same version, different builds", display.statusLabel());
        assertEquals("Client and server are compatible, but their build details differ.", display.helpText());
        assertEquals(CarryBabyAnimalsModStatus.CONFIG.clientBuild(), display.clientBuild());
        assertEquals("server456", display.serverBuild());
    }

    @Test
    void omittedBuildMetadataStillMatchesWhenBaseVersionsMatch() {
        String clientVersion = CarryBabyAnimalsModStatus.CONFIG.clientVersion();
        ModStatusConfig noBuildConfig = ModStatusConfig.builder()
                .modId(CarryBabyAnimals.MOD_ID)
                .displayName(CarryBabyAnimalsModStatus.DISPLAY_NAME)
                .clientVersion(clientVersion)
                .payloadChannel(CarryBabyAnimals.MOD_ID, CarryBabyAnimalsModStatus.PAYLOAD_PATH)
                .build();

        assertEquals(VersionStatus.MATCHED, ModStatusKit.connected(noBuildConfig, clientVersion).status());
        assertEquals(VersionStatus.MATCHED, ModStatusKit.connected(noBuildConfig, clientVersion + "+server456").status());
        assertEquals(VersionStatus.MATCHED, ModStatusKit.connected(CarryBabyAnimalsModStatus.CONFIG, clientVersion).status());
    }

    @Test
    void equalBuildMetadataMatchesWhenBaseVersionsMatch() {
        String clientVersion = CarryBabyAnimalsModStatus.CONFIG.clientVersion();
        String build = CarryBabyAnimalsModStatus.CONFIG.clientBuild();

        assertEquals(
                VersionStatus.MATCHED,
                ModStatusKit.connected(CarryBabyAnimalsModStatus.CONFIG, clientVersion + "+" + build).status()
        );
    }
}
