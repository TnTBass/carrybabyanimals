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
        assertEquals(20, CarriedBabyRenderState.carrierFor(10).orElseThrow());

        CarriedBabyRenderState.clear(10);
        assertFalse(CarriedBabyRenderState.isCarriedBaby(10));

        CarriedBabyRenderState.set(11, 21);
        CarriedBabyRenderState.clearAll();
        assertFalse(CarriedBabyRenderState.isCarriedBaby(11));
    }

    @Test
    void pruneMissingEntitiesClearsOnlyIncompletePairs() {
        CarriedBabyRenderState.set(10, 20);
        CarriedBabyRenderState.set(11, 21);

        CarriedBabyRenderState.pruneMissingEntities(entityId -> entityId == 10 || entityId == 20 || entityId == 21);

        assertTrue(CarriedBabyRenderState.isCarriedBaby(10));
        assertFalse(CarriedBabyRenderState.isCarriedBaby(11));
    }
}
