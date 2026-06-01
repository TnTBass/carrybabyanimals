package dev.jasmine.carrybabyanimals.client.config;

import dev.jasmine.carrybabyanimals.client.render.FirstPersonLargeBabyVisibilityMode;

import java.util.List;

public record ClientCarryVisualConfig(
        boolean carriedBabyReactionsEnabled,
        boolean largeBabyTuckedPoseEnabled,
        FirstPersonLargeBabyVisibilityMode firstPersonLargeBabyVisibilityMode,
        boolean sleepyCarryVisualsEnabled,
        double animalReactionIntensity,
        List<String> disabledCarriedReactionAnimals
) {
    public ClientCarryVisualConfig {
        firstPersonLargeBabyVisibilityMode = firstPersonLargeBabyVisibilityMode == null
                ? FirstPersonLargeBabyVisibilityMode.TUCKED
                : firstPersonLargeBabyVisibilityMode;
        animalReactionIntensity = Math.max(0.0D, Math.min(1.0D, animalReactionIntensity));
        disabledCarriedReactionAnimals = disabledCarriedReactionAnimals == null
                ? List.of()
                : List.copyOf(disabledCarriedReactionAnimals);
    }

    public static ClientCarryVisualConfig defaultConfig() {
        return new ClientCarryVisualConfig(
                true,
                true,
                FirstPersonLargeBabyVisibilityMode.TUCKED,
                true,
                0.75D,
                List.of()
        );
    }
}
