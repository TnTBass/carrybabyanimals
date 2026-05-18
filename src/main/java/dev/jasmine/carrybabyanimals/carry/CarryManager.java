package dev.jasmine.carrybabyanimals.carry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CarryManager {
    private final Map<UUID, CarryState> carriedByPlayer = new HashMap<>();

    public boolean beginCarry(UUID playerId, int entityId) {
        if (carriedByPlayer.containsKey(playerId)) {
            return false;
        }
        carriedByPlayer.put(playerId, new CarryState(playerId, entityId, 0L));
        return true;
    }

    public Optional<CarryState> endCarry(UUID playerId) {
        return Optional.ofNullable(carriedByPlayer.remove(playerId));
    }

    public Optional<Integer> carriedEntityId(UUID playerId) {
        return Optional.ofNullable(carriedByPlayer.get(playerId)).map(CarryState::carriedEntityId);
    }

    public boolean isCarrying(UUID playerId) {
        return carriedByPlayer.containsKey(playerId);
    }
}
