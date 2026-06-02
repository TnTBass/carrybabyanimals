package dev.jasmine.carrybabyanimals.client.render;

import net.minecraft.world.phys.Vec3;

public final class CarriedBabyPlacement {
    private CarriedBabyPlacement() {
    }

    public record PlacementResult(
            Vec3 position,
            boolean suppressForLocalFirstPerson,
            double yawDegrees,
            double pitchDegrees,
            double rollDegrees
    ) {
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

    public static PlacementResult placement(
            Vec3 carrierPosition,
            Vec3 horizontalForward,
            double carrierHeight,
            double babyHeight,
            double babyWidth,
            boolean leftMainArm,
            double animationTicks,
            CarriedBabySizeBucket sizeBucket,
            boolean localFirstPerson,
            FirstPersonLargeBabyVisibilityMode visibilityMode
    ) {
        Vec3 base = heldPosition(carrierPosition, horizontalForward, carrierHeight, babyHeight, leftMainArm, animationTicks);
        Vec3 forward = horizontalForward.normalize();
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x).normalize();
        double armSide = leftMainArm ? -1.0D : 1.0D;
        CarriedBabySizeBucket bucket = sizeBucket == null ? CarriedBabySizeBucket.MEDIUM : sizeBucket;
        FirstPersonLargeBabyVisibilityMode mode = visibilityMode == null
                ? FirstPersonLargeBabyVisibilityMode.TUCKED
                : visibilityMode;

        Vec3 position = base;
        double yawDegrees = 0.0D;
        if (bucket == CarriedBabySizeBucket.TALL) {
            position = position
                    .add(right.scale(0.24D * armSide))
                    .add(forward.scale(-0.12D))
                    .add(0.0D, -0.24D, 0.0D);
            yawDegrees = 24.0D * armSide;
        } else if (bucket == CarriedBabySizeBucket.BULKY) {
            double currentForward = position.subtract(carrierPosition).dot(forward);
            double forwardAdjustment = Math.min(0.0D, 0.34D - currentForward);
            position = position
                    .add(right.scale(0.24D * armSide))
                    .add(forward.scale(forwardAdjustment))
                    .add(0.0D, -0.18D, 0.0D);
            yawDegrees = 18.0D * armSide;
        }

        boolean large = bucket == CarriedBabySizeBucket.TALL || bucket == CarriedBabySizeBucket.BULKY;
        boolean firstPersonSafetyPlacement = bucket == CarriedBabySizeBucket.MEDIUM || large;
        boolean suppress = false;
        if (localFirstPerson && bucket == CarriedBabySizeBucket.MEDIUM) {
            position = position
                    .add(right.scale(0.20D * armSide))
                    .add(forward.scale(-0.08D))
                    .add(0.0D, -0.12D, 0.0D);
        }
        if (localFirstPerson && firstPersonSafetyPlacement) {
            if (mode == FirstPersonLargeBabyVisibilityMode.LOWERED) {
                position = position.add(0.0D, -0.42D, 0.0D);
            } else if (mode == FirstPersonLargeBabyVisibilityMode.HIDE_WHEN_OBSTRUCTING) {
                suppress = obstructsFirstPersonCenter(position, carrierPosition, forward, right, babyWidth);
            }
        }

        return new PlacementResult(position, suppress, yawDegrees, 0.0D, 0.0D);
    }

    public static Vec3 petFeedbackPosition(Vec3 heldPosition, double babyHeight) {
        return heldPosition.add(0.0D, babyHeight * 0.75D, 0.0D);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean obstructsFirstPersonCenter(Vec3 position, Vec3 carrierPosition, Vec3 forward, Vec3 right, double babyWidth) {
        Vec3 offset = position.subtract(carrierPosition);
        double lateralOffset = Math.abs(offset.dot(right));
        double forwardOffset = offset.dot(forward);
        double nearestBodyEdgeToCenter = lateralOffset - (Math.max(0.0D, babyWidth) * 0.5D);
        return forwardOffset > 0.20D && nearestBodyEdgeToCenter < 0.16D;
    }
}
