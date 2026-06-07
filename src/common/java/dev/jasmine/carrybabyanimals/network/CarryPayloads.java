package dev.jasmine.carrybabyanimals.network;

import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusServerStatus;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusVersionPayload;

import java.util.Arrays;

public final class CarryPayloads {
    private CarryPayloads() {
    }

    public record SetCarried(int babyEntityId, int carrierEntityId) {
    }

    public record ClearCarried(int babyEntityId) {
    }

    public record PetCarried() {
        public static final PetCarried INSTANCE = new PetCarried();
    }

    public record PetFeedback(int babyEntityId) {
    }

    public record ServerVersion(byte[] encodedVersion) {
        public static final int MAX_ENCODED_BYTES = 256;

        public ServerVersion {
            encodedVersion = Arrays.copyOf(encodedVersion, encodedVersion.length);
        }

        public ServerVersion(String serverVersion) {
            this(ModStatusVersionPayload.encodeServerVersion(serverVersion));
        }

        @Override
        public byte[] encodedVersion() {
            return Arrays.copyOf(encodedVersion, encodedVersion.length);
        }

        public String serverVersion() {
            return ModStatusVersionPayload.decodeServerVersion(encodedVersion);
        }

        public ModStatusServerStatus serverStatus() {
            return ModStatusVersionPayload.decodeServerStatus(encodedVersion);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ServerVersion that
                    && Arrays.equals(encodedVersion, that.encodedVersion);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(encodedVersion);
        }
    }
}
