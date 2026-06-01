package dev.jasmine.carrybabyanimals.client.render;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void tallBabiesUseLowerTuckedSidePlacement() {
        CarriedBabyPlacement.PlacementResult medium = placement(CarriedBabySizeBucket.MEDIUM, 0.7D, 0.45D, false);
        CarriedBabyPlacement.PlacementResult tall = placement(CarriedBabySizeBucket.TALL, 1.15D, 0.55D, false);

        assertTrue(tall.position().y <= medium.position().y - 0.22D);
        assertTrue(tall.position().x >= medium.position().x + 0.18D);
        assertTrue(tall.position().z <= medium.position().z - 0.08D);
        assertTrue(tall.yawDegrees() >= 15.0D);
        assertTrue(tall.yawDegrees() <= 35.0D);
        assertFalse(tall.suppressForLocalFirstPerson());
    }

    @Test
    void bulkyBabiesStayBesideCarrierInsteadOfCenteredForward() {
        CarriedBabyPlacement.PlacementResult medium = placement(CarriedBabySizeBucket.MEDIUM, 0.75D, 0.55D, false);
        CarriedBabyPlacement.PlacementResult bulky = placement(CarriedBabySizeBucket.BULKY, 0.85D, 0.95D, false);

        assertTrue(bulky.position().x >= medium.position().x + 0.20D);
        assertTrue(bulky.position().z <= 0.36D);
        assertTrue(bulky.position().y < medium.position().y);
        assertFalse(bulky.suppressForLocalFirstPerson());
    }

    @Test
    void firstPersonTallBabyKeepsCrosshairCorridorClear() {
        CarriedBabyPlacement.PlacementResult mediumBaseline = placement(CarriedBabySizeBucket.MEDIUM, 0.7D, 0.45D, true);
        CarriedBabyPlacement.PlacementResult localFirstPerson = placement(CarriedBabySizeBucket.TALL, 1.15D, 0.55D, true);

        assertTrue(Math.abs(localFirstPerson.position().x) >= 0.24D);
        assertTrue(localFirstPerson.position().y <= mediumBaseline.position().y - 0.18D);
        assertFalse(localFirstPerson.suppressForLocalFirstPerson());
    }

    @Test
    void hideWhenObstructingModeKeepsClearTuckedTallBabyVisible() {
        CarriedBabyPlacement.PlacementResult tallFirstPerson = CarriedBabyPlacement.placement(
                Vec3.ZERO,
                new Vec3(0.0D, 0.0D, 1.0D),
                1.8D,
                1.15D,
                0.55D,
                false,
                0.0D,
                CarriedBabySizeBucket.TALL,
                true,
                FirstPersonLargeBabyVisibilityMode.HIDE_WHEN_OBSTRUCTING
        );
        CarriedBabyPlacement.PlacementResult mediumFirstPerson = CarriedBabyPlacement.placement(
                Vec3.ZERO,
                new Vec3(0.0D, 0.0D, 1.0D),
                1.8D,
                0.7D,
                0.45D,
                false,
                0.0D,
                CarriedBabySizeBucket.MEDIUM,
                true,
                FirstPersonLargeBabyVisibilityMode.HIDE_WHEN_OBSTRUCTING
        );

        assertFalse(tallFirstPerson.suppressForLocalFirstPerson());
        assertFalse(mediumFirstPerson.suppressForLocalFirstPerson());
    }

    @Test
    void existingMediumPlacementRemainsNearPhaseFiveBaseline() {
        Vec3 baseline = CarriedBabyPlacement.heldPosition(
                Vec3.ZERO,
                new Vec3(0.0D, 0.0D, 1.0D),
                1.8D,
                0.7D,
                false,
                0.0D
        );

        CarriedBabyPlacement.PlacementResult medium = placement(CarriedBabySizeBucket.MEDIUM, 0.7D, 0.45D, false);

        assertEquals(baseline.x, medium.position().x, 0.06D);
        assertEquals(baseline.y, medium.position().y, 0.08D);
        assertEquals(baseline.z, medium.position().z, 0.06D);
    }

    private static CarriedBabyPlacement.PlacementResult placement(
            CarriedBabySizeBucket sizeBucket,
            double babyHeight,
            double babyWidth,
            boolean firstPerson
    ) {
        return CarriedBabyPlacement.placement(
                Vec3.ZERO,
                new Vec3(0.0D, 0.0D, 1.0D),
                1.8D,
                babyHeight,
                babyWidth,
                false,
                0.0D,
                sizeBucket,
                firstPerson,
                FirstPersonLargeBabyVisibilityMode.TUCKED
        );
    }
}
