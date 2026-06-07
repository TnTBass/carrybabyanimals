package dev.jasmine.carrybabyanimals.client.render;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CarriedBabyReactionRegistry {
    private static final CarriedBabyReaction FALLBACK = new CarriedBabyReaction(
            CarriedBabyReactionType.GENERIC_SETTLE,
            32,
            0.25D,
            0.0D,
            0.01D,
            0.0D,
            0.0D,
            2.0D,
            true
    );

    private static final Map<String, CarriedBabyReaction> REACTIONS = Map.of(
            "minecraft:chicken", new CarriedBabyReaction(
                    CarriedBabyReactionType.CHICKEN_FLAP,
                    12,
                    0.65D,
                    0.0D,
                    0.02D,
                    6.0D,
                    0.0D,
                    12.0D,
                    false
            ),
            "minecraft:rabbit", new CarriedBabyReaction(
                    CarriedBabyReactionType.RABBIT_WIGGLE,
                    10,
                    0.5D,
                    0.03D,
                    0.04D,
                    0.0D,
                    0.0D,
                    5.0D,
                    true
            ),
            "minecraft:fox", new CarriedBabyReaction(
                    CarriedBabyReactionType.FOX_CURL,
                    28,
                    0.45D,
                    -0.04D,
                    -0.01D,
                    -4.0D,
                    8.0D,
                    0.0D,
                    true
            ),
            "minecraft:panda", new CarriedBabyReaction(
                    CarriedBabyReactionType.PANDA_SNEEZE,
                    14,
                    0.35D,
                    0.0D,
                    0.05D,
                    10.0D,
                    0.0D,
                    0.0D,
                    false
            ),
            "minecraft:turtle", new CarriedBabyReaction(
                    CarriedBabyReactionType.TURTLE_HIDE,
                    24,
                    0.4D,
                    0.0D,
                    -0.04D,
                    -8.0D,
                    0.0D,
                    0.0D,
                    true
            )
    );

    private CarriedBabyReactionRegistry() {
    }

    public static CarriedBabyReaction fallback(double intensity) {
        return FALLBACK.withIntensity(intensity);
    }

    public static CarriedBabyReaction reactionFor(
            String entityTypeId,
            boolean reactionsEnabled,
            Set<String> disabledCarriedReactionAnimals,
            double intensity
    ) {
        String normalizedId = normalize(entityTypeId);
        if (!reactionsEnabled || disabled(normalizedId, disabledCarriedReactionAnimals)) {
            return fallback(intensity);
        }
        return REACTIONS.getOrDefault(normalizedId, FALLBACK).withIntensity(intensity);
    }

    private static boolean disabled(String normalizedId, Set<String> disabledCarriedReactionAnimals) {
        if (disabledCarriedReactionAnimals == null || disabledCarriedReactionAnimals.isEmpty()) {
            return false;
        }
        return disabledCarriedReactionAnimals.contains(normalizedId);
    }

    private static String normalize(String entityTypeId) {
        if (entityTypeId == null) {
            return "";
        }
        return entityTypeId.trim().toLowerCase(Locale.ROOT);
    }
}
