package dev.jasmine.carrybabyanimals.nursery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NurseryMessageCatalogTest {
    private final NurseryMessageCatalog catalog = new NurseryMessageCatalog();

    @Test
    void lavaMessageNamesUnnamedBaby() {
        String message = catalog.message(NurseryHazard.LAVA, "baby Pig", 0);

        assertTrue(message.contains("baby Pig"));
        assertTrue(message.toLowerCase().contains("lava"));
    }

    @Test
    void fallMessageNamesCustomBaby() {
        String message = catalog.message(NurseryHazard.DANGEROUS_FALL, "Shelly", 0);

        assertTrue(message.contains("Shelly"));
        assertTrue(message.toLowerCase().contains("drop"));
    }

    @Test
    void lavaMessagesHaveVariants() {
        String first = catalog.message(NurseryHazard.LAVA, "baby Pig", 0);
        String second = catalog.message(NurseryHazard.LAVA, "baby Pig", 1);

        assertNotEquals(first, second);
    }
}
