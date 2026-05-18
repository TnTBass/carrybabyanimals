package dev.jasmine.carrybabyanimals.carry;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import net.minecraft.world.InteractionResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void sneakingWithEmptyHandsWhileCarryingTriggersDeliberateDropUse() {
        assertEquals(
                InteractionResult.SUCCESS,
                CarryInteractionHandler.useWhileCarryingDecision(true, true, true, true)
        );
    }

    @Test
    void carryingStillBlocksUseWhenNotDeliberatelyDropping() {
        assertEquals(
                InteractionResult.FAIL,
                CarryInteractionHandler.useWhileCarryingDecision(true, false, true, true)
        );
        assertEquals(
                InteractionResult.FAIL,
                CarryInteractionHandler.useWhileCarryingDecision(true, true, false, true)
        );
        assertEquals(
                InteractionResult.FAIL,
                CarryInteractionHandler.useWhileCarryingDecision(true, true, true, false)
        );
    }

    @Test
    void notCarryingDoesNotConsumeUse() {
        assertEquals(
                InteractionResult.PASS,
                CarryInteractionHandler.useWhileCarryingDecision(false, true, true, true)
        );
    }

    @Test
    void entityUseWhileCarryingIsConsumedEvenWithoutSneaking() {
        assertEquals(InteractionResult.SUCCESS, CarryInteractionHandler.entityInteractDecision(true, false, true, true));
        assertEquals(InteractionResult.SUCCESS, CarryInteractionHandler.entityInteractDecision(true, true, true, true));
    }

    @Test
    void entityUsePickupRequiresSneakingAndEmptyHandsWhenNotCarrying() {
        assertEquals(InteractionResult.PASS, CarryInteractionHandler.entityInteractDecision(false, false, true, true));
        assertEquals(InteractionResult.PASS, CarryInteractionHandler.entityInteractDecision(false, true, false, true));
        assertEquals(InteractionResult.PASS, CarryInteractionHandler.entityInteractDecision(false, true, true, false));
        assertEquals(InteractionResult.SUCCESS, CarryInteractionHandler.entityInteractDecision(false, true, true, true));
    }

    private CarryInteractionHandler newHandler() {
        return new CarryInteractionHandler(null, null, null, null, null);
    }
}
