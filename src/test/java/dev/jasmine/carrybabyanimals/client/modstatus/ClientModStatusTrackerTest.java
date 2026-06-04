package dev.jasmine.carrybabyanimals.client.modstatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClientModStatusTrackerTest {
    @AfterEach
    void resetTracker() {
        ClientModStatusTracker.resetForTesting();
    }

    @Test
    void joinWithoutStatusPayloadFallsBackToServerNotDetected() {
        ClientModStatusTracker.onJoin();

        tickServerDetectionWindow();

        assertEquals("Server not detected", ClientModStatusTracker.display().statusLabel());
    }

    @Test
    void serverVersionMarksServerDetectedWithoutGameplayTraffic() {
        ClientModStatusTracker.onJoin();

        ClientModStatusTracker.onServerVersion(ClientModStatusTracker.display().clientVersion());
        tickServerDetectionWindow();

        assertEquals("Matched", ClientModStatusTracker.display().statusLabel());
    }

    private static void tickServerDetectionWindow() {
        for (int tick = 0; tick < 100; tick++) {
            ClientModStatusTracker.tick();
        }
    }
}
