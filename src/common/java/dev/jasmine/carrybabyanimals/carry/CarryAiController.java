package dev.jasmine.carrybabyanimals.carry;

import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CarryAiController {
    private final Map<UUID, Boolean> suppressedNoAiState = new HashMap<>();

    public void suppress(Mob mob) {
        if (rememberSuppressedState(mob.getUUID(), mob.isNoAi())) {
            mob.getNavigation().stop();
            mob.setNoAi(true);
        }
    }

    public void restore(Mob mob) {
        restoreSuppressedState(mob.getUUID()).ifPresent(mob::setNoAi);
    }

    boolean rememberSuppressedState(UUID mobId, boolean noAi) {
        return suppressedNoAiState.putIfAbsent(mobId, noAi) == null;
    }

    Optional<Boolean> restoreSuppressedState(UUID mobId) {
        return Optional.ofNullable(suppressedNoAiState.remove(mobId));
    }
}
