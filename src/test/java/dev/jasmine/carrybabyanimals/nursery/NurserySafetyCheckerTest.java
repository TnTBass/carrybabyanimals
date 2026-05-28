package dev.jasmine.carrybabyanimals.nursery;

import dev.jasmine.carrybabyanimals.config.CarryConfig;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NurserySafetyCheckerTest {
    private final NurserySafetyChecker checker = new NurserySafetyChecker();

    @Test
    void disabledModeAllowsUnsafeWorld() {
        FakeWorld world = new FakeWorld().lava(BlockPos.ZERO);
        CarryConfig config = config(false, true, true, true, true, true, 4, true);

        assertTrue(checker.evaluate(world, BlockPos.ZERO, config, false).allowed());
    }

    @Test
    void bypassAllowsUnsafeWorld() {
        FakeWorld world = new FakeWorld().lava(BlockPos.ZERO);

        assertTrue(checker.evaluate(world, BlockPos.ZERO, CarryConfig.defaultConfig(), true).allowed());
    }

    @Test
    void lavaAtFeetRefusesWhenEnabled() {
        FakeWorld world = new FakeWorld().lava(BlockPos.ZERO);

        assertEquals(
                NurseryHazard.LAVA,
                checker.evaluate(world, BlockPos.ZERO, CarryConfig.defaultConfig(), false).hazard().orElseThrow()
        );
    }

    @Test
    void adjacentLavaRefusesWhenEnabled() {
        FakeWorld world = new FakeWorld().lava(BlockPos.ZERO.east());

        assertEquals(
                NurseryHazard.LAVA,
                checker.evaluate(world, BlockPos.ZERO, CarryConfig.defaultConfig(), false).hazard().orElseThrow()
        );
    }

    @Test
    void fireRefusesWhenEnabled() {
        FakeWorld world = new FakeWorld().fire(BlockPos.ZERO);

        assertEquals(
                NurseryHazard.FIRE,
                checker.evaluate(world, BlockPos.ZERO, CarryConfig.defaultConfig(), false).hazard().orElseThrow()
        );
    }

    @Test
    void damagingBlockRefusesWhenEnabled() {
        FakeWorld world = new FakeWorld().damaging(BlockPos.ZERO);

        assertEquals(
                NurseryHazard.CACTUS_OR_DAMAGE,
                checker.evaluate(world, BlockPos.ZERO, CarryConfig.defaultConfig(), false).hazard().orElseThrow()
        );
    }

    @Test
    void collisionFailureRefusesAsSuffocation() {
        FakeWorld world = new FakeWorld().collisionSafe(false);

        assertEquals(
                NurseryHazard.SUFFOCATION,
                checker.evaluate(world, BlockPos.ZERO, CarryConfig.defaultConfig(), false).hazard().orElseThrow()
        );
    }

    @Test
    void fallAtThresholdRefuses() {
        FakeWorld world = new FakeWorld().openFallDistance(4);

        assertEquals(
                NurseryHazard.DANGEROUS_FALL,
                checker.evaluate(world, BlockPos.ZERO, CarryConfig.defaultConfig(), false).hazard().orElseThrow()
        );
    }

    @Test
    void fallBelowThresholdAllows() {
        FakeWorld world = new FakeWorld().openFallDistance(3);

        assertTrue(checker.evaluate(world, BlockPos.ZERO, CarryConfig.defaultConfig(), false).allowed());
    }

    private static CarryConfig config(
            boolean nurseryModeEnabled,
            boolean nurseryBlockLava,
            boolean nurseryBlockFire,
            boolean nurseryBlockCactusAndDamage,
            boolean nurseryBlockSuffocation,
            boolean nurseryBlockDangerousFalls,
            int nurseryDangerousFallDistanceBlocks,
            boolean nurseryMessagesEnabled
    ) {
        return new CarryConfig(
                CarryConfig.defaultConfig().allowedAnimals(),
                CarryConfig.defaultConfig().blockedAnimals(),
                false,
                20,
                false,
                true,
                true,
                160,
                360,
                true,
                true,
                true,
                true,
                1200,
                600,
                200,
                nurseryModeEnabled,
                nurseryBlockLava,
                nurseryBlockFire,
                nurseryBlockCactusAndDamage,
                nurseryBlockSuffocation,
                nurseryBlockDangerousFalls,
                nurseryDangerousFallDistanceBlocks,
                nurseryMessagesEnabled
        );
    }

    private static final class FakeWorld implements NurserySafetyChecker.WorldAccess {
        private final Set<BlockPos> lava = new HashSet<>();
        private final Set<BlockPos> fire = new HashSet<>();
        private final Set<BlockPos> damaging = new HashSet<>();
        private boolean collisionSafe = true;
        private int openFallDistance = 0;

        FakeWorld lava(BlockPos pos) {
            lava.add(pos);
            return this;
        }

        FakeWorld fire(BlockPos pos) {
            fire.add(pos);
            return this;
        }

        FakeWorld damaging(BlockPos pos) {
            damaging.add(pos);
            return this;
        }

        FakeWorld collisionSafe(boolean collisionSafe) {
            this.collisionSafe = collisionSafe;
            return this;
        }

        FakeWorld openFallDistance(int openFallDistance) {
            this.openFallDistance = openFallDistance;
            return this;
        }

        @Override
        public boolean hasLava(BlockPos pos) {
            return lava.contains(pos);
        }

        @Override
        public boolean hasFireHazard(BlockPos pos) {
            return fire.contains(pos);
        }

        @Override
        public boolean hasDamagingHazard(BlockPos pos) {
            return damaging.contains(pos);
        }

        @Override
        public boolean hasCollisionSpace(BlockPos feet) {
            return collisionSafe;
        }

        @Override
        public boolean hasSafeFloor(BlockPos feet) {
            return openFallDistance == 0;
        }

        @Override
        public int openFallDistance(BlockPos feet, int threshold) {
            return Math.min(openFallDistance, threshold);
        }
    }
}
