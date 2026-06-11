package dev.jasmine.carrybabyanimals.neoforge;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NeoForgePathsTest {
    @Test
    void configPathUsesNeoForgeConfigDirectoryAndModIdFileName() {
        Path configDir = Path.of("server", "config");

        assertEquals(
                configDir.resolve(CarryBabyAnimals.MOD_ID + ".json"),
                NeoForgePaths.configPath(configDir)
        );
    }
}
