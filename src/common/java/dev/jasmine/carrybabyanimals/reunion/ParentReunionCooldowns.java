package dev.jasmine.carrybabyanimals.reunion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ParentReunionCooldowns {
    private final Map<UUID, Long> lastCarrierReunionTick = new HashMap<>();
    private final Map<UUID, Long> lastBabyReunionTick = new HashMap<>();

    public boolean canReunite(UUID carrierId, UUID babyId, long gameTime, int cooldownTicks) {
        return outsideCooldown(lastCarrierReunionTick.get(carrierId), gameTime, cooldownTicks)
                && outsideCooldown(lastBabyReunionTick.get(babyId), gameTime, cooldownTicks);
    }

    public void remember(UUID carrierId, UUID babyId, long gameTime) {
        lastCarrierReunionTick.put(carrierId, gameTime);
        lastBabyReunionTick.put(babyId, gameTime);
    }

    public void clearCarrier(UUID carrierId) {
        lastCarrierReunionTick.remove(carrierId);
    }

    private static boolean outsideCooldown(Long lastTick, long gameTime, int cooldownTicks) {
        return lastTick == null || gameTime - lastTick >= cooldownTicks;
    }
}
