package dev.jasmine.carrybabyanimals.network;

import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusVersionPayload;
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
    }

    @Test
    void configuredServerVersionPayloadIsCapabilityGated() {
        RecordingPayloadSender sender = new RecordingPayloadSender();

        assertFalse(CarryNetworking.sendConfiguredServerVersionIfSupported(channel -> false, sender));
        assertNull(sender.channel);
        assertNull(sender.payload);

        assertTrue(CarryNetworking.sendConfiguredServerVersionIfSupported(channel -> {
            assertEquals("carrybabyanimals:server_version", channel);
            return true;
        }, sender));
        assertEquals("carrybabyanimals:server_version", sender.channel);
        assertEquals(CarryBabyAnimalsModStatus.CONFIG.clientVersion(), ModStatusVersionPayload.decodeServerVersion(sender.payload));
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
