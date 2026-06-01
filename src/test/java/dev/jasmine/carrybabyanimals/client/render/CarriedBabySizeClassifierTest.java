package dev.jasmine.carrybabyanimals.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CarriedBabySizeClassifierTest {
    @Test
    void classifiesSmallMediumTallAndBulkyByDimensions() {
        assertEquals(CarriedBabySizeBucket.SMALL, CarriedBabySizeClassifier.classify("modded:tiny_bird", 0.45D, 0.42D));
        assertEquals(CarriedBabySizeBucket.MEDIUM, CarriedBabySizeClassifier.classify("modded:calf", 0.8D, 0.55D));
        assertEquals(CarriedBabySizeBucket.TALL, CarriedBabySizeClassifier.classify("modded:foal", 1.05D, 0.55D));
        assertEquals(CarriedBabySizeBucket.BULKY, CarriedBabySizeClassifier.classify("modded:wooly_baby", 0.8D, 0.9D));
    }

    @Test
    void classifiesHorseCamelAndLlamaAsTallOverrides() {
        assertEquals(CarriedBabySizeBucket.TALL, CarriedBabySizeClassifier.classify("minecraft:horse", 0.8D, 0.55D));
        assertEquals(CarriedBabySizeBucket.TALL, CarriedBabySizeClassifier.classify("minecraft:camel", 0.8D, 0.55D));
        assertEquals(CarriedBabySizeBucket.TALL, CarriedBabySizeClassifier.classify("minecraft:llama", 0.8D, 0.55D));
        assertEquals(CarriedBabySizeBucket.TALL, CarriedBabySizeClassifier.classify("minecraft:trader_llama", 0.8D, 0.55D));
        assertEquals(CarriedBabySizeBucket.TALL, CarriedBabySizeClassifier.classify("minecraft:donkey", 0.8D, 0.55D));
        assertEquals(CarriedBabySizeBucket.TALL, CarriedBabySizeClassifier.classify("minecraft:mule", 0.8D, 0.55D));
    }

    @Test
    void classifiesPandaAsBulkyOverride() {
        assertEquals(CarriedBabySizeBucket.BULKY, CarriedBabySizeClassifier.classify("minecraft:panda", 0.55D, 0.55D));
    }

    @Test
    void classifiesChickenRabbitFoxAndTurtleAsSafeSpecificOverrides() {
        assertEquals(CarriedBabySizeBucket.SMALL, CarriedBabySizeClassifier.classify("minecraft:chicken", 0.7D, 0.7D));
        assertEquals(CarriedBabySizeBucket.SMALL, CarriedBabySizeClassifier.classify("minecraft:rabbit", 0.7D, 0.7D));
        assertEquals(CarriedBabySizeBucket.MEDIUM, CarriedBabySizeClassifier.classify("minecraft:fox", 0.45D, 0.45D));
        assertEquals(CarriedBabySizeBucket.MEDIUM, CarriedBabySizeClassifier.classify("minecraft:turtle", 0.35D, 0.84D));
        assertEquals(CarriedBabySizeBucket.BULKY, CarriedBabySizeClassifier.classify("minecraft:turtle", 0.35D, 0.85D));
    }

    @Test
    void fallsBackToDimensionThresholdsForUnknownEntityIds() {
        assertEquals(CarriedBabySizeBucket.SMALL, CarriedBabySizeClassifier.classify("modded:small", 0.55D, 0.55D));
        assertEquals(CarriedBabySizeBucket.MEDIUM, CarriedBabySizeClassifier.classify("modded:medium", 0.7D, 0.65D));
        assertEquals(CarriedBabySizeBucket.TALL, CarriedBabySizeClassifier.classify("modded:tall", 1.2D, 0.95D));
        assertEquals(CarriedBabySizeBucket.BULKY, CarriedBabySizeClassifier.classify("modded:wide", 0.8D, 0.95D));
    }
}
