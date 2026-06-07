package dev.jasmine.carrybabyanimals.network;

import java.util.Arrays;

public sealed interface CarryNetworkIntents {
    record SetCarriedToCarrierAndTracking(int babyEntityId, int carrierEntityId) implements CarryNetworkIntents {
    }

    record ClearCarriedForRecipient(int babyEntityId, int recipientEntityId) implements CarryNetworkIntents {
    }

    record PetFeedbackToCarrier(int babyEntityId, int carrierEntityId) implements CarryNetworkIntents {
    }

    record ServerVersionToRecipient(byte[] encodedVersion, int recipientEntityId) implements CarryNetworkIntents {
        public ServerVersionToRecipient {
            encodedVersion = Arrays.copyOf(encodedVersion, encodedVersion.length);
        }

        @Override
        public byte[] encodedVersion() {
            return Arrays.copyOf(encodedVersion, encodedVersion.length);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ServerVersionToRecipient that
                    && recipientEntityId == that.recipientEntityId
                    && Arrays.equals(encodedVersion, that.encodedVersion);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(encodedVersion);
            result = 31 * result + Integer.hashCode(recipientEntityId);
            return result;
        }
    }

    record PetCarriedToServer() implements CarryNetworkIntents {
    }
}
