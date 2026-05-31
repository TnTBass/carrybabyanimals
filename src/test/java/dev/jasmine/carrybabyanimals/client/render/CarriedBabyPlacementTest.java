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
    void tunedHeldPositionKeepsSmallBabiesCloseAndRaised() {
        Vec3 tinyBaby = CarriedBabyPlacement.heldPosition(
                Vec3.ZERO,
                new Vec3(0.0D, 0.0D, 1.0D),
                1.8D,
                0.3D,
                false,
                0.0D
        );
        Vec3 tallBaby = CarriedBabyPlacement.heldPosition(
                Vec3.ZERO,
                new Vec3(0.0D, 0.0D, 1.0D),
                1.8D,
                1.2D,
                false,
                0.0D
        );

        assertTrue(tinyBaby.z > tallBaby.z);
        assertTrue(tinyBaby.y > tallBaby.y);
        assertTrue(tinyBaby.z < 0.55D);
        assertTrue(tallBaby.z > 0.35D);
    }

    @Test
    void cosmeticBobStaysSubtleAndVerticalOnly() {
        Vec3 base = CarriedBabyPlacement.heldPosition(
                Vec3.ZERO,
                new Vec3(0.0D, 0.0D, 1.0D),
                1.8D,
                0.6D,
                false,
                0.0D
        );
        Vec3 bobbed = CarriedBabyPlacement.heldPosition(
                Vec3.ZERO,
                new Vec3(0.0D, 0.0D, 1.0D),
                1.8D,
                0.6D,
                false,
                Math.PI * 2.0D
        );

        assertEquals(base.x, bobbed.x, 1.0E-6D);
        assertEquals(base.z, bobbed.z, 1.0E-6D);
        assertTrue(Math.abs(bobbed.y - base.y) > 0.005D);
        assertTrue(Math.abs(bobbed.y - base.y) < 0.025D);
    }

    @Test
    void petFeedbackPositionFloatsAboveHeldBaby() {
        Vec3 held = new Vec3(1.0D, 1.0D, 1.0D);

        Vec3 feedback = CarriedBabyPlacement.petFeedbackPosition(held, 0.8D);

        assertEquals(new Vec3(1.0D, 1.6D, 1.0D), feedback);
    }
}
