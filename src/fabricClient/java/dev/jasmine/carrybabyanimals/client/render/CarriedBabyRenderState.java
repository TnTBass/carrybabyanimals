package dev.jasmine.carrybabyanimals.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.IntPredicate;

@Environment(EnvType.CLIENT)
public final class CarriedBabyRenderState {
    public static final RenderStateDataKey<Boolean> SUPPRESS_VANILLA_RENDER = RenderStateDataKey.create(
            () -> "carrybabyanimals:suppress_carried_baby_render"
    );

    private static final ConcurrentMap<Integer, Integer> BABY_TO_CARRIER = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, Integer> CARRIER_TO_BABY = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, LocalReactionState> LOCAL_REACTIONS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, LocalSleepyVisualState> LOCAL_SLEEPY_VISUALS = new ConcurrentHashMap<>();
    private static volatile Object currentLevelIdentity;

    private CarriedBabyRenderState() {
    }

    public static void set(int babyEntityId, int carrierEntityId) {
        // These paired maps favor lock-free render reads; a state change can be one-frame stale mid-update.
        Integer previousCarrier = BABY_TO_CARRIER.put(babyEntityId, carrierEntityId);
        if (previousCarrier != null && previousCarrier != carrierEntityId) {
            CARRIER_TO_BABY.remove(previousCarrier, babyEntityId);
            LOCAL_REACTIONS.remove(babyEntityId);
            LOCAL_SLEEPY_VISUALS.remove(babyEntityId);
        }
        Integer previousBaby = CARRIER_TO_BABY.put(carrierEntityId, babyEntityId);
        if (previousBaby != null && previousBaby != babyEntityId) {
            BABY_TO_CARRIER.remove(previousBaby, carrierEntityId);
            LOCAL_REACTIONS.remove(previousBaby);
            LOCAL_SLEEPY_VISUALS.remove(previousBaby);
        }
    }

    public static void clear(int babyEntityId) {
        Integer carrierEntityId = BABY_TO_CARRIER.remove(babyEntityId);
        if (carrierEntityId != null) {
            CARRIER_TO_BABY.remove(carrierEntityId, babyEntityId);
        }
        LOCAL_REACTIONS.remove(babyEntityId);
        LOCAL_SLEEPY_VISUALS.remove(babyEntityId);
    }

    public static boolean startLocalReaction(
            int babyEntityId,
            CarriedBabyReactionType reactionType,
            long startTick,
            int durationTicks
    ) {
        if (!isCarriedBaby(babyEntityId)) {
            return false;
        }
        LOCAL_REACTIONS.put(
                babyEntityId,
                new LocalReactionState(reactionType, startTick, durationTicks)
        );
        return true;
    }

    public static Optional<LocalReactionState> localReactionFor(int babyEntityId) {
        return Optional.ofNullable(LOCAL_REACTIONS.get(babyEntityId));
    }

    public static boolean startLocalSleepyVisual(int babyEntityId, long startTick, int durationTicks) {
        if (!isCarriedBaby(babyEntityId)) {
            return false;
        }
        LOCAL_SLEEPY_VISUALS.put(
                babyEntityId,
                LocalSleepyVisualState.fromDelay(startTick, durationTicks)
        );
        return true;
    }

    public static Optional<LocalSleepyVisualState> ensureLocalSleepyVisual(
            int babyEntityId,
            long sleepyStartTick,
            int asleepDelayTicks
    ) {
        if (!isCarriedBaby(babyEntityId)) {
            return Optional.empty();
        }
        return Optional.of(LOCAL_SLEEPY_VISUALS.computeIfAbsent(
                babyEntityId,
                ignored -> LocalSleepyVisualState.fromDelay(sleepyStartTick, asleepDelayTicks)
        ));
    }

    public static Optional<LocalSleepyVisualState> localSleepyVisualFor(int babyEntityId) {
        return Optional.ofNullable(LOCAL_SLEEPY_VISUALS.get(babyEntityId));
    }

    public static boolean sleepyVisualActiveFor(int babyEntityId, long currentTick, boolean sleepyCarryVisualsEnabled) {
        return sleepyCarryVisualsEnabled
                && localSleepyVisualFor(babyEntityId)
                .map(sleepyVisual -> sleepyVisual.activeAt(currentTick))
                .orElse(false);
    }

