package dev.jasmine.carrybabyanimals.carry;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarryManagerTest {
    @Test
    void tracksOneAnimalPerPlayer() {
        CarryManager manager = new CarryManager();
        UUID player = UUID.randomUUID();
        int firstEntityId = 10;
        int secondEntityId = 11;

        assertTrue(manager.beginCarry(player, firstEntityId));
        assertFalse(manager.beginCarry(player, secondEntityId));
        assertEquals(firstEntityId, manager.carriedEntityId(player).orElseThrow());
    }

    @Test
    void dropClearsCarriedAnimal() {
        CarryManager manager = new CarryManager();
        UUID player = UUID.randomUUID();

        assertTrue(manager.beginCarry(player, 10));
        assertTrue(manager.endCarry(player).isPresent());
        assertTrue(manager.carriedEntityId(player).isEmpty());
    }
}
