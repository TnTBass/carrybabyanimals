package com.explosive.carrybabyanimals.carry;

import java.util.UUID;

public record CarryState(UUID carrierId, int carriedEntityId, long startedAtTick) {
}
