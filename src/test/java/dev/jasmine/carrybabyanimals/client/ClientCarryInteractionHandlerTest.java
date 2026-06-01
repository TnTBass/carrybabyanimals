package dev.jasmine.carrybabyanimals.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientCarryInteractionHandlerTest {
    @Test
    void preAttackIsCancelledOnlyForLocalCarrier() {
        assertTrue(ClientCarryInteractionHandler.shouldCancelPreAttack(true));
        assertFalse(ClientCarryInteractionHandler.shouldCancelPreAttack(false));
    }

    @Test
    void localPetFeedbackRequiresLocalCarrierAndKnownBaby() {
        assertTrue(ClientCarryInteractionHandler.shouldShowLocalPetFeedback(true, true));
        assertFalse(ClientCarryInteractionHandler.shouldShowLocalPetFeedback(false, true));
        assertFalse(ClientCarryInteractionHandler.shouldShowLocalPetFeedback(true, false));
    }

    @Test
    void localPetReactionRequiresFeedbackAndEnabledClientReactions() {
        assertTrue(ClientCarryInteractionHandler.shouldStartLocalPetReaction(true, true));
        assertFalse(ClientCarryInteractionHandler.shouldStartLocalPetReaction(false, true));
        assertFalse(ClientCarryInteractionHandler.shouldStartLocalPetReaction(true, false));
    }
}
