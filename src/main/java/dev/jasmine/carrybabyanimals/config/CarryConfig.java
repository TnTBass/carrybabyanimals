package dev.jasmine.carrybabyanimals.config;

import java.util.List;

public record CarryConfig(
        List<String> allowedAnimals,
        List<String> blockedAnimals,
        boolean allowCarryingOtherPlayersTamedAnimals,
        int pettingCooldownTicks
) {
    public CarryConfig {
        allowedAnimals = allowedAnimals == null ? List.of() : List.copyOf(allowedAnimals);
        blockedAnimals = blockedAnimals == null ? List.of() : List.copyOf(blockedAnimals);
    }

    public static CarryConfig defaultConfig() {
        return new CarryConfig(List.of(), List.of(), false, 20);
    }
}
