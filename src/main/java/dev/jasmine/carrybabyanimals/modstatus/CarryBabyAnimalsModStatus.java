package dev.jasmine.carrybabyanimals.modstatus;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusConfig;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusMessages;
import dev.jasmine.carrybabyanimals.internal.modstatus.VersionStatus;
import net.fabricmc.loader.api.FabricLoader;

public final class CarryBabyAnimalsModStatus {
    public static final String DISPLAY_NAME = "Carry Baby Animals";
    public static final String UPDATE_URL = "https://modrinth.com/mod/carrybabyanimals";
    public static final String PAYLOAD_PATH = "server_version";
    public static final ModStatusConfig CONFIG = ModStatusConfig.builder()
            .modId(CarryBabyAnimals.MOD_ID)
            .displayName(DISPLAY_NAME)
            .clientVersion(currentVersion())
            .updateUrl(UPDATE_URL)
            .payloadChannel(CarryBabyAnimals.MOD_ID, PAYLOAD_PATH)
            .messages(ModStatusMessages.builder()
                    .help(VersionStatus.MATCHED, "Client and server versions match.")
                    .help(VersionStatus.DIFFERENT, "Different versions may miss or hide optional visuals. Gameplay remains compatible.")
                    .help(VersionStatus.DISCONNECTED, "Not connected to a server or world.")
                    .help(VersionStatus.SERVER_NOT_DETECTED, "This server does not appear to have Carry Baby Animals installed.")
                    .help(VersionStatus.UNKNOWN, "Waiting for the server version.")
                    .build())
            .build();

    private CarryBabyAnimalsModStatus() {
    }

    private static String currentVersion() {
        return FabricLoader.getInstance()
                .getModContainer(CarryBabyAnimals.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
