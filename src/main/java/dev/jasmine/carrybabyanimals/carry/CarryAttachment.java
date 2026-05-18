package dev.jasmine.carrybabyanimals.carry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class CarryAttachment {
    private static final double[] FRONT_DISTANCES = {1.25D, 1.75D, 0.75D, 2.25D};
    private static final double[] SIDE_OFFSETS = {0.0D, 0.5D, -0.5D, 1.0D, -1.0D};
    private static final int[] Y_OFFSETS = {0, 1, -1, 2, -2};

    public boolean attach(ServerPlayer carrier, Entity baby) {
        return baby.startRiding(carrier, true, true);
    }

    public void dropInFront(ServerPlayer carrier, Entity baby) {
        Vec3 target = chooseDropPosition(carrier, baby);
        baby.stopRiding();
        baby.snapTo(target.x, target.y, target.z, baby.getYRot(), baby.getXRot());
        if (carrier.level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunk(baby.blockPosition());
        }
    }

    private Vec3 chooseDropPosition(ServerPlayer carrier, Entity baby) {
        Vec3 forward = horizontalDirection(carrier.getLookAngle());
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        for (double distance : FRONT_DISTANCES) {
            for (double sideOffset : SIDE_OFFSETS) {
                for (int yOffset : Y_OFFSETS) {
                    Vec3 candidate = carrier.position()
                            .add(forward.scale(distance))
                            .add(side.scale(sideOffset))
                            .add(0.0D, yOffset, 0.0D);
                    Optional<Vec3> safePosition = safeBottomPosition(carrier.level(), baby, candidate);
                    if (safePosition.isPresent()) {
                        return safePosition.get();
                    }
                }
            }
        }
        return safeBottomPosition(carrier.level(), baby, carrier.position())
                .or(() -> safeBottomPosition(carrier.level(), baby, baby.position()))
                .orElse(baby.position());
    }

    private Optional<Vec3> safeBottomPosition(Level level, Entity baby, Vec3 candidate) {
        BlockPos feet = BlockPos.containing(candidate.x, candidate.y, candidate.z);
        Vec3 bottomCenter = new Vec3(candidate.x, feet.getY(), candidate.z);
        if (!hasSafeFloor(level, baby, feet) || hasFluid(level, baby, bottomCenter)) {
            return Optional.empty();
        }
        if (!level.noCollision(baby, boundingBoxAt(baby, bottomCenter))) {
            return Optional.empty();
        }
        return Optional.of(bottomCenter);
    }

    private boolean hasSafeFloor(Level level, Entity baby, BlockPos feet) {
        BlockPos floor = feet.below();
        return level.isInWorldBounds(feet)
                && level.isInWorldBounds(floor)
                && level.getBlockState(floor).entityCanStandOn(level, floor, baby);
    }

    private boolean hasFluid(Level level, Entity baby, Vec3 bottomCenter) {
        AABB bounds = boundingBoxAt(baby, bottomCenter).deflate(1.0E-5D);
        for (BlockPos pos : BlockPos.betweenClosed(bounds)) {
            if (!level.getFluidState(pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private AABB boundingBoxAt(Entity baby, Vec3 bottomCenter) {
        double width = Math.max(0.1D, baby.getBbWidth());
        double height = Math.max(0.1D, baby.getBbHeight());
        return new AABB(
                bottomCenter.x - width / 2.0D,
                bottomCenter.y,
                bottomCenter.z - width / 2.0D,
                bottomCenter.x + width / 2.0D,
                bottomCenter.y + height,
                bottomCenter.z + width / 2.0D
        );
    }

    private Vec3 horizontalDirection(Vec3 look) {
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return horizontal.normalize();
    }
}
