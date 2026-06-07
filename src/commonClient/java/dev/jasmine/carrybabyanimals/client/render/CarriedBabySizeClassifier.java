package dev.jasmine.carrybabyanimals.client.render;

import java.util.Locale;
import java.util.Set;

public final class CarriedBabySizeClassifier {
    private static final Set<String> TALL_OVERRIDES = Set.of(
            "minecraft:horse",
            "minecraft:camel",
            "minecraft:llama",
            "minecraft:trader_llama",
            "minecraft:donkey",
            "minecraft:mule"
    );

    private CarriedBabySizeClassifier() {
    }

    public static CarriedBabySizeBucket classify(String entityTypeId, double babyHeight, double babyWidth) {
        String normalizedId = normalize(entityTypeId);
        if (TALL_OVERRIDES.contains(normalizedId)) {
            return CarriedBabySizeBucket.TALL;
        }
        if ("minecraft:panda".equals(normalizedId)) {
            return CarriedBabySizeBucket.BULKY;
        }
        if ("minecraft:turtle".equals(normalizedId)) {
            return babyWidth >= 0.85D ? CarriedBabySizeBucket.BULKY : CarriedBabySizeBucket.MEDIUM;
        }
        if ("minecraft:chicken".equals(normalizedId) || "minecraft:rabbit".equals(normalizedId)) {
            return CarriedBabySizeBucket.SMALL;
        }
        if ("minecraft:fox".equals(normalizedId)) {
            return CarriedBabySizeBucket.MEDIUM;
        }

        if (babyHeight >= 1.05D) {
            return CarriedBabySizeBucket.TALL;
        }
        if (babyWidth >= 0.9D) {
            return CarriedBabySizeBucket.BULKY;
        }
        if (babyHeight <= 0.55D && babyWidth <= 0.55D) {
            return CarriedBabySizeBucket.SMALL;
        }
        return CarriedBabySizeBucket.MEDIUM;
    }

    private static String normalize(String entityTypeId) {
        if (entityTypeId == null) {
            return "";
        }
        return entityTypeId.trim().toLowerCase(Locale.ROOT);
    }
}