    public static CarriedBabySleepyVisualPhase sleepyVisualPhaseFor(
            int babyEntityId,
            long currentTick,
            boolean sleepyCarryVisualsEnabled
    ) {
        if (!sleepyCarryVisualsEnabled) {
            return CarriedBabySleepyVisualPhase.ALERT;
        }
        return localSleepyVisualFor(babyEntityId)
                .map(sleepyVisual -> sleepyVisual.phaseAt(currentTick))
                .orElse(CarriedBabySleepyVisualPhase.ALERT);
    }

    public static boolean isCarriedBaby(int babyEntityId) {
        return BABY_TO_CARRIER.containsKey(babyEntityId);
    }

    public static boolean isCarrier(int carrierEntityId) {
        return CARRIER_TO_BABY.containsKey(carrierEntityId);
    }

    public static OptionalInt carrierFor(int babyEntityId) {
        Integer carrierEntityId = BABY_TO_CARRIER.get(babyEntityId);
        return carrierEntityId == null ? OptionalInt.empty() : OptionalInt.of(carrierEntityId);
    }

    public static OptionalInt carriedBabyFor(int carrierEntityId) {
        Integer babyEntityId = CARRIER_TO_BABY.get(carrierEntityId);
        return babyEntityId == null ? OptionalInt.empty() : OptionalInt.of(babyEntityId);
    }

    public static Map<Integer, Integer> carriedBabies() {
        return Map.copyOf(BABY_TO_CARRIER);
    }

    public static void pruneMissingEntities(IntPredicate entityExists) {
        BABY_TO_CARRIER.entrySet().removeIf(entry ->
                pruneMissingCarry(entry, entityExists)
        );
    }

    public static void rememberLevel(Object levelIdentity) {
        rememberLevel(levelIdentity, entityId -> false);
    }

    public static void rememberLevel(Object levelIdentity, IntPredicate entityExists) {
        if (levelIdentity == null) {
            return;
        }
        Object previousLevelIdentity = currentLevelIdentity;
        if (previousLevelIdentity == levelIdentity) {
            return;
        }
        currentLevelIdentity = levelIdentity;
        if (previousLevelIdentity != null) {
            pruneMissingEntities(entityExists);
        }
    }

    public static void clearAll() {
        BABY_TO_CARRIER.clear();
        CARRIER_TO_BABY.clear();
        LOCAL_REACTIONS.clear();
        LOCAL_SLEEPY_VISUALS.clear();
        currentLevelIdentity = null;
    }

    private static boolean pruneMissingCarry(Map.Entry<Integer, Integer> entry, IntPredicate entityExists) {
        boolean missing = !entityExists.test(entry.getKey()) || !entityExists.test(entry.getValue());
        if (missing) {
            CARRIER_TO_BABY.remove(entry.getValue(), entry.getKey());
            LOCAL_REACTIONS.remove(entry.getKey());
            LOCAL_SLEEPY_VISUALS.remove(entry.getKey());
        }
        return missing;
    }

    public record LocalReactionState(CarriedBabyReactionType type, long startTick, int durationTicks) {
        public LocalReactionState {
            type = type == null ? CarriedBabyReactionType.GENERIC_SETTLE : type;
            durationTicks = Math.max(1, durationTicks);
        }
    }

    public record LocalSleepyVisualState(long sleepyStartTick, long asleepStartTick) {
        public LocalSleepyVisualState {
            asleepStartTick = Math.max(sleepyStartTick + 1L, asleepStartTick);
        }

        static LocalSleepyVisualState fromDelay(long sleepyStartTick, int asleepDelayTicks) {
            return new LocalSleepyVisualState(
                    sleepyStartTick,
                    sleepyStartTick + Math.max(1, asleepDelayTicks)
            );
        }

        public long startTick() {
            return sleepyStartTick;
        }

        public int durationTicks() {
            long duration = asleepStartTick - sleepyStartTick;
            return duration > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) duration;
        }

        public boolean activeAt(long currentTick) {
            return phaseAt(currentTick) != CarriedBabySleepyVisualPhase.ALERT;
        }

        public CarriedBabySleepyVisualPhase phaseAt(long currentTick) {
            if (currentTick < sleepyStartTick) {
                return CarriedBabySleepyVisualPhase.ALERT;
            }
            return currentTick < asleepStartTick
                    ? CarriedBabySleepyVisualPhase.SLEEPY
                    : CarriedBabySleepyVisualPhase.ASLEEP;
        }
    }
}
