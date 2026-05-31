package dev.jasmine.carrybabyanimals.client.render;

import net.minecraft.world.phys.Vec3;

public final class CarriedBabyPlacement {
    private CarriedBabyPlacement() {
    }

    public static Vec3 heldPosition(
            Vec3 carrierPosition,
            Vec3 horizontalForward,
            double carrierHeight,
            double babyHeight,
            boolean leftMainArm
    ) {
        return heldPosition(carrierPosition, horizontalForward, carrierHeight, babyHeight, leftMainArm, 0.0D);
    }

    public static Vec3 heldPosition(
            Vec3 carrierPosition,
            Vec3 horizontalForward,
            double carrierHeight,
            double babyHeight,
            boolean leftMainArm,
            double animationTicks
    ) {
        // Callers may pass a fallback direction that is not normalized yet.
        Vec3 forward = horizontalForward.normalize();
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x).normalize();
        double armSide = leftMainArm ? -1.0D : 1.0D;
        double sizeFactor = clamp(babyHeight / Math.max(0.1D, carrierHeight), 0.15D, 0.65D);
        double forwardDistance = 0.52D - (sizeFactor * 0.18D);
        double carriedHeight = Math.min(1.16D, carrierHeight * (0.65D - sizeFactor * 0.06D));
        double babyLowering = Math.min(0.2D, babyHeight * 0.18D);
        double gentleBob = Math.sin(animationTicks * 0.25D) * 0.018D;

        return carrierPosition
                .add(forward.scale(forwardDistance))
                .add(right.scale(0.08D * armSide))
                .add(0.0D, carriedHeight - babyLowering + gentleBob, 0.0D);
    }

    public static Vec3 petFeedbackPosition(Vec3 heldPosition, double babyHeight) {
        return heldPosition.add(0.0D, babyHeight * 0.75D, 0.0D);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
