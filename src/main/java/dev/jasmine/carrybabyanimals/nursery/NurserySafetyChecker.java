package dev.jasmine.carrybabyanimals.nursery;

import dev.jasmine.carrybabyanimals.config.CarryConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class NurserySafetyChecker {
    public NurserySafetyDecision evaluate(Level level, Entity baby, Vec3 bottomCenter, CarryConfig config, boolean bypass) {
        return evaluate(new LevelWorldAccess(level, baby, bottomCenter), BlockPos.containing(bottomCenter), config, bypass);
    }

    public NurserySafetyDecision evaluate(WorldAccess world, BlockPos feet, CarryConfig config, boolean bypass) {
        if (bypass || !config.nurseryModeEnabled()) {
            return NurserySafetyDecision.allow();
        }
        if (config.nurseryBlockLava() && hasLavaNearFeet(world, feet)) {
            return NurserySafetyDecision.refuse(NurseryHazard.LAVA);
        }
        if (config.nurseryBlockFire() && world.hasFireHazard(feet)) {
            return NurserySafetyDecision.refuse(NurseryHazard.FIRE);
        }
        if (config.nurseryBlockCactusAndDamage() && world.hasDamagingHazard(feet)) {
            return NurserySafetyDecision.refuse(NurseryHazard.CACTUS_OR_DAMAGE);
        }
        if (config.nurseryBlockSuffocation() && !world.hasCollisionSpace(feet)) {
            return NurserySafetyDecision.refuse(NurseryHazard.SUFFOCATION);
        }
        if (config.nurseryBlockDangerousFalls()
                && !world.hasSafeFloor(feet)
                && world.openFallDistance(feet, config.nurseryDangerousFallDistanceBlocks())
                >= config.nurseryDangerousFallDistanceBlocks()) {
            return NurserySafetyDecision.refuse(NurseryHazard.DANGEROUS_FALL);
        }
        return NurserySafetyDecision.allow();
    }

    private boolean hasLavaNearFeet(WorldAccess world, BlockPos feet) {
        return world.hasLava(feet)
                || world.hasLava(feet.north())
                || world.hasLava(feet.south())
                || world.hasLava(feet.east())
                || world.hasLava(feet.west())
                || world.hasLava(feet.below());
    }

    public interface WorldAccess {
        boolean hasLava(BlockPos pos);

        boolean hasFireHazard(BlockPos pos);

        boolean hasDamagingHazard(BlockPos pos);

        boolean hasCollisionSpace(BlockPos feet);

        boolean hasSafeFloor(BlockPos feet);

        int openFallDistance(BlockPos feet, int threshold);
    }

    private static final class LevelWorldAccess implements WorldAccess {
        private final Level level;
        private final Entity baby;
        private final AABB bounds;

        private LevelWorldAccess(Level level, Entity baby, Vec3 bottomCenter) {
            this.level = level;
            this.baby = baby;
            this.bounds = boundingBoxAt(baby, bottomCenter).deflate(1.0E-5D);
        }

        @Override
        public boolean hasLava(BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return state.is(Blocks.LAVA) || level.getFluidState(pos).is(FluidTags.LAVA);
        }

        @Override
        public boolean hasFireHazard(BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return state.is(Blocks.FIRE)
                    || state.is(Blocks.SOUL_FIRE)
                    || state.is(Blocks.CAMPFIRE)
                    || state.is(Blocks.SOUL_CAMPFIRE)
                    || state.is(Blocks.MAGMA_BLOCK);
        }

        @Override
        public boolean hasDamagingHazard(BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return state.is(Blocks.CACTUS)
                    || state.is(Blocks.SWEET_BERRY_BUSH)
                    || state.is(Blocks.POINTED_DRIPSTONE)
                    || state.is(Blocks.WITHER_ROSE)
                    || state.is(Blocks.POWDER_SNOW);
        }

        @Override
        public boolean hasCollisionSpace(BlockPos feet) {
            return level.noCollision(baby, bounds);
        }

        @Override
        public boolean hasSafeFloor(BlockPos feet) {
            BlockPos floor = feet.below();
            return level.isInWorldBounds(feet)
                    && level.isInWorldBounds(floor)
                    && level.getBlockState(floor).entityCanStandOn(level, floor, baby);
        }

        @Override
        public int openFallDistance(BlockPos feet, int threshold) {
            for (int distance = 1; distance <= threshold; distance++) {
                BlockPos floor = feet.below(distance);
                if (!level.isInWorldBounds(floor) || !level.getFluidState(floor).isEmpty()) {
                    return distance;
                }
                if (level.getBlockState(floor).entityCanStandOn(level, floor, baby)) {
                    return distance - 1;
                }
            }
            return threshold;
        }

        private static AABB boundingBoxAt(Entity baby, Vec3 bottomCenter) {
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
    }
}
