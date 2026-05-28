package dev.jasmine.carrybabyanimals.reunion;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ParentReunionFinderTest {
    private static final Identifier COW = Identifier.withDefaultNamespace("cow");
    private static final Identifier PIG = Identifier.withDefaultNamespace("pig");
    private static final Identifier WOLF = Identifier.withDefaultNamespace("wolf");

    @Test
    void sameTypeAdultInsideRadiusIsCompatible() {
        assertTrue(ParentReunionFinder.compatibleCandidate(
                PIG,
                true,
                Optional.empty(),
                PIG,
                false,
                Optional.empty(),
                6.0D,
                8
        ));
    }

    @Test
    void differentTypeOrBabyAdultCandidateIsRejected() {
        assertFalse(ParentReunionFinder.compatibleCandidate(
                PIG,
                true,
                Optional.empty(),
                COW,
                false,
                Optional.empty(),
                3.0D,
                8
        ));
        assertFalse(ParentReunionFinder.compatibleCandidate(
                PIG,
                true,
                Optional.empty(),
                PIG,
                true,
                Optional.empty(),
                3.0D,
                8
        ));
    }

    @Test
    void candidateOutsideRadiusIsRejected() {
        assertTrue(ParentReunionFinder.compatibleCandidate(
                PIG,
                true,
                Optional.empty(),
                PIG,
                false,
                Optional.empty(),
                8.0D,
                8
        ));
        assertFalse(ParentReunionFinder.compatibleCandidate(
                PIG,
                true,
                Optional.empty(),
                PIG,
                false,
                Optional.empty(),
                8.01D,
                8
        ));
    }

    @Test
    void tamedAnimalsRequireSameOwner() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000002");

        assertTrue(ParentReunionFinder.compatibleCandidate(
                WOLF,
                true,
                Optional.of(owner),
                WOLF,
                false,
                Optional.of(owner),
                2.0D,
                8
        ));
        assertFalse(ParentReunionFinder.compatibleCandidate(
                WOLF,
                true,
                Optional.of(owner),
                WOLF,
                false,
                Optional.of(other),
                2.0D,
                8
        ));
        assertFalse(ParentReunionFinder.compatibleCandidate(
                WOLF,
                true,
                Optional.of(owner),
                WOLF,
                false,
                Optional.empty(),
                2.0D,
                8
        ));
        assertFalse(ParentReunionFinder.compatibleCandidate(
                WOLF,
                true,
                Optional.empty(),
                WOLF,
                false,
                Optional.of(owner),
                2.0D,
                8
        ));
    }
}
