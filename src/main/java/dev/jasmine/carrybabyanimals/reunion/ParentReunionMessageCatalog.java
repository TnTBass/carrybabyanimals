package dev.jasmine.carrybabyanimals.reunion;

import java.util.List;

public final class ParentReunionMessageCatalog {
    private static final List<String> MESSAGES = List.of(
            "%s found family nearby.",
            "%s is back with a grown-up friend.",
            "%s has company again."
    );

    public String message(String babyName, int variantIndex) {
        String template = MESSAGES.get(Math.floorMod(variantIndex, MESSAGES.size()));
        return template.formatted(babyName);
    }
}
