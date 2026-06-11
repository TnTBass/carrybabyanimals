package dev.jasmine.carrybabyanimals.neoforge;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;

import java.nio.file.Path;

final class NeoForgePaths {
    private NeoForgePaths() {
    }

    static Path configPath(Path configDirectory) {
        return configDirectory.resolve(CarryBabyAnimals.MOD_ID + ".json");
    }
}
