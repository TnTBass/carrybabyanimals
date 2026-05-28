package dev.jasmine.carrybabyanimals.reunion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ParentReunionMessageCatalogTest {
    @Test
    void messageIncludesBabyName() {
        ParentReunionMessageCatalog catalog = new ParentReunionMessageCatalog();

        assertEquals("Baby Pig found family nearby.", catalog.message("baby Pig", 0));
    }

    @Test
    void messagesUseFamilyWording() {
        ParentReunionMessageCatalog catalog = new ParentReunionMessageCatalog();

        assertEquals("Baby Pig found family nearby.", catalog.message("baby Pig", 0));
        assertEquals("Baby Pig settled near family.", catalog.message("baby Pig", 1));
        assertEquals("Baby Pig is back near family.", catalog.message("baby Pig", 2));
        assertEquals("Baby Pig is close to family again.", catalog.message("baby Pig", 3));
    }

    @Test
    void customNamesKeepTheirCapitalization() {
        ParentReunionMessageCatalog catalog = new ParentReunionMessageCatalog();

        assertEquals("pumpkin found family nearby.", catalog.message("pumpkin", 0));
    }
}
