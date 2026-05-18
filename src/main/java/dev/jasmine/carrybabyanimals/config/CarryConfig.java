package dev.jasmine.carrybabyanimals.config;

import java.util.List;

public record CarryConfig(
        List<String> allowedAnimals,
        List<String> blockedAnimals,
        boolean allowCarryingOtherPlayersTamedAnimals,
        int pettingCooldownTicks,
        boolean restrictToAllowedAnimals
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
                allowedAnimals != null && !allowedAnimals.isEmpty()
        );
    }

    public CarryConfig {
        allowedAnimals = allowedAnimals == null ? List.of() : List.copyOf(allowedAnimals);
        blockedAnimals = blockedAnimals == null ? List.of() : List.copyOf(blockedAnimals);
    }

    public static CarryConfig defaultConfig() {
        return new CarryConfig(List.of(), List.of(), false, 20, false);
    }
}
