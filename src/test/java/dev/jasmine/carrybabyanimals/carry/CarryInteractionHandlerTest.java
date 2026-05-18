package dev.jasmine.carrybabyanimals.carry;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

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
    void vanillaFirstPersonPetFeedbackSpawnsInFrontOfCarrierEyes() {
        Vec3 position = CarryInteractionHandler.firstPersonPetFeedbackPosition(
                new Vec3(10.0D, 65.6D, 10.0D),
                new Vec3(0.0D, 0.0D, 1.0D)
        );

        assertEquals(10.0D, position.x, 1.0E-6D);
        assertEquals(65.45D, position.y, 1.0E-6D);
        assertEquals(10.75D, position.z, 1.0E-6D);
    }

    @Test
    void playerPassengerAttachmentScopeIsTemporary() {
        assertFalse(CarryAttachment.isPlayerPassengerAttachmentAllowed());

        assertTrue(CarryAttachment.withPlayerPassengerAttachmentAllowed(10, CarryAttachment::isPlayerPassengerAttachmentAllowed));

        assertFalse(CarryAttachment.isPlayerPassengerAttachmentAllowed());
    }

    @Test
    void playerPassengerAttachmentScopeOnlyAllowsExpectedEntityId() {
        assertFalse(CarryAttachment.isExpectedPlayerPassengerAttachment(10));

        CarryAttachment.withPlayerPassengerAttachmentAllowed(10, () -> {
            assertTrue(CarryAttachment.isExpectedPlayerPassengerAttachment(10));
            assertFalse(CarryAttachment.isExpectedPlayerPassengerAttachment(11));
            return true;
        });

        assertFalse(CarryAttachment.isExpectedPlayerPassengerAttachment(10));
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
    void useBlockWhileCarryingAllowsNavigationBlocksWithoutDropping() {
        assertEquals(
                InteractionResult.PASS,
                CarryInteractionHandler.useBlockWhileCarryingDecision(true, false, true, true, true)
        );
    }

    @Test
    void useBlockWhileCarryingStillBlocksOtherBlocks() {
        assertEquals(
                InteractionResult.FAIL,
                CarryInteractionHandler.useBlockWhileCarryingDecision(true, false, true, true, false)
        );
    }

    @Test
    void deliberateDropTakesPrecedenceOverNavigationBlockUse() {
        assertEquals(
                InteractionResult.SUCCESS,
                CarryInteractionHandler.useBlockWhileCarryingDecision(true, true, true, true, true)
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
        assertEquals(InteractionResult.SUCCESS, CarryInteractionHandler.entityInteractDecision(true, true, false, true, true));
        assertEquals(InteractionResult.SUCCESS, CarryInteractionHandler.entityInteractDecision(true, true, true, true, true));
    }

    @Test
    void entityUsePickupRequiresSneakingAndEmptyHandsWhenNotCarrying() {
        assertEquals(InteractionResult.PASS, CarryInteractionHandler.entityInteractDecision(false, true, false, true, true));
        assertEquals(InteractionResult.PASS, CarryInteractionHandler.entityInteractDecision(false, true, true, false, true));
        assertEquals(InteractionResult.PASS, CarryInteractionHandler.entityInteractDecision(false, true, true, true, false));
        assertEquals(InteractionResult.PASS, CarryInteractionHandler.entityInteractDecision(false, false, true, true, true));
        assertEquals(InteractionResult.SUCCESS, CarryInteractionHandler.entityInteractDecision(false, true, true, true, true));
    }

    @Test
    void entityUseDropRequiresCarryingMainHandSneakingAndEmptyHands() {
        assertTrue(CarryInteractionHandler.shouldDropFromEntityInteract(true, true, true, true, true));
        assertFalse(CarryInteractionHandler.shouldDropFromEntityInteract(true, false, true, true, true));
        assertFalse(CarryInteractionHandler.shouldDropFromEntityInteract(true, true, false, true, true));
        assertFalse(CarryInteractionHandler.shouldDropFromEntityInteract(true, true, true, false, true));
        assertFalse(CarryInteractionHandler.shouldDropFromEntityInteract(true, true, true, true, false));
        assertFalse(CarryInteractionHandler.shouldDropFromEntityInteract(false, true, true, true, true));
    }

    @Test
    void carryFeedbackTextNamesTheVisibleAction() {
        assertEquals("Carrying Baby Panda", CarryInteractionHandler.pickupFeedbackText("Baby Panda"));
        assertEquals("Set down baby animal", CarryInteractionHandler.dropFeedbackText());
    }

    private CarryInteractionHandler newHandler() {
        return new CarryInteractionHandler(null, null, null, null, null);
    }
}
