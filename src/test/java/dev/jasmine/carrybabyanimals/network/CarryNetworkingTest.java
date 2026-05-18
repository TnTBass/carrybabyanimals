package dev.jasmine.carrybabyanimals.network;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CarryNetworkingTest {
    @Test
    void passengerSyncRecipientsIncludeCarrierAndBothTrackingSetsOnce() {
        assertEquals(
                List.of(1, 2, 3, 4),
                List.copyOf(CarryNetworking.passengerSyncRecipientIds(1, List.of(2, 3), List.of(3, 4)))
        );
    }
}
