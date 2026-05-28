package dev.jasmine.carrybabyanimals.nursery;

import java.util.Optional;

public record NurserySafetyDecision(Optional<NurseryHazard> hazard) {
    public NurserySafetyDecision {
        hazard = hazard == null ? Optional.empty() : hazard;
    }

    public boolean allowed() {
        return hazard.isEmpty();
    }

    public static NurserySafetyDecision allow() {
        return new NurserySafetyDecision(Optional.empty());
    }

    public static NurserySafetyDecision refuse(NurseryHazard hazard) {
        return new NurserySafetyDecision(Optional.of(hazard));
    }
}
