package dev.jasmine.carrybabyanimals.reunion;

import java.util.List;

public final class ParentReunionMessageCatalog {
    private static final List<String> MESSAGES = List.of(
            "%s found family nearby.",
            "%s settled near family.",
            "%s is back near family.",
            "%s is close to family again."
    );

    public String message(String babyName, int variantIndex) {
        String template = MESSAGES.get(Math.floorMod(variantIndex, MESSAGES.size()));
        return template.formatted(capitalizeStartingBaby(babyName));
    }

    private static String capitalizeStartingBaby(String babyName) {
        if (babyName.startsWith("baby ")) {
            return "Baby " + babyName.substring("baby ".length());
        }
        return babyName;
    }
}
