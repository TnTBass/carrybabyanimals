package dev.jasmine.carrybabyanimals.nursery;

import java.util.List;
import java.util.Map;

public final class NurseryMessageCatalog {
    private static final Map<NurseryHazard, List<String>> MESSAGES = Map.of(
            NurseryHazard.LAVA,
            List.of("Not in lava, %s. Nice try.", "%s is staying far away from lava."),
            NurseryHazard.FIRE,
            List.of("%s is not a campfire snack.", "Too hot for %s. Try somewhere cozy."),
            NurseryHazard.CACTUS_OR_DAMAGE,
            List.of("%s vetoes the ouchy block.", "No prickly parking for %s."),
            NurseryHazard.SUFFOCATION,
            List.of("%s needs a little breathing room.", "That spot is too squishy for %s."),
            NurseryHazard.DANGEROUS_FALL,
            List.of("%s is staying up here. That drop is a nope.", "Tiny legs, big drop. %s stays with you.")
    );

    public String message(NurseryHazard hazard, String displayName, int variantIndex) {
        List<String> variants = MESSAGES.getOrDefault(hazard, MESSAGES.get(NurseryHazard.SUFFOCATION));
        int index = Math.floorMod(variantIndex, variants.size());
        return variants.get(index).formatted(displayName);
    }
}
