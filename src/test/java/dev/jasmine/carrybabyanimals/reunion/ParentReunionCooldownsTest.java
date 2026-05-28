package dev.jasmine.carrybabyanimals.reunion;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ParentReunionCooldownsTest {
    @Test
    void missingCooldownAllowsReunion() {
        ParentReunionCooldowns cooldowns = new ParentReunionCooldowns();

        assertTrue(cooldowns.canReunite(UUID.randomUUID(), UUID.randomUUID(), 100L, 200));
    }

    @Test
    void rememberedBabyOrCarrierBlocksUntilCooldownExpires() {
        ParentReunionCooldowns cooldowns = new ParentReunionCooldowns();
        UUID carrierId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID babyId = UUID.fromString("00000000-0000-0000-0000-000000000042");
        UUID otherBabyId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        cooldowns.remember(carrierId, babyId, 100L);

        assertFalse(cooldowns.canReunite(carrierId, otherBabyId, 299L, 200));
        assertFalse(cooldowns.canReunite(UUID.randomUUID(), babyId, 299L, 200));
        assertFalse(cooldowns.canReunite(carrierId, babyId, 299L, 200));
        assertTrue(cooldowns.canReunite(carrierId, babyId, 300L, 200));
    }

    @Test
    void clearingCarrierRemovesCarrierCooldownOnly() {
        ParentReunionCooldowns cooldowns = new ParentReunionCooldowns();
        UUID carrierId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID babyId = UUID.fromString("00000000-0000-0000-0000-000000000042");
        UUID otherBabyId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        cooldowns.remember(carrierId, babyId, 100L);
        cooldowns.clearCarrier(carrierId);

        assertTrue(cooldowns.canReunite(carrierId, otherBabyId, 101L, 200));
        assertFalse(cooldowns.canReunite(UUID.randomUUID(), babyId, 101L, 200));
    }
}
