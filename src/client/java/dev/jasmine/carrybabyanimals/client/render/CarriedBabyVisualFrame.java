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
        return evaluate(
                placement,
                reaction,
                reactionStartTick,
                currentTick,
                sleepyVisual ? CarriedBabySleepyVisualPhase.SLEEPY : CarriedBabySleepyVisualPhase.ALERT,
                rendererSpecificPoseSafe
        );
    }

    public static CarriedBabyVisualFrame evaluate(
            CarriedBabyPlacement.PlacementResult placement,
            CarriedBabyReaction reaction,
            long reactionStartTick,
            long currentTick,
            CarriedBabySleepyVisualPhase sleepyVisualPhase,
            boolean rendererSpecificPoseSafe
    ) {
        CarriedBabyPlacement.PlacementResult base = placement == null
                ? new CarriedBabyPlacement.PlacementResult(Vec3.ZERO, false, 0.0D, 0.0D, 0.0D)
                : placement;
        CarriedBabyReaction descriptor = reaction == null
                ? CarriedBabyReactionRegistry.fallback(1.0D)
                : reaction;
        CarriedBabySleepyVisualPhase phase = sleepyVisualPhase == null
                ? CarriedBabySleepyVisualPhase.ALERT
                : sleepyVisualPhase;
        CarriedBabyVisualFrame phaseBase = applySleepyPhase(base, phase, currentTick, rendererSpecificPoseSafe);
        long elapsedTicks = Math.max(0L, currentTick - reactionStartTick);
        if (elapsedTicks >= descriptor.durationTicks()) {
            return phaseBase;
        }
        if (phase != CarriedBabySleepyVisualPhase.ALERT && (!descriptor.sleepyEligible() || !rendererSpecificPoseSafe)) {
            return phaseBase;
        }

        double progress = elapsedTicks / (double) Math.max(1, descriptor.durationTicks());
        double wave = Math.sin(progress * Math.PI);
        double sleepyScale = switch (phase) {
            case ALERT -> 1.0D;
            case SLEEPY -> 0.35D;
            case ASLEEP -> 0.18D;
        };
        double amount = descriptor.amplitude() * wave * sleepyScale;
        Vec3 position = phaseBase.position().add(
                descriptor.sideOffset() * amount,
                descriptor.verticalOffset() * amount,
                0.0D
        );
        return new CarriedBabyVisualFrame(
                position,
                phaseBase.suppressForLocalFirstPerson(),
                phaseBase.yawDegrees() + descriptor.yawDegrees() * amount,
                phaseBase.pitchDegrees() + descriptor.pitchDegrees() * amount,
                phaseBase.rollDegrees() + descriptor.rollDegrees() * amount
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

    private static CarriedBabyVisualFrame applySleepyPhase(
            CarriedBabyPlacement.PlacementResult placement,
            CarriedBabySleepyVisualPhase phase,
            long currentTick,
            boolean rendererSpecificPoseSafe
    ) {
        if (phase == CarriedBabySleepyVisualPhase.ALERT) {
            return fromPlacement(placement);
        }
        double breathing = phase == CarriedBabySleepyVisualPhase.ASLEEP
                ? Math.sin(currentTick * 0.12D) * 0.006D
                : 0.0D;
        double lower = phase == CarriedBabySleepyVisualPhase.ASLEEP ? -0.15D : -0.10D;
        double pitch = rendererSpecificPoseSafe
                ? (phase == CarriedBabySleepyVisualPhase.ASLEEP ? -10.0D : -5.0D)
                : 0.0D;
        double roll = rendererSpecificPoseSafe
                ? (phase == CarriedBabySleepyVisualPhase.ASLEEP ? 3.0D : 1.5D)
                : 0.0D;
        return new CarriedBabyVisualFrame(
                placement.position().add(0.0D, lower + breathing, 0.0D),
                placement.suppressForLocalFirstPerson(),
                placement.yawDegrees(),
                placement.pitchDegrees() + pitch,
                placement.rollDegrees() + roll
        );
    }
}
