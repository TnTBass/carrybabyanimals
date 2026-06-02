package dev.jasmine.carrybabyanimals.client.render;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarriedBabyVisualFrameTest {
    @AfterEach
    void clearRenderState() {
        CarriedBabyRenderState.clearAll();
    }

    @Test
    void reactionFrameNeverExceedsDescriptorBounds() {
        CarriedBabyPlacement.PlacementResult placement = placement();
        CarriedBabyReaction reaction = new CarriedBabyReaction(
                CarriedBabyReactionType.RABBIT_WIGGLE,
                100,
                1.0D,
                9.0D,
                9.0D,
                90.0D,
                90.0D,
                90.0D,
                true
        );

        CarriedBabyVisualFrame frame = CarriedBabyVisualFrame.evaluate(placement, reaction, 0, 4, false, true);

        assertTrue(Math.abs(frame.position().x - placement.position().x) <= 0.08D);
        assertTrue(Math.abs(frame.position().y - placement.position().y) <= 0.08D);
        assertTrue(Math.abs(frame.position().z - placement.position().z) <= 0.08D);
        assertTrue(Math.abs(frame.pitchDegrees()) <= 18.0D);
        assertTrue(Math.abs(frame.yawDegrees()) <= 18.0D);
        assertTrue(Math.abs(frame.rollDegrees()) <= 18.0D);
    }

    @Test
    void petFeedbackStartsShortLocalReactionForKnownCarriedBaby() {
        CarriedBabyRenderState.set(10, 20);

        boolean started = CarriedBabyRenderState.startLocalReaction(
                10,
                CarriedBabyReactionType.CHICKEN_FLAP,
                100L,
                12
        );

        assertTrue(started);
        assertEquals(CarriedBabyReactionType.CHICKEN_FLAP, CarriedBabyRenderState.localReactionFor(10).orElseThrow().type());
    }

    @Test
    void reactionStopsWhenCarryStateClears() {
        CarriedBabyRenderState.set(10, 20);
        CarriedBabyRenderState.startLocalReaction(10, CarriedBabyReactionType.CHICKEN_FLAP, 100L, 12);

        CarriedBabyRenderState.clear(10);

        assertTrue(CarriedBabyRenderState.localReactionFor(10).isEmpty());
    }

    @Test
    void sleepyVisualSoftensEligibleReactionWithoutChangingGameplayState() {
        CarriedBabyPlacement.PlacementResult placement = placement();
        CarriedBabyReaction reaction = new CarriedBabyReaction(
                CarriedBabyReactionType.RABBIT_WIGGLE,
                10,
                1.0D,
                0.08D,
                0.08D,
                12.0D,
                0.0D,
                12.0D,
                true
        );

        CarriedBabyVisualFrame awake = CarriedBabyVisualFrame.evaluate(placement, reaction, 0, 3, false, true);
        CarriedBabyVisualFrame sleepy = CarriedBabyVisualFrame.evaluate(placement, reaction, 0, 3, true, true);

        assertTrue(Math.abs(sleepy.position().x - placement.position().x) < Math.abs(awake.position().x - placement.position().x));
        assertTrue(Math.abs(sleepy.rollDegrees()) < Math.abs(awake.rollDegrees()));
        assertFalse(CarriedBabyRenderState.isCarriedBaby(10));
    }

    @Test
    void sleepyVisualKeepsSleepPoseButSuppressesUnsafeReactionMotion() {
        CarriedBabyPlacement.PlacementResult placement = placement();
        CarriedBabyReaction reaction = new CarriedBabyReaction(
                CarriedBabyReactionType.PANDA_SNEEZE,
                10,
                1.0D,
                0.08D,
                0.08D,
                12.0D,
                0.0D,
                12.0D,
                false
        );

        CarriedBabyVisualFrame sleepy = CarriedBabyVisualFrame.evaluate(placement, reaction, 0, 3, true, true);

        assertTrue(sleepy.position().y < placement.position().y);
        assertEquals(placement.yawDegrees(), sleepy.yawDegrees(), 1.0E-6D);
        assertTrue(sleepy.pitchDegrees() < placement.pitchDegrees());
        assertTrue(sleepy.rollDegrees() > placement.rollDegrees());
    }

    @Test
    void disabledSleepyVisualsPreventFrameSoftening() {
        CarriedBabyPlacement.PlacementResult placement = placement();
        CarriedBabyReaction reaction = new CarriedBabyReaction(
                CarriedBabyReactionType.RABBIT_WIGGLE,
                10,
                1.0D,
                0.08D,
                0.08D,
                12.0D,
                0.0D,
                12.0D,
                true
        );

        CarriedBabyRenderState.set(10, 20);
        CarriedBabyRenderState.startLocalSleepyVisual(10, 0L, 40);

        CarriedBabyVisualFrame disabled = CarriedBabyVisualFrame.evaluate(
                placement,
                reaction,
                0,
                3,
                CarriedBabyRenderState.sleepyVisualActiveFor(10, 3L, false),
                true
        );
        CarriedBabyVisualFrame enabled = CarriedBabyVisualFrame.evaluate(
                placement,
                reaction,
                0,
                3,
                CarriedBabyRenderState.sleepyVisualActiveFor(10, 3L, true),
                true
        );

        assertTrue(Math.abs(enabled.position().x - placement.position().x) < Math.abs(disabled.position().x - placement.position().x));
        assertTrue(Math.abs(enabled.rollDegrees()) < Math.abs(disabled.rollDegrees()));
    }

    @Test
    void sleepyPhaseLowersAndTucksFrameWithoutPettingReaction() {
        CarriedBabyPlacement.PlacementResult placement = placement();

        CarriedBabyVisualFrame sleepy = CarriedBabyVisualFrame.evaluate(
                placement,
                null,
                0,
                120,
                CarriedBabySleepyVisualPhase.SLEEPY,
                true
        );

        assertTrue(sleepy.position().y < placement.position().y);
        assertTrue(sleepy.pitchDegrees() < placement.pitchDegrees());
        assertEquals(placement.suppressForLocalFirstPerson(), sleepy.suppressForLocalFirstPerson());
    }

    @Test
    void asleepPhaseIsMoreReadableThanSleepyAndAddsOnlyTinyBreathingMotion() {
        CarriedBabyPlacement.PlacementResult placement = placement();

        CarriedBabyVisualFrame sleepy = CarriedBabyVisualFrame.evaluate(
                placement,
                null,
                0,
                140,
                CarriedBabySleepyVisualPhase.SLEEPY,
                true
        );
        CarriedBabyVisualFrame asleep = CarriedBabyVisualFrame.evaluate(
                placement,
                null,
                0,
                140,
                CarriedBabySleepyVisualPhase.ASLEEP,
                true
        );

        assertTrue(asleep.position().y <= sleepy.position().y + 0.012D);
        assertTrue(asleep.pitchDegrees() < sleepy.pitchDegrees());
        assertTrue(Math.abs(asleep.position().y - placement.position().y) <= 0.08D);
    }

    @Test
    void alertPhasePreservesBasePlacementWithoutReaction() {
        CarriedBabyPlacement.PlacementResult placement = placement();

        CarriedBabyVisualFrame alert = CarriedBabyVisualFrame.evaluate(
                placement,
                null,
                0,
                120,
                CarriedBabySleepyVisualPhase.ALERT,
                true
        );

        assertEquals(placement.position(), alert.position());
        assertEquals(placement.pitchDegrees(), alert.pitchDegrees(), 1.0E-6D);
    }

    @Test
    void sleepyVisualTimingDoesNotAffectCarriedBabyMaps() {
        CarriedBabyRenderState.set(10, 20);

        CarriedBabyRenderState.startLocalSleepyVisual(10, 100L, 80);

        assertTrue(CarriedBabyRenderState.sleepyVisualActiveFor(10, 120L, true));
        assertEquals(20, CarriedBabyRenderState.carrierFor(10).orElseThrow());
        assertEquals(10, CarriedBabyRenderState.carriedBabyFor(20).orElseThrow());
    }

    @Test
    void multipleReactionTriggersDoNotStackAmplitude() {
        CarriedBabyRenderState.set(10, 20);
        CarriedBabyRenderState.startLocalReaction(10, CarriedBabyReactionType.CHICKEN_FLAP, 100L, 12);
        CarriedBabyRenderState.startLocalReaction(10, CarriedBabyReactionType.CHICKEN_FLAP, 104L, 12);

        CarriedBabyRenderState.LocalReactionState reaction = CarriedBabyRenderState.localReactionFor(10).orElseThrow();

        assertEquals(104L, reaction.startTick());
        assertEquals(12, reaction.durationTicks());
    }

    private static CarriedBabyPlacement.PlacementResult placement() {
        return new CarriedBabyPlacement.PlacementResult(Vec3.ZERO, false, 0.0D, 0.0D, 0.0D);
    }
}
