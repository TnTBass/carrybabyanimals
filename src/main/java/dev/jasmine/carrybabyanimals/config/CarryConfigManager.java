package dev.jasmine.carrybabyanimals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CarryConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private CarryConfig config = CarryConfig.defaultConfig();

    public CarryConfig config() {
        return config;
    }

    public void load(Path path) throws IOException {
        if (Files.notExists(path)) {
            saveDefault(path);
        }
        try {
            config = parse(Files.readString(path));
        } catch (JsonParseException | IllegalStateException exception) {
            throw new IOException("Invalid Carry Baby Animals config: " + path, exception);
        }
    }

    public void saveDefault(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, defaultConfigText());
    }

    private static String defaultConfigText() {
        String supportedNames = String.join(", ", AnimalAliasRegistry.createDefault().aliases().keySet());
        return """
            // Supported animal names: %s
            %s
            """.formatted(supportedNames, GSON.toJson(RawConfig.from(CarryConfig.defaultConfig())));
    }

    public static CarryConfig parse(String json) {
        RawConfig raw = GSON.fromJson(json, RawConfig.class);
        if (raw == null) {
            return CarryConfig.defaultConfig();
        }
        List<String> allowedAnimals = sanitizeNames(raw.allowedAnimals);
        int idleSoundMinTicks = positiveOrDefault(raw.carriedIdleSoundMinTicks, 160);
        int idleSoundMaxTicks = positiveOrDefault(raw.carriedIdleSoundMaxTicks, 360);
        if (idleSoundMaxTicks < idleSoundMinTicks) {
            idleSoundMaxTicks = idleSoundMinTicks;
        }
        return new CarryConfig(
                allowedAnimals,
                sanitizeNames(raw.blockedAnimals),
                raw.allowCarryingOtherPlayersTamedAnimals,
                raw.pettingCooldownTicks <= 0 ? 20 : raw.pettingCooldownTicks,
                !allowedAnimals.isEmpty(),
                enabledByDefault(raw.cozyFeedbackEnabled),
                enabledByDefault(raw.carriedIdleSoundsEnabled),
                idleSoundMinTicks,
                idleSoundMaxTicks,
                enabledByDefault(raw.pettingMessagesEnabled),
                enabledByDefault(raw.nameAwareMessagesEnabled),
                enabledByDefault(raw.cozyParticlesEnabled),
                enabledByDefault(raw.sleepyBabiesEnabled),
                positiveOrDefault(raw.sleepyAfterTicks, 1200),
                positiveOrDefault(raw.sleepyMessageCooldownTicks, 600),
                positiveOrDefault(raw.sleepyParticleCooldownTicks, 200)
        );
    }

    public void filterAndLogUnknownAnimalNames(AnimalAliasRegistry registry, Logger logger) {
        UnknownAnimalNames unknownNames = unknownAnimalNames(config, registry);
        for (String name : unknownNames.allowedAnimals()) {
            logger.warn("Unknown allowed animal name in Carry Baby Animals config: {}", name);
        }
        for (String name : unknownNames.blockedAnimals()) {
            logger.warn("Unknown blocked animal name in Carry Baby Animals config: {}", name);
        }
        config = new CarryConfig(
                knownNames(config.allowedAnimals(), registry),
                knownNames(config.blockedAnimals(), registry),
                config.allowCarryingOtherPlayersTamedAnimals(),
                config.pettingCooldownTicks(),
                config.restrictToAllowedAnimals(),
                config.cozyFeedbackEnabled(),
                config.carriedIdleSoundsEnabled(),
                config.carriedIdleSoundMinTicks(),
                config.carriedIdleSoundMaxTicks(),
                config.pettingMessagesEnabled(),
                config.nameAwareMessagesEnabled(),
                config.cozyParticlesEnabled(),
                config.sleepyBabiesEnabled(),
                config.sleepyAfterTicks(),
                config.sleepyMessageCooldownTicks(),
                config.sleepyParticleCooldownTicks()
        );
    }

    public static UnknownAnimalNames unknownAnimalNames(CarryConfig config, AnimalAliasRegistry registry) {
        List<String> unknownAllowed = unknownNames(config.allowedAnimals(), registry);
        List<String> unknownBlocked = unknownNames(config.blockedAnimals(), registry);
        return new UnknownAnimalNames(unknownAllowed, unknownBlocked);
    }

    private static List<String> unknownNames(List<String> names, AnimalAliasRegistry registry) {
        List<String> unknown = new ArrayList<>();
        for (String name : names) {
            if (registry.resolve(name).isEmpty()) {
                unknown.add(name);
            }
        }
        return List.copyOf(unknown);
    }

    private static List<String> knownNames(List<String> names, AnimalAliasRegistry registry) {
        List<String> known = new ArrayList<>();
        for (String name : names) {
            if (registry.resolve(name).isPresent()) {
                known.add(name);
            }
        }
        return List.copyOf(known);
    }

    private static List<String> sanitizeNames(List<String> names) {
        if (names == null) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        for (String name : names) {
            if (name == null) {
                continue;
            }
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                sanitized.add(trimmed);
            }
        }
        return List.copyOf(sanitized);
    }

    private static boolean enabledByDefault(Boolean value) {
        return value == null || value;
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    public record UnknownAnimalNames(List<String> allowedAnimals, List<String> blockedAnimals) {
        public UnknownAnimalNames {
            allowedAnimals = allowedAnimals == null ? List.of() : List.copyOf(allowedAnimals);
            blockedAnimals = blockedAnimals == null ? List.of() : List.copyOf(blockedAnimals);
        }
    }

    private static final class RawConfig {
        List<String> allowedAnimals;
        List<String> blockedAnimals;
        boolean allowCarryingOtherPlayersTamedAnimals;
        int pettingCooldownTicks;
        Boolean cozyFeedbackEnabled;
        Boolean carriedIdleSoundsEnabled;
        Integer carriedIdleSoundMinTicks;
        Integer carriedIdleSoundMaxTicks;
        Boolean pettingMessagesEnabled;
        Boolean nameAwareMessagesEnabled;
        Boolean cozyParticlesEnabled;
        Boolean sleepyBabiesEnabled;
        Integer sleepyAfterTicks;
        Integer sleepyMessageCooldownTicks;
        Integer sleepyParticleCooldownTicks;

        static RawConfig from(CarryConfig config) {
            RawConfig raw = new RawConfig();
            raw.allowedAnimals = config.allowedAnimals();
            raw.blockedAnimals = config.blockedAnimals();
            raw.allowCarryingOtherPlayersTamedAnimals = config.allowCarryingOtherPlayersTamedAnimals();
            raw.pettingCooldownTicks = config.pettingCooldownTicks();
            raw.cozyFeedbackEnabled = config.cozyFeedbackEnabled();
            raw.carriedIdleSoundsEnabled = config.carriedIdleSoundsEnabled();
            raw.carriedIdleSoundMinTicks = config.carriedIdleSoundMinTicks();
            raw.carriedIdleSoundMaxTicks = config.carriedIdleSoundMaxTicks();
            raw.pettingMessagesEnabled = config.pettingMessagesEnabled();
            raw.nameAwareMessagesEnabled = config.nameAwareMessagesEnabled();
            raw.cozyParticlesEnabled = config.cozyParticlesEnabled();
            raw.sleepyBabiesEnabled = config.sleepyBabiesEnabled();
            raw.sleepyAfterTicks = config.sleepyAfterTicks();
            raw.sleepyMessageCooldownTicks = config.sleepyMessageCooldownTicks();
            raw.sleepyParticleCooldownTicks = config.sleepyParticleCooldownTicks();
            return raw;
        }
    }
}
