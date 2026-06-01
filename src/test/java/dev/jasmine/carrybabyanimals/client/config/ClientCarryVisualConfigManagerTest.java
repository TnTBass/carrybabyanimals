package dev.jasmine.carrybabyanimals.client.config;

import dev.jasmine.carrybabyanimals.client.render.FirstPersonLargeBabyVisibilityMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientCarryVisualConfigManagerTest {
    @Test
    void defaultsEnableConservativeClientVisualPolish() {
        ClientCarryVisualConfig config = ClientCarryVisualConfig.defaultConfig();

        assertTrue(config.carriedBabyReactionsEnabled());
        assertTrue(config.largeBabyTuckedPoseEnabled());
        assertEquals(FirstPersonLargeBabyVisibilityMode.TUCKED, config.firstPersonLargeBabyVisibilityMode());
        assertTrue(config.sleepyCarryVisualsEnabled());
        assertEquals(0.75D, config.animalReactionIntensity(), 1.0E-6D);
        assertEquals(List.of(), config.disabledCarriedReactionAnimals());
    }

    @Test
    void parsesDisabledReactionsAndLoweredVisibilityMode() {
        ClientCarryVisualConfig config = ClientCarryVisualConfigManager.parse("""
                {
                  "carriedBabyReactionsEnabled": false,
                  "largeBabyTuckedPoseEnabled": true,
                  "firstPersonLargeBabyVisibilityMode": "LOWERED",
                  "sleepyCarryVisualsEnabled": false,
                  "animalReactionIntensity": 0.35,
                  "disabledCarriedReactionAnimals": ["minecraft:chicken"]
                }
                """);

        assertEquals(false, config.carriedBabyReactionsEnabled());
        assertEquals(FirstPersonLargeBabyVisibilityMode.LOWERED, config.firstPersonLargeBabyVisibilityMode());
        assertEquals(false, config.sleepyCarryVisualsEnabled());
        assertEquals(0.35D, config.animalReactionIntensity(), 1.0E-6D);
        assertEquals(List.of("minecraft:chicken"), config.disabledCarriedReactionAnimals());
    }

    @Test
    void invalidVisibilityModeFallsBackToTucked() {
        ClientCarryVisualConfig config = ClientCarryVisualConfigManager.parse("""
                {"firstPersonLargeBabyVisibilityMode": "sideways"}
                """);

        assertEquals(FirstPersonLargeBabyVisibilityMode.TUCKED, config.firstPersonLargeBabyVisibilityMode());
    }

    @Test
    void intensityIsClampedToConservativeRange() {
        ClientCarryVisualConfig high = ClientCarryVisualConfigManager.parse("""
                {"animalReactionIntensity": 9.0}
                """);
        ClientCarryVisualConfig low = ClientCarryVisualConfigManager.parse("""
                {"animalReactionIntensity": -1.0}
                """);

        assertEquals(1.0D, high.animalReactionIntensity(), 1.0E-6D);
        assertEquals(0.0D, low.animalReactionIntensity(), 1.0E-6D);
    }

    @Test
    void disabledAnimalIdsAreTrimmedAndLowercased() {
        ClientCarryVisualConfig config = ClientCarryVisualConfigManager.parse("""
                {"disabledCarriedReactionAnimals": [" Minecraft:Chicken ", "", "MODDED:Baby_Duck", null]}
                """);

        assertEquals(List.of("minecraft:chicken", "modded:baby_duck"), config.disabledCarriedReactionAnimals());
    }
}
