package dev.jasmine.carrybabyanimals.cozy;

import dev.jasmine.carrybabyanimals.config.CarryConfig;

public final class CozyFeedbackMessageCatalog {
    private static final String[] UNNAMED_PET_MESSAGES = {
            "Baby %s loves you.",
            "Baby %s snuggles closer.",
            "Baby %s makes a happy little sound."
    };
    private static final String[] NAMED_PET_MESSAGES = {
            "%s loves you.",
            "%s snuggles closer.",
            "%s makes a happy little sound."
    };
    private static final String[] UNNAMED_SLEEPY_MESSAGES = {
            "Baby %s is getting sleepy.",
            "Baby %s settles into your arms.",
            "Baby %s lets out a tiny yawn."
    };
    private static final String[] NAMED_SLEEPY_MESSAGES = {
            "%s is getting sleepy.",
            "%s settles into your arms.",
            "%s lets out a tiny yawn."
    };

    public String petMessage(String displayName, boolean hasCustomName, CarryConfig config, int variantIndex) {
        if (!config.pettingMessagesEnabled()) {
            String feedbackName = hasCustomName ? displayName : "Baby " + displayName;
            return feedbackName + " loves you.";
        }
        boolean useCustomName = hasCustomName && config.nameAwareMessagesEnabled();
        String[] messages = useCustomName ? NAMED_PET_MESSAGES : UNNAMED_PET_MESSAGES;
        return selected(messages, variantIndex).formatted(displayName);
    }

    public String sleepyMessage(String displayName, boolean hasCustomName, CarryConfig config, int variantIndex) {
        boolean useCustomName = hasCustomName && config.nameAwareMessagesEnabled();
        String[] messages = useCustomName ? NAMED_SLEEPY_MESSAGES : UNNAMED_SLEEPY_MESSAGES;
        return selected(messages, variantIndex).formatted(displayName);
    }

    public static String feedbackName(String displayName, boolean hasCustomName, boolean nameAwareMessagesEnabled) {
        return hasCustomName && nameAwareMessagesEnabled ? displayName : "Baby " + displayName;
    }

    private static String selected(String[] messages, int variantIndex) {
        return messages[Math.floorMod(variantIndex, messages.length)];
    }
}
