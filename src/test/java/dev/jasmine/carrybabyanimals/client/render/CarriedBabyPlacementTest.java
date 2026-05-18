package dev.jasmine.carrybabyanimals.client.render;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarriedBabyPlacementTest {
    @Test
    void heldPositionSitsForwardAndNearCarrierCenterline() {
        Vec3 held = CarriedBabyPlacement.heldPosition(
                Vec3.ZERO,
                new Vec3(0.0D, 0.0D, 1.0D),
                1.8D,
                0.9D,
                false
        );

        assertTrue(held.z > 0.4D);
        assertTrue(Math.abs(held.x) < 0.15D);
        assertEquals(0.94D, held.y, 0.02D);
    }

    @Test
    void petFeedbackPositionFloatsAboveHeldBaby() {
        Vec3 held = new Vec3(1.0D, 1.0D, 1.0D);

        Vec3 feedback = CarriedBabyPlacement.petFeedbackPosition(held, 0.8D);

        assertEquals(new Vec3(1.0D, 1.6D, 1.0D), feedback);
    }
}
