package dev.jasmine.carrybabyanimals.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CarryConfigManagerTest {
    @Test
    void defaultConfigAllowsDefaultSetAndBlocksNobody() {
        CarryConfig config = CarryConfig.defaultConfig();

        assertTrue(config.allowedAnimals().isEmpty());
        assertTrue(config.blockedAnimals().isEmpty());
        assertFalse(config.allowCarryingOtherPlayersTamedAnimals());
        assertEquals(20, config.pettingCooldownTicks());
    }

    @Test
    void parsedConfigUsesFriendlyNames() {
        String json = """
            {
              "allowedAnimals": ["cow", "dog"],
              "blockedAnimals": ["panda"],
              "allowCarryingOtherPlayersTamedAnimals": true,
              "pettingCooldownTicks": 40
            }
            """;

        CarryConfig config = CarryConfigManager.parse(json);

        assertEquals(List.of("cow", "dog"), config.allowedAnimals());
        assertEquals(List.of("panda"), config.blockedAnimals());
        assertTrue(config.allowCarryingOtherPlayersTamedAnimals());
        assertEquals(40, config.pettingCooldownTicks());
    }

    @Test
    void directConstructionDefensivelyCopiesLists() {
        List<String> allowed = new ArrayList<>(List.of("cow"));
        List<String> blocked = new ArrayList<>(List.of("panda"));

        CarryConfig config = new CarryConfig(allowed, blocked, false, 20);

        allowed.add("dog");
        blocked.add("fox");

        assertEquals(List.of("cow"), config.allowedAnimals());
        assertEquals(List.of("panda"), config.blockedAnimals());
        assertThrows(UnsupportedOperationException.class, () -> config.allowedAnimals().add("pig"));
        assertThrows(UnsupportedOperationException.class, () -> config.blockedAnimals().add("cat"));
    }

    @Test
    void directConstructionNormalizesNullLists() {
        CarryConfig config = new CarryConfig(null, null, false, 20);

        assertTrue(config.allowedAnimals().isEmpty());
        assertTrue(config.blockedAnimals().isEmpty());
    }

    @Test
    void parsedConfigTrimsNamesAndDropsNullOrBlankEntries() {
        String json = """
            {
              "allowedAnimals": [null, "", " cow ", "unknown_creature"],
              "blockedAnimals": ["   ", " panda ", null],
              "pettingCooldownTicks": 0
            }
            """;

        CarryConfig config = CarryConfigManager.parse(json);

        assertEquals(List.of("cow", "unknown_creature"), config.allowedAnimals());
        assertEquals(List.of("panda"), config.blockedAnimals());
        assertEquals(20, config.pettingCooldownTicks());
    }

    @Test
    void saveDefaultAllowsPathWithoutParent() throws IOException {
        CarryConfigManager manager = new CarryConfigManager();
        Path path = Path.of("carrybabyanimals-save-default-test.json");
        Files.deleteIfExists(path);

        try {
            manager.saveDefault(path);

            assertTrue(Files.exists(path));
            CarryConfig config = CarryConfigManager.parse(Files.readString(path));
            assertEquals(CarryConfig.defaultConfig(), config);
        } finally {
            Files.deleteIfExists(path);
        }
    }
}
