package dev.jasmine.carrybabyanimals.cozy;

import java.util.Optional;

record CozyFeedbackDecision(boolean playIdleSound, Optional<String> sleepyMessage, boolean spawnSleepyParticles) {
    static CozyFeedbackDecision none() {
        return new CozyFeedbackDecision(false, Optional.empty(), false);
    }
}
