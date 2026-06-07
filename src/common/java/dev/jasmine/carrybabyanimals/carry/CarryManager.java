package dev.jasmine.carrybabyanimals.carry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CarryManager {
    private final Map<UUID, CarryState> carriedByPlayer = new HashMap<>();

    public boolean beginCarry(UUID playerId, int entityId) {
        return beginCarry(playerId, entityId, 0L);
    }

    public boolean beginCarry(UUID playerId, int entityId, long startedAtTick) {
        if (carriedByPlayer.containsKey(playerId) || carrierIdFor(entityId).isPresent()) {
            return false;
        }
        carriedByPlayer.put(playerId, new CarryState(playerId, entityId, startedAtTick));
        return true;
    }

    public Optional<CarryState> endCarry(UUID playerId) {
        return Optional.ofNullable(carriedByPlayer.remove(playerId));
    }

    public Optional<Integer> carriedEntityId(UUID playerId) {
        return Optional.ofNullable(carriedByPlayer.get(playerId)).map(CarryState::carriedEntityId);
    }

    public Optional<UUID> carrierIdFor(int carriedEntityId) {
        return carriedByPlayer.values().stream()
                .filter(state -> state.carriedEntityId() == carriedEntityId)
                .map(CarryState::carrierId)
                .findFirst();
    }

    public Map<UUID, CarryState> activeCarries() {
        return Map.copyOf(carriedByPlayer);
    }

    public boolean isCarrying(UUID playerId) {
        return carriedByPlayer.containsKey(playerId);
    }
}
