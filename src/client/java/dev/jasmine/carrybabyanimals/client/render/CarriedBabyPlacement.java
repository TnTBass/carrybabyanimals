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
        // Callers may pass a fallback direction that is not normalized yet.
        Vec3 forward = horizontalForward.normalize();
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x).normalize();
        double armSide = leftMainArm ? -1.0D : 1.0D;
        double carriedHeight = Math.min(1.15D, carrierHeight * 0.58D);
        double babyLowering = Math.min(0.12D, babyHeight * 0.12D);

        return carrierPosition
                .add(forward.scale(0.48D))
                .add(right.scale(0.08D * armSide))
                .add(0.0D, carriedHeight - babyLowering, 0.0D);
    }

    public static Vec3 petFeedbackPosition(Vec3 heldPosition, double babyHeight) {
        return heldPosition.add(0.0D, babyHeight * 0.75D, 0.0D);
    }
}
