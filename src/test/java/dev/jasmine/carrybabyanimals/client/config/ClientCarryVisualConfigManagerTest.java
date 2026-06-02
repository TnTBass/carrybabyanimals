package dev.jasmine.carrybabyanimals.client.config;

import dev.jasmine.carrybabyanimals.client.render.FirstPersonLargeBabyVisibilityMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientCarryVisualConfigManagerTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void resetCurrentConfig() throws Exception {
        Path path = tempDir.resolve("reset").resolve("carrybabyanimals-client.json");
        ClientCarryVisualConfigManager.save(path, ClientCarryVisualConfig.defaultConfig());
    }

    @Test
    void defaultsEnableConservativeClientVisualPolish() {
        ClientCarryVisualConfig config = ClientCarryVisualConfig.defaultConfig();

        assertTrue(config.carriedBabyReactionsEnabled());
        assertTrue(config.largeBabyTuckedPoseEnabled());
        assertEquals(FirstPersonLargeBabyVisibilityMode.LOWERED, config.firstPersonLargeBabyVisibilityMode());
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

    @Test
    void savesUpdatedConfigAndUsesItAsCurrentConfig() throws Exception {
        Path path = tempDir.resolve("client-config").resolve("carrybabyanimals-client.json");
        ClientCarryVisualConfig updated = new ClientCarryVisualConfig(
                false,
                false,
                FirstPersonLargeBabyVisibilityMode.LOWERED,
                false,
                0.4D,
                List.of(" Minecraft:Panda ", "examplemod:duck")
        );

        ClientCarryVisualConfigManager.save(path, updated);
        ClientCarryVisualConfigManager.load(path);

        assertTrue(Files.exists(path));
        assertEquals(false, ClientCarryVisualConfigManager.config().carriedBabyReactionsEnabled());
        assertEquals(false, ClientCarryVisualConfigManager.config().largeBabyTuckedPoseEnabled());
        assertEquals(FirstPersonLargeBabyVisibilityMode.LOWERED, ClientCarryVisualConfigManager.config().firstPersonLargeBabyVisibilityMode());
        assertEquals(false, ClientCarryVisualConfigManager.config().sleepyCarryVisualsEnabled());
        assertEquals(0.4D, ClientCarryVisualConfigManager.config().animalReactionIntensity(), 1.0E-6D);
        assertEquals(List.of("minecraft:panda", "examplemod:duck"), ClientCarryVisualConfigManager.config().disabledCarriedReactionAnimals());
    }
}
