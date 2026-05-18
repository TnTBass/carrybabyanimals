package dev.jasmine.carrybabyanimals.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.IntPredicate;

public final class CarriedBabyRenderState {
    public static final RenderStateDataKey<Boolean> SUPPRESS_VANILLA_RENDER = RenderStateDataKey.create(
            () -> "carrybabyanimals:suppress_carried_baby_render"
    );

    private static final Map<Integer, Integer> BABY_TO_CARRIER = new HashMap<>();

    private CarriedBabyRenderState() {
    }

    public static void set(int babyEntityId, int carrierEntityId) {
        BABY_TO_CARRIER.put(babyEntityId, carrierEntityId);
    }

    public static void clear(int babyEntityId) {
        BABY_TO_CARRIER.remove(babyEntityId);
    }

    public static boolean isCarriedBaby(int babyEntityId) {
        return BABY_TO_CARRIER.containsKey(babyEntityId);
    }

    public static OptionalInt carrierFor(int babyEntityId) {
        Integer carrierEntityId = BABY_TO_CARRIER.get(babyEntityId);
        return carrierEntityId == null ? OptionalInt.empty() : OptionalInt.of(carrierEntityId);
    }

    public static Map<Integer, Integer> carriedBabies() {
        return Map.copyOf(BABY_TO_CARRIER);
    }

    public static void pruneMissingEntities(IntPredicate entityExists) {
        BABY_TO_CARRIER.entrySet().removeIf(entry ->
                !entityExists.test(entry.getKey()) || !entityExists.test(entry.getValue())
        );
    }

    public static void clearAll() {
        BABY_TO_CARRIER.clear();
    }
}
