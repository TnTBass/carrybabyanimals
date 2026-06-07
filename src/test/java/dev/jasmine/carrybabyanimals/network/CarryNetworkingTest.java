package dev.jasmine.carrybabyanimals.network;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusConfig;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusVersionPayload;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusVersion;
import dev.jasmine.carrybabyanimals.internal.modstatus.VersionMismatchSeverity;
import dev.jasmine.carrybabyanimals.modstatus.CarryBabyAnimalsModStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarryNetworkingTest {
    @Test
    void passengerSyncRecipientsIncludeCarrierAndBothTrackingSetsOnce() {
        assertEquals(
                List.of(1, 2, 3, 4),
                List.copyOf(CarryNetworking.passengerSyncRecipientIds(1, List.of(2, 3), List.of(3, 4)))
        );
    }

    @Test
    void serverVersionPayloadRoundTripsThroughModStatusKitEncoding() {
        CarryNetworking.ServerVersionPayload payload = new CarryNetworking.ServerVersionPayload("0.1.3");

        assertEquals("0.1.3", payload.serverVersion());
        assertEquals(
                VersionMismatchSeverity.WARN,
                ModStatusVersionPayload.decodeServerStatus(payload.encodedVersion()).versionMismatchSeverity()
        );
    }

    @Test
    void commonPacketChannelsKeepExistingNames() {
        assertEquals("set_carried", CarryPacketChannels.SET_CARRIED);
        assertEquals("clear_carried", CarryPacketChannels.CLEAR_CARRIED);
        assertEquals("pet_carried", CarryPacketChannels.PET_CARRIED);
        assertEquals("pet_feedback", CarryPacketChannels.PET_FEEDBACK);
        assertEquals("server_version", CarryPacketChannels.SERVER_VERSION);
    }

    @Test
    void commonServerVersionPayloadDefensivelyCopiesEncodedBytes() {
        byte[] encoded = ModStatusVersionPayload.encodeServerStatus(
                "0.1.3",
                "12345",
                VersionMismatchSeverity.WARN
        );
        CarryPayloads.ServerVersion payload = new CarryPayloads.ServerVersion(encoded);

        encoded[0] = 0;
        byte[] returned = payload.encodedVersion();
        returned[1] = 0;

        assertEquals("0.1.3+12345", payload.serverVersion());
        assertEquals(
                VersionMismatchSeverity.WARN,
                payload.serverStatus().versionMismatchSeverity()
        );
    }

    @Test
    void commonServerVersionIntentDefensivelyCopiesEncodedBytes() {
        byte[] encoded = ModStatusVersionPayload.encodeServerVersion("0.1.3+12345");
        CarryNetworkIntents.ServerVersionToRecipient intent =
                new CarryNetworkIntents.ServerVersionToRecipient(encoded, 42);

        encoded[0] = 0;
        byte[] returned = intent.encodedVersion();
        returned[1] = 0;

        assertEquals(42, intent.recipientEntityId());
        assertEquals("0.1.3+12345", ModStatusVersionPayload.decodeServerVersion(intent.encodedVersion()));
    }

    @Test
    void serverVersionPayloadRoundTripsBuildMetadata() {
        CarryNetworking.ServerVersionPayload payload = new CarryNetworking.ServerVersionPayload("0.1.3+12345");
        var version = ModStatusVersionPayload.decodeServerVersionInfo(payload.encodedVersion());

        assertEquals("0.1.3", version.version());
        assertEquals("12345", version.build());
        assertEquals(
                VersionMismatchSeverity.WARN,
                ModStatusVersionPayload.decodeServerStatus(payload.encodedVersion()).versionMismatchSeverity()
        );
    }

    @Test
    void serverVersionPayloadRoundTripsStructuredWarnStatus() {
        byte[] encoded = ModStatusVersionPayload.encodeServerStatus(
                "0.1.3",
                "12345",
                VersionMismatchSeverity.WARN
        );
        CarryNetworking.ServerVersionPayload payload = new CarryNetworking.ServerVersionPayload(encoded);
        var status = payload.serverStatus();

        assertEquals("0.1.3", status.serverVersion());
        assertEquals("12345", status.serverBuild());
        assertEquals(VersionMismatchSeverity.WARN, status.versionMismatchSeverity());
        assertEquals("0.1.3+12345", payload.serverVersion());
    }

    @Test
    void structuredStatusPayloadWithReleaseCandidateBuildFitsCodecLimit() {
        byte[] encoded = ModStatusVersionPayload.encodeServerStatus(
                "0.2.0",
                "manual-test-0.2.0",
                VersionMismatchSeverity.WARN
        );

        assertTrue(encoded.length > 64, "This regression payload should cover the old too-small decode cap.");
        assertTrue(encoded.length <= CarryNetworking.ServerVersionPayload.MAX_ENCODED_BYTES);
    }

    @Test
    void structuredPayloadWithBlankBuildDecodesAsNoBuild() {
        String encoded = "MSK2\nversion=0.1.3\nbuild= \nversionMismatchSeverity=WARN\n";
        var status = ModStatusVersionPayload.decodeServerStatus(encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertEquals("0.1.3", status.serverVersion());
        assertNull(status.serverBuild());
        assertEquals(VersionMismatchSeverity.WARN, status.versionMismatchSeverity());
    }

    @Test
    void configuredServerStatusPayloadIsCapabilityGatedAndWarnSeverity() {
        RecordingPayloadSender sender = new RecordingPayloadSender();

        assertFalse(CarryNetworking.sendConfiguredServerVersionIfSupported(channel -> false, sender));
        assertNull(sender.channel);
        assertNull(sender.payload);

        assertTrue(CarryNetworking.sendConfiguredServerVersionIfSupported(channel -> {
            assertEquals("carrybabyanimals:server_version", channel);
            return true;
        }, sender));
        assertEquals("carrybabyanimals:server_version", sender.channel);
        var sentStatus = ModStatusVersionPayload.decodeServerStatus(sender.payload);
        assertEquals(CarryBabyAnimalsModStatus.CONFIG.clientVersion(), sentStatus.serverVersion());
        assertEquals(CarryBabyAnimalsModStatus.CONFIG.clientBuild(), sentStatus.serverBuild());
        assertEquals(VersionMismatchSeverity.WARN, sentStatus.versionMismatchSeverity());
        assertEquals(
                ModStatusVersion.of(CarryBabyAnimalsModStatus.CONFIG.clientVersion(), CarryBabyAnimalsModStatus.CONFIG.clientBuild()).toPayloadString(),
                ModStatusVersionPayload.decodeServerVersion(sender.payload)
        );
    }

    @Test
    void legacyServerVersionSenderSupportsConfigsWithoutBuildMetadata() {
        ModStatusConfig noBuildConfig = ModStatusConfig.builder()
                .modId(CarryBabyAnimals.MOD_ID)
                .displayName(CarryBabyAnimalsModStatus.DISPLAY_NAME)
                .clientVersion(CarryBabyAnimalsModStatus.CONFIG.clientVersion())
                .payloadChannel(CarryBabyAnimals.MOD_ID, CarryBabyAnimalsModStatus.PAYLOAD_PATH)
                .build();
        RecordingPayloadSender sender = new RecordingPayloadSender();

        assertTrue(ModStatusVersionPayload.sendServerVersionIfSupported(noBuildConfig, channel -> true, sender));
        assertEquals(noBuildConfig.clientVersion(), ModStatusVersionPayload.decodeServerVersion(sender.payload));
    }

    private static final class RecordingPayloadSender implements dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusVersionPayload.PayloadSender {
        private String channel;
        private byte[] payload;

        @Override
        public void send(String channel, byte[] payload) {
            this.channel = channel;
            this.payload = payload;
        }
    }
}
