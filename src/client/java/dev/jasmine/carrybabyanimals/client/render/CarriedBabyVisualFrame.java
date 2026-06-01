package dev.jasmine.carrybabyanimals.client.render;

import net.minecraft.world.phys.Vec3;

public record CarriedBabyVisualFrame(
        Vec3 position,
        boolean suppressForLocalFirstPerson,
        double yawDegrees,
        double pitchDegrees,
        double rollDegrees
) {
    public static CarriedBabyVisualFrame evaluate(
            CarriedBabyPlacement.PlacementResult placement,
            CarriedBabyReaction reaction,
            long reactionStartTick,
            long currentTick,
            boolean sleepyVisual,
            boolean rendererSpecificPoseSafe
    ) {
        CarriedBabyPlacement.PlacementResult base = placement == null
                ? new CarriedBabyPlacement.PlacementResult(Vec3.ZERO, false, 0.0D, 0.0D, 0.0D)
                : placement;
        CarriedBabyReaction descriptor = reaction == null
                ? CarriedBabyReactionRegistry.fallback(1.0D)
                : reaction;
        long elapsedTicks = Math.max(0L, currentTick - reactionStartTick);
        if (elapsedTicks >= descriptor.durationTicks()) {
            return fromPlacement(base);
        }
        if (sleepyVisual && (!descriptor.sleepyEligible() || !rendererSpecificPoseSafe)) {
            return fromPlacement(base);
        }

        double progress = elapsedTicks / (double) Math.max(1, descriptor.durationTicks());
        double wave = Math.sin(progress * Math.PI);
        double sleepyScale = sleepyVisual ? 0.35D : 1.0D;
        double amount = descriptor.amplitude() * wave * sleepyScale;
        Vec3 position = base.position().add(
                descriptor.sideOffset() * amount,
                descriptor.verticalOffset() * amount,
                0.0D
        );
        return new CarriedBabyVisualFrame(
                position,
                base.suppressForLocalFirstPerson(),
                base.yawDegrees() + descriptor.yawDegrees() * amount,
                base.pitchDegrees() + descriptor.pitchDegrees() * amount,
                base.rollDegrees() + descriptor.rollDegrees() * amount
        );
    }

    public static CarriedBabyVisualFrame fromPlacement(CarriedBabyPlacement.PlacementResult placement) {
        return new CarriedBabyVisualFrame(
                placement.position(),
                placement.suppressForLocalFirstPerson(),
                placement.yawDegrees(),
                placement.pitchDegrees(),
                placement.rollDegrees()
        );
    }
}
