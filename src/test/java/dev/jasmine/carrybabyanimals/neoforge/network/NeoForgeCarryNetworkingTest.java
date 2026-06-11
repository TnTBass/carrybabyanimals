package dev.jasmine.carrybabyanimals.neoforge.network;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusVersionPayload;
import dev.jasmine.carrybabyanimals.internal.modstatus.VersionMismatchSeverity;
import dev.jasmine.carrybabyanimals.modstatus.CarryBabyAnimalsModStatus;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NeoForgeCarryNetworkingTest {
    @Test
    void payloadChannelsUseFabricCompatibleIds() {
        assertEquals(id("set_carried"), NeoForgeCarryNetworking.SetCarriedPayload.TYPE.id());
        assertEquals(id("clear_carried"), NeoForgeCarryNetworking.ClearCarriedPayload.TYPE.id());
        assertEquals(id("pet_carried"), NeoForgeCarryNetworking.PetCarriedPayload.TYPE.id());
        assertEquals(id("pet_feedback"), NeoForgeCarryNetworking.PetFeedbackPayload.TYPE.id());
        assertEquals(id(CarryBabyAnimalsModStatus.PAYLOAD_PATH), NeoForgeCarryNetworking.ServerVersionPayload.TYPE.id());
    }

    @Test
    void payloadFieldsMatchCommonCarryPayloads() {
        NeoForgeCarryNetworking.SetCarriedPayload setCarried =
                new NeoForgeCarryNetworking.SetCarriedPayload(42, 7);
        NeoForgeCarryNetworking.ClearCarriedPayload clearCarried =
                new NeoForgeCarryNetworking.ClearCarriedPayload(42);
        NeoForgeCarryNetworking.PetFeedbackPayload petFeedback =
                new NeoForgeCarryNetworking.PetFeedbackPayload(42);

        assertEquals(42, setCarried.babyEntityId());
        assertEquals(7, setCarried.carrierEntityId());
        assertEquals(42, clearCarried.babyEntityId());
        assertEquals(42, petFeedback.babyEntityId());
    }

    @Test
    void serverVersionPayloadDefensivelyCopiesEncodedBytes() {
        byte[] encoded = ModStatusVersionPayload.encodeServerStatus("0.2.1", "build.9", VersionMismatchSeverity.WARN);

        NeoForgeCarryNetworking.ServerVersionPayload payload =
                new NeoForgeCarryNetworking.ServerVersionPayload(encoded);
        encoded[0] = 'x';

        assertArrayEquals(
                ModStatusVersionPayload.encodeServerStatus("0.2.1", "build.9", VersionMismatchSeverity.WARN),
                payload.encodedVersion()
        );
    }

    @Test
    void structuredStatusPayloadFitsCodecLimitAndRoundTrips() {
        byte[] encoded = ModStatusVersionPayload.encodeServerStatus(
                "0.2.1-rc.1",
                "20260607.123456",
                VersionMismatchSeverity.WARN
        );

        assertTrue(encoded.length <= NeoForgeCarryNetworking.ServerVersionPayload.MAX_ENCODED_BYTES);

        NeoForgeCarryNetworking.ServerVersionPayload payload =
                new NeoForgeCarryNetworking.ServerVersionPayload(encoded);

        assertEquals("0.2.1-rc.1", payload.serverStatus().serverVersion());
        assertEquals("20260607.123456", payload.serverStatus().serverBuild());
        assertEquals(VersionMismatchSeverity.WARN, payload.serverStatus().versionMismatchSeverity());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CarryBabyAnimals.MOD_ID, path);
    }
}
