package dev.jasmine.carrybabyanimals.reunion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ParentReunionMessageCatalogTest {
    @Test
    void messageIncludesBabyName() {
        ParentReunionMessageCatalog catalog = new ParentReunionMessageCatalog();

        assertTrue(catalog.message("baby Pig", 0).contains("baby Pig"));
    }

    @Test
    void messagesVaryByIndex() {
        ParentReunionMessageCatalog catalog = new ParentReunionMessageCatalog();

        assertNotEquals(catalog.message("baby Pig", 0), catalog.message("baby Pig", 1));
        assertNotEquals(catalog.message("baby Pig", 1), catalog.message("baby Pig", 2));
    }
}
