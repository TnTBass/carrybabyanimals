package dev.jasmine.carrybabyanimals.cozy;

import dev.jasmine.carrybabyanimals.config.CarryConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CozyFeedbackSchedulerTest {
    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void disabledMasterSwitchEmitsNothing() {
        CozyFeedbackScheduler scheduler = new CozyFeedbackScheduler(new CozyFeedbackMessageCatalog(), fixedRandom(0));
        CarryConfig config = new CarryConfig(List.of(), List.of(), false, 20, false, false, true, 160, 360, true, true, true, true, 1200, 600, 200);

        CozyFeedbackDecision decision = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 2000L), config);

        assertFalse(decision.playIdleSound());
        assertTrue(decision.sleepyMessage().isEmpty());
        assertFalse(decision.spawnSleepyParticles());
    }

    @Test
    void idleSoundWaitsUntilScheduledTickThenReschedules() {
        CozyFeedbackScheduler scheduler = new CozyFeedbackScheduler(new CozyFeedbackMessageCatalog(), fixedRandom(0));

        CozyFeedbackDecision first = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 100L), CarryConfig.defaultConfig());
        CozyFeedbackDecision beforeDue = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 259L), CarryConfig.defaultConfig());
        CozyFeedbackDecision due = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 260L), CarryConfig.defaultConfig());

        assertFalse(first.playIdleSound());
        assertFalse(beforeDue.playIdleSound());
        assertTrue(due.playIdleSound());
    }

    @Test
    void sleepyFeedbackWaitsForCarryDurationAndCooldowns() {
        CozyFeedbackScheduler scheduler = new CozyFeedbackScheduler(new CozyFeedbackMessageCatalog(), fixedRandom(0));

        CozyFeedbackDecision beforeDue = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 1199L), CarryConfig.defaultConfig());
        CozyFeedbackDecision due = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 1200L), CarryConfig.defaultConfig());
        CozyFeedbackDecision particleOnly = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 1400L), CarryConfig.defaultConfig());

        assertTrue(beforeDue.sleepyMessage().isEmpty());
        assertFalse(beforeDue.spawnSleepyParticles());
        assertEquals(Optional.of("Baby Pig is getting sleepy."), due.sleepyMessage());
        assertTrue(due.spawnSleepyParticles());
        assertTrue(particleOnly.sleepyMessage().isEmpty());
        assertTrue(particleOnly.spawnSleepyParticles());
    }

    @Test
    void sleepyMessageVariantsCanVaryAfterCooldown() {
        CozyFeedbackScheduler scheduler = new CozyFeedbackScheduler(new CozyFeedbackMessageCatalog(), fixedRandom(0));
        CarryConfig config = new CarryConfig(List.of(), List.of(), false, 20, false, true, true, 160, 360, true, true, true, true, 1200, 1, 200);

        scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 1200L), config);
        CozyFeedbackDecision nextMessage = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 1201L), config);

        assertEquals(Optional.of("Baby Pig settles into your arms."), nextMessage.sleepyMessage());
    }

    @Test
    void disablingIdleSoundsDoesNotDisableSleepyFeedback() {
        CozyFeedbackScheduler scheduler = new CozyFeedbackScheduler(new CozyFeedbackMessageCatalog(), fixedRandom(0));
        CarryConfig config = new CarryConfig(List.of(), List.of(), false, 20, false, true, false, 160, 360, true, true, true, true, 1200, 600, 200);

        CozyFeedbackDecision decision = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 1200L), config);

        assertFalse(decision.playIdleSound());
        assertEquals(Optional.of("Baby Pig is getting sleepy."), decision.sleepyMessage());
        assertTrue(decision.spawnSleepyParticles());
    }

    @Test
    void disablingSleepyBabiesDoesNotDisableIdleSounds() {
        CozyFeedbackScheduler scheduler = new CozyFeedbackScheduler(new CozyFeedbackMessageCatalog(), fixedRandom(0));
        CarryConfig config = new CarryConfig(List.of(), List.of(), false, 20, false, true, true, 160, 360, true, true, true, false, 1200, 600, 200);

        CozyFeedbackDecision decision = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 1200L), config);

        assertTrue(decision.playIdleSound());
        assertTrue(decision.sleepyMessage().isEmpty());
        assertFalse(decision.spawnSleepyParticles());
    }

    @Test
    void newCarriedEntityResetsSchedulerStateForSameCarrier() {
        CozyFeedbackScheduler scheduler = new CozyFeedbackScheduler(new CozyFeedbackMessageCatalog(), fixedRandom(0));

        CozyFeedbackDecision oldCarry = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 260L), CarryConfig.defaultConfig());
        CozyFeedbackDecision newCarry = scheduler.tickSnapshot(snapshot(8, "Cow", false, 260L, 261L), CarryConfig.defaultConfig());

        assertTrue(oldCarry.playIdleSound());
        assertFalse(newCarry.playIdleSound());
    }

    private static CozyFeedbackCarrySnapshot snapshot(
            int carriedEntityId,
            String displayName,
            boolean hasCustomName,
            long startedAtTick,
            long gameTime
    ) {
        return new CozyFeedbackCarrySnapshot(PLAYER_ID, carriedEntityId, displayName, hasCustomName, startedAtTick, gameTime);
    }

    private static CozyFeedbackRandom fixedRandom(int value) {
        return (inclusiveMin, inclusiveMax) -> inclusiveMin + Math.floorMod(value, inclusiveMax - inclusiveMin + 1);
    }
}
