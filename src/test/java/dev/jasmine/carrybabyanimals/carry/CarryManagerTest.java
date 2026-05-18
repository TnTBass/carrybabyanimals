package dev.jasmine.carrybabyanimals.carry;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void findsCarrierByCarriedAnimalId() {
        CarryManager manager = new CarryManager();
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();

        assertTrue(manager.beginCarry(firstPlayer, 10));
        assertTrue(manager.beginCarry(secondPlayer, 11));

        assertEquals(secondPlayer, manager.carrierIdFor(11).orElseThrow());
        assertTrue(manager.carrierIdFor(12).isEmpty());
    }

    @Test
    void sameAnimalCannotBeCarriedByMultiplePlayers() {
        CarryManager manager = new CarryManager();
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();

        assertTrue(manager.beginCarry(firstPlayer, 10));
        assertFalse(manager.beginCarry(secondPlayer, 10));
        assertEquals(firstPlayer, manager.carrierIdFor(10).orElseThrow());
        assertTrue(manager.carriedEntityId(secondPlayer).isEmpty());
    }

    @Test
    void activeCarriesSnapshotDoesNotMutateManager() {
        CarryManager manager = new CarryManager();
        UUID player = UUID.randomUUID();

        assertTrue(manager.beginCarry(player, 10));

        assertThrows(UnsupportedOperationException.class, () -> manager.activeCarries().clear());

        assertEquals(10, manager.carriedEntityId(player).orElseThrow());
    }
}
