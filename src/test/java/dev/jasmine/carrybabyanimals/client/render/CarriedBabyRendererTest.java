package dev.jasmine.carrybabyanimals.client.render;

import dev.jasmine.carrybabyanimals.client.config.ClientCarryVisualConfig;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.FelineRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CarriedBabyRendererTest {
    @Test
    void firstPersonLargeBabyModeStillUsesLargeBucketWhenThirdPersonTuckIsDisabled() {
        ClientCarryVisualConfig visualConfig = new ClientCarryVisualConfig(
                true,
                false,
                FirstPersonLargeBabyVisibilityMode.TUCKED,
                true,
                0.75D,
                List.of()
        );

        assertEquals(
                CarriedBabySizeBucket.TALL,
                CarriedBabyRenderer.effectiveSizeBucket(CarriedBabySizeBucket.TALL, true, visualConfig)
        );
        assertEquals(
                CarriedBabySizeBucket.MEDIUM,
                CarriedBabyRenderer.effectiveSizeBucket(CarriedBabySizeBucket.TALL, false, visualConfig)
        );
    }

    @Test
    void visualFrameAppliesReadablePoseToLivingRenderState() {
        LivingEntityRenderState renderState = new LivingEntityRenderState();
        renderState.yRot = 30.0F;
        renderState.bodyRot = 35.0F;
        renderState.xRot = 4.0F;
        CarriedBabyVisualFrame frame = new CarriedBabyVisualFrame(
                new Vec3(1.0D, 2.0D, 3.0D),
                false,
                12.0D,
                -14.0D,
                5.0D
        );

        CarriedBabyRenderer.applyVisualFrame(renderState, frame, Vec3.ZERO);

        assertEquals(1.0D, renderState.x);
        assertEquals(2.0D, renderState.y);
        assertEquals(3.0D, renderState.z);
        assertEquals(42.0F, renderState.yRot);
        assertEquals(47.0F, renderState.bodyRot);
        assertEquals(-10.0F, renderState.xRot);
    }

    @Test
    void visualFrameAppliesPositionToPlainEntityRenderState() {
        EntityRenderState renderState = new EntityRenderState();
        CarriedBabyVisualFrame frame = new CarriedBabyVisualFrame(
                new Vec3(1.0D, 2.0D, 3.0D),
                false,
                12.0D,
                -14.0D,
                5.0D
        );

        CarriedBabyRenderer.applyVisualFrame(renderState, frame, Vec3.ZERO);

        assertEquals(1.0D, renderState.x);
        assertEquals(2.0D, renderState.y);
        assertEquals(3.0D, renderState.z);
    }

    @Test
    void asleepVisualFrameAppliesVanillaFelineLieDownPose() {
        FelineRenderState renderState = new FelineRenderState();
        renderState.xRot = 7.0F;
        CarriedBabyVisualFrame frame = CarriedBabyVisualFrame.evaluate(
                new CarriedBabyPlacement.PlacementResult(Vec3.ZERO, false, 0.0D, 0.0D, 0.0D),
                null,
                0,
                160,
                CarriedBabySleepyVisualPhase.ASLEEP,
                true
        );

        CarriedBabyRenderer.applyVisualFrame(renderState, frame, Vec3.ZERO);

        assertEquals(1.0F, renderState.lieDownAmount);
        assertEquals(1.0F, renderState.lieDownAmountTail);
        assertEquals(1.0F, renderState.relaxStateOneAmount);
        assertEquals(7.0F, renderState.xRot);
    }

    @Test
    void sleepyVisualFrameAppliesPartialVanillaFelineLieDownPose() {
        FelineRenderState renderState = new FelineRenderState();
        CarriedBabyVisualFrame frame = CarriedBabyVisualFrame.evaluate(
                new CarriedBabyPlacement.PlacementResult(Vec3.ZERO, false, 0.0D, 0.0D, 0.0D),
                null,
                0,
                160,
                CarriedBabySleepyVisualPhase.SLEEPY,
                true
        );

        CarriedBabyRenderer.applyVisualFrame(renderState, frame, Vec3.ZERO);

        assertEquals(0.45F, renderState.lieDownAmount);
        assertEquals(0.45F, renderState.lieDownAmountTail);
        assertEquals(0.45F, renderState.relaxStateOneAmount);
    }
}
