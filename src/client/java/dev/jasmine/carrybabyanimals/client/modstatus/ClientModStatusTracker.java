package dev.jasmine.carrybabyanimals.client.modstatus;

import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusClientState;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusDisplay;
import dev.jasmine.carrybabyanimals.modstatus.CarryBabyAnimalsModStatus;

public final class ClientModStatusTracker {
    private static final int SERVER_DETECTION_TICKS = 100;
    private static final ModStatusClientState STATE = ModStatusClientState.create(CarryBabyAnimalsModStatus.CONFIG);
    private static int unknownTicks;

    private ClientModStatusTracker() {
    }

    public static ModStatusDisplay display() {
        return STATE.display();
    }

    public static void onJoin() {
        STATE.unknown();
        unknownTicks = SERVER_DETECTION_TICKS;
    }

    public static void onServerVersion(String serverVersion) {
        STATE.connected(serverVersion);
        unknownTicks = 0;
    }

    public static void onDisconnect() {
        STATE.disconnected();
        unknownTicks = 0;
    }

    public static void tick() {
        if (unknownTicks <= 0) {
            return;
        }
        unknownTicks--;
        if (unknownTicks == 0) {
            STATE.markServerNotDetectedIfUnknown();
        }
    }

    static void resetForTesting() {
        STATE.disconnected();
        unknownTicks = 0;
    }
}
