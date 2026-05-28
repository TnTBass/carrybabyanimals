package dev.jasmine.carrybabyanimals.config;

import java.util.List;

public record CarryConfig(
        List<String> allowedAnimals,
        List<String> blockedAnimals,
        boolean allowCarryingOtherPlayersTamedAnimals,
        int pettingCooldownTicks,
        boolean restrictToAllowedAnimals,
        boolean cozyFeedbackEnabled,
        boolean carriedIdleSoundsEnabled,
        int carriedIdleSoundMinTicks,
        int carriedIdleSoundMaxTicks,
        boolean pettingMessagesEnabled,
        boolean nameAwareMessagesEnabled,
        boolean cozyParticlesEnabled,
        boolean sleepyBabiesEnabled,
        int sleepyAfterTicks,
        int sleepyMessageCooldownTicks,
        int sleepyParticleCooldownTicks
) {
    public CarryConfig(
            List<String> allowedAnimals,
            List<String> blockedAnimals,
            boolean allowCarryingOtherPlayersTamedAnimals,
            int pettingCooldownTicks
    ) {
        this(
                allowedAnimals,
                blockedAnimals,
                allowCarryingOtherPlayersTamedAnimals,
                pettingCooldownTicks,
                allowedAnimals != null && !allowedAnimals.isEmpty(),
                true,
                true,
                160,
                360,
                true,
                true,
                true,
                true,
                1200,
                600,
                200
        );
    }

    public CarryConfig(
            List<String> allowedAnimals,
            List<String> blockedAnimals,
            boolean allowCarryingOtherPlayersTamedAnimals,
            int pettingCooldownTicks,
            boolean restrictToAllowedAnimals
    ) {
        this(
                allowedAnimals,
                blockedAnimals,
                allowCarryingOtherPlayersTamedAnimals,
                pettingCooldownTicks,
                restrictToAllowedAnimals,
                true,
                true,
                160,
                360,
                true,
                true,
                true,
                true,
                1200,
                600,
                200
        );
    }

    public CarryConfig {
        allowedAnimals = allowedAnimals == null ? List.of() : List.copyOf(allowedAnimals);
        blockedAnimals = blockedAnimals == null ? List.of() : List.copyOf(blockedAnimals);
    }

    public static CarryConfig defaultConfig() {
        return new CarryConfig(
                List.of(),
                List.of(),
                false,
                20,
                false,
                true,
                true,
                160,
                360,
                true,
                true,
                true,
                true,
                1200,
                600,
                200
        );
    }
}
