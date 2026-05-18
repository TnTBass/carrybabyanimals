package dev.jasmine.carrybabyanimals.carry;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarryInteractionHandlerTest {
    @Test
    void missingPetCooldownAllowsImmediatePetRegardlessOfCooldownLength() {
        CarryInteractionHandler handler = newHandler();

        assertTrue(handler.canPet(UUID.randomUUID(), 0L, 200));
    }

    @Test
    void rememberedPetBlocksUntilCooldownExpires() {
        CarryInteractionHandler handler = newHandler();
        UUID playerId = UUID.randomUUID();

        handler.rememberPet(playerId, 100L);

        assertFalse(handler.canPet(playerId, 119L, 20));
        assertTrue(handler.canPet(playerId, 120L, 20));
    }

    @Test
    void clearingPetCooldownAllowsImmediatePetAgain() {
        CarryInteractionHandler handler = newHandler();
        UUID playerId = UUID.randomUUID();

        handler.rememberPet(playerId, 100L);
        handler.clearPetCooldown(playerId);

        assertTrue(handler.canPet(playerId, 101L, 200));
    }

    private CarryInteractionHandler newHandler() {
        return new CarryInteractionHandler(null, null, null, null, null);
    }
}
