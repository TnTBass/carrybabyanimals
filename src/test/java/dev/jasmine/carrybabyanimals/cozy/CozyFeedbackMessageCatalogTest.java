package dev.jasmine.carrybabyanimals.cozy;

import dev.jasmine.carrybabyanimals.config.CarryConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CozyFeedbackMessageCatalogTest {
    @Test
    void petMessagesUseUnnamedBabyTypeVariants() {
        CozyFeedbackMessageCatalog catalog = new CozyFeedbackMessageCatalog();

        assertEquals("Baby Pig loves you.", catalog.petMessage("Pig", false, CarryConfig.defaultConfig(), 0));
        assertEquals("Baby Pig snuggles closer.", catalog.petMessage("Pig", false, CarryConfig.defaultConfig(), 1));
        assertEquals("Baby Pig makes a happy little sound.", catalog.petMessage("Pig", false, CarryConfig.defaultConfig(), 2));
    }

    @Test
    void petMessagesUseCustomNameWhenNameAwareMessagesEnabled() {
        CozyFeedbackMessageCatalog catalog = new CozyFeedbackMessageCatalog();

        assertEquals("Shelly snuggles closer.", catalog.petMessage("Shelly", true, CarryConfig.defaultConfig(), 1));
    }

    @Test
    void disabledNameAwareMessagesUseBabyTypeStyle() {
        CarryConfig config = new CarryConfig(List.of(), List.of(), false, 20, false, true, true, 160, 360, true, false, true, true, 1200, 600, 200);
        CozyFeedbackMessageCatalog catalog = new CozyFeedbackMessageCatalog();

        assertEquals("Baby Shelly snuggles closer.", catalog.petMessage("Shelly", true, config, 1));
    }

    @Test
    void disabledVariantMessagesUseExistingSinglePetMessage() {
        CarryConfig config = new CarryConfig(List.of(), List.of(), false, 20, false, true, true, 160, 360, false, true, true, true, 1200, 600, 200);
        CozyFeedbackMessageCatalog catalog = new CozyFeedbackMessageCatalog();

        assertEquals("Shelly loves you.", catalog.petMessage("Shelly", true, config, 2));
    }

    @Test
    void sleepyMessagesUseConfiguredNameRules() {
        CozyFeedbackMessageCatalog catalog = new CozyFeedbackMessageCatalog();

        assertEquals("Baby Pig is getting sleepy.", catalog.sleepyMessage("Pig", false, CarryConfig.defaultConfig(), 0));
        assertEquals("Shelly settles into your arms.", catalog.sleepyMessage("Shelly", true, CarryConfig.defaultConfig(), 1));
    }
}
