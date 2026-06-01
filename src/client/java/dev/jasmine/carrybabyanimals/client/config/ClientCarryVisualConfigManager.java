package dev.jasmine.carrybabyanimals.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.jasmine.carrybabyanimals.client.render.FirstPersonLargeBabyVisibilityMode;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClientCarryVisualConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ClientCarryVisualConfig config = ClientCarryVisualConfig.defaultConfig();

    private ClientCarryVisualConfigManager() {
    }

    public static ClientCarryVisualConfig config() {
        return config;
    }

    public static void load() throws IOException {
        load(configPath());
    }

    public static void load(Path path) throws IOException {
        if (Files.notExists(path)) {
            saveDefault(path);
        }
        try {
            config = parse(Files.readString(path));
        } catch (JsonParseException | IllegalStateException exception) {
            throw new IOException("Invalid Carry Baby Animals client visual config: " + path, exception);
        }
    }

    public static void saveDefault(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, defaultConfigText());
    }

    public static ClientCarryVisualConfig parse(String json) {
        RawConfig raw = GSON.fromJson(json, RawConfig.class);
        if (raw == null) {
            return ClientCarryVisualConfig.defaultConfig();
        }
        ClientCarryVisualConfig defaults = ClientCarryVisualConfig.defaultConfig();
        return new ClientCarryVisualConfig(
                enabledByDefault(raw.carriedBabyReactionsEnabled),
                enabledByDefault(raw.largeBabyTuckedPoseEnabled),
                visibilityModeOrDefault(raw.firstPersonLargeBabyVisibilityMode),
                enabledByDefault(raw.sleepyCarryVisualsEnabled),
                raw.animalReactionIntensity == null
                        ? defaults.animalReactionIntensity()
                        : raw.animalReactionIntensity,
                sanitizeEntityIds(raw.disabledCarriedReactionAnimals)
        );
    }

    private static String defaultConfigText() {
        return GSON.toJson(RawConfig.from(ClientCarryVisualConfig.defaultConfig()));
    }

    private static Path configPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("carrybabyanimals-client.json");
    }

    private static boolean enabledByDefault(Boolean value) {
        return value == null || value;
    }

    private static FirstPersonLargeBabyVisibilityMode visibilityModeOrDefault(String mode) {
        if (mode == null || mode.isBlank()) {
            return FirstPersonLargeBabyVisibilityMode.TUCKED;
        }
        try {
            return FirstPersonLargeBabyVisibilityMode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return FirstPersonLargeBabyVisibilityMode.TUCKED;
        }
    }

    private static List<String> sanitizeEntityIds(List<String> entityIds) {
        if (entityIds == null) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        for (String entityId : entityIds) {
            if (entityId == null) {
                continue;
            }
            String normalized = entityId.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                sanitized.add(normalized);
            }
        }
        return List.copyOf(sanitized);
    }

    private static final class RawConfig {
        Boolean carriedBabyReactionsEnabled;
        Boolean largeBabyTuckedPoseEnabled;
        String firstPersonLargeBabyVisibilityMode;
        Boolean sleepyCarryVisualsEnabled;
        Double animalReactionIntensity;
        List<String> disabledCarriedReactionAnimals;

        static RawConfig from(ClientCarryVisualConfig config) {
            RawConfig raw = new RawConfig();
            raw.carriedBabyReactionsEnabled = config.carriedBabyReactionsEnabled();
            raw.largeBabyTuckedPoseEnabled = config.largeBabyTuckedPoseEnabled();
            raw.firstPersonLargeBabyVisibilityMode = config.firstPersonLargeBabyVisibilityMode().name();
            raw.sleepyCarryVisualsEnabled = config.sleepyCarryVisualsEnabled();
            raw.animalReactionIntensity = config.animalReactionIntensity();
            raw.disabledCarriedReactionAnimals = config.disabledCarriedReactionAnimals();
            return raw;
        }
    }
}
