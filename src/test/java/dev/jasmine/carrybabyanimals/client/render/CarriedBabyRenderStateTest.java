package dev.jasmine.carrybabyanimals.client.render;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarriedBabyRenderStateTest {
    @AfterEach
    void clearRenderState() {
        CarriedBabyRenderState.clearAll();
    }

    @Test
    void setClearAndClearAllUpdateCarriedBabyMap() {
        CarriedBabyRenderState.set(10, 20);

        assertTrue(CarriedBabyRenderState.isCarriedBaby(10));
        assertTrue(CarriedBabyRenderState.isCarrier(20));
        assertFalse(CarriedBabyRenderState.isCarrier(21));
        assertEquals(20, CarriedBabyRenderState.carrierFor(10).orElseThrow());
        assertEquals(10, CarriedBabyRenderState.carriedBabyFor(20).orElseThrow());

        CarriedBabyRenderState.clear(10);
        assertFalse(CarriedBabyRenderState.isCarriedBaby(10));
        assertFalse(CarriedBabyRenderState.isCarrier(20));
        assertTrue(CarriedBabyRenderState.carriedBabyFor(20).isEmpty());

        CarriedBabyRenderState.set(11, 21);
        CarriedBabyRenderState.clearAll();
        assertFalse(CarriedBabyRenderState.isCarriedBaby(11));
    }

    @Test
    void replacingBabyOrCarrierKeepsLookupIndexesConsistent() {
        CarriedBabyRenderState.set(10, 20);
        CarriedBabyRenderState.set(11, 20);

        assertFalse(CarriedBabyRenderState.isCarriedBaby(10));
        assertTrue(CarriedBabyRenderState.isCarrier(20));
        assertEquals(11, CarriedBabyRenderState.carriedBabyFor(20).orElseThrow());

        CarriedBabyRenderState.set(11, 21);

        assertFalse(CarriedBabyRenderState.isCarrier(20));
        assertTrue(CarriedBabyRenderState.isCarrier(21));
        assertEquals(21, CarriedBabyRenderState.carrierFor(11).orElseThrow());
    }

    @Test
    void pruneMissingEntitiesClearsOnlyIncompletePairs() {
        CarriedBabyRenderState.set(10, 20);
        CarriedBabyRenderState.set(11, 21);

        CarriedBabyRenderState.pruneMissingEntities(entityId -> entityId == 10 || entityId == 20 || entityId == 21);

        assertTrue(CarriedBabyRenderState.isCarriedBaby(10));
        assertFalse(CarriedBabyRenderState.isCarriedBaby(11));
        assertTrue(CarriedBabyRenderState.isCarrier(20));
        assertFalse(CarriedBabyRenderState.isCarrier(21));
    }

    @Test
    void rememberingNewLevelClearsStaleCarryState() {
        Object firstLevel = new Object();
        Object secondLevel = new Object();
        CarriedBabyRenderState.rememberLevel(firstLevel);
        CarriedBabyRenderState.set(10, 20);

        CarriedBabyRenderState.rememberLevel(secondLevel);

        assertFalse(CarriedBabyRenderState.isCarriedBaby(10));
        assertFalse(CarriedBabyRenderState.isCarrier(20));
    }

    @Test
    void rememberingNewLevelKeepsPairsAvailableInThatLevel() {
        Object firstLevel = new Object();
        Object secondLevel = new Object();
        CarriedBabyRenderState.rememberLevel(firstLevel);
        CarriedBabyRenderState.set(10, 20);

        CarriedBabyRenderState.rememberLevel(secondLevel, entityId -> entityId == 10 || entityId == 20);

        assertTrue(CarriedBabyRenderState.isCarriedBaby(10));
        assertTrue(CarriedBabyRenderState.isCarrier(20));
    }

    @Test
    void rememberingSameLevelKeepsCurrentCarryState() {
        Object level = new Object();
        CarriedBabyRenderState.rememberLevel(level);
        CarriedBabyRenderState.set(10, 20);

        CarriedBabyRenderState.rememberLevel(level);

        assertTrue(CarriedBabyRenderState.isCarriedBaby(10));
        assertTrue(CarriedBabyRenderState.isCarrier(20));
    }

    @Test
    void clearAllResetsRememberedLevel() {
        Object oldLevel = new Object();
        Object nextLevel = new Object();
        CarriedBabyRenderState.rememberLevel(oldLevel);
        CarriedBabyRenderState.clearAll();
        CarriedBabyRenderState.set(10, 20);

        CarriedBabyRenderState.rememberLevel(nextLevel, entityId -> false);

        assertTrue(CarriedBabyRenderState.isCarriedBaby(10));
        assertTrue(CarriedBabyRenderState.isCarrier(20));
    }
}
