package dev.jasmine.carrybabyanimals.cozy;

import java.util.UUID;

public record CozyFeedbackCarrySnapshot(
        UUID carrierId,
        int carriedEntityId,
        String displayName,
        boolean hasCustomName,
        long startedAtTick,
        long gameTime
) {
}
