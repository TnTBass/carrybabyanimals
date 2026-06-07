package dev.jasmine.carrybabyanimals.carry;

import java.util.UUID;

public record CarryState(UUID carrierId, int carriedEntityId, long startedAtTick) {
}
