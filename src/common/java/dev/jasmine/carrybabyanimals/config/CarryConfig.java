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
        int sleepyParticleCooldownTicks,
        boolean nurseryModeEnabled,
        boolean nurseryBlockLava,
        boolean nurseryBlockFire,
        boolean nurseryBlockCactusAndDamage,
        boolean nurseryBlockSuffocation,
        boolean nurseryBlockDangerousFalls,
        int nurseryDangerousFallDistanceBlocks,
        boolean nurseryMessagesEnabled,
        boolean parentReunionEnabled,
        int parentReunionRadiusBlocks,
        int parentReunionCooldownTicks,
        boolean parentReunionMessagesEnabled,
        boolean parentReunionParticlesEnabled
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
                200,
                true,
                true,
                true,
                true,
                true,
                true,
                4,
                true, // nurseryMessagesEnabled
                true, // parentReunionEnabled
                8, // parentReunionRadiusBlocks
                100, // parentReunionCooldownTicks
                true, // parentReunionMessagesEnabled
                true // parentReunionParticlesEnabled
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
                200,
                true,
                true,
                true,
                true,
                true,
                true,
                4,
                true, // nurseryMessagesEnabled
                true, // parentReunionEnabled
                8, // parentReunionRadiusBlocks
                100, // parentReunionCooldownTicks
                true, // parentReunionMessagesEnabled
                true // parentReunionParticlesEnabled
        );
    }

    public CarryConfig(
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
        this(
                allowedAnimals,
                blockedAnimals,
                allowCarryingOtherPlayersTamedAnimals,
                pettingCooldownTicks,
                restrictToAllowedAnimals,
                cozyFeedbackEnabled,
                carriedIdleSoundsEnabled,
                carriedIdleSoundMinTicks,
                carriedIdleSoundMaxTicks,
                pettingMessagesEnabled,
                nameAwareMessagesEnabled,
                cozyParticlesEnabled,
                sleepyBabiesEnabled,
                sleepyAfterTicks,
                sleepyMessageCooldownTicks,
                sleepyParticleCooldownTicks,
                true,
                true,
                true,
                true,
                true,
                true,
                4,
                true, // nurseryMessagesEnabled
                true, // parentReunionEnabled
                8, // parentReunionRadiusBlocks
                100, // parentReunionCooldownTicks
                true, // parentReunionMessagesEnabled
                true // parentReunionParticlesEnabled
        );
    }

    public CarryConfig(
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
            int sleepyParticleCooldownTicks,
            boolean nurseryModeEnabled,
            boolean nurseryBlockLava,
            boolean nurseryBlockFire,
            boolean nurseryBlockCactusAndDamage,
            boolean nurseryBlockSuffocation,
            boolean nurseryBlockDangerousFalls,
            int nurseryDangerousFallDistanceBlocks,
            boolean nurseryMessagesEnabled
    ) {
        this(
                allowedAnimals,
                blockedAnimals,
                allowCarryingOtherPlayersTamedAnimals,
                pettingCooldownTicks,
                restrictToAllowedAnimals,
                cozyFeedbackEnabled,
                carriedIdleSoundsEnabled,
                carriedIdleSoundMinTicks,
                carriedIdleSoundMaxTicks,
                pettingMessagesEnabled,
                nameAwareMessagesEnabled,
                cozyParticlesEnabled,
                sleepyBabiesEnabled,
                sleepyAfterTicks,
                sleepyMessageCooldownTicks,
                sleepyParticleCooldownTicks,
                nurseryModeEnabled,
                nurseryBlockLava,
                nurseryBlockFire,
                nurseryBlockCactusAndDamage,
                nurseryBlockSuffocation,
                nurseryBlockDangerousFalls,
                nurseryDangerousFallDistanceBlocks,
                nurseryMessagesEnabled,
                true, // parentReunionEnabled
                8, // parentReunionRadiusBlocks
                100, // parentReunionCooldownTicks
                true, // parentReunionMessagesEnabled
                true // parentReunionParticlesEnabled
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
                200,
                true,
                true,
                true,
                true,
                true,
                true,
                4,
                true, // nurseryMessagesEnabled
                true, // parentReunionEnabled
                8, // parentReunionRadiusBlocks
                100, // parentReunionCooldownTicks
                true, // parentReunionMessagesEnabled
                true // parentReunionParticlesEnabled
        );
    }
}
