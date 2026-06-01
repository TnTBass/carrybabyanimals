package dev.jasmine.carrybabyanimals.client.render;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarriedBabyReactionRegistryTest {
    @Test
    void returnsChickenFlapForChicken() {
        CarriedBabyReaction reaction = CarriedBabyReactionRegistry.reactionFor("minecraft:chicken", true, Set.of(), 1.0D);

        assertEquals(CarriedBabyReactionType.CHICKEN_FLAP, reaction.type());
    }

    @Test
    void returnsRabbitWiggleForRabbit() {
        CarriedBabyReaction reaction = CarriedBabyReactionRegistry.reactionFor("minecraft:rabbit", true, Set.of(), 1.0D);

        assertEquals(CarriedBabyReactionType.RABBIT_WIGGLE, reaction.type());
    }

    @Test
    void returnsFoxCurlForFox() {
        CarriedBabyReaction reaction = CarriedBabyReactionRegistry.reactionFor("minecraft:fox", true, Set.of(), 1.0D);

        assertEquals(CarriedBabyReactionType.FOX_CURL, reaction.type());
    }

    @Test
    void returnsPandaSneezeForPanda() {
        CarriedBabyReaction reaction = CarriedBabyReactionRegistry.reactionFor("minecraft:panda", true, Set.of(), 1.0D);

        assertEquals(CarriedBabyReactionType.PANDA_SNEEZE, reaction.type());
    }

    @Test
    void returnsTurtleHideForTurtle() {
        CarriedBabyReaction reaction = CarriedBabyReactionRegistry.reactionFor("minecraft:turtle", true, Set.of(), 1.0D);

        assertEquals(CarriedBabyReactionType.TURTLE_HIDE, reaction.type());
    }

    @Test
    void returnsGenericFallbackForUnsupportedEntity() {
        CarriedBabyReaction reaction = CarriedBabyReactionRegistry.reactionFor("modded:duckling", true, Set.of(), 1.0D);

        assertEquals(CarriedBabyReactionType.GENERIC_SETTLE, reaction.type());
    }

    @Test
    void disabledAnimalUsesGenericFallback() {
        CarriedBabyReaction disabledEntity = CarriedBabyReactionRegistry.reactionFor(
                "minecraft:chicken",
                true,
                Set.of("minecraft:chicken"),
                1.0D
        );
        CarriedBabyReaction disabledGlobally = CarriedBabyReactionRegistry.reactionFor(
                "minecraft:rabbit",
                false,
                Set.of(),
                1.0D
        );

        assertEquals(CarriedBabyReactionType.GENERIC_SETTLE, disabledEntity.type());
        assertEquals(CarriedBabyReactionType.GENERIC_SETTLE, disabledGlobally.type());
    }

    @Test
    void reactionIntensityScalesAmplitudeWithoutChangingDuration() {
        CarriedBabyReaction full = CarriedBabyReactionRegistry.reactionFor("minecraft:chicken", true, Set.of(), 1.0D);
        CarriedBabyReaction half = CarriedBabyReactionRegistry.reactionFor("minecraft:chicken", true, Set.of(), 0.5D);
        CarriedBabyReaction high = CarriedBabyReactionRegistry.reactionFor("minecraft:chicken", true, Set.of(), 9.0D);

        assertEquals(full.durationTicks(), half.durationTicks());
        assertEquals(full.amplitude() * 0.5D, half.amplitude(), 1.0E-6D);
        assertEquals(full.amplitude(), high.amplitude(), 1.0E-6D);
        assertTrue(full.durationTicks() >= 8);
        assertTrue(full.durationTicks() <= 40);
    }
}
