package dev.jasmine.carrybabyanimals.modstatus;

import dev.jasmine.carrybabyanimals.BuildInfo;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusConfig;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusMessages;
import dev.jasmine.carrybabyanimals.internal.modstatus.VersionStatus;

public final class CarryBabyAnimalsModStatus {
    private static final String MOD_ID = "carrybabyanimals";
    public static final String DISPLAY_NAME = "Carry Baby Animals";
    public static final String UPDATE_URL = "https://modrinth.com/mod/carrybabyanimals";
    public static final String PAYLOAD_PATH = "server_version";
    public static ModStatusConfig CONFIG = createConfig("unknown");

    private CarryBabyAnimalsModStatus() {
    }

    public static void useCurrentVersion(String currentVersion) {
        CONFIG = createConfig(currentVersion == null || currentVersion.isBlank() ? "unknown" : currentVersion);
    }

    private static ModStatusConfig createConfig(String currentVersion) {
        return ModStatusConfig.builder()
            .modId(MOD_ID)
            .displayName(DISPLAY_NAME)
            .clientVersion(currentVersion)
            .clientBuild(BuildInfo.BUILD_NUMBER)
            .updateUrl(UPDATE_URL)
            .payloadChannel(MOD_ID, PAYLOAD_PATH)
            .messages(ModStatusMessages.builder()
                    .help(VersionStatus.MATCHED, "Client and server versions match.")
                    .help(VersionStatus.BUILD_DIFFERENT, "Client and server are compatible, but their build details differ.")
                    .help(VersionStatus.DIFFERENT, "Different versions may miss or hide optional visuals. Gameplay remains compatible.")
                    .help(VersionStatus.DISCONNECTED, "Not connected to a server or world.")
                    .help(VersionStatus.SERVER_NOT_DETECTED, "This server does not appear to have Carry Baby Animals installed.")
                    .help(VersionStatus.UNKNOWN, "Waiting for the server version.")
                    .build())
            .build();
    }
}
