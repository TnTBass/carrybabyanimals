package dev.jasmine.carrybabyanimals.carry;

import dev.jasmine.carrybabyanimals.config.AnimalAliasRegistry;
import dev.jasmine.carrybabyanimals.config.CarryConfig;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarryEligibilityTest {
    private static final Identifier WOLF = Identifier.withDefaultNamespace("wolf");
    private static final Identifier COW = Identifier.withDefaultNamespace("cow");
    private static final CarryEligibility.PermissionSnapshot ALL_TAMED_PERMISSIONS =
            new CarryEligibility.PermissionSnapshot(true, true);
    private static final CarryEligibility eligibility = new CarryEligibility(AnimalAliasRegistry.createDefault());

    @Test
    void dogAllowedOnlyMatchesTamedWolves() {
        CarryConfig config = config(List.of("dog"), List.of(), true);

        assertFalse(eligibility.canPickUpResolved(wild(WOLF), config, ALL_TAMED_PERMISSIONS));
        assertTrue(eligibility.canPickUpResolved(ownedTamed(WOLF), config, ALL_TAMED_PERMISSIONS));
    }

    @Test
    void wolfAllowedMatchesWildWolves() {
        CarryConfig config = config(List.of("wolf"), List.of(), true);

        assertTrue(eligibility.canPickUpResolved(wild(WOLF), config, ALL_TAMED_PERMISSIONS));
    }

    @Test
    void defaultConfigAllowsKnownAnimalAliasesOnly() {
        CarryConfig config = CarryConfig.defaultConfig();

        assertTrue(eligibility.canPickUpResolved(wild(COW), config, ALL_TAMED_PERMISSIONS));
        assertFalse(eligibility.canPickUpResolved(wild(Identifier.withDefaultNamespace("zoglin")), config, ALL_TAMED_PERMISSIONS));
    }

    @Test
    void dogBlockedOnlyBlocksTamedWolves() {
        CarryConfig config = config(List.of("wolf"), List.of("dog"), true);

        assertTrue(eligibility.canPickUpResolved(wild(WOLF), config, ALL_TAMED_PERMISSIONS));
        assertFalse(eligibility.canPickUpResolved(ownedTamed(WOLF), config, ALL_TAMED_PERMISSIONS));
    }

    @Test
    void blockedWinsOverAllowed() {
        CarryConfig config = config(List.of("cow"), List.of("cow"), true);

        assertFalse(eligibility.canPickUpResolved(wild(COW), config, ALL_TAMED_PERMISSIONS));
    }

    @Test
    void diagnosticReasonNamesConfigBlock() {
        CarryConfig config = config(List.of("cow"), List.of("cow"), true);

        assertEquals(
                CarryEligibility.PickupDecision.BLOCKED_BY_CONFIG,
                eligibility.pickupDecision(wild(COW), config, ALL_TAMED_PERMISSIONS)
        );
    }

    @Test
    void unknownOnlyAllowListDenies() {
        CarryConfig config = config(List.of("mystery"), List.of(), true);

        assertFalse(eligibility.canPickUpResolved(wild(COW), config, ALL_TAMED_PERMISSIONS));
    }

    @Test
    void emptyEffectiveAllowListStillDeniesWhenAllowListWasConfigured() {
        CarryConfig config = new CarryConfig(List.of(), List.of(), true, 20, true);

        assertFalse(eligibility.canPickUpResolved(wild(COW), config, ALL_TAMED_PERMISSIONS));
    }

    @Test
    void ownedTamedRequiresCarryTamedPermission() {
        CarryConfig config = config(List.of("dog"), List.of(), true);

        assertFalse(eligibility.canPickUpResolved(
                ownedTamed(WOLF),
                config,
                new CarryEligibility.PermissionSnapshot(false, true)
        ));
        assertTrue(eligibility.canPickUpResolved(
                ownedTamed(WOLF),
                config,
                new CarryEligibility.PermissionSnapshot(true, false)
        ));
        assertEquals(
                CarryEligibility.PickupDecision.TAMED_PERMISSION_DENIED,
                eligibility.pickupDecision(
                        ownedTamed(WOLF),
                        config,
                        new CarryEligibility.PermissionSnapshot(false, true)
                )
        );
    }

    @Test
    void othersTamedRequiresConfigAndPermission() {
        assertFalse(eligibility.canPickUpResolved(
                othersTamed(WOLF),
                config(List.of("dog"), List.of(), false),
                ALL_TAMED_PERMISSIONS
        ));
        assertFalse(eligibility.canPickUpResolved(
                othersTamed(WOLF),
                config(List.of("dog"), List.of(), true),
                new CarryEligibility.PermissionSnapshot(true, false)
        ));
        assertTrue(eligibility.canPickUpResolved(
                othersTamed(WOLF),
                config(List.of("dog"), List.of(), true),
                ALL_TAMED_PERMISSIONS
        ));
    }

    private static CarryConfig config(List<String> allowed, List<String> blocked, boolean allowOthersTamed) {
        return new CarryConfig(allowed, blocked, allowOthersTamed, 20);
    }

    private static CarryEligibility.CarryCandidate wild(Identifier entityId) {
        return new CarryEligibility.CarryCandidate(entityId, false, false);
    }

    private static CarryEligibility.CarryCandidate ownedTamed(Identifier entityId) {
        return new CarryEligibility.CarryCandidate(entityId, true, true);
    }

    private static CarryEligibility.CarryCandidate othersTamed(Identifier entityId) {
        return new CarryEligibility.CarryCandidate(entityId, true, false);
    }
}
