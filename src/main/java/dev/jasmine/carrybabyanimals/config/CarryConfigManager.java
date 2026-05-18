package dev.jasmine.carrybabyanimals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
        config = parse(Files.readString(path));
    }

    public void saveDefault(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, GSON.toJson(CarryConfig.defaultConfig()));
    }

    public static CarryConfig parse(String json) {
        RawConfig raw = GSON.fromJson(json, RawConfig.class);
        if (raw == null) {
            return CarryConfig.defaultConfig();
        }
        return new CarryConfig(
                sanitizeNames(raw.allowedAnimals),
                sanitizeNames(raw.blockedAnimals),
                raw.allowCarryingOtherPlayersTamedAnimals,
                raw.pettingCooldownTicks <= 0 ? 20 : raw.pettingCooldownTicks
        );
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

    private static final class RawConfig {
        List<String> allowedAnimals;
        List<String> blockedAnimals;
        boolean allowCarryingOtherPlayersTamedAnimals;
        int pettingCooldownTicks;
    }
}
