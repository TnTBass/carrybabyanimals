package dev.jasmine.carrybabyanimals.internal.modstatus;

/**
 * Informational client/server version status.
 */
public enum VersionStatus {
    MATCHED,
    BUILD_DIFFERENT,
    DIFFERENT,
    DISCONNECTED,
    SERVER_NOT_DETECTED,
    UNKNOWN;

    public StatusTone tone() {
        return switch (this) {
            case MATCHED, BUILD_DIFFERENT -> StatusTone.GREEN;
            case DIFFERENT -> StatusTone.ORANGE;
            case DISCONNECTED, SERVER_NOT_DETECTED, UNKNOWN -> StatusTone.GRAY;
        };
    }
}
