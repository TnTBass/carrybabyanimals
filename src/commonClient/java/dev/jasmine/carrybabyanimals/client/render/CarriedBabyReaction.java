package dev.jasmine.carrybabyanimals.client.render;

public record CarriedBabyReaction(
        CarriedBabyReactionType type,
        int durationTicks,
        double amplitude,
        double sideOffset,
        double verticalOffset,
        double pitchDegrees,
        double yawDegrees,
        double rollDegrees,
        boolean sleepyEligible
) {
    public CarriedBabyReaction {
        type = type == null ? CarriedBabyReactionType.GENERIC_SETTLE : type;
        durationTicks = Math.max(8, Math.min(40, durationTicks));
        amplitude = Math.max(0.0D, Math.min(1.0D, amplitude));
        sideOffset = clamp(sideOffset, -0.08D, 0.08D);
        verticalOffset = clamp(verticalOffset, -0.08D, 0.08D);
        pitchDegrees = clamp(pitchDegrees, -18.0D, 18.0D);
        yawDegrees = clamp(yawDegrees, -18.0D, 18.0D);
        rollDegrees = clamp(rollDegrees, -18.0D, 18.0D);
    }

    CarriedBabyReaction withIntensity(double intensity) {
        double clampedIntensity = Math.max(0.0D, Math.min(1.0D, intensity));
        return new CarriedBabyReaction(
                type,
                durationTicks,
                amplitude * clampedIntensity,
                sideOffset,
                verticalOffset,
                pitchDegrees,
                yawDegrees,
                rollDegrees,
                sleepyEligible
        );
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
