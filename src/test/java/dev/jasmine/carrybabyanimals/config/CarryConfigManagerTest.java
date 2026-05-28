package dev.jasmine.carrybabyanimals.config;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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
    void defaultConfigEnablesCozyFeedbackConservatively() {
        CarryConfig config = CarryConfig.defaultConfig();

        assertTrue(config.cozyFeedbackEnabled());
        assertTrue(config.carriedIdleSoundsEnabled());
        assertEquals(160, config.carriedIdleSoundMinTicks());
        assertEquals(360, config.carriedIdleSoundMaxTicks());
        assertTrue(config.pettingMessagesEnabled());
        assertTrue(config.nameAwareMessagesEnabled());
        assertTrue(config.cozyParticlesEnabled());
        assertTrue(config.sleepyBabiesEnabled());
        assertEquals(1200, config.sleepyAfterTicks());
        assertEquals(600, config.sleepyMessageCooldownTicks());
        assertEquals(200, config.sleepyParticleCooldownTicks());
    }

    @Test
    void defaultConfigEnablesNurseryModeConservatively() {
        CarryConfig config = CarryConfig.defaultConfig();

        assertTrue(config.nurseryModeEnabled());
        assertTrue(config.nurseryBlockLava());
        assertTrue(config.nurseryBlockFire());
        assertTrue(config.nurseryBlockCactusAndDamage());
        assertTrue(config.nurseryBlockSuffocation());
        assertTrue(config.nurseryBlockDangerousFalls());
        assertEquals(4, config.nurseryDangerousFallDistanceBlocks());
        assertTrue(config.nurseryMessagesEnabled());
    }

    @Test
    void defaultConfigEnablesParentReunionConservatively() {
        CarryConfig config = CarryConfig.defaultConfig();

        assertTrue(config.parentReunionEnabled());
        assertEquals(8, config.parentReunionRadiusBlocks());
        assertEquals(100, config.parentReunionCooldownTicks());
        assertTrue(config.parentReunionMessagesEnabled());
        assertTrue(config.parentReunionParticlesEnabled());
    }

    @Test
    void parsedParentReunionConfigUsesExplicitValues() {
        String json = """
            {
              "parentReunionEnabled": false,
              "parentReunionRadiusBlocks": 5,
              "parentReunionCooldownTicks": 400,
              "parentReunionMessagesEnabled": false,
              "parentReunionParticlesEnabled": false
            }
            """;

        CarryConfig config = CarryConfigManager.parse(json);

        assertFalse(config.parentReunionEnabled());
        assertEquals(5, config.parentReunionRadiusBlocks());
        assertEquals(400, config.parentReunionCooldownTicks());
        assertFalse(config.parentReunionMessagesEnabled());
        assertFalse(config.parentReunionParticlesEnabled());
    }

    @Test
    void parsedParentReunionConfigNormalizesInvalidTimingAndRadius() {
        CarryConfig config = CarryConfigManager.parse("""
            {
              "parentReunionRadiusBlocks": 0,
              "parentReunionCooldownTicks": -1
            }
            """);

        assertEquals(8, config.parentReunionRadiusBlocks());
        assertEquals(100, config.parentReunionCooldownTicks());
    }

    @Test
    void parsedNurseryConfigUsesExplicitValues() {
        String json = """
            {
              "nurseryModeEnabled": false,
              "nurseryBlockLava": false,
              "nurseryBlockFire": false,
              "nurseryBlockCactusAndDamage": false,
              "nurseryBlockSuffocation": false,
              "nurseryBlockDangerousFalls": false,
              "nurseryDangerousFallDistanceBlocks": 8,
              "nurseryMessagesEnabled": false
            }
            """;

        CarryConfig config = CarryConfigManager.parse(json);

        assertFalse(config.nurseryModeEnabled());
        assertFalse(config.nurseryBlockLava());
        assertFalse(config.nurseryBlockFire());
        assertFalse(config.nurseryBlockCactusAndDamage());
        assertFalse(config.nurseryBlockSuffocation());
        assertFalse(config.nurseryBlockDangerousFalls());
        assertEquals(8, config.nurseryDangerousFallDistanceBlocks());
        assertFalse(config.nurseryMessagesEnabled());
    }

    @Test
    void parsedNurseryConfigNormalizesInvalidFallDistance() {
        CarryConfig config = CarryConfigManager.parse("""
            {
              "nurseryDangerousFallDistanceBlocks": 0
            }
            """);

        assertEquals(4, config.nurseryDangerousFallDistanceBlocks());
    }

    @Test
    void parsedOlderConfigDefaultsMissingNurseryBooleansToEnabled() {
        CarryConfig config = CarryConfigManager.parse("{}");

        assertTrue(config.nurseryModeEnabled());
        assertTrue(config.nurseryBlockLava());
        assertTrue(config.nurseryBlockFire());
        assertTrue(config.nurseryBlockCactusAndDamage());
        assertTrue(config.nurseryBlockSuffocation());
        assertTrue(config.nurseryBlockDangerousFalls());
        assertTrue(config.nurseryMessagesEnabled());
    }

    @Test
    void parsedCozyConfigUsesExplicitValues() {
        String json = """
            {
              "cozyFeedbackEnabled": false,
              "carriedIdleSoundsEnabled": false,
              "carriedIdleSoundMinTicks": 40,
              "carriedIdleSoundMaxTicks": 80,
              "pettingMessagesEnabled": false,
              "nameAwareMessagesEnabled": false,
              "cozyParticlesEnabled": false,
              "sleepyBabiesEnabled": false,
              "sleepyAfterTicks": 200,
              "sleepyMessageCooldownTicks": 100,
              "sleepyParticleCooldownTicks": 50
            }
            """;

        CarryConfig config = CarryConfigManager.parse(json);

        assertFalse(config.cozyFeedbackEnabled());
        assertFalse(config.carriedIdleSoundsEnabled());
        assertEquals(40, config.carriedIdleSoundMinTicks());
        assertEquals(80, config.carriedIdleSoundMaxTicks());
        assertFalse(config.pettingMessagesEnabled());
        assertFalse(config.nameAwareMessagesEnabled());
        assertFalse(config.cozyParticlesEnabled());
        assertFalse(config.sleepyBabiesEnabled());
        assertEquals(200, config.sleepyAfterTicks());
        assertEquals(100, config.sleepyMessageCooldownTicks());
        assertEquals(50, config.sleepyParticleCooldownTicks());
    }

    @Test
    void parsedCozyConfigNormalizesInvalidTimingValues() {
        String json = """
            {
              "carriedIdleSoundMinTicks": -1,
              "carriedIdleSoundMaxTicks": 20,
              "sleepyAfterTicks": 0,
              "sleepyMessageCooldownTicks": -5,
              "sleepyParticleCooldownTicks": -10
            }
            """;

        CarryConfig config = CarryConfigManager.parse(json);

        assertEquals(160, config.carriedIdleSoundMinTicks());
        assertEquals(160, config.carriedIdleSoundMaxTicks());
        assertEquals(1200, config.sleepyAfterTicks());
        assertEquals(600, config.sleepyMessageCooldownTicks());
        assertEquals(200, config.sleepyParticleCooldownTicks());
    }

    @Test
    void parsedOlderConfigDefaultsMissingCozyBooleansToEnabled() {
        CarryConfig config = CarryConfigManager.parse("{}");

        assertTrue(config.cozyFeedbackEnabled());
        assertTrue(config.carriedIdleSoundsEnabled());
        assertTrue(config.pettingMessagesEnabled());
        assertTrue(config.nameAwareMessagesEnabled());
        assertTrue(config.cozyParticlesEnabled());
        assertTrue(config.sleepyBabiesEnabled());
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
        assertTrue(config.restrictToAllowedAnimals());
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
        assertTrue(config.restrictToAllowedAnimals());
    }

    @Test
    void unknownAnimalNamesReportsAllowedAndBlockedEntries() {
        CarryConfig config = new CarryConfig(
                List.of("cow", "mystery", "DOG", "missing_pet"),
                List.of("panda", "void_horse"),
                false,
                20
        );

        CarryConfigManager.UnknownAnimalNames unknownNames = CarryConfigManager.unknownAnimalNames(
                config,
                AnimalAliasRegistry.createDefault()
        );

        assertEquals(List.of("mystery", "missing_pet"), unknownNames.allowedAnimals());
        assertEquals(List.of("void_horse"), unknownNames.blockedAnimals());
    }

    @Test
    void loadedConfigRemovesUnknownOnlyAllowedNamesFromEffectiveConfig() throws IOException {
        CarryConfigManager manager = loadConfig("""
            {
              "allowedAnimals": ["not_real"]
            }
            """);

        manager.filterAndLogUnknownAnimalNames(AnimalAliasRegistry.createDefault(), LoggerFactory.getLogger(CarryConfigManagerTest.class));

        assertTrue(manager.config().allowedAnimals().isEmpty());
        assertTrue(manager.config().restrictToAllowedAnimals());
    }

    @Test
    void loadedConfigKeepsValidAllowedNamesWhenRemovingUnknownNames() throws IOException {
        CarryConfigManager manager = loadConfig("""
            {
              "allowedAnimals": ["cow", "not_real", "dog"],
              "blockedAnimals": ["panda", "missing_pet"]
            }
            """);

        manager.filterAndLogUnknownAnimalNames(AnimalAliasRegistry.createDefault(), LoggerFactory.getLogger(CarryConfigManagerTest.class));

        assertEquals(List.of("cow", "dog"), manager.config().allowedAnimals());
        assertEquals(List.of("panda"), manager.config().blockedAnimals());
        assertTrue(manager.config().restrictToAllowedAnimals());
    }

    @Test
    void malformedLoadedConfigKeepsDefaultConfigAndThrowsIOException() throws IOException {
        CarryConfigManager manager = new CarryConfigManager();
        Path path = Path.of("carrybabyanimals-malformed-load-test.json");
        Files.deleteIfExists(path);

        try {
            Files.writeString(path, "{ \"allowedAnimals\": ");

            assertThrows(IOException.class, () -> manager.load(path));
            assertEquals(CarryConfig.defaultConfig(), manager.config());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    void saveDefaultAllowsPathWithoutParent() throws IOException {
        CarryConfigManager manager = new CarryConfigManager();
        Path path = Path.of("carrybabyanimals-save-default-test.json");
        Files.deleteIfExists(path);

        try {
            manager.saveDefault(path);

            assertTrue(Files.exists(path));
            assertFalse(Files.readString(path).contains("restrictToAllowedAnimals"));
            CarryConfig config = CarryConfigManager.parse(Files.readString(path));
            assertEquals(CarryConfig.defaultConfig(), config);
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    void saveDefaultIncludesSupportedAnimalCommentAndStillLoads() throws IOException {
        CarryConfigManager manager = new CarryConfigManager();
        Path path = Path.of("carrybabyanimals-save-default-comment-test.json");
        Files.deleteIfExists(path);

        try {
            manager.saveDefault(path);

            String saved = Files.readString(path);
            assertTrue(saved.contains("// Supported animal names:"));
            assertTrue(saved.contains("cow"));
            assertTrue(saved.contains("trader_llama"));
            assertTrue(saved.contains("dog"));

            CarryConfigManager loaded = new CarryConfigManager();
            loaded.load(path);
            assertEquals(CarryConfig.defaultConfig(), loaded.config());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private static CarryConfigManager loadConfig(String json) throws IOException {
        CarryConfigManager manager = new CarryConfigManager();
        Path path = Path.of("carrybabyanimals-load-test.json");
        Files.deleteIfExists(path);

        try {
            Files.writeString(path, json);
            manager.load(path);
            return manager;
        } finally {
            Files.deleteIfExists(path);
        }
    }
}
